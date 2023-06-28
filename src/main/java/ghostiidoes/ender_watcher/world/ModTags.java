package ghostiidoes.ender_watcher.world;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public class ModTags {
    public static final TagKey<Structure> VOID_EYE_LOCATED = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(EnderWatcherMod.MOD_ID, "void_eye_located"));
    public static final TagKey<Block> ENDER_WATCHER_UNHOLDABLE = TagKey.of(RegistryKeys.BLOCK, new Identifier(EnderWatcherMod.MOD_ID, "ender_watcher_unholdable"));
}
