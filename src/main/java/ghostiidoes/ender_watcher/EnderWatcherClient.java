package ghostiidoes.ender_watcher;

import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.entity.client.WatcherRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class EnderWatcherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ModEntities.ENDER_WATCHER, WatcherRenderer::new);
    }
}
