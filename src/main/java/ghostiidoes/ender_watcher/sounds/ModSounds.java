package ghostiidoes.ender_watcher.sounds;

import net.minecraft.client.sound.Sound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public class ModSounds {

    public static final Identifier WATCHER_PLACE_ID = new Identifier("ender_watcher:watcher_place");
    public static SoundEvent WATCHER_PLACE_EVENT = SoundEvent.of(WATCHER_PLACE_ID);
    public static final Identifier WATCHER_BREAK_ID = new Identifier("ender_watcher:watcher_break");
    public static SoundEvent WATCHER_BREAK_EVENT = SoundEvent.of(WATCHER_BREAK_ID);
    public static Identifier WATCHER_STARE_ID = new Identifier("ender_watcher:watcher_stare");
    public static SoundEvent WATCHER_STARE_EVENT = SoundEvent.of(WATCHER_STARE_ID);

    public static void registerModSounds() {
        Registry.register(Registries.SOUND_EVENT, WATCHER_PLACE_ID, WATCHER_PLACE_EVENT);
        Registry.register(Registries.SOUND_EVENT, WATCHER_BREAK_ID, WATCHER_BREAK_EVENT);
        Registry.register(Registries.SOUND_EVENT, WATCHER_STARE_ID, WATCHER_STARE_EVENT);
    }

}
