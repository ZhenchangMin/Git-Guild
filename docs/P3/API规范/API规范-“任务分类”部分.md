# API 规范：任务分类与标签

## 1. 通用约定

基础路径：`/api/v1`

认证规则：需要登录或可选登录的接口通过 `Authorization` 请求头传递 JWT。

认证方式：

```http
Authorization: Bearer <JWT_TOKEN>
```

各接口是否必须携带该请求头，以接口内"请求头"表格为准。

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
  "details": "name 不能为空",
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0002"
}
```

## 2. 业务码/错误码

| 业务码       | HTTP 状态码    | 说明   |
| --------- | ----------- | ---- |
| `SUCCESS` | `200 / 201` | 请求成功 |

| 错误码                        | HTTP 状态码 | 说明                  |
| -------------------------- | -------- | ------------------- |
| `VALIDATION_FAILED`        | `400`    | 参数缺失或格式错误           |
| `UNAUTHORIZED`             | `401`    | 未登录或令牌无效            |
| `FORBIDDEN`                | `403`    | 当前用户无权限             |
| `CATEGORY_NOT_FOUND`       | `404`    | 任务分类不存在             |
| `TAG_NOT_FOUND`            | `404`    | 任务标签不存在             |
| `CATEGORY_NAME_DUPLICATED` | `409`    | 同名分类已存在             |
| `TAG_NAME_DUPLICATED`      | `409`    | 同名标签已存在             |
| `CATEGORY_IN_USE`          | `409`    | 分类被任务引用，不能禁用或删除     |
| `TAG_IN_USE`               | `409`    | 标签被任务引用，不能禁用或删除     |
| `CATEGORY_ALREADY_DISABLED`| `409`    | 分类已处于禁用状态           |
| `TAG_ALREADY_DISABLED`     | `409`    | 标签已处于禁用状态           |
| `INTERNAL_ERROR`           | `500`    | 服务器内部错误             |

## 3. 浏览任务分类

接口：`GET /api/v1/quest-categories`

功能：列出所有任务分类，供任务发布、筛选、推荐和管理后台使用。

权限：公开访问。已登录的管理员可看到被禁用的分类。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                                  |
| --------------- | -------- | --- | ----------------------------------- |
| `Authorization` | `String` | 否   | 登录令牌。未登录可访问；管理员登录后可查询被禁用分类 |

Query 参数：

| 参数名              | 类型        | 必填  | 说明                                |
| ---------------- | --------- | --- | --------------------------------- |
| `keyword`        | `String`  | 否   | 分类名称关键词模糊匹配                       |
| `enabled`        | `Boolean` | 否   | 是否只看启用的分类，默认 `true`，仅管理员可设为 `false` |
| `withQuestCount` | `Boolean` | 否   | 是否在响应中带回每个分类下的任务数量，默认 `false`     |
| `sortBy`         | `String`  | 否   | 排序字段，支持 `questCount`，默认按 `categoryId` 升序 |
| `sortOrder`      | `String`  | 否   | 排序方向，`asc` 或 `desc`，默认 `asc`            |

请求示例（未登录）：

```http
GET /api/v1/quest-categories?withQuestCount=true&sortBy=questCount&sortOrder=desc
```

请求示例（管理员登录）：

```http
GET /api/v1/quest-categories?withQuestCount=true&sortBy=questCount&sortOrder=desc
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
        "categoryId": 1,
        "name": "后端开发",
        "description": "围绕服务端、API 与数据存储的任务",
        "enabled": true,
        "questCount": 12
      },
      {
        "categoryId": 2,
        "name": "前端开发",
        "description": "围绕 Web 界面与交互的任务",
        "enabled": true,
        "questCount": 7
      }
    ]
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0101"
}
```

### 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "enabled 必须为布尔值",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0102"
}
```

## 4. 创建任务分类

接口：`POST /api/v1/quest-categories`

功能：管理员新增一个任务分类，用于后续任务发布时引用。

