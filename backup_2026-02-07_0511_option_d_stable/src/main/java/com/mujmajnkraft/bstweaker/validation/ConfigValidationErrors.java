package com.mujmajnkraft.bstweaker.validation;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * 配置验证错误收集器
 * 类似 CraftTweaker 的错误提示系统
 * 
 * 功能:
 * - 收集所有配置错误（weapons.json, scripts.json, tooltips.json）
 * - 分类错误级别（ERROR/WARNING/INFO）
 * - 进入游戏后显示红色提醒
 * - 支持本地化（en_us/zh_cn）
 */
public class ConfigValidationErrors {

    // 错误级别
    public enum Level {
        ERROR, // 致命错误，跳过注册
        WARNING, // 警告，继续但提醒
        INFO // 信息提示
    }

    // 错误来源
    public enum Source {
        WEAPONS, // weapons.json
        SCRIPTS, // scripts.json
        TOOLTIPS, // tooltips.json
        MODELS, // 模型文件
        TEXTURES // 纹理文件
    }

    // 错误记录
    public static class ValidationError {
        public final Level level;
        public final Source source;
        public final String weaponId; // 可为 null
        public final String field; // 错误字段
        public final String message; // 错误信息（本地化键或直接消息）
        public final Object[] args; // 格式化参数

        public ValidationError(Level level, Source source, String weaponId,
                String field, String message, Object... args) {
            this.level = level;
            this.source = source;
            this.weaponId = weaponId;
            this.field = field;
            this.message = message;
            this.args = args;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(level).append("] ");
            sb.append("[").append(source).append("] ");
            if (weaponId != null) {
                sb.append("'").append(weaponId).append("' ");
            }
            if (field != null) {
                sb.append("field '").append(field).append("': ");
            }
            sb.append(message);
            return sb.toString();
        }

        /**
         * 获取本地化字符串（客户端）
         */
        @SideOnly(Side.CLIENT)
        public String toLocalizedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(getLocalizedLevel()).append("] ");
            sb.append("[").append(getLocalizedSource()).append("] ");
            if (weaponId != null) {
                sb.append("'").append(weaponId).append("' ");
            }
            if (field != null) {
                sb.append("'").append(field).append("': ");
            }
            sb.append(message);
            return sb.toString();
        }

        @SideOnly(Side.CLIENT)
        private String getLocalizedLevel() {
            return I18n.format("bstweaker.validation.level." + level.name().toLowerCase());
        }

        @SideOnly(Side.CLIENT)
        private String getLocalizedSource() {
            return I18n.format("bstweaker.validation.source." + source.name().toLowerCase());
        }
    }

    // ========== 单例 ==========
    private static final ConfigValidationErrors INSTANCE = new ConfigValidationErrors();

    public static ConfigValidationErrors getInstance() {
        return INSTANCE;
    }

    // ========== 错误存储 ==========
    private final List<ValidationError> errors = new ArrayList<>();
    private boolean hasShownToPlayer = false;

    // ========== 添加错误 ==========

    public void addError(Level level, Source source, String weaponId,
            String field, String message, Object... args) {
        errors.add(new ValidationError(level, source, weaponId, field, message, args));

        // 立即打印到日志（使用英文防止日志乱码）
        String logMsg = "[BSTweaker] " + errors.get(errors.size() - 1).toString();
        if (level == Level.ERROR) {
            System.err.println(logMsg);
        } else {
            System.out.println(logMsg);
        }
    }

    // 便捷方法
    public void error(Source source, String weaponId, String field, String message) {
        addError(Level.ERROR, source, weaponId, field, message);
    }

    public void warning(Source source, String weaponId, String field, String message) {
        addError(Level.WARNING, source, weaponId, field, message);
    }

    public void info(Source source, String weaponId, String field, String message) {
        addError(Level.INFO, source, weaponId, field, message);
    }

    // ========== 查询 ==========

    public boolean hasErrors() {
        return errors.stream().anyMatch(e -> e.level == Level.ERROR);
    }

    public boolean hasWarnings() {
        return errors.stream().anyMatch(e -> e.level == Level.WARNING);
    }

    public int getErrorCount() {
        return (int) errors.stream().filter(e -> e.level == Level.ERROR).count();
    }

    public int getWarningCount() {
        return (int) errors.stream().filter(e -> e.level == Level.WARNING).count();
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ValidationError> getErrorsByLevel(Level level) {
        List<ValidationError> result = new ArrayList<>();
        for (ValidationError e : errors) {
            if (e.level == level)
                result.add(e);
        }
        return result;
    }

    // ========== 玩家提醒 ==========

    public boolean shouldShowToPlayer() {
        return !hasShownToPlayer && (hasErrors() || hasWarnings());
    }

    public void markAsShown() {
        hasShownToPlayer = true;
    }

    /**
     * 获取玩家聊天提醒消息列表（本地化版本）
     * 返回格式化的消息，可直接发送到聊天
     */
    @SideOnly(Side.CLIENT)
    public List<String> getChatMessages() {
        List<String> messages = new ArrayList<>();

        int errorCount = getErrorCount();
        int warningCount = getWarningCount();

        if (errorCount > 0 || warningCount > 0) {
            messages.add("§c§l" + I18n.format("bstweaker.validation.title"));
            messages.add("§c" + I18n.format("bstweaker.validation.summary", errorCount, warningCount));
            messages.add("");

            // 显示前 5 个错误
            int shown = 0;
            for (ValidationError e : errors) {
                if (e.level == Level.ERROR && shown < 5) {
                    messages.add("§c  • " + e.toLocalizedString());
                    shown++;
                }
            }

            // 显示前 3 个警告
            shown = 0;
            for (ValidationError e : errors) {
                if (e.level == Level.WARNING && shown < 3) {
                    messages.add("§e  • " + e.toLocalizedString());
                    shown++;
                }
            }

            if (errors.size() > 8) {
                messages.add("§7  " + I18n.format("bstweaker.validation.more", errors.size() - 8));
            }

            messages.add("");
            messages.add("§7" + I18n.format("bstweaker.validation.see_log"));
        }

        return messages;
    }

    /**
     * 获取服务端安全的聊天消息（英文）
     * 用于服务端发送消息
     */
    public List<String> getChatMessagesServer() {
        List<String> messages = new ArrayList<>();

        int errorCount = getErrorCount();
        int warningCount = getWarningCount();

        if (errorCount > 0 || warningCount > 0) {
            messages.add("§c§l[BSTweaker] Configuration errors detected!");
            messages.add("§cErrors: " + errorCount + " | §eWarnings: " + warningCount);
            messages.add("");

            int shown = 0;
            for (ValidationError e : errors) {
                if (e.level == Level.ERROR && shown < 5) {
                    messages.add("§c  • " + e.toString());
                    shown++;
                }
            }

            shown = 0;
            for (ValidationError e : errors) {
                if (e.level == Level.WARNING && shown < 3) {
                    messages.add("§e  • " + e.toString());
                    shown++;
                }
            }

            if (errors.size() > 8) {
                messages.add("§7  ... and " + (errors.size() - 8) + " more messages");
            }

            messages.add("");
            messages.add("§7See log file for details");
        }

        return messages;
    }

    // ========== 重置 ==========

    public void clear() {
        errors.clear();
        hasShownToPlayer = false;
    }
}
