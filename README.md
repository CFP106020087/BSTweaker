# BSTweaker - BetterSurvival 武器扩展模组

通过 JSON 配置文件为 BetterSurvival 添加自定义武器，继承原版武器的全部功能。

---

## 快速开始

### 1. 安装
- 需要 **Minecraft Forge 1.12.2** + **BetterSurvival**
- 将 `bstweaker-1.0.0.jar` 放入 `mods/` 文件夹
- 启动游戏，自动生成 `config/bstweaker/weapons.json`

### 2. 编辑配置
打开 `config/bstweaker/weapons.json`，添加你的武器定义。

### 3. 重启游戏
武器会自动注册，可在创造物品栏找到。

---

## 配置文件格式

```json
{
    "weapons": [
        {
            "id": "my_weapon_id",
            "type": "nunchaku",
            "displayName": "我的武器",
            "damageModifier": 0.6,
            "speedModifier": 0.25,
            "texture": "bstweaker:items/my_weapon",
            "material": {
                "name": "MyMaterial",
                "harvestLevel": 2,
                "durability": 300,
                "efficiency": 6.5,
                "damage": 3.0,
                "enchantability": 16,
                "repairItem": "minecraft:iron_ingot"
            }
        }
    ]
}
```

---

## 武器类型

| type | 武器 | 特殊能力 |
|------|------|----------|
| `nunchaku` | 双截棍 | 按住右键旋转攻击，连击加成 |
| `hammer` | 战锤 | 右键地面冲击，眩晕效果 |
| `dagger` | 匕首 | 背刺造成2倍伤害 |
| `battleaxe` | 战斧 | 高伤害武器 |
| `spear` | 长矛 | 右键投掷，攻击距离+1 |

---

## 参数说明

### 武器属性

| 参数 | 说明 | 示例 |
|------|------|------|
| `id` | 武器唯一ID（英文小写+下划线） | `"flame_dagger"` |
| `type` | 武器类型（见上表） | `"dagger"` |
| `displayName` | 显示名称 | `"火焰匕首"` |
| `damageModifier` | 伤害倍率（相对于剑） | `0.5` |
| `speedModifier` | 攻速因子（越小越快） | `0.6` |
| `texture` | 纹理路径 | `"bstweaker:items/flame_dagger"` |

### 材质属性

| 参数 | 说明 | 参考值 |
|------|------|--------|
| `name` | 材质名称 | `"BlazeMetal"` |
| `harvestLevel` | 挖掘等级 | 木=0, 石=1, 铁=2, 钻石=3 |
| `durability` | 耐久度 | 铁=250, 钻石=1561 |
| `efficiency` | 挖掘效率 | 铁=6.0, 钻石=8.0 |
| `damage` | 基础伤害加成 | 铁=2.0, 钻石=3.0 |
| `enchantability` | 附魔能力 | 铁=14, 钻石=10 |
| `repairItem` | 修复物品 | `"minecraft:diamond"` |

---

## 添加纹理和模型

### 1. 使用资源包

创建资源包结构：
```
resourcepacks/
└── my_weapons/
    ├── pack.mcmeta
    └── assets/
        └── bstweaker/
            ├── textures/items/
            │   └── my_weapon.png
            └── models/item/
                └── my_weapon.json
```

### 2. 模型文件示例

`my_weapon.json`:
```json
{
    "parent": "item/handheld",
    "textures": {
        "layer0": "bstweaker:items/my_weapon"
    }
}
```

### 3. 语言文件

添加到资源包的 `lang/zh_cn.lang`:
```
item.bstweaker.my_weapon_id.name=我的武器名称
```

---

## 完整示例

5种武器的配置示例：

```json
{
    "weapons": [
        {
            "id": "blazing_nunchaku",
            "type": "nunchaku",
            "displayName": "炙铁双截棍",
            "damageModifier": 0.6,
            "speedModifier": 0.25,
            "texture": "bstweaker:items/blazing_nunchaku",
            "material": {
                "name": "BlazingIron",
                "harvestLevel": 2,
                "durability": 300,
                "efficiency": 6.5,
                "damage": 3.0,
                "enchantability": 16,
                "repairItem": "minecraft:iron_ingot"
            }
        },
        {
            "id": "obsidian_hammer",
            "type": "hammer",
            "displayName": "黑曜石战锤",
            "damageModifier": 1.5,
            "speedModifier": 1.4,
            "texture": "bstweaker:items/obsidian_hammer",
            "material": {
                "name": "Obsidian",
                "harvestLevel": 3,
                "durability": 1500,
                "efficiency": 4.0,
                "damage": 5.0,
                "enchantability": 8,
                "repairItem": "minecraft:obsidian"
            }
        },
        {
            "id": "shadow_dagger",
            "type": "dagger",
            "displayName": "暗影匕首",
            "damageModifier": 0.45,
            "speedModifier": 0.5,
            "texture": "bstweaker:items/shadow_dagger",
            "material": {
                "name": "ShadowSteel",
                "harvestLevel": 2,
                "durability": 180,
                "efficiency": 7.0,
                "damage": 2.0,
                "enchantability": 22,
                "repairItem": "minecraft:ender_pearl"
            }
        },
        {
            "id": "titan_battleaxe",
            "type": "battleaxe",
            "displayName": "泰坦战斧",
            "damageModifier": 1.3,
            "speedModifier": 1.4,
            "texture": "bstweaker:items/titan_battleaxe",
            "material": {
                "name": "Titanium",
                "harvestLevel": 3,
                "durability": 1800,
                "efficiency": 8.5,
                "damage": 4.5,
                "enchantability": 10,
                "repairItem": "minecraft:iron_ingot"
            }
        },
        {
            "id": "storm_spear",
            "type": "spear",
            "displayName": "雷霆长矛",
            "damageModifier": 0.9,
            "speedModifier": 0.9,
            "texture": "bstweaker:items/storm_spear",
            "material": {
                "name": "StormMetal",
                "harvestLevel": 2,
                "durability": 400,
                "efficiency": 7.5,
                "damage": 3.0,
                "enchantability": 18,
                "repairItem": "minecraft:gold_ingot"
            }
        }
    ]
}
```

---

## 注意事项

1. **JSON 格式必须正确** - 使用 JSON 验证工具检查语法
2. **ID 必须唯一** - 不能与已有武器ID重复
3. **纹理路径区分大小写** - 建议全部使用小写
4. **修改配置后需重启游戏** - 配置在启动时加载

---

## 开发者构建

```bash
# 构建 BetterSurvival（如需要）
cd BetterSurvival-master
./gradlew build
cp build/libs/better_survival*.jar ../BSTweaker/libs/better_survival.jar

# 构建 BSTweaker
cd ../BSTweaker
./gradlew build
# 输出: build/libs/bstweaker-1.0.0.jar
```
