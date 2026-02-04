# BSTweaker TODO

## 📝 文档工作（优先）

- [ ] 完善 README.md - CurseForge 发布描述
- [ ] 更新 SCRIPT_API.md - 脚本系统 API 文档
- [ ] 创建 CONFIG_GUIDE.md - 配置文件指南
  - [ ] weapons.json 字段说明
  - [ ] tooltips.json 使用方法
  - [ ] scripts.json 事件列表
  - [ ] 模型文件命名规范（三层结构）

## ✅ 已完成的基础功能

- [x] 武器注册系统 (TweakerWeaponInjector)
- [x] 模型自动注册 (ClientEventHandler)
- [x] 资源注入系统 (ResourceInjector)
- [x] Base Model 自动生成（三层 Override 结构）
- [x] 热重载命令 `/bstweaker reload`
- [x] Tooltip 系统
- [x] 脚本效果系统
- [x] Always 谓词热重载支持

## 🎯 接下来的有趣功能

- [ ] **GeckoLib 支持** - 3D 武器模型
- [ ] **自定义附魔** - 配置化附魔效果
- [ ] **NBT 修改器** - 动态武器属性
- [ ] **材质包兼容** - 更好的热重载体验
- [ ] **GUI 编辑器** - 可视化武器配置

---

*回滚点: b721323 (POLISH: Consolidate ResourceInjector)*
