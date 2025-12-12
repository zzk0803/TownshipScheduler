# Township Scheduler

## 项目简介

Township Scheduler 是一个以经典模拟经营游戏 Township 为背景的,基于 [Timefold](https://timefold.ai) （原 OptaPlanner）构建的智能调度求解器项目。本项目旨在我自己学习如何使用 Timefold解决我自己的问题，并结合 Vaadin 构建直观的可视化界面。

核心目标是：依据玩家给定的订单算出所需要安排的生产任务，并通过 `Timefold` 求解器智能地为这些活动分配最优的执行日期/时间和工厂，同时保证不违反设定的约束。

当前状态：这是一个正在进行中的学习与探索型项目。项目功能已基本可用，但仍在持续迭代优化中。

特别注意，本项目不是游戏，不是游戏外挂或作弊器，也不是什么生产级或是消费者级可用的APS系统或是排程软件。只是以游戏为背景作为引申题材，依托开源项目(`SpringBoot`+`Vaadin`+`Timefold`)所开发的玩具项目。

## 数据来源说明

本项目部分游戏数据（如产品配方、生产时间、工厂类型等）来源于公开的 [Goods|Township Fandom Wiki](https://township.fandom.com/wiki/Goods#All_Goods_List)。  
这些数据仅用于**个人学习和非商业研究目的**
数据仅在首次运行时爬取一次，用于初始化本地数据库，项目的基本功能依赖于此。  

本项目**不隶属于 Playrix（Township 开发商）或 Fandom**，所有游戏相关内容版权归原作者所有。  
如有任何版权疑虑，请联系作者，我们将立即处理。

## 建模背景
* 订单具有不同的种类，不同的种类的订单具有不同的奖励和限制（比如时间窗口限制）。
* 订单包含若干物品及其物品数量。
* 物品具有原材料结构，一些物品既是产品也作为原材料使用。
* 物品的生产依赖特定的工厂，物品的生产需要时间。。
* 工厂可以生产一系列物品.
* 有的工厂能同时生产多个物品。大多数工厂具有生产队列，一次只能生产一个物品，生产完成后接着生产下一个。
* 工厂的生产队列任务数量有限制。
* 玩家们一般每隔一段时间上线（比如每隔5分钟、每隔1小时），(他/她)上线一次需要尽可能安排多的任务，以保证完成游戏目标
* 其他游戏内的特性，如库存限制，工厂收割窗口及其数量限制，订单的手动完成，加速工具，金币等暂不考虑。

## 技术亮点与难点

本项目运用 Timefold 的基本特性，通过`@ShadowVariable` 与自定义`VariableListener`动态维护的[SchedulingProducingArrangement.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingProducingArrangement.java)时间顺序与实际执行时段，模拟了ChainVariable或PlanningListVariable的顺序特性，实现了链式时间模型和多层排序（时间+id)，解决 Township 游戏调度问题：

* 混合工厂模型：系统同时处理两种工厂类型——队列型（如面包房、饲料厂，织布厂等任务按顺序执行）和槽位型（如田地、农场等）。 

* 动态时间窗约束：生产活动[SchedulingProducingArrangement.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingProducingArrangement.java)的执行时间不仅取决于工厂类型，还受到玩家自定义的工作日历和睡眠时间的限制。这使得时间变量的值域和约束计算变得动态且复杂。我的实现是使用 `@ShadowVariable` 和自定义的 `VariableListener` ([SchedulingProducingArrangementFactorySequenceVariableListener.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/utility/SchedulingProducingArrangementFactorySequenceVariableListener.java)) 委托`@PlanningEntity`[SchedulingFactoryInstance.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingFactoryInstance.java)维护TreeMap来动态计算每个生产活动的实际开始和结束时间并保持datetime及其生产队列的顺序。 

* 依赖关系：生产活动之间存在多层次的前置依赖（例如，生产面包需要先生产小麦和面粉），在求解器开始之前完成计算。 

* 多目标优化：系统在满足所有硬性约束的前提下，通过软约束进行多目标优化，包括最小化订单完成时间、尽早安排生产以及避免在玩家休息时间安排任务等。

## Township Scheduler 约束 

1. **forbidBrokenFactoryAbility**：硬性约束，避免【生产活动】超出【工厂】的队列容量限制
2. **forbidBrokenPrerequisiteArrangement**：硬性约束，避免【生产活动】违反先后顺序
3. **shouldNotBrokenDeadlineOrder**：容忍约束，避免【生产活动】超过特定的违约时间
4. **shouldNotBrokenCalendarEnd**：容忍约束，避免【生产活动】超过work-calendar的时间
5. **preferNotArrangeInPlayerSleepTime**：优化约束，【生产活动】不能在“玩家”睡觉时间排
6. **preferMinimizeOrderCompletedDateTime**：优化约束，最小化订单完成时间
7. **preferArrangeDateTimeAsSoonAsPassible**：优化约束，最好安排【生产活动】最早越好
8. **preferMinimizeProductArrangeDateTimeSlotUsage**：优化约束，最好在一个[SchedulingDateTimeSlot.java](src/main/java/zzk/townshipscheduler/backend/scheduling/model/SchedulingDateTimeSlot.java)里尽可能多的安排

## 技术栈

- **后端**：Spring Boot
- **前端**：Vaadin Platform
- **求解器**：Timefold
- **数据库**：H2 内存数据库（开发环境）
- **构建工具**：Maven

## 项目结构概览
src/main/java/zzk/townshipscheduler/
``` text
├── backend/               # 核心业务逻辑与数据持久层
│   ├── crawling/          # 从 Township Wiki 爬取并处理游戏数据
│   ├── dao/               # Spring Data JPA 仓库
│   ├── persistence/       # JPA 实体定义
│   └── service/           # 业务服务
├── scheduling/            # **Timefold 核心模块**
│   ├── model/             # 规划实体 (`@PlanningEntity`) 和解决方案 (`@PlanningSolution`)
│   ├── utility/           # 自定义 `VariableListener` 和比较器
│   └── score/             # **约束定义 (`ConstraintProvider`)**
└── ui/                    # Vaadin 前端视图与组件
```

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

## 运行步骤
1. **克隆项目**
    git clone https://github.com/zzk0803/TownshipScheduler
2. **安装依赖**
    mvn clean install
3. **运行项目**
    mvn spring-boot:run

## 配置说明
- **数据库配置**：`src/main/resources/application.properties`
- **调度引擎配置**：`src/main/resources/timefold-township-config.xml`
