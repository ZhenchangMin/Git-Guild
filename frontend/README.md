# Frontend — Git Guild

Vue 3 + Vite 单页应用。

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| Node.js | 20 及以上 |
| npm | 随 Node.js 一起安装，无需单独安装 |

> 检查版本：`node -v` 和 `npm -v`

## 启动步骤

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖（首次 clone 或 package.json 更新后执行）
npm install

# 3. 启动开发服务器
npm run dev
```

启动成功后，浏览器访问 **http://localhost:5173**

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

## IDE 推荐

- **VS Code** + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 插件（Vue 官方支持，需禁用 Vetur）
- **WebStorm** — 内置 Vue 支持，开箱即用
