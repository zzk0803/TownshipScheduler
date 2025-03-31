# Township Scheduler

## 项目简介

Township 是playrix的模拟经营游戏。

Township Scheduler 是以此为课题的玩具项目，用于我自己学习Timefold和Vaadin的使用。
主要功能是依据给出的【订单】计算需要的【生产活动】，再通过Timefold计算进行这些【生产活动】日期时间和工厂。

## Township Scheduler 约束 

1. **forbidMismatchFactory**：硬性约束，避免将【生产活动】安排在不是对于的工厂上
2. **forbidBrokenQueueFactoryAbility**：硬性约束，避免【生产活动】超出工厂的最大生产队列容量
3. **forbidBrokenSlotFactoryAbility**：硬性约束，避免【生产活动】超出田地、农场的最大生产槽容量
4. **forbidBrokenPrerequisite**：硬性约束，避免【生产活动】违反先后顺序
5. **shouldEveryArrangementAssigned**：容忍约束，【生产活动】能排尽排
6. **shouldNotBrokenDeadlineOrder**：容忍约束，有时间限制的【生产活动】不得超时
7. **shouldNotArrangeInPlayerSleepTime**：容忍约束，不在晚上睡觉时安排【生产活动】
8. **preferArrangeAsSoonAsPassable**：软性约束，【生产活动】能尽早排就尽早排

## 技术栈

- **后端**：Spring Boot
- **前端**：Vaadin Platform
- **求解器**：Timefold
- **数据库**：H2 内存数据库（开发环境）
- **构建工具**：Maven
- **代码工具**：Lombok, Spotless（代码格式化）

## 功能模块

1. **资源调度**：通过 Timefold 实现智能调度算法，优化资源分配。
2. **订单管理**：支持订单的创建、修改、删除和查询。
3. **数据爬取**：从指定网站爬取数据并进行处理。
4. **可视化**：提供资源调度和订单管理的可视化界面。

## 运行步骤
1. **克隆项目**

2. **安装依赖**
    mvn clean install
3. **运行项目**
    mvn spring-boot:run

## 配置说明
- **数据库配置**：`src/main/resources/application.properties`
- **调度引擎配置**：`src/main/resources/timefold-township-config.xml`
