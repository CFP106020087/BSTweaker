---
description: Create stable version backup with git tag, commit, push, and local backup
---
# 创建稳定版本备份流程

当代码达到一个稳定可用的状态时，使用此流程创建完整备份。

## 流程步骤

### 步骤 1: 创建 Git 回滚点 (Tag)
```powershell
# 使用当前时间戳创建 tag
git tag backup_$(Get-Date -Format 'yyyy-MM-dd_HHmm')
```

### 步骤 2: Commit 代码变更
```powershell
# 添加所有变更并提交
git add -A
git commit -m "feat: [功能描述]"
```

### 步骤 3: Push 到远程
```powershell
# 推送 tag 和代码
git push --tags
git push
```

### 步骤 4: 创建本地备份
```powershell
# 创建带时间戳和版本描述的备份文件夹
$timestamp = Get-Date -Format 'yyyy-MM-dd_HHmm'
$versionName = "[版本描述]"  # 例如: stable_fast_reload
$backupName = "backup_${timestamp}_${versionName}"

New-Item -ItemType Directory -Path $backupName -Force
Copy-Item -Path src -Destination "$backupName\src" -Recurse
Copy-Item -Path build.gradle -Destination $backupName
Write-Host "Backup created: $backupName"
```

## 命名规范

- **Git Tag**: `backup_YYYY-MM-DD_HHMM`
- **本地备份文件夹**: `backup_YYYY-MM-DD_HHMM_[版本描述]`
- **版本描述示例**: `stable_fast_reload`, `stable_spinning_fix`, `before_refactor`

## 恢复方法

### 从 Git Tag 恢复
```powershell
git checkout backup_2026-02-06_2348
```

### 从本地备份恢复
手动复制 `backup_xxx/src` 回项目目录。

## 注意事项

> [!WARNING]
> 执行 git 命令前必须先告知用户并等待确认！
