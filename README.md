# Learning Platform 微服务权限管理系统

基于 **Spring Boot 3 + Spring Cloud Netflix (Eureka) + Sa-Token + MyBatis-Flex + Vue 3 + TypeScript** 的前后端分离微服务权限管理系统。
提供 RBAC 权限模型、动态菜单路由、数据权限、多数据源切换、代码生成器、服务器 / Redis / MySQL / 服务注册监控等能力。

---

## 一、技术栈

### 后端

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| JDK | Java | 21 |
| 基础框架 | Spring Boot | 3.2.5 |
| 微服务 | Spring Cloud | 2023.0.1 |
| 微服务 | Spring Cloud Netflix (Eureka) | 2023.0.1 |
| 网关 | Spring Cloud Gateway (WebFlux) | 随 Cloud |
| 认证鉴权 | Sa-Token（spring-boot3 / reactor-spring-boot3） | 1.38.0 |
| ORM | MyBatis-Flex | 1.11.7 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis（Lettuce） | 7.x |
| 连接池 | HikariCP | 随 Boot |
| 系统信息 | OSHI | 6.4.13 |
| 工具库 | Hutool | 5.8.27 |
| 接口文档 | Knife4j（OpenAPI3 + springdoc） | 4.5.0 |
| 流程引擎 | Warm-Flow（Dromara 开源，Apache-2.0，`warm-flow-mybatis-plus-sb3-starter`） | 1.8.9 |
| 定时任务 | Spring `ThreadPoolTaskScheduler` + Quartz CronExpression（独立服务 lp-job） | — |
| 对象存储 | 本地磁盘 / MinIO / RustFS（S3 协议） | — |
| 消息推送 | 站内信 / JavaMail / WebSocket | 随 Boot |
| 构建 | Maven（多环境 profile，按环境打包） | 3.9+ |

### 前端

| 分类 | 选型 |
| --- | --- |
| 框架 | Vue 3.4（`<script setup lang="ts">`） |
| 构建 | Vite 5 |
| 语言 | TypeScript 5 |
| UI | Element Plus 2.7 + @element-plus/icons-vue |
| 状态管理 | Pinia |
| 路由 | Vue Router 4（后端动态路由） |
| 图表 | ECharts 5 |
| 请求 | Axios |
| 样式 | SCSS |

---

## 二、整体架构

```
                        ┌───────────────────────────────┐
                        │   Browser  (Vue3 + TS + Vite) │
                        │   http://127.0.0.1:5173       │
                        └───────────────┬───────────────┘
                                        │  /api  (Vite 代理)
                                        ▼
                        ┌───────────────────────────────┐
                        │   lp-gateway   :8080          │
                        │   Spring Cloud Gateway        │
                        │   Sa-Token 全局鉴权过滤器      │
                        └──┬────────┬────────┬─────────┘
                           │ lb://  │        │
        ┌──────────────────▼─┐ ┌────▼──────┐ ┌▼───────────────────┐
        │ lp-system   :9201  │ │lp-file    │ │lp-flow      :9204  │
        │ 认证/用户/角色/菜单 │ │:9203      │ │Warm-Flow 流程引擎  │
        │ 字典/参数/部门/日志 │ │本地/MinIO │ │定义/发起/待办/历史 │
        │ 消息中心           │ │/RustFS    │ │                    │
        └─────────┬──────────┘ └─────┬─────┘ └─────────┬──────────┘
                  │                  │                 │
                  │        ┌─────────▼─────────────────▼──────┐
                  │        │ lp-monitor  :9202                │
                  │        │ 服务器 / Redis / MySQL / 慢 SQL   │
                  │        └──────────────────────────────────┘
                  │
                  │        ┌──────────────────────────────────┐
                  └───────►│ lp-job      :9205                │
                           │ 定时任务调度 / 运行批次日志       │
                           └──────────────────────────────────┘
        ┌─────────┴────────────┐
        │                │
        ▼                ▼
   ┌─────────┐     ┌──────────┐        ┌────────────────┐
   │  MySQL  │     │  Redis   │        │ lp-registry    │
   │ 业务库  │     │ 会话/验证码│       │ Eureka :8849   │
   │ +多数据源│     │ 在线用户  │        │ 注册中心        │
   └─────────┘     └──────────┘        └────────────────┘
```

### 模块划分

```
LearningPlatform
├── pom.xml                          # 聚合父工程（依赖版本统一管理 + dev/test/prod profile）
├── sql/lp.sql                       # 建库建表 + 初始化数据
├── sql/upgrade_v2.sql               # v2 增量升级（文件/消息/定时任务/流程菜单与表）
├── sql/upgrade_v3.sql               # v3 增量升级（流程中心菜单、流程沟通评论表）
├── sql/upgrade_v4.sql               # v4 增量升级（warm-flow 表单模板、已阅记录表与权限）
├── sql/upgrade_v5.sql               # v5 增量升级（服务注册监控菜单）
├── sql/upgrade_v6.sql               # v6 增量升级（任务运行日志表、上次运行字段、运行日志菜单）
├── sql/warm-flow/                   # Warm-Flow（Dromara）MySQL 建表脚本（flow_*）
├── lp-registry                      # 注册中心 :8849（Eureka Server）
├── lp-common                        # 公共模块（被所有服务依赖）
│   └── org.yeharn.lp.common
│       ├── core        R / PageResult / PageQuery / BaseEntity / ServiceException / Constants
│       ├── config      JacksonConfig / WebMvcConfig / SaTokenConfig / Knife4jConfig / RedisConfig
│       ├── security    LoginUser / SecurityUtils / SessionStpInterface
│       ├── redis       RedisUtils
│       ├── datasource  @DS / DataSourceContextHolder / DynamicDataSource / DSAspect / Manager
│       └── log         @OperLog / BusinessType / OperLogAspect / OperLogEvent
├── lp-gateway                       # 网关 :8080
├── lp-modules
│   ├── lp-system                    # 系统服务 :9201（认证/权限/消息中心）
│   ├── lp-job                       # 定时任务服务 :9205（调度 / 运行批次日志）
│   ├── lp-file                      # 文件服务 :9203（本地 / MinIO / RustFS）
│   ├── lp-flow                      # 流程服务 :9204（Warm-Flow，独立数据层 MyBatis-Plus）
│   └── lp-monitor                   # 监控服务 :9202（含慢 SQL 审计）
└── lp-ui                            # 前端工程
```

服务端口：

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| lp-gateway | 8080 | 统一入口，路由 + 鉴权 |
| lp-system | 9201 | 系统业务（认证/权限/消息） |
| lp-monitor | 9202 | 监控采集 |
| lp-file | 9203 | 文件服务（本地 / MinIO / RustFS） |
| lp-flow | 9204 | 流程服务（Warm-Flow Dromara） |
| lp-job | 9205 | 定时任务（调度 / cron 可视化 / 运行批次日志） |
| MySQL | 3306 | 数据库 `learning_platform` |
| Redis | 6379 | 密码 `Redis@ZnSoft` |
| lp-registry | 8849 | 注册中心（Eureka Server，启动即用，无需数据库） |
| lp-ui (dev) | 5173 | Vite 开发服务器 |

---

## 三、快速开始

### 1. 环境准备

- JDK 21（设置 `JAVA_HOME`）
- Maven 3.9+
- MySQL 8.x，账号 `root / MySQL@ZnSoft`
- Redis 7.x，密码 `Redis@ZnSoft`
- Node.js 18+ / npm 9+

### 2. 初始化数据库

