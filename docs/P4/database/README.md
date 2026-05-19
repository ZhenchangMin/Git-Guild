# GitGuild 数据库初始化说明

## 1. 文件说明

本目录用于存放 GitGuild P4 阶段的 MySQL 数据库初始化脚本。

当前文件说明：

- `init.sql`：MySQL 数据库建表脚本，用于创建 GitGuild 项目所需的核心业务表。

## 2. 环境要求

- MySQL 8.0 或以上版本
- 字符集：`utf8mb4`
- 存储引擎：`InnoDB`

## 3. 初始化数据库

在项目根目录执行：

```bash
mysql -u root -p < docs/P4/database/init.sql