package com.mujmajnkraft.bstweaker.config;

/**
 * 材质定义，用于创建自定义 ToolMaterial
 */
public class MaterialDefinition {
    /** 材质唯一名称 */
    public String name;

    /** 挖掘等级 (0=木, 1=石, 2=铁, 3=钻石) */
    public int harvestLevel = 2;

    /** 耐久度 */
    public int durability = 250;

    /** 挖掘效率 */
    public float efficiency = 6.0f;

    /** 攻击伤害加成 */
    public float damage = 2.0f;

    /** 附魔能力 */
    public int enchantability = 14;

    /** 修复物品 (如 "minecraft:iron_ingot") */
    public String repairItem = "";

    public MaterialDefinition() {
    }

    public MaterialDefinition(String name) {
        this.name = name;
    }
}
