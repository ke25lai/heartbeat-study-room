# 心跳 · 在线自习室

> 一个面向面试的在线自习室项目：用**原生 Servlet + Tomcat WebSocket** 吃透底层原理，叠加 **Java 21 虚拟线程**、**Redis ZSet 排行榜**、**Docker Compose** 三个深度技术点。每个技术都按「落地改动 + 原理 + 追问预案」准备（见 [技术设计文档](./技术设计文档.md) 第 18、19 章）。

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)
![Vue](https://img.shields.io/badge/Vue-3-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

多人共处一室、实时看见彼此状态、共同计时、完成后打卡，统计专注时长并排名。核心体验是「**陪伴感 + 可见的专注积累**」。

## ⚡ 快速体验（零配置，推荐面试官/ reviewer）

不想装 MySQL / Redis？用内置的 **H2 内存库** 一键启动，开箱即有演示数据（排行榜、房间、趋势图都非空）：

1. 确认已安装 **JDK 21** 和 **Node.js 18+**
2. 双击项目根目录的 **`start-demo.bat`**（Windows）
   - 自动构建后端 jar、安装前端依赖、启动两个服务并打开浏览器
   - 数据存在内存，**每次重启自动重建**，无需任何数据库配置
3. 用演示账号登录（密码统一 `demo123`）：

   | 账号 | 密码 | 角色 |
   |------|------|------|
   | `demo_zhang` / `demo_alin` / `demo_ming` 等 6 个 | `demo123` | 普通用户（有专注记录/排行榜数据） |
   | `admin` | `admin123` | 管理员（可体验管理后台） |

> macOS / Linux 用户：在后端目录执行 `mvnw spring-boot:run -Dspring-boot.run.profiles=h2`，前端照常 `npm install && npm run dev`。
> H2 控制台：http://localhost:8090/h2-console （JDBC URL 填 `jdbc:h2:mem:heartbeat`）。

## 技术栈

| 层 | 技术 | 亮点 |
|----|------|------|
| 后端 | Java 21 + Spring Boot 3.3 + 原生 Servlet | 刻意不用 Spring MVC，讲清 Servlet 底层 |
| 实时 | Tomcat 原生 WebSocket（JSR-356） | HTTP 升级握手、服务端权威时钟；Endpoint 需 `@Component` 才会被 `ServerEndpointExporter` 扫描注册 |
| 并发 | ConcurrentHashMap + LongAdder | 房间实时态、细粒度锁 |
| 存储 | MySQL 8 / H2（本地） + JdbcTemplate | 直写 SQL，事务保证 |
| 新特性 | Java 21 虚拟线程 | `spring.threads.virtual.enabled=true` 一行开启 |
| 缓存 | Redis ZSet 排行榜 | 跳表原理、缓存三大问题；未启动 Redis 时自动降级到 DB 聚合 |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts | 移动端 H5 适配 |
| 工程化 | Docker 多阶段构建 + docker-compose | 一条命令起全栈 |

## 本地运行（完整开发模式，MySQL 持久化）

> 只想快速看效果请用上面的「快速体验」。`start-demo.bat` 用 H2 免配置；下面这套用 MySQL 持久化，数据可用 Navicat 管理。

环境要求：**JDK 21**、**Node.js 18+**、**MySQL 8**。

### 方式一：一键启动（Windows 推荐）

双击项目根目录的 **`start.bat`**，它会自动启动后端（8090）和前端（5173）两个窗口，并打开浏览器。首次运行会自动打包后端 jar。重复双击不会重复启动（有端口检测）。关闭那两个服务窗口即停止。

> 后端用 `java -jar` 启动而非 `spring-boot:run`：因为中文路径下 `spring-boot:run` fork 子进程时 classpath 编码会错乱导致找不到主类，jar 方式无此问题。改了后端代码后，重新双击前先手动打包一次：`cd backend && mvnw.cmd -q -DskipTests package`。

### 方式二：手动分别启动

```bash
# 后端（需 JDK 21，无需本地 Maven，脚本自动下载）
cd backend
./mvnw spring-boot:run

# 前端（另开终端）
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 ，注册账号即可使用。后端接口：http://localhost:8090 。

### 数据库配置

数据库密码**不写死在代码里**，通过环境变量提供（推荐）：

```bash
# Windows PowerShell
$env:DB_PASSWORD = "你的MySQL密码"
$env:DB_USER = "root"        # 可选，默认 root

# macOS / Linux
export DB_PASSWORD=你的MySQL密码
```

配置读取逻辑在 `backend/src/main/resources/application-mysql.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/heartbeat?...
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}   # 未设置环境变量时用示例默认值 root
```

- 数据库名 `heartbeat`，首次需**先建好空库**，应用启动会自动建表 + 导入班级种子数据：
  ```sql
  CREATE DATABASE IF NOT EXISTS heartbeat DEFAULT CHARACTER SET utf8mb4;
  ```
- **免安装方案**：把 `application.yml` 的 `spring.profiles.active` 改回 `h2`，使用内存库，重启后数据清空，无需 MySQL。
- Redis 用于排行榜缓存（本机 6379 未启动也能降级运行，走 DB 聚合）。

> 后端端口统一为 **8090**（`application.yml` 的 `server.port`）。若临时改用其它端口（如 8091），需同步把 `frontend/vite.config.ts` 中 `/api`、`/ws` 两处 `target` 的 `8090` 一起改掉，并重启 `npm run dev`。

### 管理员账号

应用启动时会自动创建默认管理员账号（幂等，已存在则重置）：

- 用户名：`admin`
- 密码：`admin123`
- 登录后顶部导航出现「管理后台」入口，可查看平台统计、封禁/解封用户、设置/撤销管理员、删除房间。

> ⚠️ **安全提示**：这是本地演示用的默认密码（初始化器每次启动会重置）。**部署到公网前务必修改**：可通过 `ADMIN_PASSWORD` 环境变量注入（见 `AdminInitializer`），或直接改库里的 `password_hash`。管理员权限由 `role=ADMIN` 判定，`/api/admin/*` 全部经 `AdminFilter` 拦截，非管理员返回 403。

## Docker 一键启动（全栈）

```bash
docker compose up --build
```

- 前端需先构建：`cd frontend && npm run build`，产物在 `dist/`。
- compose 内含 MySQL（root/root）、Redis、后端三个服务，环境变量在 `docker-compose.yml` 中配置，可按需修改。

## 核心功能

- 注册 / 登录（BCrypt 加盐 + Session）
- 创建 / 加入房间（密码、人数上限、匿名）
  - 匿名加入后昵称显示为「匿名者」、userId 不下发，用唯一 clientKey 区分多个匿名者，服务端权威判定（客户端无法伪造）
- 实时在线列表（学习中 / 休息中 / 空闲）
- 共同计时 + 番茄钟（专注/休息到点**自动双向循环**，配 Web Audio 合成的阶段提示音）
- 结束打卡（可选感想）+ 专注时长落库（服务端权威时钟，按专注段累计）
- 每日 / 每周排行榜（可按班级分组，Redis ZSet 缓存）
- 个人主页：打卡日记 + 时长趋势图
- 白噪音（雨声 / 咖啡馆 / 白噪音，**Web Audio 实时合成，无需 MP3 文件**，点击即响）
- 房主踢人 / 封禁（Filter 前置权限拦截，非房主返回 403）
- **管理后台**（管理员专属）：平台统计、用户管理（封禁/解封、升降级角色）、房间管理（删除违规房间）
- 移动端 H5 适配 + WebSocket 断线自动重连

## 目录结构

```
心跳/
├── 技术设计文档.md          # 完整设计（19 章，含实习技术演进与 RBAC 权限设计）
├── README.md
├── start.bat                # Windows 一键启动（MySQL 持久化模式）
├── start-demo.bat           # Windows 零配置体验（H2 内存库 + 演示数据）
├── docker-compose.yml       # MySQL + Redis + app 编排
├── backend/                 # Spring Boot 后端
│   ├── mvnw                 # Maven Wrapper（无需本地安装 Maven）
│   ├── Dockerfile           # 多阶段构建
│   └── src/main/java/com/heartbeat/
│       ├── servlet/         # 原生 HttpServlet（HTTP 入口）
│       ├── filter/          # Filter 链（编码/认证/房主/管理员权限）
│       ├── websocket/       # @ServerEndpoint 实时频道
│       ├── manager/         # 内存态（ConcurrentHashMap）
│       ├── service/         # 业务 + 事务边界
│       └── dao/             # JdbcTemplate 数据访问
└── frontend/                # Vue 3 前端
    └── src/
        ├── views/           # 登录/大厅/房间/排行/我的/管理后台
        ├── store/           # Pinia 状态
        ├── ws/              # WebSocket 连接与重连
        └── utils/           # Web Audio 提示音
```

## 面试讲解要点

见 `技术设计文档.md` 第 16 章（面试要点速查）、第 18 章（实习导向技术演进）与第 19 章（管理后台与 RBAC 权限设计）。核心叙事：

> 用原生 Servlet 和 Tomcat WebSocket 打底吃透底层，其上用 Java 21 虚拟线程升级并发模型、Redis ZSet 做排行榜、Docker 完成工程化交付——深度优先，每个技术都讲得清原理、扛得住追问。

## License

[MIT](./LICENSE)