```bash
mysql -uroot -p'MySQL@ZnSoft' < sql/lp.sql
# v2 增量（已有 v1 库时执行）：文件/消息/定时任务/流程菜单与表结构
mysql -uroot -p'MySQL@ZnSoft' learning_platform < sql/upgrade_v2.sql
# v3 增量（流程中心菜单、流程沟通评论表）
mysql -uroot -p'MySQL@ZnSoft' learning_platform < sql/upgrade_v3.sql
# Warm-Flow 工作流 flow_* 表（仅首次部署流程服务时执行，可重复执行）
mysql -uroot -p'MySQL@ZnSoft' learning_platform < sql/warm-flow/warm-flow-mysql.sql
# v4 增量（表单模板、已阅记录表与权限）
mysql -uroot -p'MySQL@ZnSoft' learning_platform < sql/upgrade_v4.sql
```

`lp.sql` 会创建 `learning_platform` 库、全部业务表以及初始化数据（菜单、角色、用户、字典、参数、公告、数据源、代码生成示例表）。

> 操作日志与登录日志采用**按月分表**：`sys_oper_log_YYYYMM` / `sys_login_log_YYYYMM` 由服务在首次写入时自动 `CREATE TABLE ... LIKE` 创建，无需手工建月表；`sys_oper_log` / `sys_login_log` 作为历史归档表保留。

### 3. 启动注册中心

```bash
java -jar lp-registry/target/lp-registry.jar
# 注册中心控制台: http://127.0.0.1:8849 （纯内存注册表，无需数据库，启动即用）
```

### 4. 构建并启动后端

```bash
# 默认（dev）环境
mvn -DskipTests clean package
# 按环境打包：dev / test / prod
mvn -DskipTests clean package -P prod

java -jar lp-registry/target/lp-registry.jar
java -jar lp-gateway/target/lp-gateway.jar
java -jar lp-modules/lp-system/target/lp-system.jar
java -jar lp-modules/lp-job/target/lp-job.jar
java -jar lp-modules/lp-monitor/target/lp-monitor.jar
java -jar lp-modules/lp-file/target/lp-file.jar
java -jar lp-modules/lp-flow/target/lp-flow.jar
```

`-P dev|test|prod` 会激活 `application-{env}.yml`（见「十七、多环境打包与日志分表」），无需手工改配置。

启动顺序建议：lp-registry → lp-system / lp-job / lp-monitor / lp-file / lp-flow → lp-gateway。也可直接在 IDE 中运行七个 `*Application` 主类。

### 5. 启动前端

