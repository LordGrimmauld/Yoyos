package com.jozufozu.yoyos;

import com.google.common.eventbus.Subscribe;
import com.jozufozu.yoyos.client.RenderYoYo;
import com.jozufozu.yoyos.common.CommonProxy;
import com.jozufozu.yoyos.common.EntityYoyo;
import com.jozufozu.yoyos.tinkers.YoyoCore;
import com.jozufozu.yoyos.tinkers.materials.AxleMaterialStats;
import com.jozufozu.yoyos.tinkers.materials.BodyMaterialStats;
import com.jozufozu.yoyos.tinkers.materials.CordMaterialStats;
import com.jozufozu.yoyos.tinkers.modifiers.ModExtension;
import com.jozufozu.yoyos.tinkers.modifiers.ModFloating;
import com.jozufozu.yoyos.tinkers.modifiers.ModGardening;
import com.jozufozu.yoyos.tinkers.modifiers.ModLubricated;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.lang3.tuple.Pair;
import slimeknights.mantle.pulsar.pulse.Pulse;
import slimeknights.tconstruct.common.ModelRegisterUtil;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.library.tools.ToolPart;
import slimeknights.tconstruct.tools.TinkerMaterials;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.ArrayList;
import java.util.List;

@Pulse( id = "yoyos",
        description = "Projectile melee weapons!",
        forced = true,
        modsRequired = "tconstruct")
public class TinkersYoyos extends AbstractTinkerPulse {

    public static ToolPart YOYO_AXLE;
    public static ToolPart YOYO_BODY;
    public static ToolPart YOYO_CORD;

    public static ToolCore YOYO;

    public static Modifier EXTENSION;
    public static Modifier FLOATING;
    public static Modifier LUBRICATED;
    public static Modifier GARDENING;

    @SidedProxy(clientSide = "com.jozufozu.yoyos.TinkersYoyos$YoyoClientProxy", serverSide = "com.jozufozu.yoyos.common.CommonProxy$ServerProxy")
    public static CommonProxy proxy;

    static {
        Material.UNKNOWN.addStats(new BodyMaterialStats(1F, 2F, 400));
        Material.UNKNOWN.addStats(new AxleMaterialStats(0.5F, 1F));
        Material.UNKNOWN.addStats(new CordMaterialStats(0.2F, 5F));
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        registerToolParts();
        registerTools();

        for(Pair<Item, ToolPart> toolPartPattern : toolPartPatterns) {
            registerStencil(toolPartPattern.getLeft(), toolPartPattern.getRight());
        }

        EXTENSION = registerModifier(new ModExtension());
        EXTENSION.addItem("string");
        EXTENSION.addItem("blockWool", 1, 4);

        FLOATING = registerModifier(new ModFloating(3));
        FLOATING.addItem(new ItemStack(Items.FISH, 1, ItemFishFood.FishType.PUFFERFISH.getMetadata()), 1, 1);

        LUBRICATED = registerModifier(new ModLubricated(5));
        LUBRICATED.addItem(new ItemStack(Items.DYE, 1, 0), 1, 1);

        GARDENING = registerModifier(new ModGardening());
        GARDENING.addItem(Items.SHEARS);

        proxy.preInit(event);
    }

    @Subscribe
    public void init(FMLInitializationEvent event) {
        registerToolBuilding();
        registerMaterialStats();

        proxy.init(event);
    }

    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    private void registerToolParts() {
        YOYO_AXLE = registerToolPart(new ToolPart(Material.VALUE_Fragment * 6), "yoyo_axle");
        YOYO_BODY = registerToolPart(new ToolPart(Material.VALUE_Ingot * 4), "yoyo_body");
        YOYO_CORD = registerToolPart(new ToolPart(Material.VALUE_Ingot * 2), "yoyo_cord");
    }

    private void registerTools() {
        YOYO = registerTool(new YoyoCore(), "yoyo");
    }

    private void registerToolBuilding() {
        TinkerRegistry.registerToolForgeCrafting(YOYO);
    }

