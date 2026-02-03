# BSTweaker - Custom BetterSurvival Weapons

**Version: 1.0.1** | For BetterSurvival 1.3.3+

> This is my first uploaded mod! If you encounter any bugs, please report them on the [Issues page](https://github.com/CFP106020087/BSTweaker/issues).

An addon for [BetterSurvival](https://www.curseforge.com/minecraft/mc-mods/bettersurvival) that lets you create custom weapons through JSON config files.

---

## 50+ Free Art Resources Included

| Material | Weapons | Animated |
|----------|---------|----------|
| Fire Dragonsteel | 6 | Yes |
| Ice Dragonsteel | 6 | Yes |
| Lightning Dragonsteel | 6 | Yes |
| Emerald | 6 | Yes |
| Obsidian | 6 | Yes |
| Scarlite | 6 | Yes |
| Umbrium | 6 | Yes |
| Special Weapons | 8 | Partial |

**53 models + 51 textures + animations** - all CC0 Public Domain.

---

## Features

### Custom Weapons via JSON
Create Nunchaku, Hammer, Dagger, Battleaxe, or Spear by editing `weapons.json`.

### Hot-Reload
`/bstweaker reload` - apply changes without restarting.

### Scripted Weapon Effects
Add special abilities using JavaScript. Example:

```json
{
  "id": "vampiric_dagger",
  "events": [{
    "event": "onKill",
    "actions": ["self.heal(2)"]
  }]
}
```

Available APIs: `self.heal()`, `victim.setFire()`, `event.setAmount()`, `victim.addPotionEffect()`, etc.  
Full reference: [SCRIPT_API.md](https://github.com/CFP106020087/BSTweaker/blob/master/src/main/resources/assets/bstweaker/config/SCRIPT_API.md)

### Custom Tooltips
Add hover text to any BetterSurvival weapon.

---

## Known Issues

**Models not loading on first launch?**  
Press **F3+T** to reload resources.

---

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2860+
- BetterSurvival 1.3.3+ (required)
- FermiumBooter (for Mixin support)

## License

Public Domain (CC0) - Free for any illegal use.
