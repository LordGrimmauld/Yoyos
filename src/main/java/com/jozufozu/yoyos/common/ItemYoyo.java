/*
 * Copyright (c) 2018 Jozsef Augusztiny
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jozufozu.yoyos.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.jozufozu.yoyos.Yoyos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class ItemYoyo extends Item implements IYoyo
{
    private final float attackDamage;
    protected final ToolMaterial material;
    protected final BiFunction<World, EntityPlayer, EntityYoyo> factory;
    protected RenderOrientation renderOrientation = RenderOrientation.Vertical;

    protected EntityInteraction entityInteraction = ItemYoyo::attackEntity;
    protected BlockInteraction blockInteraction;

    public ItemYoyo(String name, ToolMaterial material)
    {
        this(name, material, EntityYoyo::new);
    }

    public ItemYoyo(String name, ToolMaterial material, BiFunction<World, EntityPlayer, EntityYoyo> entityFactory)
    {
        this.material = material;
        this.maxStackSize = 1;
        this.setMaxDamage(material.getMaxUses());
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.attackDamage = 3.0F + material.getAttackDamage();
        this.factory = entityFactory;
        
        this.setUnlocalizedName(String.format("%s.%s", Yoyos.MODID, name));
        this.setRegistryName(Yoyos.MODID, name);

        this.addPropertyOverride(new ResourceLocation(Yoyos.MODID, "thrown"), (stack, worldIn, entityIn) -> entityIn instanceof EntityBat ? 1 : 0);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }
    
    @Override
    public Set<String> getToolClasses(ItemStack stack)
    {
        return ImmutableSet.of("yoyo");
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment)
    {
        return enchantment == Yoyos.COLLECTING || enchantment.type == EnumEnchantmentType.ALL || enchantment.type == EnumEnchantmentType.WEAPON;
    }

    @Override
    public boolean hasEffect(ItemStack stack)
    {
        return super.hasEffect(stack) || this == Yoyos.CREATIVE_YOYO;
    }
    
    public int getItemEnchantability()
    {
        return this.material.getEnchantability();
    }
    
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair)
    {
        ItemStack mat = this.material.getRepairItemStack();
        return OreDictionary.itemMatches(mat, repair, false) || super.getIsRepairable(toRepair, repair);
    }

    public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos, EntityLivingBase entityLiving)
    {
        if ((double)state.getBlockHardness(worldIn, pos) != 0.0D)
        {
            this.damageItem(stack, 2, entityLiving);
        }

        return true;
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker)
    {
        this.damageItem(stack, 1, attacker);
        return true;
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        ItemStack itemStack = playerIn.getHeldItem(hand);
        if (!worldIn.isRemote && !EntityYoyo.CASTERS.containsKey(playerIn))
        {
            if (itemStack.getItemDamage() <= itemStack.getMaxDamage() || this == Yoyos.CREATIVE_YOYO)
            {
                EntityYoyo yoyo = factory.apply(worldIn, playerIn);
                worldIn.spawnEntity(yoyo);

                worldIn.playSound(null, yoyo.posX, yoyo.posY, yoyo.posZ, Yoyos.YOYO_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

                playerIn.addExhaustion(0.05F);
            }
        }
        
        return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
    }
    
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(I18n.format("yoyos.info.weight.name", getWeight(stack)));
        tooltip.add(I18n.format("yoyos.info.length.name", getLength(stack)));
    
        int duration = getDuration(stack);
        Object arg = (duration < 0 ? I18n.format("stat.yoyo.infinite.name") : ((float) duration) / 20F);
        tooltip.add(I18n.format("yoyos.info.duration.name", arg));
        
        if (stack.isItemEnchanted())
            tooltip.add("");
    }
    
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot, ItemStack stack)
    {
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
    
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND || equipmentSlot == EntityEquipmentSlot.OFFHAND)
        {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", getAttackDamage(stack), 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.4000000953674316D, 0));
        }
    
        return multimap;
    }

    public ItemYoyo setBlockInteraction(BlockInteraction blockInteraction) {
        this.blockInteraction = blockInteraction;
        return this;
    }

    public ItemYoyo setEntityInteraction(EntityInteraction entityInteraction) {
        this.entityInteraction = entityInteraction;
        return this;
    }

    public ItemYoyo setRenderOrientation(RenderOrientation renderOrientation) {
        this.renderOrientation = renderOrientation;
        return this;
    }

    @Override
    public float getAttackDamage(ItemStack yoyo)
    {
        if (this == Yoyos.SHEAR_YOYO) return ModConfig.vanillaYoyos.shearYoyo.damage;
        if (this == Yoyos.STICKY_YOYO) return ModConfig.vanillaYoyos.stickyYoyo.damage;
        if (this == Yoyos.DIAMOND_YOYO) return ModConfig.vanillaYoyos.diamondYoyo.damage;
        if (this == Yoyos.GOLD_YOYO) return ModConfig.vanillaYoyos.goldYoyo.damage;
        if (this == Yoyos.IRON_YOYO) return ModConfig.vanillaYoyos.ironYoyo.damage;
        if (this == Yoyos.STONE_YOYO) return ModConfig.vanillaYoyos.stoneYoyo.damage;
        if (this == Yoyos.WOODEN_YOYO) return ModConfig.vanillaYoyos.woodenYoyo.damage;
        if (this == Yoyos.CREATIVE_YOYO) return ModConfig.vanillaYoyos.creativeYoyo.damage;

        return attackDamage;
    }

    @Override
    public float getWeight(ItemStack yoyo)
    {
        if (this == Yoyos.SHEAR_YOYO) return ModConfig.vanillaYoyos.shearYoyo.weight;
        if (this == Yoyos.STICKY_YOYO) return ModConfig.vanillaYoyos.stickyYoyo.weight;
        if (this == Yoyos.DIAMOND_YOYO) return ModConfig.vanillaYoyos.diamondYoyo.weight;
        if (this == Yoyos.GOLD_YOYO) return ModConfig.vanillaYoyos.goldYoyo.weight;
        if (this == Yoyos.IRON_YOYO) return ModConfig.vanillaYoyos.ironYoyo.weight;
        if (this == Yoyos.STONE_YOYO) return ModConfig.vanillaYoyos.stoneYoyo.weight;
        if (this == Yoyos.WOODEN_YOYO) return ModConfig.vanillaYoyos.woodenYoyo.weight;
        if (this == Yoyos.CREATIVE_YOYO) return ModConfig.vanillaYoyos.creativeYoyo.weight;
        
        return 1.0f;
    }
    
    @Override
    public float getLength(ItemStack yoyo)
    {
        if (this == Yoyos.SHEAR_YOYO) return ModConfig.vanillaYoyos.shearYoyo.length;
        if (this == Yoyos.STICKY_YOYO) return ModConfig.vanillaYoyos.stickyYoyo.length;
        if (this == Yoyos.DIAMOND_YOYO) return ModConfig.vanillaYoyos.diamondYoyo.length;
        if (this == Yoyos.GOLD_YOYO) return ModConfig.vanillaYoyos.goldYoyo.length;
        if (this == Yoyos.IRON_YOYO) return ModConfig.vanillaYoyos.ironYoyo.length;
        if (this == Yoyos.STONE_YOYO) return ModConfig.vanillaYoyos.stoneYoyo.length;
        if (this == Yoyos.WOODEN_YOYO) return ModConfig.vanillaYoyos.woodenYoyo.length;
        if (this == Yoyos.CREATIVE_YOYO) return ModConfig.vanillaYoyos.creativeYoyo.length;
    
        return 1.0f;
    }
    
    @Override
    public int getDuration(ItemStack yoyo)
    {
        if (this == Yoyos.SHEAR_YOYO) return ModConfig.vanillaYoyos.shearYoyo.duration;
        if (this == Yoyos.STICKY_YOYO) return ModConfig.vanillaYoyos.stickyYoyo.duration;
        if (this == Yoyos.DIAMOND_YOYO) return ModConfig.vanillaYoyos.diamondYoyo.duration;
        if (this == Yoyos.GOLD_YOYO) return ModConfig.vanillaYoyos.goldYoyo.duration;
        if (this == Yoyos.IRON_YOYO) return ModConfig.vanillaYoyos.ironYoyo.duration;
        if (this == Yoyos.STONE_YOYO) return ModConfig.vanillaYoyos.stoneYoyo.duration;
        if (this == Yoyos.WOODEN_YOYO) return ModConfig.vanillaYoyos.woodenYoyo.duration;
        if (this == Yoyos.CREATIVE_YOYO) return ModConfig.vanillaYoyos.creativeYoyo.duration;
    
        return 10;
    }
    
    @Override
    public int getAttackSpeed(ItemStack yoyo)
    {
        return 10;
    }
    
    @Override
    public RenderOrientation renderOrientation(ItemStack yoyo)
    {
        return renderOrientation;
    }
    
    @Override
    public int collecting(ItemStack yoyo)
    {
        if (this == Yoyos.CREATIVE_YOYO) return Integer.MAX_VALUE / 2;
        return EnchantmentHelper.getEnchantmentLevel(Yoyos.COLLECTING, yoyo);
    }
    
    @Override
    public void damageItem(ItemStack yoyo, int amount, EntityLivingBase player)
    {
        yoyo.damageItem(amount, player);
    }

    @Override
    public void entityInteraction(ItemStack yoyo, EntityPlayer player, EnumHand hand, EntityYoyo yoyoEntity, Entity targetEntity)
    {
        entityInteraction.doInteraction(this, yoyo, player, hand, yoyoEntity, targetEntity);
    }

    @Override
    public boolean interactsWithBlocks(ItemStack yoyo)
    {
        return blockInteraction == null;
    }

    @Override
    public void blockInteraction(ItemStack yoyo, EntityPlayer player, World world, BlockPos pos, IBlockState state, Block block, EntityYoyo yoyoEntity)
    {
        blockInteraction.doInteraction(this, yoyo, player, world, pos, state, block, yoyoEntity);
    }

    public static void garden(IYoyo yoyo, ItemStack yoyoStack, EntityPlayer player, World world, BlockPos pos, IBlockState state, Block block, EntityYoyo yoyoEntity)
    {
        if (block instanceof IShearable)
        {
            IShearable shearable = (IShearable) block;
            if (shearable.isShearable(yoyoStack, world, pos))
            {
                List<ItemStack> drops = shearable.onSheared(yoyoStack, world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, yoyoStack));
                for (ItemStack drop : drops)
                {
                    if (yoyoEntity.isCollecting())
                    {
                        yoyoEntity.collectDrop(drop);
                    }
                    else
                    {
                        float f = 0.7F;
                        double d = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                        double d1 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                        double d2 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                        EntityItem entityitem = new EntityItem(world, (double) pos.getX() + d, (double) pos.getY() + d1, (double) pos.getZ() + d2, drop);
                        entityitem.setDefaultPickupDelay();
                        player.world.spawnEntity(entityitem);
                    }
                }

                if (!player.isCreative()) yoyo.damageItem(yoyoStack, 1, player);

                player.addStat(StatList.getBlockStats(block));

                world.playSound(null, pos, block.getSoundType(state, world, pos, yoyoEntity).getBreakSound(), SoundCategory.BLOCKS, 1, 1);
                block.removedByPlayer(state, world, pos, player, true);
                world.playEvent(2001, pos.toImmutable(), Block.getStateId(state));
                return;
            }
        }

        if (block instanceof BlockBush)
        {
            if (!player.isCreative()) yoyo.damageItem(yoyoStack, 1, player);

            if (block.removedByPlayer(state, world, pos, player, true))
            {
                world.playSound(null, pos, block.getSoundType(state, world, pos, yoyoEntity).getBreakSound(), SoundCategory.BLOCKS, 1, 1);
                world.playEvent(2001, pos.toImmutable(), Block.getStateId(state));
                yoyoEntity.markBlockForDropGathering(pos);
                block.harvestBlock(world, player, pos, state, world.getTileEntity(pos), yoyoStack);
                block.breakBlock(world, pos, state);
            }
        }
    }

    public static void harvest(IYoyo yoyo, ItemStack yoyoStack, EntityPlayer player, World world, BlockPos pos, IBlockState state, Block block, EntityYoyo yoyoEntity)
    {
        if (block instanceof BlockCrops)
        {
            if (state.getValue(BlockCrops.AGE) == 7)
            {
                world.playSound(null, pos, block.getSoundType(state, world, pos, yoyoEntity).getBreakSound(), SoundCategory.BLOCKS, 1, 1);
                world.playEvent(2001, pos.toImmutable(), Block.getStateId(state));
                yoyoEntity.markBlockForDropGathering(pos);
                block.harvestBlock(world, player, pos, state, world.getTileEntity(pos), yoyoStack);
                world.setBlockState(pos, state.withProperty(BlockCrops.AGE, 0));
            }
        }
    }

    public static void shearEntity(IYoyo yoyo, ItemStack yoyoStack, EntityPlayer attacker, EnumHand yoyoHand, EntityYoyo yoyoEntity, Entity targetEntity)
    {
        World world = targetEntity.world;

        if (targetEntity instanceof IShearable)
        {
            IShearable shearable = (IShearable) targetEntity;
            BlockPos pos = new BlockPos(targetEntity.posX, targetEntity.posY, targetEntity.posZ);

            if (shearable.isShearable(yoyoStack, world, pos))
            {
                List<ItemStack> drops = shearable.onSheared(yoyoStack, world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, yoyoStack));

                for (ItemStack stack : drops)
                {
                    if (yoyoEntity.isCollecting())
                    {
                        yoyoEntity.collectDrop(stack);
                    }
                    else
                    {
                        EntityItem ent = targetEntity.entityDropItem(stack, 1.0F);

                        if (ent == null) continue;

                        ent.motionY += world.rand.nextFloat() * 0.05F;
                        ent.motionX += (world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F;
                        ent.motionZ += (world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F;
                    }
                }

                if (!attacker.isCreative()) yoyo.damageItem(yoyoStack, 1, attacker);
            }
        }
        else
        {
            attackEntity(yoyo, yoyoStack, attacker, yoyoHand, yoyoEntity, targetEntity);
        }
    }

    public static void attackEntity(IYoyo yoyo, ItemStack stack, EntityPlayer attacker, EnumHand yoyoHand, EntityYoyo yoyoEntity, Entity targetEntity)
    {
        if (!ForgeHooks.onPlayerAttackTarget(attacker, targetEntity))
            return;

        if (targetEntity.canBeAttackedWithItem())
        {
            if (!targetEntity.hitByEntity(attacker))
            {
                float damage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                float attackModifier;

                if (targetEntity instanceof EntityLivingBase)
                    attackModifier = EnchantmentHelper.getModifierForCreature(stack, ((EntityLivingBase) targetEntity).getCreatureAttribute());
                else
                    attackModifier = EnchantmentHelper.getModifierForCreature(stack, EnumCreatureAttribute.UNDEFINED);

                if (damage > 0.0F || attackModifier > 0.0F)
                {
                    int knockbackModifier = 0;
                    knockbackModifier = knockbackModifier + EnchantmentHelper.getKnockbackModifier(attacker);

                    boolean critical = attacker.fallDistance > 0.0F && !attacker.onGround && !attacker.isOnLadder() && !attacker.isInWater() && !attacker.isPotionActive(MobEffects.BLINDNESS) && !attacker.isRiding() && targetEntity instanceof EntityLivingBase;
                    critical = critical && !attacker.isSprinting();

                    if (critical)
                        damage *= 1.5F;

                    damage = damage + attackModifier;

                    float targetHealth = 0.0F;
                    boolean setEntityOnFire = false;
                    int fireAspect = EnchantmentHelper.getFireAspectModifier(attacker);

                    if (targetEntity instanceof EntityLivingBase)
                    {
                        targetHealth = ((EntityLivingBase) targetEntity).getHealth();

                        if (fireAspect > 0 && !targetEntity.isBurning())
                        {
                            setEntityOnFire = true;
                            targetEntity.setFire(1);
                        }
                    }

                    double motionX = targetEntity.motionX;
                    double motionY = targetEntity.motionY;
                    double motionZ = targetEntity.motionZ;
                    boolean didDamage = targetEntity.attackEntityFrom(new EntityDamageSourceIndirect("yoyo", yoyoEntity, attacker), damage);

                    if (didDamage)
                    {
                        if (knockbackModifier > 0)
                        {
                            if (targetEntity instanceof EntityLivingBase)
                                ((EntityLivingBase) targetEntity).knockBack(attacker, (float) knockbackModifier * 0.5F, (double) MathHelper.sin(attacker.rotationYaw * 0.017453292F), (double) (-MathHelper.cos(attacker.rotationYaw * 0.017453292F)));
                            else
                                targetEntity.addVelocity((double) (-MathHelper.sin(attacker.rotationYaw * 0.017453292F) * (float) knockbackModifier * 0.5F), 0.1D, (double) (MathHelper.cos(attacker.rotationYaw * 0.017453292F) * (float) knockbackModifier * 0.5F));
                        }

                        if (targetEntity instanceof EntityPlayerMP && targetEntity.velocityChanged)
                        {
                            ((EntityPlayerMP) targetEntity).connection.sendPacket(new SPacketEntityVelocity(targetEntity));
                            targetEntity.velocityChanged = false;
                            targetEntity.motionX = motionX;
                            targetEntity.motionY = motionY;
                            targetEntity.motionZ = motionZ;
                        }

                        if (critical)
                        {
                            attacker.world.playSound(null, yoyoEntity.posX, yoyoEntity.posY, yoyoEntity.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, attacker.getSoundCategory(), 1.0F, 1.0F);
                            attacker.onCriticalHit(targetEntity);
                        }

                        if (!critical)
                        {
                            attacker.world.playSound(null, yoyoEntity.posX, yoyoEntity.posY, yoyoEntity.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, attacker.getSoundCategory(), 1.0F, 1.0F);
                        }

                        if (attackModifier > 0.0F)
                            attacker.onEnchantmentCritical(targetEntity);

                        attacker.setLastAttackedEntity(targetEntity);

                        if (targetEntity instanceof EntityLivingBase)
                            EnchantmentHelper.applyThornEnchantments((EntityLivingBase) targetEntity, attacker);

                        EnchantmentHelper.applyArthropodEnchantments(attacker, targetEntity);

                        if (stack != ItemStack.EMPTY && targetEntity instanceof EntityLivingBase)
                        {
                            stack.hitEntity((EntityLivingBase) targetEntity, attacker);

                            if (stack.getCount() <= 0)
                            {
                                attacker.setHeldItem(yoyoHand, ItemStack.EMPTY);
                                ForgeEventFactory.onPlayerDestroyItem(attacker, stack, yoyoHand);
                            }
                        }

                        if (targetEntity instanceof EntityLivingBase)
                        {
                            float f5 = targetHealth - ((EntityLivingBase) targetEntity).getHealth();
                            attacker.addStat(StatList.DAMAGE_DEALT, Math.round(f5 * 10.0F));

                            if (fireAspect > 0)
                                targetEntity.setFire(fireAspect * 4);

                            if (attacker.world instanceof WorldServer && f5 > 2.0F)
                            {
                                int k = (int) ((double) f5 * 0.5D);
                                ((WorldServer) attacker.world).spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR, targetEntity.posX, targetEntity.posY + (double) (targetEntity.height * 0.5F), targetEntity.posZ, k, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        attacker.addExhaustion(0.3F);
                    }
                    else
                    {
                        attacker.world.playSound(null, yoyoEntity.posX, yoyoEntity.posY, yoyoEntity.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, attacker.getSoundCategory(), 1.0F, 1.0F);

                        if (setEntityOnFire)
                            targetEntity.extinguish();
                    }
                }
            }
        }
    }
}
