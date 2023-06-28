package ghostiidoes.ender_watcher.event;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.entity.custom.WatcherEntity;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class AttackEntityHandler implements AttackEntityCallback {

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {

        if (entity instanceof EndermanEntity && !world.isClient()) {

            Identifier identifier = world.getRegistryKey().getValue();
            if (!identifier.equals(DimensionTypes.THE_END_ID)) {
                return ActionResult.PASS;
            }

            Random random = new Random();

            boolean shouldSpawn = false;

            Box box = entity.getBoundingBox().expand(10);
            List<WatcherEntity> watchersNearby = entity.getWorld().getEntitiesByType(ModEntities.ENDER_WATCHER, box, EntityPredicates.VALID_ENTITY);

            if (player.isCreative() || player.isSpectator()) {
                return ActionResult.PASS;
            }

            if (watchersNearby.size() == 0) {
                if (random.nextInt(3) == 0) {
                    shouldSpawn = true;
                }
            }
            else {
                for (int i = watchersNearby.size(); i > 0; i--) {
                    watchersNearby.get(i-1).setTarget(player);
                }
            }

            // spawn the watcher
            if (shouldSpawn) {
                WatcherEntity watcherEntity = new WatcherEntity(ModEntities.ENDER_WATCHER, world);

                // randomize position and place on ground
                int spawnRange = 5;
                BlockPos spawnPos;
                int spawnX = player.getBlockX() + random.nextInt(spawnRange * 2) - spawnRange;
                int spawnY = player.getBlockY() + random.nextInt(15);
                int spawnZ = player.getBlockZ() + random.nextInt(spawnRange * 2) - spawnRange;
                spawnPos = new BlockPos(spawnX, spawnY, spawnZ);

                if (Math.abs(spawnX) > 500 && Math.abs(spawnZ) > 500) {
                    if (randomPos(spawnX, spawnY, spawnZ, watcherEntity)) {
                        world.spawnEntity(watcherEntity);
                    }
                }
            }
        }

        return ActionResult.PASS;
    }

    private boolean randomPos(double x, double y, double z, WatcherEntity watcherEntity) {

        x += 0.5;
        y += 0.5;
        z += 0.5;

        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        if (y < 1) {
            return false;
        }

        for (int newY = (int)y; newY >= 0; newY--) {
            mutable.move(Direction.DOWN);
            if (watcherEntity.getWorld().getBlockState(mutable).isSolidBlock(watcherEntity.getWorld(), mutable)) {
                y = newY + 0.5;
                break;
            }
            if (newY == 0) {
                return false;
            }
        }
        BlockState blockState = watcherEntity.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (!bl || bl2) {
            return false;
        }
        Vec3d vec3d = watcherEntity.getPos();
        watcherEntity.setPos(x, y, z);

        // Cast effects on spawn
        watcherEntity.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(watcherEntity));
        if (!watcherEntity.isSilent()) {
            watcherEntity.getWorld().playSound(null, watcherEntity.prevX, watcherEntity.prevY, watcherEntity.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, watcherEntity.getSoundCategory(), 1.0f, 1.0f);
            watcherEntity.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }

        return true;
    }
}
