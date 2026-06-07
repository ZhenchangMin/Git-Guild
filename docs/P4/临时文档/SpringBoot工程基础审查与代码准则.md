# Spring Boot 工程基础审查与代码准则

### Overall Assessment

当前后端工程已经具备 Maven、Spring Boot、JPA、Security、JWT、Redis、Mail、Swagger、MySQL 驱动和领域模块包名，方向与 P2/P3 的“模块化单体 + REST + JWT + Gitea 适配”一致。主要问题是工程仍停留在脚手架阶段：Maven Wrapper 不完整导致 README 中的启动命令不可用，启动类排除了数据源自动配置导致 JPA/MySQL 设计无法真正落地，安全与 JWT 层只有依赖没有实现，Controller/Service/DTO/异常处理等基础结构尚未形成。

### **Prioritized Recommendations**

1. 修复 Maven Wrapper，保证 `./mvnw test` 和 `./mvnw spring-boot:run` 可以按 README 运行。
2. 移除 `DataSourceAutoConfiguration` 排除项，让 JPA、MySQL 和 Repository 能按 P3 数据库设计正常启动。
3. 建立 `common`、`security`、`user`、`quest` 等基础包结构，先落地统一响应、异常处理、JWT 鉴权和认证上下文。
4. 补齐 Spring Security 配置、JWT 生成/解析、JWT 过滤器、认证入口和权限拒绝处理。
5. 将 Controller、Service、Repository、DTO、Domain 的职责边界写进工程规范并在每个模块一致执行。
6. 清理已跟踪的 `backend/target/` 构建产物，避免编译结果污染代码审查和提交历史。
7. 为后端增加 H2 测试 Profile 或 Testcontainers，避免测试依赖本地 MySQL、Redis、Gitea 状态。

### **Detailed Feedback**

---

**[BUILD]** - Maven Wrapper 不完整导致 README 启动命令不可用

**Original Code:**

```text
backend/mvnw
backend/mvnw.cmd
```

```text
./mvnw: 112: cannot open ./.mvn/wrapper/maven-wrapper.properties: No such file
cannot read distributionUrl property in ./.mvn/wrapper/maven-wrapper.properties
```

**Suggested Improvement:**

```bash
# 在 backend/ 目录重新生成 Maven Wrapper 文件
mvn -N wrapper:wrapper

# 应提交以下文件
backend/mvnw
backend/mvnw.cmd
backend/.mvn/wrapper/maven-wrapper.properties
backend/.mvn/wrapper/maven-wrapper.jar
```

**Rationale:**
README 明确要求使用项目自带的 `mvnw`，但当前缺少 `.mvn/wrapper/maven-wrapper.properties`，任何成员按文档执行都会失败。P4 进入开发阶段后，构建命令必须可复现，否则 CI/CD、测试和本地开发都会被阻塞。

---

**[CORRECTNESS]** - 启动类排除了数据源自动配置，破坏 JPA/MySQL 设计

**Original Code:**

```java
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

**Suggested Improvement:**

```java
@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

**Rationale:**
P2/P3 设计已经选择 MySQL + Spring Data JPA，`application.yml` 也配置了 `spring.datasource` 和 `spring.jpa`。排除 `DataSourceAutoConfiguration` 会让 Repository、Entity、事务和数据库连接无法按设计工作。测试不应通过禁用数据源来绕过问题，而应使用独立测试 Profile、H2 或 Testcontainers。

---

**[SECURITY]** - 已引入 Security/JWT 依赖，但缺少鉴权链路

**Original Code:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
```

**Suggested Improvement:**

```text
src/main/java/com/gitguild/backend/security/
├── SecurityConfig.java
├── JwtAuthenticationFilter.java
├── JwtTokenProvider.java
├── JwtAuthenticationEntryPoint.java
├── JwtAccessDeniedHandler.java
└── CurrentUser.java
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**Rationale:**
P3 API 规范要求需要登录的接口通过 `Authorization: Bearer <JWT_TOKEN>` 鉴权，接取人、审核人等身份必须从 JWT 解析。只有依赖没有过滤器、令牌服务和异常处理，会导致权限边界无法执行，Controller 也可能错误地信任前端传入的 `userId`。

---

**[MAINTAINABILITY]** - 模块只有 `package-info.java`，缺少可执行的基础分层结构

**Original Code:**

```text
src/main/java/com/gitguild/backend/
├── common/package-info.java
├── user/package-info.java
├── quest/package-info.java
├── codehost/package-info.java
├── recommendation/package-info.java
├── review/package-info.java
├── guide/package-info.java
├── notification/package-info.java
└── growth/package-info.java
```

**Suggested Improvement:**

```text
src/main/java/com/gitguild/backend/
├── common/
│   ├── api/
│   ├── exception/
│   └── validation/
├── security/
├── user/
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── quest/
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
└── codehost/
    ├── adapter/
    ├── controller/
    ├── service/
    └── dto/
```

**Rationale:**
P4 会并行开发多个模块，如果没有统一分层结构，Controller 很容易直接写业务规则或访问 Repository，Service 也容易混入 HTTP 细节和 JWT 解析逻辑。先建立基础包结构能降低合并冲突，并让后续代码审查有明确标准。

---

**[ERROR HANDLING]** - 缺少统一响应体和全局异常处理

**Original Code:**

```java
/** 通用层：统一响应体、全局异常处理、工具类。 */
package com.gitguild.backend.common;
```

**Suggested Improvement:**

```java
public record ApiResponse<T>(
    String code,
    String message,
    T data,
    OffsetDateTime timestamp,
    String traceId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "OK", data, OffsetDateTime.now(), null);
    }
}
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
            .status(ex.httpStatus())
            .body(new ApiResponse<>(ex.code(), ex.getMessage(), null, OffsetDateTime.now(), null));
    }
}
```

