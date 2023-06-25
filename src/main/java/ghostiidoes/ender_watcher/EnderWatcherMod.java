package ghostiidoes.ender_watcher;

import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.entity.custom.WatcherEntity;
import ghostiidoes.ender_watcher.item.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderWatcherMod implements ModInitializer {
	public static final String MOD_ID = "ender_watcher";
    public static final Logger LOGGER = LoggerFactory.getLogger("ender_watcher");

	@Override
	public void onInitialize() {
		ModItems.registerModItems();

		FabricDefaultAttributeRegistry.register(ModEntities.ENDER_WATCHER, WatcherEntity.setAttributes());
	}
}