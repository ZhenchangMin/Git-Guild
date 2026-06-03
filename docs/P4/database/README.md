# GitGuild 数据库初始化说明

## 1. 文件说明

本目录用于存放 GitGuild P4 阶段的 MySQL 数据库初始化脚本。

当前文件说明：

- `init.sql`：MySQL 数据库建表脚本，用于创建 GitGuild 项目所需的核心业务表。
- `seed-local-admin.sql`：本地开发演示用 ADMIN 账号种子数据。

## 2. 环境要求

- MySQL 8.0 或以上版本
- 字符集：`utf8mb4`
- 存储引擎：`InnoDB`

## 3. 初始化数据库

在项目根目录执行：

```bash
mysql -u root -p < docs/P4/database/init.sql
```

如需创建本地演示 ADMIN 账号，在完成建表后执行：

```bash
mysql -u root -p < docs/P4/database/seed-local-admin.sql
```

本地演示账号：

```text
邮箱：admin@gitguild.local
密码：admin123
角色：ADMIN
```
