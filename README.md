# BSTweaker

> **My first mod uploaded to CurseForge!** 🎉
>
> ⚠️ **Disclaimer:** I'm still learning mod development. If you encounter any bugs or issues, please feel free to report them on the [Issues page](https://github.com/CFP106020087/BSTweaker/issues). All feedback is appreciated!

A lightweight addon for [BetterSurvival](https://www.curseforge.com/minecraft/mc-mods/bettersurvival) that enables **JSON-based custom weapons** with hot-reload support.

---

## ✨ Core Features

### 🗡️ Custom BetterSurvival Weapons
Create new weapons that inherit all original BetterSurvival weapon mechanics via JSON config.

**Supported weapon types:**
| Type | Class | Special Mechanics |
|------|-------|-------------------|
| `nunchaku` | ItemNunchaku | Hold right-click to spin, combo hits |
| `hammer` | ItemHammer | Ground slam, stun effect |
| `dagger` | ItemDagger | 2x backstab damage |
| `battleaxe` | ItemBattleAxe | High damage |
| `spear` | ItemSpear | Throwable, +1 reach |

**Example weapon definition** (`config/bstweaker/weapons.json`):
```json
{
  "weapons": [{
    "id": "obsidian_hammer",
    "type": "hammer",
    "material": {
      "name": "Obsidian",
      "harvestLevel": 3,
      "durability": 1500,
      "efficiency": 4.0,
      "damage": 5.0,
      "enchantability": 8
    },
    "damageModifier": 1.5,
    "speedModifier": 1.4
  }]
}
```

Weapons are created using reflection to instantiate the original BetterSurvival item classes, ensuring full enchantment and mechanic compatibility.

---

### ⚡ Hot-Reload System
Edit configs while the game is running. No restart needed!

**Command:** `/bstweaker reload`

**What gets reloaded:**
1. `tooltips.json` - Display names and hover text
2. `scripts.json` - Weapon special effects
3. `config/bstweaker/textures/` - Texture files (.png, .mcmeta)
4. `config/bstweaker/lang/` - Language files

**Technical implementation:**
- Rescans `DynamicResourcePack` resource maps
- Deletes cached GPU textures via `TextureManager.deleteTexture()`
- Rebuilds texture atlas via `TextureMap.loadTextureAtlas()`
- Rebuilds model cache via `ItemModelMesher.rebuildCache()`

> ⚠️ **Note:** New weapons require game restart (item registration happens during mod loading).

---

### 📝 Custom Tooltips
Add display names and hover text to **any** weapon - including existing BetterSurvival weapons.

**Config:** `config/bstweaker/tooltips.json`
```json
{
  "tooltips": [{
    "id": "diamonddagger",
    "displayName": "§bSharp Diamond Dagger",
    "tooltip": [
      "§7A razor-sharp blade",
      "§e◆ Backstab: 2x damage"
    ]
  }]
}
```

**Features:**
- `§` color codes supported (§c=red, §6=gold, §l=bold, etc.)
- `@key` prefix for lang file translation (e.g., `@bstweaker.weapon.desc`)
- Script `_comment` fields auto-display as purple effect descriptions

---

### ⚔️ JavaScript Weapon Effects
Define special abilities using JavaScript syntax executed by Nashorn engine.

**Config:** `config/bstweaker/scripts.json`
```json
{
  "scripts": [{
    "id": "vampiric_dagger",
    "events": [{
      "_comment": "Lifesteal: Heal 2 HP on kill",
      "event": "onKill",
      "actions": ["self.heal(2)"]
    }]
  }]
}
```

**Event types:**
| Event | Trigger | Available Variables |
|-------|---------|---------------------|
| `onHit` | Attack lands | self, victim, event |
| `onKill` | Enemy killed | self, victim |
| `onHurt` | You take damage | self, victim (attacker), event |
| `whenHeld` | Holding weapon | self (every 5 ticks) |

**Script API:**
```javascript
// Health
self.heal(2)
self.setHealth(10)
self.getMaxHealth()

// Invincibility frames
victim.setHurtResistantTime(0)  // Reset for rapid hits

// Potion effects
victim.addPotionEffect("poison", 100, 1)  // id, duration, amplifier
self.removePotionEffect("wither")
self.hasPotionEffect("speed")  // returns true/false

// Damage manipulation (onHit/onHurt only)
event.getAmount()
event.setAmount(event.getAmount() * 2)  // Double damage
event.cancel()  // Cancel the hit

// Misc
victim.setFire(5)  // Ignite for 5 seconds
self.isBurning()
self.isInWater()
```

---

### 🎨 Dynamic Resource Pack
Place textures directly in `config/bstweaker/` - no resource pack needed!

**Directory structure:**
```
config/bstweaker/
├── textures/           # Place .png files here
│   ├── myweapon.png
│   └── myweapon.png.mcmeta  # Animation (optional)
├── models/             # Custom models (optional)
│   └── myweapon.json
└── lang/               # Language files
    ├── en_us.lang
    └── zh_cn.lang
```

**Auto-features:**
- Auto-generates item models if not provided (using `item/handheld` parent)
- Auto-converts BetterSurvival model format (`*_normal.json` → `*.json`)
- Supports animated textures via `.mcmeta` files
- Resources served via custom `IResourcePack` implementation

---

## 📦 Installation

**Requirements:**
- Minecraft 1.12.2
- Forge 14.23.5+
- [BetterSurvival](https://www.curseforge.com/minecraft/mc-mods/bettersurvival)

**Steps:**
1. Drop `bstweaker-x.x.x.jar` into `mods/`
2. Launch game → configs auto-generate in `config/bstweaker/`
3. Edit JSON files, run `/bstweaker reload`

---

## 📁 Config Files Reference

| File | Purpose | Hot-Reload |
|------|---------|------------|
| `weapons.json` | Define new weapons (id, type, material, stats) | ❌ Restart required |
| `tooltips.json` | Display names and hover tooltips | ✅ Yes |
| `scripts.json` | JavaScript special effects | ✅ Yes |
| `SCRIPT_API.md` | API reference documentation | - |
| `textures/*.png` | Weapon textures | ✅ Yes |
| `models/*.json` | Custom item models | ✅ Yes |
| `lang/*.lang` | Localization | ✅ Yes |

---

## 🛠️ Building from Source

```bash
./gradlew build
# Output: build/libs/bstweaker-x.x.x.jar
```

---

## 📄 License

MIT License - Feel free to use in modpacks!

---

*Made with ❤️ for the Minecraft modding community*
