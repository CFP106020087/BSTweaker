# BSTweaker Script API Reference / 脚本 API 参考

## Event Types / 事件类型

| Event / 事件 | Trigger / 触发时机 | Variables / 可用变量 |
|--------------|-------------------|---------------------|
| `onHit` | Attack hits / 攻击命中 | self, victim, event |
| `onKill` | Kill enemy / 击杀敌人 | self, victim |
| `onHurt` | Taking damage / 受到伤害 | self, victim (attacker), event |
| `whenHeld` | Holding weapon / 主手持有 | self |

---

## self / victim API

```javascript
// Health / 血量
self.getHealth()
self.setHealth(10)
self.heal(2)
self.getMaxHealth()

// Invincibility frames / 无敌帧
victim.getHurtResistantTime()
victim.setHurtResistantTime(0)  // Reset / 清空

// Potion effects / 药水效果
self.getPotionEffect('poison')         // Returns PotionEffect or null / 返回 PotionEffect 或 null
self.addPotionEffect('speed', 200, 1)  // id, duration, amplifier / id, 时长, 等级
self.removePotionEffect('poison')
self.hasPotionEffect('wither')         // Returns true/false / 返回 true/false

// Other / 其他
self.setFire(5)     // Ignite for 5 seconds / 点燃5秒
self.isBurning()
self.isInWater()
```

---

## event API (onHit / onHurt)

```javascript
event.getAmount()           // Get damage value / 获取伤害值
event.setAmount(10)         // Set damage value / 设置伤害值
event.cancel()              // Cancel event / 取消事件
```

---

## PotionEffect API

```javascript
var eff = self.getPotionEffect('poison');
if (eff != null) {
    eff.getAmplifier()      // Get amplifier level / 获取等级
    eff.getDuration()       // Get remaining duration / 获取持续时间
    eff.setAmplifier(2)     // Set amplifier level / 设置等级
}
```

---

## Examples / 示例

### Reset invincibility frames / 清空无敌帧
```json
"actions": ["victim.setHurtResistantTime(0)"]
```

### Double damage / 伤害翻倍
```json
"actions": ["event.setAmount(event.getAmount() * 2)"]
```

### Cap potion effect level / 压制药水效果到指定等级
```json
"actions": [
  "var eff = self.getPotionEffect('poison');",
  "if (eff != null && eff.getAmplifier() > 2) { eff.setAmplifier(2); }"
]
```

### Heal on kill / 击杀回血
```json
"actions": ["self.heal(2)"]
```

### Ignite on hit / 命中点燃
```json
"actions": ["victim.setFire(3)"]
```

### Lifesteal / 吸血
```json
"actions": ["self.heal(event.getAmount() * 0.1)"]
```
