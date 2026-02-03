# Custom Assets - Original Artwork

These are custom artwork assets created specifically for BSTweaker, separate from the original BetterSurvival mod resources.

## Directory Structure

```
custom_assets/
├── models/item/     (63 files) - Item model JSONs
├── textures/items/  (93 files) - Texture PNGs and MCMETAs  
└── lang/            (1 file)   - zh_cn.lang
```

---

## Models (63 files)

### BSTweaker Custom Weapons (12 models)
| Material | Normal | Spinning |
|----------|--------|----------|
| Chaos Ingot | itembstweaker_chaosingotnunchaku.json | itembstweaker_chaosingotnunchakuspinning.json |
| Evil Ingot | itembstweaker_evilingotnunchaku.json | itembstweaker_evilingotnunchakuspinning.json |
| Fiery Ingot | itembstweaker_fieryingotnunchaku.json | itembstweaker_fieryingotnunchakuspinning.json |
| Netherite Ingot | itembstweaker_netheriteingotnunchaku.json | itembstweaker_netheriteingotnunchakuspinning.json |
| Steeleaf Ingot | itembstweaker_steeleafingotnunchaku.json | itembstweaker_steeleafingotnunchakuspinning.json |

### Emerald Weapons (6 models)
- itememeraldbattleaxe.json, itememeralddagger.json, itememeraldhammer.json
- itememeraldnunchaku.json, itememeraldnunchakuspinning.json, itememeraldspear.json

### Fire Dragonsteel Weapons (7 models)
- itemfiredragonsteel battleaxe.json, itemfiredragonsteel dagger.json
- itemfiredragonsteel hammer.json, itemfiredragonsteel nunchaku.json
- itemfiredragonsteel nunchakuspinning.json, itemfiredragonsteel spear.json
- itemfiredragonsteelbattleaxe.json

### Ice Dragonsteel Weapons (8 models)
- itemicedragonsteel battleaxe.json, itemicedragonsteel dagger.json
- itemicedragonsteel hammer.json, itemicedragonsteel nunchaku.json
- itemicedragonsteel nunchakuspinning.json, itemicedragonsteel spear.json
- itemicedragon nunchaku.json, itemicedragon nunchakuspinging.json

### Lightning Dragonsteel Weapons (6 models)
- itemlightingdragonsteel battleaxe.json, itemlightingdragonsteel dagger.json
- itemlightingdragonsteel hammer.json, itemlightingdragonsteel nunchaku.json
- itemlightingdragonsteel nunchakuspinning.json, itemlightingdragonsteel spear.json

### Obsidian Weapons (6 models)
- itemobsidian battleaxe.json, itemobsidian dagger.json
- itemobsidian hammer.json, itemobsidian nunchaku.json
- itemobsidian nunchakuspinning.json, itemobsidian spear.json

### Scarlite Weapons (6 models)
- itemscarlitebattleaxe.json, itemscarlitedagger.json, itemscarlitehammer.json
- itemscarlitenunchaku.json, itemscarlitenunchakuspinning.json, itemscarlitespear.json

### Umbrium Weapons (6 models)
- itemumbrium battleaxe.json, itemumbrium dagger.json
- itemumbrium hammer.json, itemumbrium nunchaku.json
- itemumbrium nunchakuspinning.json, itemumbrium spear.json

### Special Weapons (6 models)
- deathdagger_death.json
- hungerhammer.json
- itemrefineobnunchaku.json, itemrefineobnunchakuspinning.json
- itemsentientnunchaku.json, itemsentientnunchakuspinning.json
- itemwarspear.json
- itemill battleaxe.json

---

## Textures (93 files)

Corresponding PNG textures and MCMETA animation files for all models above.

### Animation Files (.mcmeta)
Many textures include animation metadata files for spinning effects:
- *nunchakuspinning.png.mcmeta - Spinning animation frames
- *battleaxe.png.mcmeta - Animated battleaxe effects (dragonsteel)
- *dagger.png.mcmeta - Animated dagger effects
- etc.

---

## Language Files

### zh_cn.lang
Chinese localization for custom weapons.

---

## Usage

To use these assets in a mod:
1. Copy the relevant files to your mod's `assets/<modid>/` directory
2. Update the namespace references in model JSONs if needed
3. Register items with matching registry names

---

*Last updated: 2026-02-03*
