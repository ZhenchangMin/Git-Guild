# Git-Guild API 规范 - “排行榜与徽章”部分

## 1. 模块目标

排行榜与徽章展示模块用于把用户完成任务后产生的成长数据，以公开排行榜和个人徽章墙的形式展示出来。

本模块基于现有成长系统数据：

- `growth_profiles.total_xp`：用户累计 XP。
- `growth_profiles.level`：用户等级。
- `growth_profiles.completed_quest_count`：用户已完成任务数量。
- `contribution_records.completed_at`：用户首次或历次贡献完成时间。

当前阶段徽章采用规则计算方式，不额外引入徽章表；后续如需要支持自定义徽章、佩戴徽章、运营配置徽章，可再扩展持久化表。

## 2. 通用约定

- Base URL：`/api/v1`
- 请求与响应均使用 JSON。
- 响应统一使用项目已有 `ApiResponse` 包装。
- 时间字段使用 ISO-8601 格式。
- 需要认证的接口必须在请求头携带：

```http
Authorization: Bearer <accessToken>
```

统一成功响应结构：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "timestamp": "2026-06-02T20:30:00+08:00",
  "traceId": "req-xxxxxxxx"
}
```

统一失败响应结构：

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "data": null,
  "details": "limit must be between 1 and 100",
  "timestamp": "2026-06-02T20:30:00+08:00",
  "traceId": "req-xxxxxxxx"
}
```

## 3. XP 排行榜

### 3.1 查询 XP 排行榜

- Method：`GET`
- Path：`/api/v1/leaderboards/xp`
- 是否需要认证：否
- 说明：按累计 XP 查询用户排行榜，用于首页、探索页或个人中心侧边栏展示。

#### Query 参数

| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
| --- | --- | --- | --- | --- | --- |
| period | string | 否 | ALL_TIME | 当前仅支持 ALL_TIME | 排行榜统计周期 |
| limit | integer | 否 | 10 | 1-100 | 返回条数 |

#### 排序规则

1. `totalXp` 降序。
2. `completedQuestCount` 降序。
3. `userId` 升序。

#### 成功响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "period": "ALL_TIME",
    "generatedAt": "2026-06-02T20:30:00+08:00",
    "items": [
      {
        "rank": 1,
        "userId": 3001,
        "username": "alice",
        "level": 4,
        "totalXp": 320,
        "completedQuestCount": 5
      }
    ]
  },
  "timestamp": "2026-06-02T20:30:00+08:00",
  "traceId": "req-xxxxxxxx"
}
```

#### 错误响应

| HTTP 状态码 | errorCode | 触发条件 |
| --- | --- | --- |
| 400 | VALIDATION_FAILED | `period` 不是 `ALL_TIME` |
| 400 | VALIDATION_FAILED | `limit` 不在 1-100 范围内 |
| 500 | INTERNAL_ERROR | 服务端内部错误 |

## 4. 我的徽章

### 4.1 查询当前用户徽章展示

- Method：`GET`
- Path：`/api/v1/users/me/badges`
- 是否需要认证：是
- 说明：查询当前登录用户的徽章获得状态与进度。

#### 徽章规则

| badgeId | 名称 | 获得条件 | progress 取值 |
| --- | --- | --- | --- |
| FIRST_COMPLETION | 首次贡献 | 完成任务数量 >= 1 | `completedQuestCount` |
| XP_APPRENTICE | XP 学徒 | 累计 XP >= 100 | `totalXp` |
| QUEST_EXPLORER | 任务探索者 | 完成任务数量 >= 3 | `completedQuestCount` |
| LEVEL_RISER | 等级新星 | 用户等级 >= 3 | `level` |

#### 成功响应

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "items": [
      {
        "badgeId": "FIRST_COMPLETION",
        "name": "首次贡献",
        "description": "完成第一个任务后获得",
        "condition": "完成任务数量 >= 1",
        "earned": true,
        "earnedAt": "2026-06-02T18:12:00+08:00",
        "progress": 1,
        "target": 1
      },
      {
        "badgeId": "QUEST_EXPLORER",
        "name": "任务探索者",
        "description": "完成 3 个任务后获得",
        "condition": "完成任务数量 >= 3",
        "earned": false,
        "earnedAt": null,
        "progress": 1,
        "target": 3
      }
    ]
  },
  "timestamp": "2026-06-02T20:30:00+08:00",
  "traceId": "req-xxxxxxxx"
}
```

#### 字段说明

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| badgeId | string | 徽章唯一标识 |
| name | string | 徽章名称 |
| description | string | 展示文案 |
| condition | string | 获得条件 |
| earned | boolean | 是否已经获得 |
| earnedAt | string/null | 获得时间；规则徽章当前使用首次贡献完成时间，未获得时为 null |
| progress | integer | 当前进度 |
| target | integer | 达成目标 |

#### 错误响应

| HTTP 状态码 | errorCode | 触发条件 |
| --- | --- | --- |
| 401 | UNAUTHORIZED | 未登录或访问令牌无效 |
| 500 | INTERNAL_ERROR | 服务端内部错误 |

## 5. 工程约束

- 排行榜查询不暴露邮箱、角色、密码哈希等敏感字段。
- 徽章接口只能查询当前登录用户，不能通过传入 `userId` 查询他人徽章。
- 当前排行榜只统计已有成长档案的用户；从未完成任务的用户不会进入 XP 排行榜。
- 当前徽章不写数据库，避免在规则尚未稳定时引入额外迁移成本。
