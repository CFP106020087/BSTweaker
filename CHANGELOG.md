# BSTweaker Changelog

## v1.0.2 (2026-02-05)

### Immediate Hot Reload

- `/bstweaker reload` instantly updates textures and models in-game
- No resource pack reload, no game restart needed
- ~20ms for standard textures (vs ~2-3s for F3+T)
- Config option `enableFastReload` to disable if needed
- Note: Complex animated textures still require F3+T

### Full Auto-Generation

- Models, lang files, and tooltips generated automatically
- Only need: `weapons.json` + texture files
- Two lines of JSON to create a weapon

### 5 Emerald Example Weapons

- Dagger, Hammer, Spear, Battleaxe, Nunchaku
- Ready-to-use templates in `weapons.json`

### Planned Features

- More scripted effects
- Silent hot reload for weapon upgrade effects
- Special weapons with unique abilities
- Custom attributes (e.g. chance to not consume spear when thrown)
- Transformable weapons with multiple modes
- Mixed ability custom weapons
- In-game GUI for weapon creation (no JSON editing)
- API for other mods to register weapons
- Per-weapon ability tuning (e.g. disarm chance)
- GeckoLib support for advanced models

---

## v1.0.1

- Initial release
- JSON weapon configuration
- Dynamic resource pack
- Scripted effects
