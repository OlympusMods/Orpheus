package com.github.olympus.orpheus;

import com.github.olympus.orpheus.client.OrpheusMenu;
import com.github.olympus.orpheus.client.OrpheusClient;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(Orpheus.MOD_ID)
public class Orpheus {
    public static final String MOD_ID = "orpheus";

    public Orpheus(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(Orpheus::onRegisterKeys);
        }
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        OrpheusClient.configure(FMLPaths.CONFIGDIR.get());
        OrpheusMenu.register();
        for (KeyMapping key : OrpheusClient.keys().all()) {
            event.register(key);
        }
    }
}