权限：登录用户，角色必须为 `ADMIN`。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                                |
| --------------- | -------- | --- | --------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`     |

请求体：

| 参数名           | 类型       | 必填  | 说明                              |
| ------------- | -------- | --- | ------------------------------- |
| `name`        | `String` | 是   | 分类名称，长度 1-32，全平台唯一              |
| `description` | `String` | 否   | 分类说明，长度 0-200                   |
| `enabled`     | `Boolean`| 否   | 该分类是否启用，默认 `true`                  |

请求示例：

```http
POST /api/v1/quest-categories
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：

```json
{
  "name": "测试与质量",
  "description": "围绕单元测试、集成测试和质量保障的任务",
  "enabled": true
}
```

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务分类已创建",
  "data": {
    "categoryId": 9,
    "name": "测试与质量",
    "description": "围绕单元测试、集成测试和质量保障的任务",
    "enabled": true,
    "createdAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0103"
}
```

### 失败响应

HTTP 状态码：`409 Conflict`

错误码：`CATEGORY_NAME_DUPLICATED`

```json
{
  "code": "CATEGORY_NAME_DUPLICATED",
  "message": "同名分类已存在",
  "details": "name=测试与质量",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0104"
}
```

## 5. 更新任务分类

接口：`PATCH /api/v1/quest-categories/{categoryId}`

功能：管理员修改分类的名称、描述或启用状态。

权限：登录用户，角色必须为 `ADMIN`。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                              |
| --------------- | -------- | --- | ------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`   |

Path 参数：

| 参数名          | 类型     | 必填  | 说明        |
| ------------ | ------ | --- | --------- |
| `categoryId` | `Long` | 是   | 任务分类 ID   |

请求体：以下字段均为可选，未传字段保持原值；至少需要传一个字段。

| 参数名           | 类型        | 必填  | 说明                                  |
| ------------- | --------- | --- | ----------------------------------- |
| `name`        | `String`  | 否   | 新分类名称，长度 1-32，全平台唯一                 |
| `description` | `String`  | 否   | 新分类说明，长度 0-200                      |
| `enabled`     | `Boolean` | 否   | 是否启用；从 `true` 改为 `false` 时禁用，反之重新启用；被引用分类仍允许改名 |

请求示例：

```http
PATCH /api/v1/quest-categories/9
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例:

```json
{
  "description": "围绕单元测试、集成测试、E2E 测试和质量保障的任务"
}
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务分类已更新",
  "data": {
    "categoryId": 9,
    "name": "测试与质量",
    "description": "围绕单元测试、集成测试、E2E 测试和质量保障的任务",
    "enabled": true,
    "updatedAt": "2026-05-10T20:10:00+08:00"
  },
  "timestamp": "2026-05-10T20:10:00+08:00",
  "traceId": "req-0105"
}
```

### 失败响应

HTTP 状态码：`409 Conflict`

错误码：`CATEGORY_IN_USE`（仅在尝试禁用时触发）

```json
{
  "code": "CATEGORY_IN_USE",
  "message": "分类被任务引用，无法禁用",
  "details": "categoryId=9, referencingQuestCount=3",
  "timestamp": "2026-05-10T20:10:00+08:00",
  "traceId": "req-0106"
}
```

## 6. 浏览任务标签

接口：`GET /api/v1/quest-tags`

功能：列出所有任务标签，供任务发布、筛选和推荐使用。

权限：公开访问。已登录的管理员可看到禁用的标签。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                                  |
| --------------- | -------- | --- | ----------------------------------- |
| `Authorization` | `String` | 否   | 登录令牌。未登录可访问；管理员登录后可查询禁用标签 |

Query 参数：

| 参数名              | 类型        | 必填  | 说明                                       |
| ---------------- | --------- | --- | ---------------------------------------- |
| `keyword`        | `String`  | 否   | 标签名称关键词模糊匹配                              |
| `enabled`        | `Boolean` | 否   | 是否只看启用的标签，默认 `true`，仅管理员可设为 `false`        |
| `withQuestCount` | `Boolean` | 否   | 是否在响应中带回每个标签下的任务数量，默认 `false`            |
| `page`           | `Integer` | 否   | 页码，从 `1` 开始，默认 `1`                       |
| `size`           | `Integer` | 否   | 每页数量，默认 `20`，最大 `100`                    |

请求示例：

```http
GET /api/v1/quest-tags?keyword=新手&page=1&size=20
```
请求示例（管理员登录）：

```http
GET /api/v1/quest-tags?keyword=新手&page=1&size=20
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
        "tagId": 5,
        "name": "新手友好",
        "color": "#22c55e",
        "enabled": true,
        "questCount": 16
      },
      {
        "tagId": 8,
        "name": "首次贡献",
        "color": "#3b82f6",
        "enabled": true,
        "questCount": 4
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 2,
    "totalPages": 1
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0107"
}
```

### 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "size 不能超过 100",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0108"
}
```

