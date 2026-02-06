---
description: BSTweaker 开发安全规则
---

# 安全规则

## 1. Git 操作限制

### 绝对禁止私自执行：
- `git reset`
- `git revert`
- `git checkout` (切换分支或恢复文件)
- `git stash`
- 任何形式的回滚操作

### 必须先告知用户并等待确认后才能执行：
- `git commit`
- `git push`
- `git add`
- 任何 git 命令

## 2. .gitignore 规则

### 绝对禁止添加：
- `bstweaker/` (会忽略所有源文件)
- 任何可能匹配 `src/main/java` 下文件的规则

### 添加 .gitignore 规则前必须：
1. 告知用户要添加什么
2. 解释为什么需要
3. 等待用户确认

## 3. 代码修改规则

### 修改前必须：
1. 说明要修改哪个文件
2. 说明修改内容
3. 等待用户确认（除非用户明确说"直接修"）

### Fallback 规则：
- **不要疯狂添加 fallback** 除非有明确必要
- `mujmajnkraftsbettersurvival` 是主命名空间，资源应该在这个空间下工作
- 添加 fallback 前先确认是否真的需要

## 4. 测试流程

每次请用户测试时，先执行清理：
```powershell
# 清理 run 目录（保留 mods 和 config）
Get-ChildItem -Path run -Exclude mods,config | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
# 运行客户端
./gradlew runClient
```

## 5. 违规后果

如果违反以上规则，用户将降低 AI 权限。
