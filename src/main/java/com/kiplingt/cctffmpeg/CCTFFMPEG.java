package com.kiplingt.cctffmpeg;

import com.mojang.logging.LogUtils;
import com.kiplingt.cctffmpeg.block.FFMPEGConverterBlock;
import com.kiplingt.cctffmpeg.blockentity.FFMPEGConverterBlockEntity;
import com.kiplingt.cctffmpeg.menu.FFMPEGConverterMenu;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.IContainerFactory;
import org.slf4j.Logger;

@Mod(CCTFFMPEG.MODID)
public final class CCTFFMPEG {
    public static final String MODID = "cctffmpeg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<FFMPEGConverterBlock> FFMPEG_CONVERTER = BLOCKS.register("ffmpeg_converter",
        () -> new FFMPEGConverterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> FFMPEG_CONVERTER_ITEM = ITEMS.registerSimpleBlockItem("ffmpeg_converter", FFMPEG_CONVERTER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FFMPEGConverterBlockEntity>> FFMPEG_CONVERTER_BE =
        BLOCK_ENTITY_TYPES.register("ffmpeg_converter", () -> BlockEntityType.Builder.of(FFMPEGConverterBlockEntity::new, FFMPEG_CONVERTER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<FFMPEGConverterMenu>> FFMPEG_CONVERTER_MENU = MENUS.register("ffmpeg_converter",
        () -> new MenuType<>((IContainerFactory<FFMPEGConverterMenu>) (id, inventory, data) -> {
            BlockPos pos = data.readBlockPos();
            BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
            if (blockEntity instanceof FFMPEGConverterBlockEntity converter) return new FFMPEGConverterMenu(id, inventory, converter);
            return new FFMPEGConverterMenu(id, inventory, null);
        }, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.cctffmpeg"))
        .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
        .icon(() -> FFMPEG_CONVERTER_ITEM.get().getDefaultInstance())
        .displayItems((parameters, output) -> output.accept(FFMPEG_CONVERTER_ITEM.get()))
        .build());

    public CCTFFMPEG(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(CCTFFMPEG::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), FFMPEG_CONVERTER_BE.get(), (blockEntity, side) -> blockEntity.getPeripheral());
    }
}