```bash
cd lp-ui
npm install --include=dev
npm run dev          # http://127.0.0.1:5173
# 生产构建
npm run build        # 产物在 lp-ui/dist
npm run type-check   # vue-tsc --noEmit
```

Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`（同时开启 `ws: true` 供 WebSocket 升级），前端只需访问网关；文件预览地址 `/api/files/**` 由网关转发到 `lp-file`。

### 6. 默认账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| admin | admin123 | 超级管理员 | 全部权限（`*:*:*`） |
| test | admin123 | 普通用户 | 仅用户查询与日志、在线、监控查看等只读权限 |

> 种子密码为 BCrypt 加密串，登录时由 `BCryptPasswordEncoder` 校验。

---

## 四、功能清单

### 系统管理

- **用户管理**：分页/条件查询、新增、修改、删除（逻辑删除）、重置密码、状态启停、CSV 导出、用户名唯一校验、部门树归属、角色分配
- **角色管理**：CRUD、菜单权限树授权、角色下拉选项、按角色查询用户、数据权限范围（全部/自定义/本部门/本部门及以下/仅本人）
- **菜单管理**：目录 / 菜单 / 按钮三级模型、树形表格、图标、路由与组件路径、权限标识、显示与缓存开关
- **部门管理**：树形结构、祖级列表 `ancestors` 维护、负责人与联系方式
- **字典管理**：字典类型 + 字典数据两级维护，前端可按类型取字典项
- **参数管理**：系统参数键值对维护
- **通知公告**：公告 CRUD，工作台展示
- **数据源管理**：多数据源 CRUD、连接测试、连接池运行状态查看
- **文件服务（lp-file 独立服务）**：图片 / 视频 / 文件 三类分栏，上传（10 / 100 / 50 MB 限制）、预览、下载、删除，存储模式（本地 / MinIO / RustFS）页面可切换
- **消息中心**：收件箱 / 已发送、未读角标、全部已读、发送站内信（多用户 + 消息类型），推送模式（站内信 / 邮件 / WebSocket）页面可切换

### 监控与日志

- **在线用户**：基于 Redis 中的 Sa-Token 会话列出在线用户，支持强制下线
- **登录日志**：登录账号、IP、地点、浏览器、操作系统、状态、提示信息，**按月分表** + 月份筛选、删除、清空
- **操作日志**：`@OperLog` 注解 + AOP 自动记录请求模块、业务类型、方法、参数、返回值、耗时、异常信息，支持详情抽屉查看，**按月分表** + 月份筛选、删除、清空
- **服务监控**：OSHI 采集 CPU、内存、JVM、磁盘、网络、服务器与 JVM 运行时信息，前端 10 秒自动刷新
- **Redis 监控**：`INFO` 指标、内存、连接数、命中率、Key 数量、命令统计、持久化信息
- **MySQL 监控**：`SHOW GLOBAL STATUS/VARIABLES`、连接数、QPS/TPS、缓冲池命中、慢查询、当前会话列表、HikariCP 连接池状态
- **服务注册监控**：基于 Eureka 注册中心枚举全部服务与实例，并行探测各实例 `/actuator/health`，展示注册地址、健康状态、组件明细（db / redis 等）、响应耗时、注册时间、租约与元数据版本，30 秒自动刷新
- **慢 SQL 审计**：`performance_schema` 慢 SQL 列表（SQL 文本、执行次数、总/平均/最大耗时、扫描行数、库表），长事务与长 SQL 会话列表，支持按 `threadId` **KILL** 慢 SQL / 锁表会话

### 系统工具

- **代码生成器**：读取数据库表 → 导入配置 → 编辑字段 → 预览 → 下载 ZIP，支持指定数据源
- **接口文档**：Knife4j 聚合网关文档，侧边栏外链菜单一键打开，支持 Bearer Token 在线调试
- **定时任务（lp-job 独立服务）**：`ThreadPoolTaskScheduler` + Quartz CronExpression 调度（支持秒级与年字段的 6/7 段表达式）、cron 七维可视化构建（秒/分/时/日/月/周/年）并实时预览未来 6 次运行时间、启停、立即执行、上次/下次运行时间、创建人；运行批次日志独立落库（`sys_job_log`，不写系统操作日志），任务列表一键跳转查看运行状态 / 耗时 / 参数 / 结果
- **流程中心（lp-flow 独立服务）**：仿钉钉可视化流程设计器（主干分支画布 + 属性面板 + 条件/办理人表达式 + 表单模板设计 + DefJson 导入导出）、流程定义（版本化部署）、流程实例（流程图高亮当前节点 / 审批记录 / 沟通评论 / 已阅记录 / 挂起激活终止）、我的任务（办理、任意节点驳回与退回、加签、减签、转办、委派、拿回、撤销、任意跳转、已阅）、流程历史（耗时与状态回溯）

### 工作台与个人中心

- **首页控制台**：用户/角色/菜单/部门等统计卡片、7 天登录趋势折线图、部门人员分布环形图、最近登录记录、公告列表
- **个人中心**：基本资料、修改密码、我的角色与权限

---

## 五、认证与权限设计

### 5.1 权限模型（RBAC）

参考「用户—角色—权限（菜单/按钮）」模型，采用标准 RBAC + 数据权限：

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
     │                              │
     └── sys_dept                   └── sys_role_dept（自定义数据权限）
```

- `sys_menu.menu_type`：`M` 目录 / `C` 菜单 / `F` 按钮
- `sys_menu.perms`：权限标识，如 `system:user:add`；`C` 类型同时作为菜单可见权限
- 超级管理员角色 `role_key = admin` 直接持有通配权限 `*:*:*`，绕过后端逐项校验
- 前端按钮级控制：`v-hasPerm="['system:user:add']"` 指令，无权限时移除 DOM
- 前端路由级控制：登录后调用 `/auth/routers` 获取当前用户可访问菜单，动态 `addRoute`
- 后端接口级控制：`@SaCheckPermission("system:user:list")`，由 `StpInterfaceImpl` 提供权限集合

### 5.2 登录流程

```
浏览器                    网关 :8080                lp-system :9201            Redis
  │  GET /auth/captcha       │                          │                        │
  │──────────────────────────▶──────────────────────────▶ 生成 uuid + 图形验证码  │
  │                                                  SET captcha:code:{uuid} ───▶│
  │◀────── { uuid, img(base64) } ────────────────────────│                        │
  │                          │                          │                        │
  │  POST /auth/login        │  路由 + 放行（白名单）    │                        │
  │  {username,password,code,uuid} ─────────────────────▶ 校验验证码 → 校验密码   │
  │                          │                          │ 组装 LoginUser         │
  │                          │                          │ StpUtil.login(userId)  │
  │                          │                          │ 会话写入 Redis ────────▶│
  │◀────── { token, expiresIn } ─────────────────────────│                        │
  │                          │                          │                        │
  │  GET /auth/info          │  Sa-Token 校验 token     │                        │
  │  Authorization: Bearer x ───────────────────────────▶ 返回用户/角色/权限      │
  │  GET /auth/routers       │                          │ 菜单 → RouterVo 树     │
  │  GET /system/dashboard/stats                        │ 工作台聚合数据          │
```

要点：

1. **验证码**：图形验证码文本存于 Redis `captcha:code:{uuid}`（120 秒有效），一次性消费；可通过参数 `sys.account.captchaEnabled` 关闭；前端登录页支持点击刷新。
2. **会话载体**：`LoginUser`（userId、username、nickname、deptId/deptName、roles、permissions、loginIp、browser、os、token）存入 Sa-Token Session，`token-name=Authorization`、`token-prefix=Bearer`。
3. **网关鉴权**：`SaTokenGatewayConfig` 注册全局过滤器，白名单（`/auth/captcha`、`/auth/login` 等）直接放行，其余路径校验 token 并透传；未登录返回 401 JSON。
4. **服务内鉴权**：业务服务同样引入 Sa-Token，通过 `StpInterfaceImpl` 返回角色与权限列表，注解生效。
5. **登录增强**：记录登录 IP、浏览器、操作系统至 `sys_login_log`；`is-concurrent=true`、`is-share=false` 支持多端登录与单端踢出，`StpUtil.logoutByTokenValue(token)` 实现在线用户强制下线；勾选「记住我」时令牌有效期 7 天，否则 1 天。

### 5.3 数据权限

`DataScopeService` 依据角色 `data_scope` 生成部门范围条件：

| 值 | 含义 |
| --- | --- |
| 1 | 全部数据 |
| 2 | 自定义部门（`sys_role_dept`） |
| 3 | 本部门数据 |
| 4 | 本部门及以下数据 |
| 5 | 仅本人数据 |

### 5.4 统一响应与异常

```json
{ "code": 200, "msg": "操作成功", "data": {} }
```

- `200` 成功、`401` 未登录/登录失效、`403` 无权限、`500` 服务异常
- `GlobalExceptionHandler` 统一捕获 `ServiceException`、参数校验异常、Sa-Token 异常与兜底异常
- 前端 `request.ts` 统一解包 `data`，遇到 401 弹出重新登录提示，403 提示无权限

---

## 六、多数据源设计

### 6.1 组成

| 组件 | 职责 |
| --- | --- |
| `DataSourceInfo` | 数据源描述（key、名称、驱动、URL、账号、密码、状态） |
| `DynamicDataSource` | 继承 `AbstractDataSource` 的路由数据源，内部维护 `Map<String, HikariDataSource>`，按 `DataSourceContextHolder` 的 key 取池 |
| `DataSourceContextHolder` | ThreadLocal 保存当前线程数据源 key |
| `@DS("slave")` | 方法 / 类级注解，声明使用哪个数据源 |
| `DSAspect` | AOP 环绕通知，进入时设置 key，退出时恢复原 key，保证嵌套调用安全 |
| `DynamicDataSourceManager` | 运行时构建、注册、移除、测试连接、输出连接池统计 |
| `LpDataSourceProperties` | 动态池参数（`lp.datasource.max-pool-size`） |
| `sys_datasource` | 数据源配置持久化表，密码经 AES 加密存储（`lp.security.aes-key`） |
| `DatasourceInitializer` | 启动时加载 `sys_datasource` 中启用的数据源并注册 |

### 6.2 使用方式

```java
@DS("slave")
public List<Map<String, Object>> listFromSlave() {
    // 该方法内的 SQL 走 slave 数据源
}
```

代码生成器已接入该能力：`/tool/gen/db/list?dsKey=slave` 可读取任意已注册数据源的表结构。

### 6.3 数据源管理接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/system/datasource/list` | 分页列表 |
| GET | `/system/datasource/{id}` | 详情 |
| POST | `/system/datasource` | 新增 |
| PUT | `/system/datasource` | 修改 |
| DELETE | `/system/datasource/{ids}` | 删除 |
| POST | `/system/datasource/test` | 测试连接（返回数据库版本/耗时） |
| GET | `/system/datasource/pool/{dsKey}` | 连接池运行状态 |

---

## 七、代码生成器设计

### 7.1 流程

```
① 选择数据源 dsKey
        │
② GET /tool/gen/db/list          读取 information_schema.tables
        │
③ POST /tool/gen/import          写入 gen_table / gen_table_column
        │
④ PUT  /tool/gen                 编辑包名、模块名、业务名、作者、字段类型与查询方式
        │
⑤ GET  /tool/gen/preview/{id}    CodeTemplate 渲染，返回「文件名 → 代码」
        │
⑥ GET  /tool/gen/download/{id}   打包 ZIP 下载
```

### 7.2 生成产物

- 后端：`Entity`、`Mapper`、`Service`、`ServiceImpl`、`Controller`（含 `@SaCheckPermission`、`@OperLog`、分页查询）
- 前端：`index.vue` 列表页（查询表单、表格、分页、新增/编辑弹窗、删除）
- 自动推导：Java 类型（`BigDecimal` / `LocalDateTime` / `LocalDate` / `LocalTime` 等）、驼峰命名、注释、是否主键/自增/必填/查询条件、字典绑定

### 7.3 相关接口

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/tool/gen/list` | `tool:gen:list` |
| GET | `/tool/gen/db/list` | `tool:gen:import` |
| GET | `/tool/gen/db/columns` | `tool:gen:import` |
| POST | `/tool/gen/import` | `tool:gen:import` |
| GET | `/tool/gen/{tableId}` | `tool:gen:query` |
| PUT | `/tool/gen` | `tool:gen:edit` |
| DELETE | `/tool/gen/{tableIds}` | `tool:gen:remove` |
| GET | `/tool/gen/preview/{tableId}` | `tool:gen:preview` |
| GET | `/tool/gen/download/{tableId}` | `tool:gen:download` |

---

## 八、监控模块

`lp-monitor` 独立部署，避免监控采集影响业务服务。

| 接口 | 说明 |
| --- | --- |
| `GET /monitor/server` | CPU（使用率、核心数、负载）、内存（物理/交换区）、JVM（堆/非堆/GC/线程/类加载/运行时长）、磁盘（分区、读写）、网络（网卡流量）、服务器与 JVM 基础信息 |
| `GET /monitor/redis` | Redis `INFO` 解析结果、内存占用、连接数、命中率、Key 数量、命令调用统计、持久化与主从信息 |
| `GET /monitor/mysql` | 全局状态与变量、连接与线程、QPS/TPS、缓冲池命中率、慢查询、当前进程列表、HikariCP 连接池 |
| `GET /monitor/mysql/slow` | 慢 SQL 审计：`performance_schema.events_statements_summary_by_digest` 聚合的 SQL 文本、执行次数、总/平均/最大耗时、扫描行数、库表 |
| `GET /monitor/mysql/long?threshold=3` | 执行超过 N 秒的长 SQL 与长事务会话（`information_schema.processlist` + `innodb_trx`） |
| `GET /monitor/mysql/kill?threadId=` | KILL 指定会话，用于终止慢 SQL 或锁表 SQL |
| `GET /monitor/service` | 服务注册监控：枚举注册中心服务实例并聚合健康探测结果 |

- 权限标识：`monitor:server:list`、`monitor:redis:list`、`monitor:mysql:list`、`monitor:mysql:kill`、`monitor:service:list`
- 前端页面位于 `lp-ui/src/views/monitor/{server,redis,mysql,service}`，服务监控 10 秒轮询刷新、服务注册监控 30 秒自动刷新；MySQL 页含「慢 SQL 审计」「长 SQL 与事务」Tab，KILL 按钮受 `monitor:mysql:kill` 控制
- 数据来源：OSHI（系统）、`StringRedisTemplate` + `INFO`（Redis）、`SHOW GLOBAL STATUS/VARIABLES` + `performance_schema` + `information_schema`（MySQL）、`DiscoveryClient` + 各实例 `/actuator/health`（服务注册）

---

## 九、数据库表

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 用户 |
| `sys_role` | 角色（含 `data_scope`） |
| `sys_menu` | 菜单 / 按钮权限 |
| `sys_dept` | 部门（树形，`ancestors`） |
| `sys_user_role` | 用户-角色关联 |
| `sys_role_menu` | 角色-菜单关联 |
| `sys_role_dept` | 角色-部门关联（自定义数据权限） |
| `sys_dict_type` | 字典类型 |
| `sys_dict_data` | 字典数据 |
| `sys_config` | 系统参数 |
| `sys_notice` | 通知公告 |
| `sys_login_log` / `sys_login_log_yyyyMM` | 登录日志（历史归档表 + 按月分表） |
| `sys_oper_log` / `sys_oper_log_yyyyMM` | 操作日志（历史归档表 + 按月分表） |
| `sys_datasource` | 动态数据源配置 |
| `sys_file` | 文件记录（分类、存储类型、URL、大小） |
| `sys_message` | 站内消息（一人一条、已读标记、消息类型） |
| `sys_job` | 定时任务（cron、处理器、状态、上次运行时间与状态） |
| `sys_job_log` | 定时任务运行批次日志（触发方式/状态/起止时间/耗时/结果） |
| `flow_definition` / `flow_node` / `flow_skip` / `flow_instance` / `flow_task` / `flow_his_task` / `flow_user` / `flow_form` | Warm-Flow 流程引擎表（`sql/warm-flow` 脚本初始化） |
| `lp_flow_comment` | 流程沟通评论（实例维度的讨论记录） |
| `lp_flow_form` | 流程表单模板（可视化表单设计器产物，定义通过 `form_path` 引用） |
| `lp_flow_read` | 流程已阅记录（含抄送知会留痕） |
| `gen_table` | 代码生成业务表 |
| `gen_table_column` | 代码生成字段表 |

约定：业务表统一带 `create_by / create_time / update_by / update_time / remark`；可逻辑删除的表使用 `del_flag`（`0` 正常、`2` 删除，MyBatis-Flex 全局逻辑删除配置）。

---

## 十、主要接口一览

> 所有接口经网关 `http://127.0.0.1:8080` 访问，除登录/验证码外均需 `Authorization: Bearer {token}`。

| 模块 | 前缀 | 代表接口 |
| --- | --- | --- |
| 认证 | `/auth` | `GET /captcha`、`POST /login`、`GET /info`、`GET /routers`、`POST /logout` |
| 工作台 | `/system/dashboard` | `GET /stats` |
| 用户 | `/system/user` | `GET /list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}`、`PUT /resetPwd`、`PUT /changeStatus`、`GET /export`、`GET /checkUsername` |
| 角色 | `/system/role` | CRUD、`GET /optionselect`、`PUT /auth`、`PUT /changeStatus`、`GET /users/{roleId}`、`GET /checkKey` |
| 菜单 | `/system/menu` | CRUD、`GET /treeselect`、`GET /roleMenuTree/{roleId}` |
| 部门 | `/system/dept` | CRUD、`GET /treeselect` |
| 字典 | `/system/dict` | 类型与数据 CRUD |
| 参数 | `/system/config` | CRUD |
| 公告 | `/system/notice` | CRUD |
| 数据源 | `/system/datasource` | CRUD、`POST /test`、`GET /pool/{dsKey}` |
| 文件（lp-file） | `/file` | `GET /list`、`GET /mode`、`PUT /mode/{mode}`、`POST /upload`、`GET /download/{id}`、`DELETE /{ids}`；静态资源 `/files/**` |
| 消息 | `/system/message` | `GET /list`、`GET /unread-count`、`POST /send`、`PUT /read`、`PUT /read-all`、`GET /mode`、`PUT /mode/{mode}` |
| 日志 | `/log` | `GET /login/list`、`GET /oper/list`、`GET /oper/{id}`、`GET /months`、清理接口（均支持 `month`） |
| 在线 | `/online` | `GET /list`、`DELETE /{token}`（强制下线） |
| 个人中心 | `/profile` | `GET`、`PUT`、`PUT /password` |
| 代码生成 | `/tool/gen` | 见第七章 |
| 定时任务（lp-job） | `/tool/job` | CRUD、`PUT /changeStatus`、`POST /run/{jobId}`、`GET /info`、`GET /preview`（cron 预览）、`GET /log/list`、`GET /log/{logId}`（运行批次） |
| 流程引擎（lp-flow） | `/flow` | `GET /definitions`、`POST /model`、`POST /deploy`、`POST /start`、`GET /tasks`、`GET /all-tasks`、`POST /tasks/{taskId}/complete`、`POST /tasks/{taskId}/reject`、`GET /instances`、`GET /history` |
| 监控 | `/monitor` | `GET /server`、`GET /redis`、`GET /mysql`、`GET /mysql/slow`、`GET /mysql/long`、`GET /mysql/kill` |

---

## 十一、接口文档（Knife4j）

### 11.1 访问入口

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 网关聚合文档 | `http://127.0.0.1:8080/doc.html` | 一次查看系统、监控、文件、流程四组接口 |
| 系统服务文档 | `http://127.0.0.1:9201/doc.html` | 直连 lp-system |
| 监控服务文档 | `http://127.0.0.1:9202/doc.html` | 直连 lp-monitor |
| 文件服务文档 | `http://127.0.0.1:9203/doc.html` | 直连 lp-file |
| 流程服务文档 | `http://127.0.0.1:9204/doc.html` | 直连 lp-flow |
| OpenAPI 描述 | `/v3/api-docs`、`/lp-system/v3/api-docs`、`/lp-monitor/v3/api-docs`、`/lp-file/v3/api-docs`、`/lp-flow/v3/api-docs` | 可导入 Postman / Apifox |

### 11.2 后端配置

- `lp-common` 引入 `knife4j-openapi3-jakarta-spring-boot-starter`（4.5.0），业务服务自动获得 `/doc.html` 与 `/v3/api-docs`
- `Knife4jConfig` 统一声明文档标题、版本、联系人、许可证，并注册 `Authorization`（HTTP Bearer）安全方案，便于在文档页调试需登录的接口
- `lp-system` / `lp-monitor` 的 `application.yml` 中通过 `springdoc.*`、`knife4j.*` 指定扫描包、中文界面；自定义标题可通过 `lp.api-docs.title/description/version` 覆盖
- `lp-gateway` 引入 `knife4j-gateway-spring-boot-starter`，采用 **manual 聚合** 模式，将 `/lp-system/v3/api-docs`、`/lp-monitor/v3/api-docs` 聚合到统一 UI
- 网关为两个文档路由配置 `RewritePath` 过滤器，剥离服务前缀后转发到各自 `/v3/api-docs`
- Sa-Token 白名单已放行 `/doc.html`、`/webjars/**`、`/v3/api-docs/**`、`/*/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-resources/**`、`/files/**`（文件预览）、`/ws/**`（WebSocket 握手无法携带自定义请求头，token 由 `/ws/message` 处理器自行校验）

### 11.3 菜单与前端

- `sys_menu` 新增顶级菜单「接口文档」（`menu_id = 4`），`path` 直接填写外链地址 `http://127.0.0.1:8080/doc.html`，`icon = Document`
- 后端 `SysMenuService` 识别 `http://` / `https://` / `//` 开头的菜单，生成 `RouterVo` 时直接输出外链，并写入 `meta.link`，不注册前端路由
- 前端 `permission` store 跳过外链路由；`SidebarItem` 对外链渲染为 `<a target="_blank">`，点击在新窗口打开文档
- 菜单管理中新增菜单时，路由地址填写完整 URL 即可创建外链菜单
- 非本机部署时，将菜单 `path` 中的 `127.0.0.1:8080` 替换为实际网关地址（或在「菜单管理」中直接修改）

---

## 十二、前端设计

### 12.1 目录结构

```
lp-ui/src
├── api          auth.ts / system.ts / monitor.ts / gen.ts / extras.ts
├── directive    permission.ts（v-hasPerm）
├── layout       index.vue + Sidebar / SidebarItem / Navbar
├── router       index.ts（静态路由 + 全局守卫 + 动态路由注入）
├── stores       user.ts / permission.ts / app.ts（主题）
├── styles       index.scss / themes.scss（5 套主题变量）
├── types        index.ts（全部接口类型）
├── utils        auth.ts / request.ts / websocket.ts
└── views
    ├── login                登录页（动态角色动画）
    ├── dashboard            控制台
    ├── system               user / role / menu / dept / dict / config / notice / datasource / file / message
    ├── monitor              server / redis / mysql / online / loginlog / operlog
    ├── tool/gen             代码生成器
    ├── tool/job             定时任务（lp-job 服务；CronBuilder 可视化构建）
    ├── tool/job/log         任务运行批次日志
    ├── tool/flow            流程中心（definition / instance / task / history + FlowDesigner、FlowViewer 组件）
    ├── profile              个人中心
    └── error                403 / 404
```

### 12.2 动态路由

1. 路由守卫发现已登录但未加载路由 → 依次请求 `/auth/info`、`/auth/routers`
2. `permission` store 依据 `RouterVo.component` 通过 `import.meta.glob('../views/**/*.vue')` 懒加载匹配组件
3. `router.addRoute` 注入布局子路由，最后追加 `/:pathMatch(.*)*` 兜底到 404
4. 刷新页面时若 token 有效则重建路由，避免 404

