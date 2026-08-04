# Township Scheduler

> English version: [README_EN.md](README_EN.md)

## 项目简介

Township Scheduler 是一个以经典模拟经营游戏 Township 为背景的,基于 [Timefold](https://timefold.ai) （原 OptaPlanner）构建的智能调度求解器项目。本项目旨在我自己学习如何使用 Timefold解决我自己的问题，并结合 Vaadin 构建直观的可视化界面。

核心目标是：依据玩家给定的订单算出所需要安排的生产任务，并通过 `Timefold` 求解器智能地为这些活动分配最优的执行日期/时间和工厂，同时保证不违反设定的约束。

当前状态：这是一个正在进行中的学习与探索型项目。项目功能已基本可用，但仍在持续迭代优化中。

特别注意，本项目不是游戏，不是游戏外挂或作弊器，也不是什么生产级或是消费者级可用的APS系统或是排程软件。只是以游戏为背景作为引申题材，依托开源项目(`SpringBoot`+`Vaadin`+`Timefold`)所开发的玩具项目。

## 数据来源说明

本项目部分游戏数据（如产品配方、生产时间、工厂类型等）来源于公开的 [Goods|Township Fandom Wiki](https://township.fandom.com/wiki/Goods#All_Goods_List)。  
这些数据仅用于**个人学习和非商业研究目的**
数据仅在首次运行时爬取一次，用于初始化本地数据库。
或者可以通过下载的离线网页(mhtml)解析解析以完成数据准备。
没有这些数据，后面的功能根本无从谈起。

本项目**不隶属于 Playrix（Township 开发商）或 Fandom**，所有游戏相关内容版权归原作者所有。  
如有任何版权疑虑，请联系作者，我们将立即处理。

## 建模背景

Township 是一款模拟经营游戏，核心玩法是依据订单完成相应的生产链。例如：你看到火车上有一个订单，需要 6 个牛奶。而牛奶需要牛饲料，牛饲料需要小麦和玉米。所以你需要先生产小麦和玉米，完成后生产牛饲料，最后才生产牛奶。

关键领域特征：

- **订单**具有不同种类，不同种类的订单具有不同的奖励和限制（如时间窗口限制）。
- 订单包含若干**物品**及其数量。
- 物品具有**原材料结构（BOM）**，一些物品既是产品也作为原材料使用。
- 物品的生产依赖特定的**工厂**，生产需要固定时间。
- 一个工厂可以生产一系列物品。有的工厂能同时生产多个物品；大多数工厂具有**生产队列**，一次只能生产一个物品，完成后接着生产下一个。
- 工厂的生产队列任务数量有**容量限制**。
- 玩家一般每隔一段时间上线（如每隔 10 分钟、30 分钟、1 小时），每次上线需要尽可能安排多的任务，以保证完成游戏目标。
- 其他游戏内特性（如产品收割、仓库大小限制、工厂收割窗口、订单手动完成、加速工具、金币等）暂不考虑。


## 核心问题

### 难点：离散时间点下的链式时间建模

通常来说，要实现带前置依赖的链式时间模式，需要使用 `@PlanningListVariable` 配合 `@ShadowVariable`，综合考虑前置任务的结束时间来计算当前任务的开始时间和结束时间。

但在这个场景中，**一个过于具体的时间戳并没有意义**。取而代之的是**离散的时间点**——这些时间点具有相同的间隔（如建模背景所述）。玩家关心的是"每个时间点应该做哪些事情"，而不是"在 14:37:02 开始生产"。

求解器需要关心：每个时间点安排了什么、各自的生产时间和完成时间是什么，在不违反相关约束的同时要尽早尽快。

### 最初的尝试：`@PlanningListVariable` + `@ShadowVariable`

我首先尝试了标准方案：使用 `@PlanningListVariable` 管理任务顺序，配合 `@ShadowVariable` 计算链式时间。

**效果不好。**

核心问题在于：在我的模型中，"在规划列表中的位置"和"被分配的时间点"是两个独立的规划变量。列表顺序说"A 在 B 之前生产"，但如果 B 被分配的时间点比 A 更早，影子变量的计算结果在**业务逻辑上就是错误的**。

在 UI 上的具体表现为：
- 生产安排 A 在规划列表中排在 B **前面**
- 但 B 被分配的时间点比 A **更早**
- 导致 B 计算出的开始/结束时间反而在 A 之前，尽管它在列表中排在 A "后面"
- 影子变量的结果从业务角度来看是错误的

在规模较小时（物品少、BOM 层级浅），求解器有时能找到列表顺序与时间点顺序恰好一致的解，问题不明显甚至不存在。但随着 BOM 层级加深、安排数量增多，**这种不一致变得普遍且越来越难以通过约束来纠正**。

我花了相当长时间试图通过添加约束来强制列表顺序与时间点顺序一致，但感觉更像是在跟模型搏斗，而不是在建模问题本身。

### 最终的解决方案

将**时间点**视为 `@PlanningVariable`，链式时间计算单独实现：

1. 通过 [SchedulingPlayer](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingPlayer.java) 持有所有的 [SchedulingProducingArrangement](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingProducingArrangement.java)。
2. 在 `@ShadowVariable` 计算时，**先以时间点排序**，再按依赖顺序计算所有的生产时间和完成时间，以 Map 的形式保存。
3. 之后每个 `SchedulingProducingArrangement` 再通过自己的 `@ShadowVariable` 从 Map 中查询自己的生产时间和完成时间。

关键洞察：**在这个领域中，时间点顺序才是事实依据，而不是列表顺序。**

如果将来某一天 `@PlanningListVariable` 支持以另外一个 `@PlanningVariable` 为准安排顺序，这个 workaround 就不再需要了。


## 其他技术点
* 通过`jakarta.mail`解析mhtml。
* 通过`jsoup`完成网页结构的解析。
* 通过`commons-text`和`evo-inflector`处理英文单词，以便于BOM关系保存到JPA实体
* 通过`jgrapht`帮助保存BOM关系为带权有向图
* 实践了JPA的*EntityGraph*优化查询性能
* 实践了Vaadin的Signal实现
* Vaadin自定义组件以及Lit自定义组件
* 使用了`vis-timeline`实现了简易的甘特图

## Township Scheduler 约束 

1. **forbidBrokenFactoryAbility**：硬约束，避免【生产活动】超出【工厂】的队列容量限制
2. **forbidBrokenPrerequisiteArrangement**：硬约束，避免【生产活动】违反先后顺序
3. **shouldNotBrokenDeadlineOrder**：软约束，避免【生产活动】超过特定的违约时间
4. **shouldNotBrokenCalendarEnd**：软约束，避免【生产活动】超过work-calendar的时间
5. **preferNotArrangeInPlayerSleepTime**：软约束，【生产活动】不能在“玩家”睡觉时间排
6. **preferMinimizeOrderCompletedDateTime**：软约束，最小化订单完成时间
7. **preferArrangeDateTimeAsSoonAsPassible**：软约束，最好安排【生产活动】最早越好
8. **preferMinimizeProductArrangeDateTimeSlotUsage**：软约束，最好在一个[SchedulingDateTimeSlot.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingDateTimeSlot.java)里尽可能多的安排
9**preferLoadBalanceArrangementsInFactoryInstance**：软约束，在多实例工厂中实现负载均衡

## 技术栈

- **后端**：Spring Boot
- **前端**：Vaadin Platform
- **求解器**：Timefold
- **数据库**：H2 内存数据库
- **构建工具**：Maven

## 功能模块

1. **数据爬取**：从Township WiKi的页面爬取数据并处理存储，包括物品信息、工厂类型、物料清单、生产时长。
2. **订单管理**：订单的创建、删除和查询，为排程调度做准备。
3. **排程调度**：通过 Timefold 实现调度，优化资源分配，列出时间线-工厂-生产任务清单。

## 屏幕截图
![(1)orders_product_selection_view.png](readme/%281%29orders_product_selection_view.png)
![(2)scheduling_preparation_view.png](readme/%282%29scheduling_preparation_view.png)
![(3)scheduling_view_brief_article.png](readme/%283%29scheduling_view_brief_article.png)
![(4)scheduling_view_treegrid_article.png](readme/%284%29scheduling_view_treegrid_article.png)
![(5)scheduling_view_timeline_by_factory.png](readme/%285%29scheduling_view_timeline.png)
![(6)scheduling_view_report.png](readme/%286%29scheduling_view_report.png)

## 运行步骤
1. **克隆项目**
    git clone https://github.com/zzk0803/TownshipScheduler
2. **安装依赖**
    mvn clean install
3. **运行项目**
    mvn spring-boot:run