    private void registerMaterialStats() {
        /*Metals*/
        TinkerRegistry.addMaterialStats(TinkerMaterials.bronze,
                new BodyMaterialStats(3.5F, 2F, 450),
                new AxleMaterialStats(0.5F, 1F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.copper,
                new BodyMaterialStats(3.0F, 1.9F, 210),
                new AxleMaterialStats(0.4F, 1F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.electrum,
                new BodyMaterialStats(3.0F, .3F, 50),
                new AxleMaterialStats(0.0F, 0.8F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.lead,
                new BodyMaterialStats(3.5F, 6F, 350),
                new AxleMaterialStats(1.6F, 0.7F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.silver,
                new BodyMaterialStats(5.0F, 1.7F, 250),
                new AxleMaterialStats(0.3F, 0.95F));


        TinkerRegistry.addMaterialStats(TinkerMaterials.iron,
                new BodyMaterialStats(4.0F, 3.5F, 204),
                new AxleMaterialStats(0.9F, 0.85F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.pigiron,
                new BodyMaterialStats(4.5F, 3.7F, 380),
                new AxleMaterialStats(1.2F, 1.2F));

        /*Nether*/
        TinkerRegistry.addMaterialStats(TinkerMaterials.ardite,
                new BodyMaterialStats(3.6F, 3.2F, 990),
                new AxleMaterialStats(0.1F, 1.4F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.cobalt,
                new BodyMaterialStats(4.4F, 0.3F, 530),
                new AxleMaterialStats(0.4F, 2.3F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.manyullyn,
                new BodyMaterialStats(8.7F, 4.0F, 820),
                new AxleMaterialStats(0.1F, 1.2F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.firewood,
                new BodyMaterialStats(5.0F, 2.7F, 550),
                new AxleMaterialStats(3.8F, 0.95F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.netherrack,
                new BodyMaterialStats(3.0F, 3.4F, 230),
                new AxleMaterialStats(3.6F, 0.8F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.blaze,
                new AxleMaterialStats(0.2F, 0.8F));

        /*Naturals*/
        TinkerRegistry.addMaterialStats(TinkerMaterials.cactus,
                new AxleMaterialStats(2.5F, 0.6F),
                new BodyMaterialStats(3.4F, 0.8F, 210));

        TinkerRegistry.addMaterialStats(TinkerMaterials.flint,
                new AxleMaterialStats(0.0F, 0.2F),
                new BodyMaterialStats(2.9F, 0.2F, 150));

        TinkerRegistry.addMaterialStats(TinkerMaterials.prismarine,
                new AxleMaterialStats(0.7F, 0.9F),
                new BodyMaterialStats(6.0F, 1.3F, 430));

        TinkerRegistry.addMaterialStats(TinkerMaterials.endstone,
                new AxleMaterialStats(0.5F, 0.85F),
                new BodyMaterialStats(2.9F, 0.4F, 420));

        TinkerRegistry.addMaterialStats(TinkerMaterials.sponge,
                new AxleMaterialStats(4F, 1.2F),
                new BodyMaterialStats(0.0F, 0.3F, 1050));

        TinkerRegistry.addMaterialStats(TinkerMaterials.obsidian,
                new AxleMaterialStats(0.1F, 0.9F),
                new BodyMaterialStats(4.2F, 1.2F, 120));

        TinkerRegistry.addMaterialStats(TinkerMaterials.stone,
                new AxleMaterialStats(2.0F, 0.5F),
                new BodyMaterialStats(3.0F, 2.0F, 120));

        TinkerRegistry.addMaterialStats(TinkerMaterials.wood,
                new AxleMaterialStats(3.1F, 1.0F),
                new BodyMaterialStats(2.0F, 1.1F, 35));

        /*Misc*/
        TinkerRegistry.addMaterialStats(TinkerMaterials.paper,
                new BodyMaterialStats(1.5F, 0.1F, 5),
                new AxleMaterialStats(1.2F, 0.2F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.string,
                new CordMaterialStats(0.01F, 6F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.bone,
                new BodyMaterialStats(2.5F, .3F, 200),
                new AxleMaterialStats(0.01F, 1.1F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.ice,
                new BodyMaterialStats(4.5F, 0.9F, 20),
                new AxleMaterialStats(0.0F, 0.1F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.vine,
                new CordMaterialStats(1.2F, 8F));


        /*Slime*/
        TinkerRegistry.addMaterialStats(TinkerMaterials.blueslime,
                new BodyMaterialStats(1.2F, 0.6F, 800),
                new AxleMaterialStats(2F, 0.3F),
                new CordMaterialStats(1.0F, 8F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.magmaslime,
                new BodyMaterialStats(3.3F, 0.6F, 450),
                new AxleMaterialStats(1.3F, 0.3F),
                new CordMaterialStats(1.0F, 8F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.knightslime,
                new BodyMaterialStats(6.1F, 0.6F, 600),
                new AxleMaterialStats(2F, 0.3F),
                new CordMaterialStats(2.0F, 16F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.slime,
                new BodyMaterialStats(1.2F, 0.6F, 1000),
                new AxleMaterialStats(2F, 0.3F),
                new CordMaterialStats(1.0F, 8F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.slimevine_blue,
                new CordMaterialStats(1.5F, 10F));

        TinkerRegistry.addMaterialStats(TinkerMaterials.slimevine_purple,
                new CordMaterialStats(1.9F, 14F));
    }

    public static class YoyoClientProxy extends CommonProxy {

        @Override
        public void preInit(FMLPreInitializationEvent event) {
            super.preInit(event);

            RenderingRegistry.registerEntityRenderingHandler(EntityYoyo.class, RenderYoYo::new);

            toolParts.forEach(  ModelRegisterUtil::registerPartModel);
            tools.forEach(      ModelRegisterUtil::registerToolModel);

            List<IModifier> yoyoMods = new ArrayList<>();

            yoyoMods.add(TinkerModifiers.modMendingMoss);
            yoyoMods.add(TinkerModifiers.modSharpness);
            yoyoMods.add(TinkerModifiers.modShulking);
            yoyoMods.add(TinkerModifiers.modSoulbound);
            yoyoMods.add(TinkerModifiers.modLuck);
            yoyoMods.add(TinkerModifiers.modReinforced);
            yoyoMods.add(TinkerModifiers.modNecrotic);
            yoyoMods.add(TinkerModifiers.modEmerald);
            yoyoMods.add(EXTENSION);
            yoyoMods.add(LUBRICATED);
            yoyoMods.add(FLOATING);
            yoyoMods.add(GARDENING);

            for (IModifier modifier : yoyoMods)
                ModelRegisterUtil.registerModifierModel(modifier, new ResourceLocation(Yoyos.MODID, "models/item/modifiers/" + modifier.getIdentifier()));
        }

        @Override
        public void postInit(FMLPostInitializationEvent event) {
            super.postInit(event);
            ToolBuildGuiInfo info = new ToolBuildGuiInfo(YOYO);
            info.addSlotPosition(28, 62);
            info.addSlotPosition(8, 30);
            info.addSlotPosition(50, 48);
            info.addSlotPosition(29, 38);
            TinkerRegistryClient.addToolBuilding(info);
        }

        @Override
        public boolean isServer() {
            return false;
        }
    }
}
