package com.github.olympus.orpheus;

import com.github.olympus.orpheus.client.OrpheusClient;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ForgeClient {
    private ForgeClient() {
    }

    static void register() {
        RegisterKeyMappingsEvent.BUS.addListener(ForgeClient::onRegisterKeys);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        OrpheusClient.configure(FMLPaths.CONFIGDIR.get());
        for (KeyMapping key : OrpheusClient.keys().all()) {
            event.register(key);
        }
    }
}
