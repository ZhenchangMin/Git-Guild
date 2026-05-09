# Backend — Git Guild

Spring Boot 3 单体应用，内部按领域模块组织，对外提供 REST API。

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| Java | 17（推荐 [Eclipse Temurin](https://adoptium.net/)） |
| Maven | 使用项目自带的 `mvnw`，无需单独安装 |
| Docker + Docker Compose | 用于启动 MySQL / Redis / Gitea |

> 检查版本：`java -version`

## 启动步骤

### 第一步：启动基础设施

在**项目根目录**执行（MySQL、Redis、Gitea 三个服务）：

```bash
docker compose up -d

# 确认三个容器均为 healthy 再继续
docker compose ps
```

### 第二步：配置环境变量（可选）

默认配置已预设本地开发参数，直接启动即可。如需自定义密码或配置邮件/Gitea：

```bash
# 在项目根目录
cp .env.example .env    # 按需修改 .env 中的值
```

### 第三步：启动 Spring Boot 应用

```bash
# 在 backend/ 目录下执行
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

启动成功后访问：
- **Swagger UI（API 文档）**：http://localhost:8080/swagger-ui.html
- **API JSON**：http://localhost:8080/api-docs

## 常用命令

| 命令 | 说明 |
|------|------|
| `./mvnw spring-boot:run` | 启动开发服务器 |
| `./mvnw clean package` | 编译 + 测试 + 打包 JAR |
| `./mvnw test` | 仅运行测试 |
| `./mvnw clean package -DskipTests` | 打包但跳过测试 |

## 模块结构

后端按 8 个业务模块组织，每个模块独立负责一块业务：

```
src/main/java/com/gitguild/backend/
├── BackendApplication.java       # Spring Boot 启动入口
│
├── common/                       # 通用层（优先搭好再写业务代码）
│                                 # 待实现：ApiResponse 统一响应体、GlobalExceptionHandler
│
├── user/                         # 用户与认证（P0）
│                                 # 待实现：User 实体、AuthService、JWT 工具类、SecurityConfig
│
├── codehost/                     # 代码托管底座适配（P0）
│                                 # 待实现：GiteaAdapter、GitHubImportAdapter、Webhook 接收器
│
├── quest/                        # 任务与悬赏（P0）
│                                 # 待实现：Quest 实体、QuestService、QuestController
│
├── recommendation/               # 推荐匹配（P0）
│                                 # 待实现：RecommendationService、推荐算法
│
├── review/                       # 提交与审核反馈（P0）
│                                 # 待实现：Submission、ReviewService、ReviewController
│
├── guide/                        # 新手引导与项目理解（P0）
│                                 # 待实现：QuestTemplate、ProjectGuide
│
├── notification/                 # 通知（P0）
│                                 # 待实现：Notification 实体、MailService、NotificationService
│
└── growth/                       # 成长激励（P1 阶段实现）
                                  # 待实现：GrowthRecord、XpService；徽章/排行榜为 P2
```

## 开发约定

### 模块内部分层

每个业务模块按以下结构组织：

```
{module}/
├── domain/       # JPA 实体类（@Entity）
├── repository/   # Spring Data JPA 接口
├── service/      # 业务逻辑（接口 + Impl 实现类）
├── controller/   # REST Controller，路径统一用 /api/{module}
└── dto/          # 请求和响应的数据传输对象
```

### 数据库

- 开发环境：`ddl-auto=update`，JPA 自动建表
- **生产部署前**切换为 `validate`，并用 Flyway 管理迁移脚本

### 配置文件

`application.yml` 包含 dev / prod 两个 profile，默认使用 dev。
所有密码、token 均从环境变量读取，不要硬编码到代码里。