### 12.3 登录页

移植自 `login_animated-master` 的动态角色动画：4 个角色（含眼球/瞳孔鼠标追踪、随机眨眼）、登录成功与失败的口型与表情反馈、全屏撒花效果；左侧为品牌与动画区，右侧为表单区，支持验证码刷新、记住账号、演示账号快捷填充。

---

## 十三、关键配置

### 网关 `lp-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080
spring:
  cloud:
    gateway:
      routes:
        - id: lp-system
          uri: lb://lp-system
          predicates:
            - Path=/auth/**,/system/**,/tool/**,/log/**,/online/**,/profile/**,/ws/**
        - id: lp-file
          uri: lb://lp-file
          predicates:
            - Path=/file/**,/files/**
        - id: lp-flow
          uri: lb://lp-flow
          predicates:
            - Path=/flow/**
        - id: lp-monitor
          uri: lb://lp-monitor
          predicates:
            - Path=/monitor/**
sa-token:
  token-name: Authorization
  token-prefix: Bearer
```

### 系统服务 `lp-modules/lp-system/src/main/resources/application.yml`

关键项：`server.port=9201`、MySQL `learning_platform`、Redis 密码、`mybatis-flex` 逻辑删除（`0` 正常 / `2` 删除）、`sa-token` 会话参数、`lp.security.aes-key`（数据源密码加密）、`lp.datasource.max-pool-size`。

v2 新增 `lp.*` 配置（均可用环境变量覆盖）：

```yaml
lp:
  mail:
    enabled: false                   # 消息中心模式为 email 时生效
    from: ${LP_MAIL_FROM:}
  xxl-job:
    enabled: false                   # 需先部署 xxl-job-admin
    admin-addresses: ${LP_XXL_ADMIN:http://127.0.0.1:8081/xxl-job-admin}
    appname: lp-system
    port: 9999
  job:
    pool-size: 4                     # 内置调度线程池
spring:
  mail:                              # 邮件服务器（LP_MAIL_HOST / LP_MAIL_USERNAME / LP_MAIL_PASSWORD）
    host: ${LP_MAIL_HOST:}
    port: ${LP_MAIL_PORT:465}
```

### 文件服务 `lp-modules/lp-file/src/main/resources/application.yml`

```yaml
server:
  port: 9203
lp:
  file:
    storage-path: ${LP_FILE_PATH:}   # 为空自动判断：Windows -> C:/lp/upload，其他 -> /data/lp/upload
    mode: local                      # 页面切换后持久化到 sys_config: sys.file.mode
    s3:                              # MinIO 与 RustFS 同为 S3 协议
      endpoint: ${LP_S3_ENDPOINT:http://127.0.0.1:9000}
      access-key: ${LP_S3_AK:}
      secret-key: ${LP_S3_SK:}
      bucket: ${LP_S3_BUCKET:lp-files}
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 200MB
```

### 流程服务 `lp-modules/lp-flow/src/main/resources/application.yml`

```yaml
server:
  port: 9204
mybatis-plus:                    # Warm-Flow 数据层使用 MyBatis-Plus（与业务服务隔离）
  mapper-locations: classpath*:/warm/flow/*.xml
  configuration:
    map-underscore-to-camel-case: true
warm-flow:
  enabled: true
  banner: false
  framework: SPRING_BOOT
  key-type: snowflake
  data-source-type: mysql
  logic-delete: true
  ui: false                      # 使用 lp-ui 自带设计器，关闭引擎内置 UI
  token-name: Authorization
```

> `lp-flow` 依赖 `lp-common` 时排除了 MyBatis-Flex starter，避免两套 ORM 争抢 `SqlSessionFactory`；`@MapperScan` 需同时扫描 `org.dromara.warm.flow.orm.mapper` 完成引擎数据层对接。

### 前端环境变量

```
VITE_APP_BASE_API=/api      # 开发环境经 Vite 代理到网关 8080
```

---

## 十四、安全与约定

- 令牌通过 Header 传递，前端存于 `localStorage`（key `lp-token`），不依赖 Cookie
- 密码使用 BCrypt 存储；数据源密码使用 AES 加密后入库
- 接口权限以后端注解为准，前端 `v-hasPerm` 仅为体验优化
- 逻辑删除由 MyBatis-Flex 全局接管，业务代码无需手写 `del_flag` 条件
- 操作日志通过 Spring 事件异步落库，避免阻塞主流程
- 生产环境请修改默认密码、`aes-key`、数据库/Redis 凭据，并开启 HTTPS 与注册中心安全加固（Eureka Basic Auth 或网络隔离）
- 生产环境建议关闭接口文档：设置 `knife4j.production=true`、`springdoc.api-docs.enabled=false`，并移除网关与业务服务中 `/doc.html`、`/v3/api-docs/**` 的白名单

---

## 十五、文件服务与消息中心

### 15.1 文件服务（独立服务 lp-file：本地 / MinIO / RustFS）

| 存储模式 | 说明 |
| --- | --- |
| `local`（默认） | 写入本地磁盘，路径规则 `{root}/file/yyyyMM/{uuid}.{ext}`；Windows 根目录 `C:/lp/upload`，Linux 为 `/data/lp/upload`（可用 `LP_FILE_PATH` 覆盖） |
| `minio` / `rustfs` | 统一走 S3 协议（同一套 `S3FileStorage`），仅 endpoint / AK / SK / bucket 不同 |

- 分类：`image` / `video` / `file`，前端三个 Tab 分栏；上传前校验 10MB / 100MB / 50MB 上限
- 元数据表 `sys_file`：`original_name`、`file_name`、`file_path`、`url`、`file_size`、`file_type`、`category`、`storage_type`、创建人 / 时间
- 访问地址：`/files/**` 经网关路由到 **lp-file** 的本地静态映射，上传返回如 `/files/file/202611/xxx.txt`（该路径已加入网关与服务的鉴权白名单，便于 `<img>` 直接加载）
- 模式切换：`PUT /file/mode/{mode}` 将模式写入 `sys_config`（`sys.file.mode`），重启仍生效；`GET /file/mode` 返回当前模式与本地根路径
- 接口（独立服务 `lp-file`，端口 9203）：`GET /file/list`、`GET /file/mode`、`PUT /file/mode/{mode}`、`POST /file/upload`、`GET /file/download/{id}`、`DELETE /file/{ids}`

### 15.2 消息中心（站内信 / 邮件 / WebSocket）

- 通道模式：`inner`（默认，站内信）/ `email` / `websocket`，持久化在 `sys_config`（`sys.message.mode`），页面下拉即时切换
- 一人一条记录：发送给 N 位用户写入 N 行 `sys_message`，支持收件箱 / 已发送、未读过滤、标记已读、全部已读、删除
- WebSocket：`/ws/message?token=xxx`（Sa-Token 校验），30 秒心跳、断线 10 秒重连；收到推送时 `ElNotification` 弹出，Navbar 未读徽标 30 秒轮询 `/system/message/unread-count`
- 邮件：配置 `spring.mail.*` 且 `lp.mail.enabled=true` 后，模式为 `email` 时同步发信（JavaMail）
- 开发环境 Vite 已为 `/api`（`ws: true`）与 `/ws` 配置代理；生产环境 Nginx 需为 `/ws/` 单独配置 `Upgrade` / `Connection` 头
- 接口：`GET /system/message/list?box=inbox|sent`、`GET /system/message/unread-count`、`POST /system/message/send`、`PUT /system/message/read`、`PUT /system/message/read-all`、`GET|PUT /system/message/mode`

---

## 十六、定时任务与流程引擎

### 16.1 定时任务（独立服务 lp-job :9205）

- 服务独立：任务调度从系统服务拆出（`lp-modules/lp-job`，端口 9205），与其他服务一样具备 dev/test/prod 配置与分类日志（business/sql/framework）；网关 `/tool/job/**` 优先路由至 lp-job
- 调度内核：Spring `ThreadPoolTaskScheduler`（`lp.job.pool-size`）+ Quartz `CronExpression` 动态排程——每次执行后按最新配置重算下次触发，天然支持含「年」的 7 段表达式；同一任务串行执行，上一批次未结束自动跳过
- 任务处理器：实现 `LpJob` 接口并注册为 Spring Bean 即可通过 Bean 名称调度；内置 `jobLogCleanupJob`（清理 N 天前运行日志）与 `serviceHealthCheckJob`（巡检注册中心全部实例健康）
- cron 可视化：前端 `CronBuilder` 组件按秒 / 分钟 / 小时 / 日 / 月 / 周（星期）/ 年七个维度构建（每 X、每 N X、指定值，日周互斥自动置 `?`），实时预览未来 6 次运行时间；后端 `GET /tool/job/preview` 提供同等预览
- 列表增强：上次运行时间（含成功 / 失败状态点）、下次运行时间（按 cron 实时计算）、创建人
- 运行批次日志：每次执行（自动调度 / 手动执行）写入 `sys_job_log`（批次号、触发方式、触发人、状态、起止时间、耗时、参数、结果 / 异常），**不写系统操作日志**；任务列表「查看批次」跳转 `/tool/job/log` 查看与筛选，双击行或点详情查看完整运行信息
- 接口：`GET /tool/job/list|info|preview|log/list|log/{logId}`、`POST /tool/job`、`PUT /tool/job|changeStatus`、`DELETE /tool/job/{ids}`、`POST /tool/job/run/{jobId}`

### 16.2 流程引擎（Dromara Warm-Flow 1.8.9，独立服务 lp-flow）

- 定位：国产轻量工作流引擎（Apache-2.0，支持商用免授权），JSON 模型（非 BPMN），仅 8 张 `flow_*` 表；Maven 坐标 `org.dromara.warm:warm-flow-mybatis-plus-sb3-starter:1.8.9`
- 表结构：`sql/warm-flow/warm-flow-mysql.sql` 初始化 `flow_definition / flow_node / flow_skip / flow_instance / flow_task / flow_his_task / flow_user / flow_form`
- 审批操作：通过（`skip`）、退回上一节点（`rejectLast`）、任意节点退回（`rejectAtWill`，开始/网关节点自动下探为业务节点）、撤销（`revoke`，发起人）、拿回（`taskBack`）、任意跳转（`passAtWill`，管理员）、终止（`termination`）、转办（`transfer`）、委派（`depute`）、加签（`addSignature`）、减签（`reductionSignature`）、票签（`nodeRatio` 按比例签署）、已阅（`lp_flow_read` 业务留痕）
- 网关与并行：互斥网关（NodeType 3）、并行网关（4）、包容网关（5），由连线 `skipCondition` 决策
- 条件表达式：内置运算符（`gt@@amount|1000` 等于/不等于/大于/大于等于/小于/小于等于/包含）与 SpEL（`spel@@${amount > 1000}`），默认分支用 `default@@${1==1}`，可扩展自定义策略（`ExpressionStrategy`）
- 办理人表达式：指定用户、`${handler}`（发起人）、`role:x` / `dept:x`（自动展开成员）、SpEL（`#{@bean.method()}`）；当前用户权限由 `FlowPermissionHandler`（用户 id + 角色/部门标识）提供
- 流程变量：发起时表单数据 + 附加参数写入实例 `variable`，随流转传递；内置注入 `handler` / `initiator`；支持 `mergeVariable` 运行时更新
- 监听器扩展：全局 `FlowGlobalListener` 监听任务创建，节点 ext 配置驱动自动化——`autoApprove` 自动审批、`remote` 远程访问（HTTP 调用业务系统）、`script` SpEL 脚本执行并回写流程变量、`cc` 抄送留痕；全部事务提交后异步执行
- 版本化部署：设计器保存统一走 `importDef` 新增版本 + `publish` 发布（旧版本保留，在途实例不受影响），列表按 `flow_code` 聚合展示最新版本
- 仿钉钉设计器：主干分支画布（开始/审批/网关分支/结束卡片 + 悬浮添加按钮）、属性面板（审批方式 会签/或签/票签、办理人五类表达式、条件表达式内置与 SpEL、自动化扩展）、表单模板绑定（`lp_flow_form`）、DefJson 导入导出
- 表单设计：字段表格编辑器（10 种类型：input/textarea/number/amount/select/radio/checkbox/date/switch/attachment + 必填 + 栅格），发起页按字段渲染并校验必填，附加参数（ext）作为扩展表单一起提交
- 实例管控：挂起 / 激活（`unActive` / `active`）、终止、级联删除；流程图按连线 BFS 计算层级并标记节点状态（已完成/进行中/未开始），前端 `FlowViewer` 高亮当前节点
- 沟通：`lp_flow_comment` 按实例维度留痕，发言自动标记已阅
- 接口：定义 `GET/POST/DELETE /flow/definitions...`、`POST /flow/deploy`、`GET /flow/forms...`、`POST /flow/forms`；发起 `POST /flow/start`；查询 `GET /flow/tasks/page|done`、`GET /flow/instances/page`、`GET /flow/instances/{id}/detail|records|reject-nodes|comments|variables|reads`、`GET /flow/candidates|roles|depts`、`POST /flow/design/preview`；任务 `POST /flow/tasks/{taskId}/complete|reject|reject-to|add-sign|reduce-sign|transfer|delegate|reclaim|withdraw|read|jump`；实例 `POST /flow/instances/{id}/suspend|activate|terminate`、`DELETE /flow/instances/{id}`
- 沟通：`lp_flow_comment` 表按实例维度记录讨论，与审批意见共同形成完整留痕
- 接口（独立服务 `lp-flow`，端口 9204）：定义 `GET /flow/definitions`、`GET /flow/definitions/{id}/model`、`POST /flow/model`、`POST /flow/deploy`、`DELETE /flow/definitions/{id}`；发起 `POST /flow/start`；分页查询 `GET /flow/tasks/page`、`GET /flow/tasks/done`、`GET /flow/instances/page`、`GET /flow/instances/{id}/detail|records|reject-nodes|comments`、`GET /flow/candidates`；任务动作 `POST /flow/tasks/{taskId}/complete|reject|reject-to|add-sign|reduce-sign|transfer|delegate|reclaim|withdraw|read`；实例管控 `POST /flow/instances/{id}/suspend|activate|terminate`、`DELETE /flow/instances/{id}`；兼容旧接口 `GET /flow/tasks`、`GET /flow/all-tasks`、`GET /flow/instances`、`GET /flow/history`

---

## 十七、多环境打包、日志分类与日志分表

### 17.1 多环境（本地 / 测试 / 线上）

父 `pom.xml` 定义 `dev`、`test`、`prod` 三个 profile，打包时写入 `spring.profiles.active`：

```bash
mvn -DskipTests clean package -P dev     # 默认：本地
mvn -DskipTests clean package -P test    # 测试环境
mvn -DskipTests clean package -P prod    # 线上环境
```

`application.yml` 使用 `spring.profiles.active: @profiles.active@`，由 Maven 资源过滤替换；各服务均提供 `application-dev.yml` / `application-test.yml` / `application-prod.yml`，分别配置 MySQL、Redis、注册中心地址（`EUREKA_ADDR`）、文件根路径与文档开关。

### 17.2 日志分类（各服务独立 logback-spring.xml）

六个可启动服务（`lp-registry` / `lp-gateway` / `lp-system` / `lp-flow` / `lp-file` / `lp-monitor`）各自在 `src/main/resources/logback-spring.xml` 定义日志策略，统一落盘到**项目根目录**（与 `README.md` 同级）的 `logs/` 下，并按模块名称分子目录：

```text
logs/                            # 项目根，与 README.md 同级
├── lp-registry/                 # 注册中心
│   ├── lp-registry.log
│   ├── lp-registry-error.log
│   └── lp-registry-framework.log
├── lp-gateway/
├── lp-system/
│   ├── lp-system.log              # 全量：INFO 及以上，含全部分类
│   ├── lp-system-error.log        # 仅 ERROR：告警与排障入口
│   ├── lp-system-business.log     # 业务：org.yeharn.lp.system.*
│   ├── lp-system-sql.log          # SQL：Mapper / JdbcTemplate（保留 7 天）
│   ├── lp-system-framework.log    # 框架：Spring / Eureka / Sa-Token / 连接池 / XXL-JOB
│   └── archive/                   # 跨天或超 50MB 后的 gz 归档
├── lp-monitor/
├── lp-file/
└── lp-flow/
```

- 业务包 `org.yeharn.lp.xxx` 写入 `-business`；数据访问（Mapper、`JdbcTemplate`、MyBatis-Flex / MyBatis-Plus）写入 `-sql`；Spring、Eureka、Sa-Token、HikariCP、Lettuce、MinIO、XXL-JOB 等写入 `-framework`；未命中分类的日志进入全量文件
- 分类日志同时汇总进全量与错误文件（`additivity` 保持默认），既能按类排查，也能整体回溯
- 滚动策略：单文件 50MB 或按天滚动并 gz 压缩；保留期全量 30 天、错误 60 天、SQL 7 天，配合 `totalSizeCap` 防止磁盘写满
- 日志根目录默认 `logs/`：命令行在**项目根**执行 `java -jar xxx/target/xxx.jar`、或 IDE 运行配置 WorkingDirectory 为项目根时，日志即统一写入项目根 `logs/<模块名>/`；如需自定义位置，可用 `-DLOG_PATH=/data/logs` 或环境变量 `LOG_PATH` 覆盖，子目录名取 `spring.application.name`
- SQL 日志开关：把对应 Mapper 包或 `org.springframework.jdbc.core.JdbcTemplate` 的 `level` 在 `DEBUG` / `INFO` 之间调整即可；`lp-flow` 通过 `JdbcTemplate` 与 Warm-Flow 引擎 Mapper 访问 `flow_*` 表，`lp-monitor` 通过 `JdbcTemplate` 执行监控查询
- 网关无数据库访问，只生成全量 / 错误 / 业务 / 框架四类；注册中心无业务 / SQL 分类，只生成全量 / 错误 / 框架三类

### 17.3 操作日志、登录日志按月分表

- 表名 `sys_oper_log_yyyyMM` / `sys_login_log_yyyyMM`，首次写入时执行 `CREATE TABLE IF NOT EXISTS ... LIKE sys_oper_log` 自动建月表，无需人工干预
- 列表、详情、删除、清空均携带 `month` 参数并按月路由到对应月表；未传月份默认当前月
- `GET /log/months?type=oper|login` 返回已有月份集合，前端下拉筛选
- `sys_oper_log` / `sys_login_log` 保留为历史归档表（v1 数据），可按需按月迁移或清理

---

## 十八、UI 主题系统与液态玻璃

- 内置 5 套主题：极光蓝 `aurora`（默认）、暮光紫 `violet`、青碧 `emerald`、玫瑰 `rose`、暗夜 `midnight`
- 实现方式：`src/styles/themes.scss` 在 `:root` 与 `html[data-theme="xxx"]` 下定义 `--lp-primary`、`--lp-bg-*`、`--lp-card-bg`、`--lp-radius` 等变量，暗夜主题额外覆盖 Element Plus 的 `--el-*` 变量
- `app` store 的 `applyTheme()` 写入 `html[data-theme]` 与 `lp-dark` 类，选择结果存 `localStorage['lp-theme']`，刷新保持
- 玻璃拟态：卡片 / 搜索栏 / 表格容器 / 弹层统一 `backdrop-filter: blur()` + 半透明背景 + 大圆角，按钮与表单控件圆润化，背景叠加登录页同源的流动光斑动画
- 切换入口：Navbar 主题下拉（色点预览），侧边栏、布局背景随主题变量联动

### 18.1 组件尺寸与语义色体系（v1.1）

- 尺寸令牌：`--el-component-size: 34px`（small 28 / large 40）与 `--el-font-size-base: 13px` 全局统一，按钮 / 输入 / 选择 / 表格行 / 分页 / 标签高度随令牌联动；胶囊按钮（`border-radius: 100px`）+ 语义色投影 + 悬浮微动效
- 语义色令牌：`--lp-success / warning / danger / info` 全主题稳定，暗夜主题自动提亮；业务页面禁止写死色值，一律使用 `var()` 或 `color-mix(in srgb, var(--lp-primary) n%, transparent)` 派生
- 表面层次：`--lp-surface / surface-hover / surface-sunken / border / divider` 五级表面变量，表格表头 / 描述列表 / 输入框底纹全部接入
- 统计卡配色由主题派生（`sc-primary / sc-info / sc-success / sc-warning / sc-danger / sc-violet / sc-cyan / sc-slate`），切换主题即时生效
- 图表颜色随主题：控制台 ECharts 轴线 / 标签 / 主色 / 面积渐变 / 饼图色板均通过 `getComputedStyle` 读取主题变量，`watch(theme)` 自动重绘

### 18.2 多分辨率适配（v1.1）

- 超宽屏 ≥1920：内容区 `max-width: 1760px` 居中，避免表格过度拉伸
- 平板 ≤992：侧边栏自动折叠为 64px 图标模式（布局监听 `resize`，不覆盖用户偏好）；搜索表单两列布局（`:has(.el-button)` 排除按钮项），表格容器横向滚动，弹窗 92% / 抽屉 86% 宽
- 手机 ≤768：搜索表单单列、分页居中并隐藏每页条数与跳页、`.hide-sm` 工具类隐藏次要内容、面包屑与用户名自动隐藏（Navbar `compact` 模式）
- 断点内全局兜底：`.el-dialog` / `.el-drawer` / `.el-message-box` 宽度自适应，图表高度压缩

### 18.3 监控中心科技风（v1.1，`src/styles/monitor.scss`）

- `.tech-page` 深空渐变背景 + 网格光效（`mask` 渐隐）+ 循环扫描光带，固定深色观感、脱离主题切换
- `.tech-header` 霓虹标题（呼吸灯脉冲点 + 发光文字 + 英文副标）、`.tech-metrics` 自动栅格指标卡（DIN 数字字体 + 霓虹光晕，按阈值 `is-warn / is-danger` 变色）
- `.tech-page` 内 Element Plus 组件（卡片 / 表格 / 描述列表 / 进度条 / 输入框 / 分页 / 标签）统一深色玻璃化：卡片顶部霓虹渐变线、表格 hover 高亮、输入框霓虹描边
- `.tech-gauge` 环形仪表（进度颜色按压力阈值读取主题语义色）、`.tech-kv` 键值行、`.tech-grid` 响应式栅格
- 监控页进度条颜色（CPU / 内存 / 磁盘）读取 `--tech-cyan / --lp-warning / --lp-danger`，主题切换 + 10 秒轮询后自动同步

---

## 十九、已验证项

- 三个后端服务（8080 / 9201 / 9202）均可正常启动并注册至 Eureka（lp-registry :8849）
- 网关端到端链路：验证码 → 登录 → `/auth/info` → `/auth/routers` → 用户/角色/菜单/字典/参数/公告/数据源/日志/在线/工作台/代码生成/三类监控接口均返回 `200`
- 权限校验：`test` 用户（`common` 角色）访问 `/system/user/list`、`/monitor/server` 正常，访问 `/system/role/list`、`/system/menu/list` 返回 `403`，符合预期
- 前端 `vue-tsc --noEmit` 通过，`vite build` 产物正常生成
- UI 体系：1920 / 1024 / 414 三档分辨率截图验证（侧边栏自动折叠、搜索表单两列转单列、面包屑与用户名隐藏）；`aurora` / `violet` 主题切换后统计卡、图表、侧边栏即时联动；监控中心（服务监控 / Redis / 在线用户）深色科技风、指标卡阈值变色与仪表盘配色正常
- Knife4j：网关 `/doc.html`（`200`）与 `/v3/api-docs/swagger-config` 正常，聚合分组为「系统服务 / 监控服务 / 文件服务 / 流程服务」，四个 `/lp-xxx/v3/api-docs` 均返回 `200`，Bearer 安全方案生效；`/auth/routers` 返回「接口文档」外链菜单（`meta.link`）
- 文件服务（lp-file，9203）：`/file/upload` 返回 `url=/files/file/202611/{uuid}.txt`、`storageType=local`，`/file/list` 可分页查到记录，`/file/mode` 返回 `local` 与本地根目录 `C:/lp/upload`，静态资源 `/files/**` 直连返回 `200`
- 消息中心：发送返回「已发送给 1 位用户」，未读数 `1`，标记已读后归零；`sys_message` 表结构已补齐 `update_by / update_time`
- 定时任务（lp-job，9205）：`/tool/job/list` 返回上次/下次运行时间与创建人，启停、手动执行、新增删除正常；运行批次日志落 `sys_job_log`（自动调度 triggerBy=system、手动执行 triggerBy=admin），操作不再写入系统操作日志；cron 可视化构建器七个维度与未来 6 次运行时间预览正常
- 流程服务（lp-flow，9204）：
  - `/flow/definitions` 返回自动部署的「请假审批」（key=leave，version=1）
  - `POST /flow/start` 发起成功（实例 `2117881150932336641`），`/flow/tasks` 返回「主管审批」待办，`POST /flow/tasks/{id}/complete` 通过后 `/flow/history` 状态为 `1`（审批通过）、含耗时
  - `POST /flow/model` 表单化建模部署成功（key=demo_flow），验证 JSON 模型生成逻辑
- 服务健康：lp-system / lp-file / lp-flow / lp-monitor / lp-job 的 `/actuator/health` 均为 `UP`（邮件未配置时已排除 mail 健康检查），Eureka 中注册 `lp-gateway/lp-system/lp-job/lp-monitor/lp-file/lp-flow` 六个实例
- 慢 SQL：`/monitor/mysql/slow`、`/monitor/mysql/long` 返回审计数据，KILL 接口按 `threadId` 生效
- 日志分表：`sys_oper_log_202611`（2 条）、`sys_login_log_202611`（6 条）自动创建并写入，历史数据保留在 `sys_oper_log` / `sys_login_log`
- Maven 多环境：`-P dev|test|prod` 打包后 `application.yml` 中 `spring.profiles.active` 正确替换

---

## 二十、可扩展方向

- 引入 Seata 处理跨服务分布式事务
- 接入 Sentinel 实现网关与服务级限流熔断
- 日志月表冷数据转存 Elasticsearch / ClickHouse，并提供自动归档策略
- 文件服务接入 OSS / COS 等公有云存储，消息中心接入短信与钉钉 / 企业微信机器人
- Warm-Flow 扩展定时催办提醒、抄送通知推送与委派链审计报表，亦可平滑接入官方 flow-long-ui 设计器
- 文件服务增加分片上传、断点续传与图片压缩，并对 `/files/**` 增加防盗链签名
