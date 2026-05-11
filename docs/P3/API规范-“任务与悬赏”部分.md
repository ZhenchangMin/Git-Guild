# API 规范：任务与悬赏

## 1. 通用约定

基础路径：`/api/v1`

认证规则：需要登录或可选登录的接口通过 `Authorization` 请求头传递 JWT。

认证方式：

```http
Authorization: Bearer <JWT_TOKEN>
```

各接口是否必须携带该请求头，以接口内“请求头”表格为准。

统一成功响应：

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0001"
}
```

统一失败响应：

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "title 不能为空",
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0002"
}
```

## 2. 业务码/错误码

| 业务码       | HTTP 状态码    | 说明   |
| --------- | ----------- | ---- |
| `SUCCESS` | `200 / 201` | 请求成功 |

| 错误码                      | HTTP 状态码 | 说明            |
| ------------------------ | -------- | ------------- |
| `VALIDATION_FAILED`      | `400`    | 参数缺失或格式错误     |
| `UNAUTHORIZED`           | `401`    | 未登录或令牌无效      |
| `FORBIDDEN`              | `403`    | 当前用户无权限       |
| `QUEST_NOT_FOUND`        | `404`    | 任务不存在         |
| `REPOSITORY_NOT_FOUND`   | `404`    | 仓库不存在         |
| `ISSUE_NOT_FOUND`        | `404`    | Issue 不存在     |
| `ISSUE_NOT_AVAILABLE`    | `409`    | Issue 不可发布为任务 |
| `QUEST_NOT_ACCEPTABLE`   | `409`    | 任务当前状态不可接取    |
| `QUEST_ALREADY_ASSIGNED` | `409`    | 任务已被其他用户接取    |
| `DUPLICATE_ASSIGNMENT`   | `409`    | 当前用户已接取该任务    |
| `INTERNAL_ERROR`         | `500`    | 服务器内部错误       |

## 3. 发布任务

接口：`POST /api/v1/quests`

功能：委托人基于仓库 Issue 创建悬赏任务。创建后任务进入 `PENDING_ADMIN_REVIEW`，等待管理员审核上架。

权限：登录用户，角色为 `MAINTAINER` 或 `ADMIN`。

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `Authorization` | `String` | 是 | 登录令牌，格式为 `Bearer <JWT_TOKEN>` |

请求体：

| 参数名                  | 类型              | 必填  | 说明                 |
| -------------------- | --------------- | --- | ------------------ |
| `repositoryId`       | `Long`          | 是   | 关联仓库 ID            |
| `issueId`            | `Long`          | 是   | 关联 Issue ID        |
| `title`              | `String`        | 是   | 任务标题               |
| `description`        | `String`        | 是   | 任务背景和需求说明          |
| `completionCriteria` | `String`        | 是   | 完成标准               |
| `difficulty`         | `String`        | 是   | 难度：`A`、`B`、`C`、`D` |
| `techStack`          | `Array<String>` | 是   | 技术栈                |
| `estimatedHours`     | `Integer`       | 是   | 预计完成小时数            |
| `rewardXp`           | `Integer`       | 是   | 奖励 XP              |
| `categoryId`         | `Long`          | 是   | 任务分类 ID            |
| `tagIds`             | `Array<Long>`   | 否   | 标签 ID 列表，可用于标记“新手友好”等任务特征 |

请求示例：

```http
POST /api/v1/quests
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：

```json
{
  "repositoryId": 1001,
  "issueId": 3001,
  "title": "实现 Issue 同步状态页面",
  "description": "为仓库管理页补充 Issue 同步状态展示。",
  "completionCriteria": "展示最近同步时间、同步状态和失败原因。",
  "difficulty": "C",
  "techStack": ["Vue", "Spring Boot"],
  "estimatedHours": 6,
  "rewardXp": 180,
  "categoryId": 2,
  "tagIds": [5, 8]
}
```

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务已创建，等待管理员审核",
  "data": {
    "questId": 5001,
    "title": "实现 Issue 同步状态页面",
    "status": "PENDING_ADMIN_REVIEW",
    "repositoryId": 1001,
    "issueId": 3001,
    "difficulty": "C",
    "rewardXp": 180,
    "createdAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0003"
}
```

### 失败响应

HTTP 状态码：`409 Conflict`

错误码：`ISSUE_NOT_AVAILABLE`

```json
{
  "code": "ISSUE_NOT_AVAILABLE",
  "message": "该 Issue 当前不可发布为任务",
  "details": "Issue 已关闭或已关联未完成任务",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0004"
}
```

## 4. 浏览任务列表

接口：`GET /api/v1/quests`

功能：浏览任务列表，支持分类、标签、难度、技术栈、状态等筛选。

权限：公开访问。登录后可额外返回当前用户的接取状态。

字段说明：

| 字段名                  | 说明                                                   |
| -------------------- | ---------------------------------------------------- |
| `descriptionPreview` | 任务描述摘要，由后端根据 `description` 自动截断生成，用于任务列表展示，不由委托人单独填写 |
| `syncStatus`         | 关联仓库的同步状态，由后端在导入或同步仓库时维护，前端只读取展示                     |

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `Authorization` | `String` | 否 | 登录令牌。未登录可访问；登录后可返回当前用户的接取状态 |

Query 参数：