**Rationale:**
P3 API 规范要求成功和失败响应都包含 `code`、`message`、`timestamp`、`traceId`。如果每个 Controller 自己组装响应，会很快出现字段不一致、错误码不一致和异常泄漏问题。

---

**[SECURITY]** - 开发环境 JWT 默认密钥过弱，容易被误用于真实环境

**Original Code:**

```yaml
jwt:
  secret: ${JWT_SECRET:dev-secret-please-change-in-production-env}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

**Suggested Improvement:**

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

```java
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    @NotBlank
    @Size(min = 32)
    String secret,
    @Min(300000)
    long expirationMs
) {}
```

**Rationale:**
JWT 密钥是认证系统的核心秘密，不应依赖一个可预测的默认值。即使是开发环境，也应通过 `.env` 或本地环境变量显式声明，避免部署时误用默认密钥。

---

**[REPOSITORY HYGIENE]** - `backend/target/` 构建产物已被 Git 跟踪

**Original Code:**

```text
backend/target/classes/com/gitguild/backend/BackendApplication.class
backend/target/test-classes/com/gitguild/backend/BackendApplicationTests.class
backend/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst
```

**Suggested Improvement:**

```bash
git rm -r --cached backend/target
git status --short
```

**Rationale:**
`.gitignore` 已经声明忽略 `backend/target/`，但这些文件已经被跟踪。构建产物进入版本库会污染 diff、制造无意义冲突，并掩盖真实源码变化。

---

**[TESTABILITY]** - 测试只有空的上下文加载测试，无法验证工程基础约束

**Original Code:**

```java
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**Suggested Improvement:**

```java
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

```yaml
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:gitguild-test;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**Rationale:**
基础测试至少应验证 Spring 容器、数据源、JPA、安全配置和异常处理可以同时启动。空测试配合排除数据源只证明“绕过关键基础设施后能启动”，不能证明后端工程基础可用。

---

### Generated Code Rules

- Controller 只负责 HTTP 入参校验、认证上下文读取、调用 Service 和返回 DTO。
- Controller 禁止直接访问 Repository、EntityManager、Gitea 客户端、Redis 客户端或邮件客户端。
- Controller 禁止信任前端传入的 `userId`、`role`、`adminId`、`reviewerId` 等身份字段。
- Controller 的请求体必须使用 `*Request` DTO，并通过 `@Valid` 触发参数校验。
- Controller 的响应必须使用 `*Response` DTO，不直接返回 JPA Entity。
- Controller 路径统一使用 `/api/v1/{resource}`，不要混用 `/api/{module}` 和 `/api/v1/{resource}`。
- Service 负责业务规则、状态流转、权限语义校验、事务边界和领域对象编排。
- Service 可以依赖 Repository、外部适配器接口、事件发布器和其他模块的应用服务。
- Service 禁止依赖 `HttpServletRequest`、`HttpServletResponse`、`SecurityContextHolder` 或前端请求细节。
- Service 方法入参优先使用 command/query 对象，不直接接收 Controller 的 Request DTO。
- Service 必须通过 `@Transactional` 声明写操作事务边界，只读查询使用 `@Transactional(readOnly = true)`。
- Service 抛出业务异常必须使用统一 `BusinessException`，并携带业务错误码和 HTTP 状态语义。
- Repository 只负责持久化查询，不承载业务规则、鉴权规则或外部系统调用。
- Domain Entity 只表达持久化状态和少量领域判断，不处理 HTTP、JWT、邮件或 Gitea API。
- DTO 必须按接口场景拆分 Request、Response、Command、Query，不复用 Entity 作为传输对象。
- JWT 签发、解析、过期校验和签名校验只能放在 `security` 层。
- JWT Filter 只负责读取 Bearer Token、验证 Token、构造 Authentication 和写入 SecurityContext。
- JWT Filter 禁止查询业务数据、修改任务状态、创建用户或调用业务 Service 执行业务流程。
- 当前登录用户信息通过 `CurrentUser` 或认证主体传递给 Controller，再转换为 Service command。
- 权限规则分两层处理，路由级权限放在 SecurityConfig，业务对象级权限放在 Service。
- `AuthService` 负责注册、登录、密码校验和令牌签发，不负责具体 HTTP Cookie 或 Header 写入。
- `PasswordEncoder` 必须由 Security 配置统一注入，禁止手写散列算法或保存明文密码。
- JWT 密钥必须从环境变量注入，并在启动时校验长度，禁止使用生产可预测默认值。
- 所有异常必须由 `GlobalExceptionHandler` 转换为统一 `ApiResponse` 失败结构。
- 所有成功响应必须包含统一业务码、消息、数据、时间戳和请求追踪字段。
- P0 模块优先实现用户认证、任务、管理员审核、代码托管适配、提交审核、推荐和通知。
- 推荐匹配是 P0 核心模块，禁止在后端结构中把推荐实现降级为可选附加层。
- 代码托管模块必须依赖 `CodeHostAdapter` 抽象，业务模块禁止直接调用 Gitea 或 GitHub SDK。
- Webhook 接口必须验证签名、记录事件标识、保证幂等，并只发布内部事件。
- 数据库迁移进入 P4 后应优先使用迁移脚本，避免长期依赖 `ddl-auto=update`。
- 测试必须至少覆盖 Controller 鉴权、Service 状态流转、Repository 查询和 JWT 失败路径。
- 每个新增接口必须同步补充请求 DTO、响应 DTO、错误码、权限规则和测试用例。
