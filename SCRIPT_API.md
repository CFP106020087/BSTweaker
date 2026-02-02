# BSTweaker 脚本 API 参考

## 事件类型

| 事件 | 触发时机 | 可用变量 |
|------|---------|---------|
| `onHit` | 攻击命中 | self, victim, event |
| `onKill` | 击杀敌人 | self, victim |
| `whenHeld` | 主手持有 | self |

---

## self / victim API

```javascript
// 血量
self.getHealth()
self.setHealth(10)
self.heal(2)
self.getMaxHealth()

// 无敌帧
victim.getHurtResistantTime()
victim.setHurtResistantTime(0)  // 清空

// 药水效果
self.getPotionEffect('poison')      // 返回 PotionEffect 或 null
self.addPotionEffect('speed', 200, 1)  // id, 时长, 等级
self.removePotionEffect('poison')
self.hasPotionEffect('wither')      // 返回 true/false

// 其他
self.setFire(5)     // 点燃5秒
self.isBurning()
self.isInWater()
```

---

## event API (onHit)

```javascript
event.getAmount()           // 获取伤害值
event.setAmount(10)         // 设置伤害值
event.cancel()              // 取消事件
```

---

## PotionEffect API

```javascript
var eff = self.getPotionEffect('poison');
if (eff != null) {
    eff.getAmplifier()      // 获取等级
    eff.getDuration()       // 获取持续时间
    eff.setAmplifier(2)     // 设置等级
}
```

---

## 示例

### 清空无敌帧
```json
"actions": ["victim.setHurtResistantTime(0)"]
```

### 伤害翻倍
```json
"actions": ["event.setAmount(event.getAmount() * 2)"]
```

### 压制药水效果到指定等级
```json
"actions": [
  "var eff = self.getPotionEffect('poison');",
  "if (eff != null && eff.getAmplifier() > 2) { eff.setAmplifier(2); }"
]
```

### 击杀回血
```json
"actions": ["self.heal(2)"]
```
