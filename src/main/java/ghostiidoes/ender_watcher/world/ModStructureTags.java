package ghostiidoes.ender_watcher.world;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public class ModStructureTags {
    public static final TagKey<Structure> VOID_EYE_LOCATED = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(EnderWatcherMod.MOD_ID, "void_eye_located"));
}
