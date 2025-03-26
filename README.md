# Township Scheduler

## 项目简介

Township Scheduler 是一个基于 Java 的乡镇资源调度系统，结合 Vaadin 前端框架与 Spring Boot
后端技术，提供资源调度优化、订单管理、数据爬取及可视化功能。系统采用 Timefold（OptaPlanner）进行智能调度算法实现。

## 技术栈

- **后端**：Spring Boot, Spring Data JPA, Spring Security
- **前端**：Vaadin Web Components
- **调度引擎**：Timefold（优化算法）
- **数据库**：H2 内存数据库（开发环境）
- **构建工具**：Maven
- **代码工具**：Lombok, Spotless（代码格式化）

## 功能模块

1. **资源调度**：通过 Timefold 实现智能调度算法，优化资源分配。
2. **订单管理**：支持订单的创建、修改、删除和查询。
3. **数据爬取**：从指定网站爬取数据并进行处理。
4. **可视化**：提供资源调度和订单管理的可视化界面。

## 项目结构

plaintext src/main/java/zzk/townshipscheduler ├── backend │ ├── crawling │ ├── dao │ ├── persistence │ ├── scheduling │
└── service ├── ui │ ├── components │ ├── eventbus │ ├── pojo │ └── views └── Application.java

## 运行步骤
1. **克隆项目**
   bash git clone https://your-repository-url.git
2. **安装依赖**
   bash mvn clean install
3. **运行项目**
   bash mvn spring-boot:run

## 配置说明
- **数据库配置**：`src/main/resources/application.properties`
- **调度引擎配置**：`src/main/resources/timefold-township-config.xml`

## 依赖管理
项目依赖管理通过 `pom.xml` 文件进行，主要依赖包括：
- Spring Boot
- Vaadin
- Timefold Solver
- Lombok
- H2 Database

## 代码格式化
项目使用 Spotless 进行代码格式化，配置文件位于 `eclipse-formatter.xml`。

## 贡献指南
1. Fork 项目并创建您的分支。
2. 提交您的更改并确保通过所有测试。
3. 提交 Pull Request，并描述您的更改。

## 许可证
本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。
