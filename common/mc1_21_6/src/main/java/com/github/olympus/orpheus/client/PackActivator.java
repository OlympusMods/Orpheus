package com.github.olympus.orpheus.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

/** Turns a resource pack folder on and reloads the game's resources so new songs are noticed. */
public final class PackActivator {
    private PackActivator() {
    }

    /**
     * Makes sure the pack in resourcepacks/&lt;folderName&gt; is enabled, then reloads the resources.
     * Must run on the game thread.
     */
    public static void enableAndReload(Minecraft minecraft, String folderName) {
        PackRepository repository = minecraft.getResourcePackRepository();
        // Scan the resourcepacks folder, the pack may have been created just now.
        repository.reload();
        String id = "file/" + folderName;
        if (!repository.getAvailableIds().contains(id)) {
            OrpheusClient.LOGGER.warn("Resource pack {} was not found, only reloading the resources", id);
            minecraft.reloadResourcePacks();
            return;
        }
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        if (selected.contains(id)) {
            // Already on, but its files changed.
            minecraft.reloadResourcePacks();
            return;
        }
        // Lowest priority: the pack only adds songs, it must never override another pack's files.
        selected.add(0, id);
        repository.setSelected(selected);
        // Saves the selection and reloads the resources because the list changed.
        minecraft.options.updateResourcePacks(repository);
    }
}
