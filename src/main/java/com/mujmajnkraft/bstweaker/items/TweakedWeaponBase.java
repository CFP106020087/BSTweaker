package com.mujmajnkraft.bstweaker.items;

import java.util.List;

import com.google.common.collect.Multimap;
import com.mujmajnkraft.bettersurvival.BetterSurvival;
import com.mujmajnkraft.bettersurvival.integration.InFCompat;
import com.mujmajnkraft.bettersurvival.integration.InFLightningForkCompat;
import com.mujmajnkraft.bettersurvival.integration.SoManyEnchantmentsCompat;
import com.mujmajnkraft.bstweaker.config.MaterialDefinition;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentSweepingEdge;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

/**
 * BSTweaker 自定义武器基类
 * 从 ItemCustomWeapon 复制逻辑，但允许自定义伤害和攻速修正
 */
public class TweakedWeaponBase extends Item {

    private final float attackDamage;
    private final double attackSpeed;
    private final ToolMaterial material;

    public TweakedWeaponBase(ToolMaterial material, float damageModifier, float speedModifier) {
        this.material = material;
        this.maxStackSize = 1;
        this.attackDamage = (3.0F + material.getAttackDamage()) * damageModifier;
        this.attackSpeed = -2.4000000953674316 * speedModifier;
        this.setMaxDamage(material.getMaxUses());
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    public ToolMaterial getMaterial() {
        return material;
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return true;
    }

    @Override
    public int getItemEnchantability() {
        return this.material.getEnchantability();
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos,
            EntityLivingBase entityLiving) {
        if ((double) state.getBlockHardness(worldIn, pos) != 0.0D) {
            stack.damageItem(2, entityLiving);
        }
        return true;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        ItemStack mat = this.material.getRepairItemStack();
        if (!mat.isEmpty() && OreDictionary.itemMatches(mat, repair, false))
            return true;
        if (OreDictionary.doesOreNameExist("ingot" + this.material.name())) {
            for (ItemStack stack : OreDictionary.getOres("ingot" + this.material.name())) {
                if (OreDictionary.itemMatches(stack, repair, false)) {
                    return true;
                }
            }
        }
        return super.getIsRepairable(toRepair, repair);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 子类可以覆盖添加特定信息
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", this.attackDamage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", this.attackSpeed, 0));
        }
        return multimap;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (!(enchantment instanceof EnchantmentSweepingEdge) &&
                (enchantment.type == EnumEnchantmentType.WEAPON ||
                        (BetterSurvival.isSMELoaded
                                && SoManyEnchantmentsCompat.isWeaponSMEEnchant(enchantment.type)))) {
            return true;
        } else {
            return super.canApplyAtEnchantingTable(stack, enchantment);
        }
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }
}
