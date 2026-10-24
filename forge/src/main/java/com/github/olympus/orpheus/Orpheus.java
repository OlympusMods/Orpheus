package com.github.olympus.orpheus;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Orpheus.MOD_ID)
public class Orpheus {
    public static final String MOD_ID = "orpheus";

    public Orpheus() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClient.register();
        }
    }
}
