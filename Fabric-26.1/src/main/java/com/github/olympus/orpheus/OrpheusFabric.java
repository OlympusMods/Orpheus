package com.github.olympus.orpheus;

import com.github.olympus.orpheus.client.OrpheusClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public final class OrpheusFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OrpheusClient.configure(FabricLoader.getInstance().getConfigDir());
        for (KeyMapping key : OrpheusClient.keys().all()) {
            KeyMappingHelper.registerKeyMapping(key);
        }
    }
}
