# Milestone 插件配置指南

本文档旨在帮助管理员理解如何创建和配置 Milestone 插件的成就文件。

## 1. 目录结构与加载机制

插件会自动加载 `plugins/Milestone/milestones/` 文件夹下的所有 `.yml` 配置文件。
- **支持多级目录**：您可以创建子文件夹来分类管理不同类型的成就（例如 `newbie/`, `daily/`, `challenge/`）。
- **文件名随意**：文件名不影响功能，建议使用具有描述性的名称（如 `mining.yml`）。

## 2. 成就 ID 与 命名空间 (Namespace)

Milestone 插件使用 **ID** 来唯一标识每一个成就。ID 的格式决定了成就在 Minecraft 内部的 **命名空间 (Namespace)**。

### 命名规则
在 YAML 配置文件中，成就的键名即为其 ID。

- **格式**：`namespace:key`
- **解析逻辑**：
  - 如果 ID 包含冒号（例如 `newbie:first_join`）：
    - **Namespace**: `newbie`
    - **Key**: `first_join`
  - 如果 ID **不**包含冒号（例如 `first_join`）：
    - **Namespace**: `milestone` (默认)
    - **Key**: `first_join`

**最佳实践**：建议为不同类别的成就使用不同的前缀（命名空间），例如 `pvp:kill_10`, `mining:diamond`，这样便于管理和避免冲突。

---

## 3. 配置文件结构

一个完整的配置文件通常包含两部分：**类别定义 (categories)** 和 **成就定义 (milestones)**。

### 3.1 类别定义 (Categories)
用于在 UI 界面中对成就进行分组显示。

```yaml
categories:
  my_category:          # 类别 ID，在成就中引用
    name: "我的分类"     # 显示名称
    icon: DIAMOND_SWORD # 图标材质
    description: "这是我的自定义分类"
```

### 3.2 成就定义 (Milestones)

```yaml
milestones:
  # 1. 唯一的成就 ID (namespace:key)
  "my_category:root":
    type: ONE_TIME      # 类型: ONE_TIME (一次性), COUNTER (计数型)
    category: "my_category" # 所属类别 ID
    parent: null        # 父成就 ID (根成就填 null 或不写)
    icon: GRASS_BLOCK   # 显示图标
    title: "<color:#FFD479>起始点"  # 标题 (支持 RGB)
    description:        # 描述
      - "<color:#E6E6E6>这是第一个成就"
    visible: true       # 是否在 UI 中可见
    show_toast: true       # 是否显示右上角弹窗 (Toast)
    announce_to_chat: true # 是否在聊天栏广播 (仅控制插件自定义广播，原版广播已强制禁用)
    trigger:
      type: JOIN        # 触发类型
      value: ""         # 触发参数 (如方块名、实体名)
      progress: 1       # (可选) 每次触发增加的进度，默认为 1
    rewards:
      - "give %player% diamond 1"
      - "say 恭喜 %player%！"

  # 2. 子成就示例
  "my_category:step_1":
    parent: "my_category:root" # 指定父成就，形成树状结构
    type: COUNTER
    max: 10             # 目标数量 (仅 COUNTER 类型需要)
    category: "my_category"
    icon: STONE
    title: "挖掘大师"
    description:
      - "挖掘 10 个石头"
    trigger:
      type: BLOCK_BREAK
      value: STONE
```

## 4. 颜色代码与配色方案

本插件采用统一的现代 RGB 配色方案，支持在 `messages.yml` 和成就配置文件中混合使用以下三种颜色格式：

### 支持的格式

1. **MiniMessage (推荐)**
   - 语法: `<color:#RRGGBB>`, `<red>`, `<bold>`, `<reset>`
   - 示例: `<color:#FF0000>这是一个红色的标题`

2. **Legacy (传统)**
   - 语法: `&c`, `&l`, `&r`
   - 示例: `&c这是一个红色的标题`

3. **Hex (十六进制)**
   - 语法: `&#RRGGBB` 或 `{#RRGGBB}`
   - 示例: `&#FF0000这是一个红色的标题`

### 默认配色表

为了保持视觉风格统一，建议在自定义消息或成就描述中使用以下标准配色：

| 用途 | 颜色代码 | 颜色预览 |
| :--- | :--- | :--- |
| **主文本** | `<color:#E6E6E6>` | 浅灰白 |
| **次级/说明** | `<color:#A0A0A0>` | 深灰 |
| **标题/重点** | `<color:#FFD479>` | 暖金 |
| **成功** | `<color:#6BFF95>` | 柔绿 |
| **错误** | `<color:#FF6B6B>` | 柔红 |