| 参数名                | 类型        | 必填  | 说明                                           |
| ------------------ | --------- | --- | -------------------------------------------- |
| `keyword`          | `String`  | 否   | 搜索关键词，采用模糊匹配                                 |
| `categoryId`       | `Long`    | 否   | 分类 ID                                        |
| `tagIds`           | `String`  | 否   | 标签 ID，多个值用逗号分隔；多个标签之间为全部匹配 |
| `difficulty`       | `String`  | 否   | 难度：`A`、`B`、`C`、`D`                           |
| `techStack`        | `String`  | 否   | 技术栈筛选，采用精确匹配；多个值用逗号分隔，多个技术栈之间为任一匹配 |
| `status`           | `String`  | 否   | 任务状态，默认 `PUBLISHED`                          |
| `sortBy`           | `String`  | 否   | 排序字段：`createdAt`、`rewardXp`、`estimatedHours` |
| `sortOrder`        | `String`  | 否   | 排序方向：`asc`、`desc`，默认 `desc`                  |
| `page`             | `Integer` | 否   | 页码，从 `1` 开始，默认 `1`                           |
| `size`             | `Integer` | 否   | 每页数量，默认 `4`，最大 `50`                          |

备注：`keyword`、`categoryId`、`tagIds`、`difficulty`、`techStack`、`status` 等筛选条件同时存在时，结果必须同时满足所有条件。

请求示例：

```http
GET /api/v1/quests?difficulty=C&techStack=Vue&tagIds=5&page=1&size=4
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "items": [
      {
        "questId": 5001,
        "title": "实现 Issue 同步状态页面",
        "descriptionPreview": "为仓库管理页补充 Issue 同步状态展示。",
        "difficulty": "C",
        "techStack": ["Vue", "Spring Boot"],
        "rewardXp": 180,
        "estimatedHours": 6,
        "status": "PUBLISHED",
        "category": {
          "categoryId": 2,
          "name": "前端开发"
        },
        "tags": [
          {
            "tagId": 5,
            "name": "新手友好",
            "color": "#67C23A"
          }
        ],
        "repository": {
          "repositoryId": 1001,
          "name": "git-guild"
        },
        "createdAt": "2026-05-10T20:00:00+08:00"
      }
    ],
    "page": 1,
    "size": 4,
    "totalItems": 1,
    "totalPages": 1
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0005"
}
```

### 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "difficulty 只能为 A、B、C、D",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0006"
}
```

## 5. 查看任务详情

接口：`GET /api/v1/quests/{questId}`

功能：查看任务详情，包括任务说明、完成标准、关联仓库、关联 Issue 和接取状态。

权限：公开访问。未上架任务仅发布者或管理员可查看。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                            |
| --------------- | -------- | --- | ----------------------------- |
| `Authorization` | `String` | 否   | 登录令牌。查看已上架任务时可省略；查看未上架任务时必须提供 |

Path 参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questId` | `Long` | 是 | 任务 ID |

请求示例：

```http
GET /api/v1/quests/5001
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "questId": 5001,
    "title": "实现 Issue 同步状态页面",
    "description": "为仓库管理页补充 Issue 同步状态展示。",
    "completionCriteria": "展示最近同步时间、同步状态和失败原因。",
    "difficulty": "C",
    "techStack": ["Vue", "Spring Boot"],
    "estimatedHours": 6,
    "rewardXp": 180,
    "status": "PUBLISHED",
    "publisher": {
      "userId": 2001,
      "username": "maintainer01"
    },
    "repository": {
      "repositoryId": 1001,
      "name": "git-guild",
      "syncStatus": "SYNCED"
    },
    "issue": {
      "issueId": 3001,
      "externalIssueId": "42",
      "title": "Issue 同步状态不可见",
      "status": "OPEN"
    },
    "category": {
      "categoryId": 2,
      "name": "前端开发"
    },
    "tags": [
      {
        "tagId": 5,
        "name": "新手友好",
        "color": "#67C23A"
      }
    ],
    "assignment": {
      "assigned": false,
      "assignee": null,
      "acceptedAt": null
    },
    "createdAt": "2026-05-10T20:00:00+08:00",
    "updatedAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0007"
}
```

### 失败响应

HTTP 状态码：`404 Not Found`

错误码：`QUEST_NOT_FOUND`

```json
{
  "code": "QUEST_NOT_FOUND",
  "message": "任务不存在",
  "details": "questId=5001",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0008"
}
```

## 6. 接取任务

接口：`POST /api/v1/quests/{questId}/assignments`

功能：登录用户接取已发布且未被占用的任务。成功后任务状态从 `PUBLISHED` 变为 `IN_PROGRESS`。

权限：登录用户。冒险家和项目维护者均可接取任务；管理员默认不参与普通任务接取。

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `Authorization` | `String` | 是 | 登录令牌，格式为 `Bearer <JWT_TOKEN>`；接取人由后端根据该令牌确定，前端不传 `userId` |

Path 参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questId` | `Long` | 是 | 任务 ID |

请求示例：

```http
POST /api/v1/quests/5001/assignments
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务接取成功",
  "data": {
    "assignmentId": 7001,
    "questId": 5001,
    "assignee": {
      "userId": 3001,
      "username": "adventurer01"
    },
    "questStatus": "IN_PROGRESS",
    "assignmentStatus": "ACTIVE",
    "acceptedAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0009"
}
```

### 失败响应

HTTP 状态码：`409 Conflict`

错误码：`QUEST_ALREADY_ASSIGNED`

```json
{
  "code": "QUEST_ALREADY_ASSIGNED",
  "message": "任务已被其他用户接取",
  "details": "currentAssignee=adventurer02",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0010"
}
```

HTTP 状态码：`409 Conflict`

错误码：`QUEST_NOT_ACCEPTABLE`

```json
{
  "code": "QUEST_NOT_ACCEPTABLE",
  "message": "任务当前状态不可接取",
  "details": "currentStatus=IN_REVIEW",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0011"
}
```
