# Milestone

Milestone 是一个服务器级的可扩展成就系统，提供自定义进度树、统一的美化显示、奖励执行与排行榜统计，兼容 Paper/Folia 生态与主流周边插件。

## 特性
- 自定义进度：按分类/父子关系组织里程碑，支持任意数量与层级
- 现代显示：统一 RGB 配色与图标，屏蔽原版重复广播
- 奖励系统：消息/广播/标题/药水/经济奖励等，可组合配置
- 触发行为：入服、挖掘/放置、击杀、食用、合成等常见事件
- 排行榜：周榜、月榜、历史总榜缓存，支持 PAPI 展示
- 数据持久化：SQLite/MySQL 存储玩家进度
- Folia 支持：异步与分区安全的执行调度

## 运行环境
- 服务端：Paper/Folia 1.21+
- 依赖（可选）：ProtocolLib（进度界面与聊天拦截增强）、Vault（经济奖励）、PlaceholderAPI（变量展示）
- Java：17+

## 安装与启动
- 将构建的插件 JAR 放入 `plugins/` 后重启服务器
- 首次启动会在 `plugins/Milestone/` 下生成：
  - `config.yml`：模式、屏蔽命名空间、数据库配置等
  - `messages.yml`：多语言消息与配色
  - `milestones/`：示例分类与里程碑文件

## 配置要点
- 进度模式（`config.yml` → `advancement.mode`）
  - `DISABLE_VANILLA`：仅显示 Milestone，自带进度全部隐藏
  - `HYBRID`：混合显示，可按命名空间/ID屏蔽原版（默认）
  - `VANILLA_ONLY`：仅显示原版（不推荐）
- 屏蔽列表：`advancement.block-namespaces` 与 `advancement.block-ids`
- 数据库：`database.type` 支持 `sqlite` 与 `mysql`；按需填写 MySQL 连接信息

## 里程碑编写（YAML）
位于 `plugins/Milestone/milestones/`，支持多级目录与多文件加载。

- ID 与命名空间
  - 使用 `namespace:key` 唯一标识；未显式写命名空间时默认取文件夹名或 `milestone`
- 常用字段
  - `type`: `ONE_TIME` | `COUNTER` | `MULTI_CONDITION` | `HIDDEN`
  - `parent`: 父里程碑 ID（根不写或设为空）
  - `icon`: 显示图标（Minecraft 材质名）
  - `title`/`description`: 标题与描述（支持 RGB/MiniMessage）
  - `max`: 计数型目标值（仅 COUNTER）
  - `trigger`: `type`/`value`/`progress`（触发类型、参数与每次增量）
  - `rewards`: 奖励列表（见下）
  - `show_toast`/`announce_to_chat`/`visible`: UI 控制项
- 模板与增强
  - `templates:` 定义可复用片段；里程碑可通过 `template` 引用并用 `augments` 变量替换、`overrides` 覆盖字段

## 奖励格式
- `[message]` 私聊消息
- `[broadcast]` 全服广播
- `[title]` 标题 `主标题;副标题`
- `[potion]` 药水 `类型:秒数:等级`
- `[money]` 经济奖励（需 Vault）
- `[player]` 以玩家身份执行命令
- `[op]` 临时 OP 执行命令（执行后撤销）
- 默认无前缀将以控制台执行（在 Folia 上默认禁用）

## PlaceholderAPI 变量
- 基础：`%milestone_status_<id>%`、`%milestone_is_completed_<id>%`、`%milestone_current_<id>%`、`%milestone_max_<id>%`、`%milestone_percent_<id>%`
- 排行榜：`%milestone_top_<weekly|monthly|total>_<name|score>_<rank>%`
- 百分比名单：`%milestone_<weekly|monthly>_top_percent_names_<percent>%`

## 管理命令与权限
- 主命令：`/milestone`（别名 `mls`，权限 `milestone.use`）
- 管理子命令（权限 `milestone.admin`，默认 OP）：
  - `reload`：重载配置与消息
  - `grant <player> <id>`：授予里程碑完成
  - `revoke <player> <id>`：撤销里程碑完成
  - `progress <player> <id>`：查询玩家里程碑进度

## 存储实现
- SQLite：默认启用，文件位于 `plugins/Milestone/milestone.db`
- MySQL：HikariCP 连接池；表结构包含玩家 ID/名、里程碑 ID、当前计数、完成状态与完成时间

## 构建
- 本仓库使用 Gradle Kotlin DSL：`./gradlew build`
- 产物位于 `build/libs/`

## 开源许可
- 详见仓库中的 `LICENSE`