---

## 5. 触发器类型 (Trigger Types)

| 类型 | 说明 | value 参数示例 |
| :--- | :--- | :--- |
| `JOIN` | 加入服务器 | (留空) |
| `BLOCK_BREAK` | 破坏方块 | `STONE`, `DIAMOND_ORE`, `*` (任意) |
| `BLOCK_PLACE` | 放置方块 | `OAK_LOG`, `*` |
| `KILL_ENTITY` | 击杀实体 | `ZOMBIE`, `PLAYER`, `*` |
| `CONSUME_ITEM` | 消耗物品 (吃/喝) | `APPLE`, `POTION` |
| `CRAFT_ITEM` | 合成物品 | `DIAMOND_SWORD` |

## 7. 奖励系统 (Rewards)

在 `rewards` 列表中，您可以使用以下前缀来执行不同类型的奖励操作：

| 前缀 | 说明 | 示例 | 备注 |
| :--- | :--- | :--- | :--- |
| `[player]` | 以玩家身份执行命令 | `[player] me 完成了成就！` | 默认行为 |
| `[console]` | 以控制台身份执行命令 | `[console] say %player% 真棒` | **Folia 核心自动禁用** |
| `[op]` | 临时赋予 OP 权限执行命令 | `[op] fly` | 执行后自动撤销 OP |
| `[message]` | 发送私聊消息 | `[message] <color:#6BFF95>奖励已发放！` | 支持 RGB |
| `[broadcast]` | 发送全服广播 | `[broadcast] <color:#FFD479>恭喜 %player%！` | 支持 RGB |
| `[title]` | 发送标题 | `[title] 恭喜;完成成就` | 格式: 主标题;副标题 |
| `[potion]` | 给予药水效果 | `[potion] SPEED;200;1` | 格式: 类型;Tick;等级 |
| `[money]` | 给予金钱 (需 Vault) | `[money] 100` | 需要安装 Vault 和经济插件 |

示例：
```yaml
    rewards:
      - "[console] give %player% diamond 1"
      - "[message] <green>你获得了 1 颗钻石！"
      - "[money] 100"
```

## 8. PlaceholderAPI 变量

插件提供了一系列 PAPI 变量，用于在计分板、聊天栏或菜单中展示成就信息。

### 8.1 基础变量
- `%milestone_status_<id>%`: 玩家是否完成成就 (`completed` / `incomplete`)
- `%milestone_is_completed_<id>%`: 是否完成 (`true` / `false`)
- `%milestone_current_<id>%`: 当前进度数值
- `%milestone_max_<id>%`: 目标进度数值
- `%milestone_percent_<id>%`: 完成百分比 (0-100)

### 8.2 排行榜变量 (Leaderboards)
数据每 5 分钟自动更新一次。支持查询 **Weekly** (本周, 周一至周一), **Monthly** (本月), **Total** (历史总榜)。

**格式**: `%milestone_top_<type>_<field>_<rank>%`
- `<type>`: `weekly`, `monthly`, `total`
- `<field>`: `name` (玩家名), `score` (完成数量)
- `<rank>`: 排名 (1, 2, 3...)

**示例**:
- `%milestone_top_weekly_name_1%`: 本周第一名玩家名
- `%milestone_top_total_score_1%`: 历史总榜第一名完成数量
- `%milestone_top_monthly_name_3%`: 本月第三名玩家名

### 8.3 百分比排名变量
展示前 X% 的玩家列表。

**格式**: `%milestone_<type>_top_percent_names_<percent>%`
- `<type>`: `weekly`, `monthly`
- `<percent>`: 百分比 (1-100)

**示例**:
- `%milestone_weekly_top_percent_names_10%`: 本周完成度前 10% 的玩家列表 (逗号分隔)

## 9. 常见问题

- **Q: 为什么成就没有显示？**
  - A: 检查 `parent` ID 是否正确。如果是根成就，确保它没有父节点。检查 `category` 是否已定义。
- **Q: 修改配置后如何生效？**
  - A: 建议重启服务器，或者使用 `/milestone reload` 重载命令（仅重载配置，成就变动建议重启）。
- **Q: 不同文件的 ID 可以重复吗？**
  - A: **不可以**。所有加载的文件中的 ID 必须全局唯一。建议使用 `文件名:ID` 的方式命名以避免冲突。
- **Q: 为什么会有两条广播消息？**
  - A: 最新版本已修复此问题。插件会自动屏蔽原版的白色广播消息，`announce_to_chat` 仅控制插件自定义的彩色广播。