## 7. 创建任务标签

接口：`POST /api/v1/quest-tags`

功能：管理员新增一个任务标签，用于后续任务发布时引用。

权限：登录用户，角色必须为 `ADMIN`。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                              |
| --------------- | -------- | --- | ------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`   |

请求体：

| 参数名       | 类型        | 必填  | 说明                                |
| --------- | --------- | --- | --------------------------------- |
| `name`    | `String`  | 是   | 标签名称，长度 1-16，全平台唯一                |
| `color`   | `String`  | 否   | 标签展示颜色，6 位十六进制，如 `#22c55e`，默认 `#64748b` |
| `enabled` | `Boolean` | 否   | 是否启用，默认 `true`                    |

请求示例：

```http
POST /api/v1/quest-tags
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：

```json
{
  "name": "文档",
  "color": "#0ea5e1",
  "enabled": true
}
```

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务标签已创建",
  "data": {
    "tagId": 12,
    "name": "文档",
    "color": "#0ea5e1",
    "enabled": true,
    "createdAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0109"
}
```

### 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "color 必须为以 # 开头的 6 位十六进制颜色",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0110"
}
```

## 8. 更新任务标签

接口：`PATCH /api/v1/quest-tags/{tagId}`

功能：管理员修改标签的名称、颜色或启用状态。

权限：登录用户，角色必须为 `ADMIN`。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                              |
| --------------- | -------- | --- | ------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`   |

Path 参数：

| 参数名      | 类型     | 必填  | 说明      |
| -------- | ------ | --- | ------- |
| `tagId`  | `Long` | 是   | 标签 ID   |

请求体：以下字段均为可选，未传字段保持原值；至少需要传一个字段。

| 参数名       | 类型        | 必填  | 说明                                  |
| --------- | --------- | --- | ----------------------------------- |
| `name`    | `String`  | 否   | 新标签名称，长度 1-16，全平台唯一                 |
| `color`   | `String`  | 否   | 新标签颜色，6 位十六进制                       |
| `enabled` | `Boolean` | 否   | 是否启用；从 `true` 改为 `false` 时禁用，反之重新启用 |

请求示例：

```http
PATCH /api/v1/quest-tags/12
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：

```json
{
  "name": "技术文档",
  "color": "#0284c7"
}
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务标签已更新",
  "data": {
    "tagId": 12,
    "name": "技术文档",
    "color": "#0284c7",
    "enabled": true,
    "updatedAt": "2026-05-10T20:10:00+08:00"
  },
  "timestamp": "2026-05-10T20:10:00+08:00",
  "traceId": "req-0111"
}
```

### 失败响应

HTTP 状态码：`404 Not Found`

错误码：`TAG_NOT_FOUND`

```json
{
  "code": "TAG_NOT_FOUND",
  "message": "任务标签不存在",
  "details": "tagId=12",
  "timestamp": "2026-05-10T20:10:00+08:00",
  "traceId": "req-0112"
}
```
