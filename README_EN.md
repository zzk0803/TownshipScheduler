# Township Scheduler

> 中文版本: [README.md](README.md)

An intelligent scheduling solver project inspired by the classic simulation game *Township*, built on [Timefold](https://timefold.ai) (formerly OptaPlanner). The goal is to learn how to use Timefold to solve real-world scheduling problems, combined with Vaadin for intuitive visualization.

**Core objective:** Given a set of player orders, derive all required production tasks, then use the Timefold solver to intelligently assign the optimal date/time and factory for each task — without violating any configured constraints.

**Current status:** This is an ongoing learning and exploration project. Core functionality is working, but iterative improvements continue.

> ⚠️ **Disclaimer:** This project is not a game, not a game cheat/mod, and not a production-grade or consumer-grade APS/scheduling system. It is a personal learning project built on top of open-source frameworks (Spring Boot + Vaadin + Timefold), using the game as thematic inspiration.
## Data Source

Some game data (product recipes, production times, factory types, etc.) is sourced from the public [Township Fandom Wiki – Goods](https://township.fandom.com/wiki/Goods#All_Goods_List).

- Data is used solely for personal learning and non-commercial research.
- Data is scraped **once** on first run to initialize the local database, or parsed from a downloaded offline `.mhtml` file.
- Without this data, none of the downstream functionality would be possible.
- This project is not affiliated with Playrix (Township developer) or Fandom. All game-related content belongs to their respective copyright holders.
- If you have any copyright concerns, please contact the author and we will address it immediately.

## Modeling Background

Township is a simulation/management game. The core gameplay is fulfilling orders through production chains. For example: you see an order on the train requiring 6× Milk. Milk requires Cattle Feed, which requires Wheat and Corn. So you must produce Wheat and Corn first, then Cattle Feed, then Milk.

Key domain characteristics:

- Orders come in different types, each with different rewards and constraints (e.g., time-window limits).
- An order contains multiple items with quantities.
- Items have a raw-material structure (BOM); some items are both products and raw materials.
- Each item depends on a specific factory and requires a fixed production time.
- A factory can produce a set of items. Some factories can produce multiple items simultaneously; most have a production queue (one at a time, FIFO).
- Factory queue capacity is limited.
- Players typically log in at discrete intervals (every 10 min / 30 min / 1 hr) and want to queue as many tasks as possible per session to meet game objectives.
- Other in-game mechanics (harvesting, warehouse limits, harvest windows, manual order completion, speed-up tools, coins, etc.) are out of scope.

## Core Problem

### The challenge: chained-time modeling with discrete time slots

Typically, implementing a chained-time pattern with prerequisites requires `@PlanningListVariable` combined with `@ShadowVariable` — computing each task's start/end time based on its predecessor's completion.

However, in this scenario, **a precise timestamp is meaningless**. What matters is the **discrete time slot** (as described in the modeling background, these slots share uniform intervals). Players care about *what to do at each login tick*, not "start at 14:37:02."

The solver needs to determine, for each time slot:
- What tasks are arranged
- Their respective production and completion times
- That no constraints are violated while scheduling as early and densely as possible

### What I tried first: `@PlanningListVariable` + `@ShadowVariable`

I attempted the standard approach: using `@PlanningListVariable` for task ordering combined with `@ShadowVariable` for chained time computation.

**It did not work well for this scenario.**

The core issue: in my model, the "position in the planning list" and the "assigned time slot" are two independent planning variables. The list order says "produce A before B," but if B's assigned time slot is earlier than A's, the shadow variable computation produces **business-logically incorrect results**.

In my UI, this manifested as:
- Arrangement A appears **before** B in the planning list
- But B's assigned time slot is **earlier** than A's
- So B's computed start/end time ends up *before* A's, despite being "after" A in the list
- The shadow variable result is simply wrong from a business perspective

At small scale (few items, shallow BOM depth), the solver sometimes finds valid solutions where list order and time-slot order happen to align — the problem is barely noticeable or even absent. But as the BOM grows deeper and the number of arrangements increases, **the inconsistency becomes dominant and increasingly difficult to correct through constraints alone**.

I spent considerable effort trying to add constraints to force list-order consistency with time-slot order, but it felt like fighting the model rather than modeling the problem.

### The workaround I landed on

Treat the **time slot** as a `@PlanningVariable`, and implement chained-time computation separately:

1. A [`SchedulingPlayer`](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingPlayer.java) entity holds **all** [`SchedulingProducingArrangement`](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingProducingArrangement.java) planning entities.
2. When the `@ShadowVariable` listener fires, it **sorts all arrangements by their assigned time slot first**, then computes production/completion times in dependency order, storing results in a `Map`.
3. Each `SchedulingProducingArrangement` then queries its own start/end time from that Map via its own `@ShadowVariable`.

The key insight: **in this domain, time-slot order is the ground truth, not list order.**

If `@PlanningListVariable` ever supports ordering driven by a separate `@PlanningVariable`, this workaround would no longer be necessary.

## Other Technical Points

- `jakarta.mail` for parsing `.mhtml` files
- `jsoup` for web page structure parsing
- `commons-text` and `evo-inflector` for English word processing (BOM relationship persistence to JPA entities)
- `jgrapht` for BOM graph traversal and storage
- JPA `EntityGraph` for query performance optimization
- Vaadin Signals for reactive UI state
- Vaadin custom components and Lit-based web components
- `vis-timeline` for a simple Gantt-chart visualization

## Constraints

1. **forbidBrokenFactoryAbility**:Production arrangement must not exceed factory queue capacity
2. **forbidBrokenPrerequisiteArrangement**:Production arrangement must not violate prerequisite order
3. **shouldNotBrokenDeadlineOrder**:Avoid exceeding the order's deadline
4. **shouldNotBrokenCalendarEnd**:Avoid exceeding the work-calendar end time
5. **preferNotArrangeInPlayerSleepTime**:Avoid scheduling during player sleep hours
6. **preferMinimizeOrderCompletedDateTime**:Minimize order completion time
7. **preferArrangeDateTimeAsSoonAsPassible**:Schedule arrangements as early as possible
8. **preferMinimizeProductArrangeDateTimeSlotUsage**:Maximize task density per [`SchedulingDateTimeSlot`](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingDateTimeSlot.java)
   9**preferLoadBalanceArrangementsInFactoryInstance**:Load-balance arrangements across multi-instance factories

## Tech Stack

- **Backend**：Spring Boot
- **Frontend**：Vaadin Platform
- **Solver**：Timefold
- **Database**：H2 Database
- **Build**：Maven

## Features

- **Data Ingestion** – Scrape and process data from the Township Fandom Wiki (items, factory types, BOM relationships, production times), or parse an offline `.mhtml` file.
- **Order Management** – Create, delete, and query orders as scheduling input.
- **Scheduling & Visualization** – Run the Timefold solver, then display a timeline → factory → task breakdown with a Gantt chart.

## Screenshots
![(1)orders_product_selection_view.png](readme/%281%29orders_product_selection_view.png)
![(2)scheduling_preparation_view.png](readme/%282%29scheduling_preparation_view.png)
![(3)scheduling_view_brief_article.png](readme/%283%29scheduling_view_brief_article.png)
![(4)scheduling_view_treegrid_article.png](readme/%284%29scheduling_view_treegrid_article.png)
![(5)scheduling_view_timeline_by_factory.png](readme/%285%29scheduling_view_timeline.png)
![(6)scheduling_view_report.png](readme/%286%29scheduling_view_report.png)

## Getting Started

```bash
# Clone the repository
git clone https://github.com/zzk0803/TownshipScheduler
```

# Install dependencies
mvn clean install

# Run the project
mvn spring-boot:run
