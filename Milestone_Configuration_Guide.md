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
    # --- 基础设置 ---
    type: ONE_TIME      # 类型: ONE_TIME (一次性), COUNTER (计数型)
    category: "my_category" # 所属类别 ID
    parent: null        # 父成就 ID (根成就填 null 或不写)
    
    # --- 显示设置 ---
    icon: GRASS_BLOCK   # 显示图标
    title: "<color:#FFD479>起始点"  # 标题 (支持 RGB)
    description:        # 描述
      - "<color:#E6E6E6>这是第一个成就"
    visible: true       # 是否在 UI 中可见
    
    # --- 通知设置 (可选, 默认 true) ---
    show_toast: true       # 是否显示右上角弹窗 (Toast)
    announce_to_chat: true # 是否在聊天栏广播

    # --- 触发条件 ---
    trigger:
      type: JOIN        # 触发类型
      value: ""         # 触发参数 (如方块名、实体名)
      progress: 1       # (可选) 每次触发增加的进度，默认为 1

    # --- 奖励 ---
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

## 4. 颜色格式支持

插件支持多种颜色格式，可以在 `title`, `description`, `rewards` 中混合使用：

1. **MiniMessage (推荐)**: `<color:#RRGGBB>`, `<red>`, `<bold>`
   - 示例: `<color:#FF0000>红色标题`
2. **Hex 格式**: `&#RRGGBB` 或 `{#RRGGBB}`
   - 示例: `&#FF0000红色标题`
3. **Legacy 格式**: `&c`, `&l`
   - 示例: `&c红色标题`

## 5. 触发器类型 (Trigger Types)

| 类型 | 说明 | value 参数示例 |
| :--- | :--- | :--- |
| `JOIN` | 加入服务器 | (留空) |
| `BLOCK_BREAK` | 破坏方块 | `STONE`, `DIAMOND_ORE`, `*` (任意) |
| `BLOCK_PLACE` | 放置方块 | `OAK_LOG`, `*` |
| `KILL_ENTITY` | 击杀实体 | `ZOMBIE`, `PLAYER`, `*` |
| `CONSUME_ITEM` | 消耗物品 (吃/喝) | `APPLE`, `POTION` |
| `CRAFT_ITEM` | 合成物品 | `DIAMOND_SWORD` |

## 6. 常见问题

- **Q: 为什么成就没有显示？**
  - A: 检查 `parent` ID 是否正确。如果是根成就，确保它没有父节点。检查 `category` 是否已定义。
- **Q: 修改配置后如何生效？**
  - A: 建议重启服务器，或者使用插件提供的重载命令（如果已实现）。
- **Q: 不同文件的 ID 可以重复吗？**
  - A: **不可以**。所有加载的文件中的 ID 必须全局唯一。建议使用 `文件名:ID` 的方式命名以避免冲突。
