package ghostiidoes.ender_watcher.item;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.item.custom.VoidEyeItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModItems {

    // items
    public static final Item VOID_EYE = registerItem("void_eye",
            new VoidEyeItem(new FabricItemSettings()));

    public static final Item ENDER_WATCHER_SPAWN_EGG = registerItem("ender_watcher_spawn_egg",
            new SpawnEggItem(ModEntities.ENDER_WATCHER, 0x321616, 0x000000,
                    new FabricItemSettings()));

    // register functions
    public static void addItemsToItemGroup() {
        addToItemGroup(ItemGroups.FUNCTIONAL, VOID_EYE);
        addToItemGroup(ItemGroups.TOOLS, VOID_EYE);
        addToItemGroup(ItemGroups.SPAWN_EGGS, ENDER_WATCHER_SPAWN_EGG);
    }

    // utility functions
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(EnderWatcherMod.MOD_ID, name), item);
    }

    private static void addToItemGroup(RegistryKey<ItemGroup> group, Item item) {
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
    }

    public static void registerModItems() {
        EnderWatcherMod.LOGGER.info("Registering Mod Items for " + EnderWatcherMod.MOD_ID);
        addItemsToItemGroup();
    }
}
