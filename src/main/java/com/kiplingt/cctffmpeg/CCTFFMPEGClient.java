package com.kiplingt.cctffmpeg;

import com.kiplingt.cctffmpeg.client.FFMPEGConverterScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CCTFFMPEG.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CCTFFMPEGClient {
    private CCTFFMPEGClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CCTFFMPEG.FFMPEG_CONVERTER_MENU.get(), FFMPEGConverterScreen::new);
    }
}
