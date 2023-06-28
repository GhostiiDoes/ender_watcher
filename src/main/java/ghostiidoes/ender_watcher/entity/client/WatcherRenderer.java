package ghostiidoes.ender_watcher.entity.client;

import ghostiidoes.ender_watcher.entity.custom.WatcherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WatcherRenderer extends GeoEntityRenderer<WatcherEntity> {
    public WatcherRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WatcherModel());
    }
}
