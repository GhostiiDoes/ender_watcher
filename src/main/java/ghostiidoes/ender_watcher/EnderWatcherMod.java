package ghostiidoes.ender_watcher;

import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.entity.custom.WatcherEntity;
import ghostiidoes.ender_watcher.event.AttackEntityHandler;
import ghostiidoes.ender_watcher.item.ModItems;
import ghostiidoes.ender_watcher.sounds.ModSounds;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderWatcherMod implements ModInitializer {
	public static final String MOD_ID = "ender_watcher";
    public static final Logger LOGGER = LoggerFactory.getLogger("ender_watcher");

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModSounds.registerModSounds();

		FabricDefaultAttributeRegistry.register(ModEntities.ENDER_WATCHER, WatcherEntity.setAttributes());

		AttackEntityCallback.EVENT.register(new AttackEntityHandler());
	}
}