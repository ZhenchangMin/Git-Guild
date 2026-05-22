# Frontend — Git Guild

Vue 3 + Vite 单页应用。

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| Node.js | 20 及以上 |
| npm | 随 Node.js 一起安装，无需单独安装 |

> 检查版本：`node -v` 和 `npm -v`

## 本地预览

当前项目处于开发阶段，暂未部署线上网站。前端页面需要在本地通过 npm 启动后预览。

### 开发预览（推荐）

适合日常开发和调试，代码保存后会自动热更新。

```bash
# 从仓库根目录进入前端目录
cd frontend

# 首次运行或依赖变化后安装依赖
npm install

# 启动 Vite 开发服务器
npm run dev
```

启动成功后，终端会显示本地访问地址，默认通常是：

```text
http://localhost:5173
```

### 构建后预览

适合在提交前检查生产构建结果。该方式会先生成 `dist/`，再用本地预览服务器查看构建后的静态资源。

```bash
cd frontend
npm install
npm run build
npm run preview
```

启动成功后，终端会显示预览地址，默认通常是：

```text
http://localhost:4173
```

如果端口被占用，可以手动指定端口：

```bash
npm run preview -- --host 127.0.0.1 --port 4174
```

## 安装项目所需 UI 依赖

当前脚手架只含基础 Vue 3，开发前请安装项目约定的依赖：

```bash
npm install element-plus @element-plus/icons-vue
npm install vue-router pinia axios
npm install -D unplugin-auto-import unplugin-vue-components eslint eslint-plugin-vue @eslint/js
```

> 安装完之后记得把 `package-lock.json` 一起提交，保证 CI 和队友使用相同版本。

## 常用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器（热更新） |
| `npm run build` | 构建生产包，输出到 `dist/` |
| `npm run preview` | 本地预览生产构建结果 |

## 目录结构（规划）

```
src/
├── api/          # Axios 实例封装，各模块 API 调用函数
├── router/       # Vue Router 路由表
├── stores/       # Pinia 状态管理（auth、quest 等）
├── views/        # 页面级组件，对应路由 component
├── components/   # 可复用子组件
├── assets/       # 静态资源（图片、图标）
├── App.vue       # 根组件
├── main.js       # 入口文件
└── style.css     # 全局样式
```
