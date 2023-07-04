package ghostiidoes.ender_watcher.entity.custom;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import ghostiidoes.ender_watcher.entity.ModEntities;
import ghostiidoes.ender_watcher.sounds.ModSounds;
import ghostiidoes.ender_watcher.world.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Predicate;

public class WatcherEntity extends HostileEntity implements Angerable, GeoEntity {

    private static final UUID ATTACKING_SPEED_BOOST_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
    private static final EntityAttributeModifier ATTACKING_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost", (double)0.15f, EntityAttributeModifier.Operation.ADDITION);

    private static final TrackedData<Optional<BlockState>> CARRIED_BLOCK = DataTracker.registerData(WatcherEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(WatcherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> PROVOKED = DataTracker.registerData(WatcherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(60, 79);
    private BlockPos TARGET_BLOCK;
    private int lastAngrySoundAge = Integer.MIN_VALUE;
    private int angerTime;
    @Nullable
    private UUID angryAt;

    // Constructor + attributes
    public WatcherEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0f);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 3.55f;
    }

    ////////////////////
    // AI

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ChasePlayerGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new PickUpBlockGoal(this));
        this.goalSelector.add(2, new PlaceBlockGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal((PathAwareEntity)this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new TeleportTowardsPlayerGoal(this, this::shouldAngerAt));
        this.targetSelector.add(2, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(3, new ActiveTargetGoal<EndermiteEntity>((MobEntity)this, EndermiteEntity.class, true, false));
        this.targetSelector.add(4, new UniversalAngerGoal<WatcherEntity>(this, false));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        int ageWhenTargetSet;
        if (target == null) {
            ageWhenTargetSet = 0;
            this.dataTracker.set(ANGRY, false);
            this.dataTracker.set(PROVOKED, false);
            entityAttributeInstance.removeModifier(ATTACKING_SPEED_BOOST);
        } else {
            ageWhenTargetSet = this.age;
            this.dataTracker.set(ANGRY, true);
            if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
            }
        }
    }

    @Override
    public void tickMovement() {
        if (this.getWorld().isClient) {
            for (int i = 0; i < 2; ++i) {
                this.getWorld().addParticle(ParticleTypes.PORTAL, this.getParticleX(0.5), this.getRandomBodyY() - 0.25, this.getParticleZ(0.5), (this.random.nextDouble() - 0.5) * 2.0, -this.random.nextDouble(), (this.random.nextDouble() - 0.5) * 2.0);
            }
        }
        this.jumping = false;
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }
        super.tickMovement();
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean attacked = super.tryAttack(target);
        if (attacked && target instanceof LivingEntity) {
            // teleport with target
            if (this.random.nextInt(10) > (this.getHealth() / 8)) {
                Box box = this.getBoundingBox().expand(32);
                List<EndermanEntity> nearbyEndermen = this.getWorld().getEntitiesByType(EntityType.ENDERMAN, box, EntityPredicates.VALID_ENTITY);
                EnderWatcherMod.LOGGER.info("Nearby endermen are " + nearbyEndermen);

                // anger random nearby enderman
                if (nearbyEndermen.size() > 1) {
                    int randomNumber = this.random.nextInt(nearbyEndermen.size() - 1);
                    nearbyEndermen.get(randomNumber).setTarget((LivingEntity) target);
                    EnderWatcherMod.LOGGER.info("Size was greater than 1");
                }
                else if (nearbyEndermen.size() == 1) {
                    nearbyEndermen.get(0).setTarget((LivingEntity) target);
                    EnderWatcherMod.LOGGER.info("Size was 1");
                }

                ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20), this);
                for (int i = 0; i < 32; ++i) {
                    if (!this.teleportWithTarget(target)) continue;
                    break;
                }
            }
        }
        return attacked;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isAngry() ? SoundEvents.ENTITY_ENDERMAN_SCREAM : SoundEvents.ENTITY_ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDERMAN_DEATH;
    }

    @Override
    public boolean hurtByWater() { return true; }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CARRIED_BLOCK, Optional.empty());
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(PROVOKED, false);
    }

    @Override
    public void onDamaged(DamageSource damageSource) {
        super.onDamaged(damageSource);
    }

    public boolean isTargetVisible() {
        boolean isHiding;

        if (this.getTarget() == null) {
            isHiding = false;
        }

        if (this.canSee(this.getTarget())) {
            isHiding = false;
        }
        else {
            setTargetBlock();
            isHiding = true;
        }

        return isHiding;
    }


    public void setTargetBlock() {
        Vec3d vec3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        Vec3d vec3d2 = new Vec3d(this.getTarget().getX(), this.getTarget().getEyeY(), this.getTarget().getZ());
        if (vec3d2.distanceTo(vec3d) > 128.0) {
            return;
        }
        TARGET_BLOCK = this.getWorld().raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getBlockPos();
    }

    private boolean canPlaceOn(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove, BlockState state, BlockPos pos) {
        return stateAbove.isAir() && !state.isAir() && !state.isOf(Blocks.BEDROCK) && state.isFullCube(world, pos) && carriedState.canPlaceAt(world, posAbove) && world.getOtherEntities(this, Box.from(Vec3d.of(posAbove))).isEmpty();
    }

    public void setCarriedBlock(@Nullable BlockState state) {
        this.dataTracker.set(CARRIED_BLOCK, Optional.ofNullable(state));
    }

    @Nullable
    public BlockState getCarriedBlock() {
        return this.dataTracker.get(CARRIED_BLOCK).orElse(null);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        boolean bl = source.getSource() instanceof PotionEntity;
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) || bl) {
            boolean bl2 = bl && this.damageFromPotion(source, (PotionEntity)source.getSource(), amount);
            for (int i = 0; i < 64; ++i) {
                if (!this.teleportRandomly()) continue;
                return true;
            }
            return bl2;
        }
        boolean bl2 = super.damage(source, amount);
        if (!this.getWorld().isClient() && !(source.getAttacker() instanceof LivingEntity) && this.random.nextInt(10) != 0) {
            this.teleportRandomly();
        }
        return bl2;
    }

    private boolean damageFromPotion(DamageSource source, PotionEntity potion, float amount) {
        boolean bl;
        ItemStack itemStack = potion.getStack();
        Potion potion2 = PotionUtil.getPotion(itemStack);
        List<StatusEffectInstance> list = PotionUtil.getPotionEffects(itemStack);
        boolean bl2 = bl = potion2 == Potions.WATER && list.isEmpty();
        if (bl) {
            return super.damage(source, amount);
        }
        return false;
    }

    public void setProvoked() {
        this.dataTracker.set(PROVOKED, true);
    }

    @Override
    public int getAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.angerTime = angerTime;
    }

    public boolean isProvoked() {
        return this.dataTracker.get(PROVOKED);
    }
    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    @Nullable
    @Override
    public UUID getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (ANGRY.equals(data) && this.isProvoked() && this.getWorld().isClient) {
            this.playAngrySound();
        }
        super.onTrackedDataSet(data);
    }

    public void playAngrySound() {
        if (this.age >= this.lastAngrySoundAge + 400) {
            this.lastAngrySoundAge = this.age;
            if (!this.isSilent()) {
                this.getWorld().playSound(this.getX(), this.getEyeY(), this.getZ(), ModSounds.WATCHER_STARE_EVENT, this.getSoundCategory(), 2.5f, 1.0f, false);
            }
        }
    }

    protected boolean teleportWithTarget(Entity target) {
        double d = this.getX() + (this.random.nextDouble() - 0.5) * 32.0;
        double e = this.getY() + (double)(this.random.nextInt(5));
        double f = this.getZ() + (this.random.nextDouble() - 0.5) * 32.0;
        if (this.teleportTo(d, e, f)) {
            target.teleport(d, e, f);
        }
        return this.teleportTo(d, e, f);
    }

    protected boolean teleportRandomly() {
        if (this.getWorld().isClient() || !this.isAlive()) {
            return false;
        }
        double d = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
        double e = this.getY() + (double)(this.random.nextInt(64) - 32);
        double f = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
        return this.teleportTo(d, e, f);
    }

    boolean teleportTo(Entity entity) {
        Vec3d vec3d = new Vec3d(this.getX() - entity.getX(), this.getBodyY(0.5) - entity.getEyeY(), this.getZ() - entity.getZ());
        vec3d = vec3d.normalize();
        double d = 16.0;
        double e = this.getX() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.x * 16.0;
        double f = this.getY() + (double)(this.random.nextInt(16) - 8) - vec3d.y * 16.0;
        double g = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.z * 16.0;
        return this.teleportTo(e, f, g);
    }

    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        while (mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }
        BlockState blockState = this.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (!bl || bl2) {
            return false;
        }
        Vec3d vec3d = this.getPos();
        boolean bl3 = this.teleport(x, y, z, true);
        if (bl3) {
            this.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(this));
            if (!this.isSilent()) {
                this.getWorld().playSound(null, this.prevX, this.prevY, this.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0f, 1.0f);
                this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
        return bl3;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    @Override
    public void chooseRandomAngerTime()  {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    boolean isPlayerSeeing(PlayerEntity player) {
        ItemStack itemStack = player.getInventory().armor.get(3);
        if (itemStack.isOf(Blocks.CARVED_PUMPKIN.asItem())) {
            return false;
        }
        else if (player.canSee(this)) {
            Vec3d playerRotation = player.getRotationVec(1.0f).normalize();
            Vec3d watcherPosition = new Vec3d(this.getX() - player.getX(), (this.getY() + 1.0f) - player.getEyeY(), this.getZ() - player.getZ());
            double distance = watcherPosition.length();
            double e = playerRotation.dotProduct(watcherPosition = watcherPosition.normalize());
            if (e < 1.0 - 0.6 / distance) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

    boolean isPlayerStaring(PlayerEntity player) {
        ItemStack itemStack = player.getInventory().armor.get(3);
        if (itemStack.isOf(Blocks.CARVED_PUMPKIN.asItem())) {
            return false;
        }
        Vec3d vec3d = player.getRotationVec(1.0f).normalize();
        Vec3d vec3d2 = new Vec3d(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double d = vec3d2.length();
        double e = vec3d.dotProduct(vec3d2 = vec3d2.normalize());
        if (e > 1.0 - 0.025 / d) {
            return player.canSee(this);
        }
        return false;
    }

    ///////////////
    // CUSTOM GOALS

    static class ChasePlayerGoal extends Goal {
        private final WatcherEntity ender_watcher;
        @Nullable
        private LivingEntity target;

        public ChasePlayerGoal(WatcherEntity ender_watcher) {
            this.ender_watcher = ender_watcher;
            this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            this.target = this.ender_watcher.getTarget();
            if (!(this.target instanceof PlayerEntity)) {
                return false;
            }
            double d = this.target.squaredDistanceTo(this.ender_watcher);
            if (d > 256.0) {
                return false;
            }
            return this.ender_watcher.isPlayerStaring((PlayerEntity)this.target);
        }

        @Override
        public void start() {
            this.ender_watcher.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.ender_watcher.getLookControl().lookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
        }
    }

    static class TeleportTowardsPlayerGoal extends ActiveTargetGoal<PlayerEntity> {
        private final WatcherEntity ender_watcher;
        @Nullable
        private PlayerEntity targetPlayer;
        private int lookAtPlayerWarmup;
        private int ticksSinceUnseenTeleport;
        private int proximityTime;
        private final TargetPredicate staringPlayerPredicate;
        private final TargetPredicate validTargetPredicate = TargetPredicate.createAttackable().ignoreVisibility();
        private final Predicate<LivingEntity> angerPredicate;

        public TeleportTowardsPlayerGoal(WatcherEntity ender_watcher, @Nullable Predicate<LivingEntity> targetPredicate) {
            super(ender_watcher, PlayerEntity.class, 10, false, false, targetPredicate);
            this.ender_watcher = ender_watcher;
            this.angerPredicate = playerEntity -> (ender_watcher.isPlayerStaring((PlayerEntity) playerEntity) || ender_watcher.shouldAngerAt((LivingEntity) playerEntity)) && !ender_watcher.hasPassengerDeep((Entity) playerEntity);
            this.staringPlayerPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(this.angerPredicate);
        }

        @Override
        public boolean canStart() {
            this.targetPlayer = this.ender_watcher.getWorld().getClosestPlayer(this.staringPlayerPredicate, this.ender_watcher);
            return this.targetPlayer != null;
        }

        @Override
        public void start() {
            this.lookAtPlayerWarmup = this.getTickCount(5);
            this.ticksSinceUnseenTeleport = 0;
            this.ender_watcher.setProvoked();
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            super.stop();
        }

        @Override
        public boolean shouldContinue() {
            if (this.targetPlayer != null) {
                if (!this.angerPredicate.test(this.targetPlayer)) {
                    return false;
                }
                this.ender_watcher.lookAtEntity(this.targetPlayer, 10.0f, 10.0f);
                return true;
            }
            if (this.targetEntity != null) {
                if (this.ender_watcher.hasPassengerDeep(this.targetEntity)) {
                    return false;
                }
                if (this.validTargetPredicate.test(this.ender_watcher, this.targetEntity)) {
                    return true;
                }
            }
            return super.shouldContinue();
        }

        @Override
        public void tick() {
            if (this.ender_watcher.getTarget() == null) {
                super.setTargetEntity(null);
            }
            if (this.targetPlayer != null) {
                if (--this.lookAtPlayerWarmup <= 0) {
                    this.targetEntity = this.targetPlayer;
                    this.targetPlayer = null;
                    super.start();
                }
            } else {
                if (this.targetEntity != null && !this.ender_watcher.hasVehicle()) {
                    if (this.targetEntity.distanceTo(this.ender_watcher) < 10) {
                        if (this.proximityTime < 20) {
                            this.proximityTime++;
                        }
                        if (this.proximityTime >= 20 && ender_watcher.isPlayerSeeing((PlayerEntity)this.targetEntity)) {
                            if (this.ender_watcher.teleportRandomly()) {
                                this.proximityTime = 0;
                            }
                        }
                        this.ticksSinceUnseenTeleport = 0;
                    } else if (this.targetEntity.squaredDistanceTo(this.ender_watcher) > 256.0 && this.ticksSinceUnseenTeleport++ >= this.getTickCount(30) && this.ender_watcher.teleportTo(this.targetEntity)) {
                        this.ticksSinceUnseenTeleport = 0;
                    }
                }
                super.tick();
            }
        }
    }

    static class PlaceBlockGoal extends Goal {
        private final WatcherEntity ender_watcher;

        public PlaceBlockGoal(WatcherEntity enderman) {
            this.ender_watcher = enderman;
        }

        @Override
        public boolean canStart() {
            if (this.ender_watcher.getCarriedBlock() == null) {
                return false;
            }
            if (!this.ender_watcher.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                return false;
            }
            return this.ender_watcher.getRandom().nextInt(WatcherEntity.PlaceBlockGoal.toGoalTicks(5)) == 0;
        }

        @Override
        public void tick() {
            Random random = this.ender_watcher.getRandom();
            World world = this.ender_watcher.getWorld();
            int i = MathHelper.floor(this.ender_watcher.getX() - 1.0 + random.nextDouble() * 2.0);
            int j = MathHelper.floor(this.ender_watcher.getY() + random.nextDouble() * 2.0);
            int k = MathHelper.floor(this.ender_watcher.getZ() - 1.0 + random.nextDouble() * 2.0);
            BlockPos blockPos = new BlockPos(i, j, k);
            BlockState blockState = world.getBlockState(blockPos);
            BlockPos blockPos2 = blockPos.down();
            BlockState blockState2 = world.getBlockState(blockPos2);
            BlockState blockState3 = this.ender_watcher.getCarriedBlock();
            if (blockState3 == null) {
                return;
            }
            if (this.canPlaceOn(world, blockPos, blockState3 = Block.postProcessState(blockState3, this.ender_watcher.getWorld(), blockPos), blockState, blockState2, blockPos2)) {
                world.setBlockState(blockPos, blockState3, Block.NOTIFY_ALL);
                world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(this.ender_watcher, blockState3));
                this.ender_watcher.getWorld().playSound(null, this.ender_watcher.prevX, this.ender_watcher.prevY, this.ender_watcher.prevZ, ModSounds.WATCHER_PLACE_EVENT, this.ender_watcher.getSoundCategory(), 1.0f, 1.0f);
                this.ender_watcher.setCarriedBlock(null);
            }
        }

        private boolean canPlaceOn(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove, BlockState state, BlockPos pos) {
            return stateAbove.isAir() && !state.isAir() && !state.isOf(Blocks.BEDROCK) && state.isFullCube(world, pos) && carriedState.canPlaceAt(world, posAbove) && world.getOtherEntities(this.ender_watcher, Box.from(Vec3d.of(posAbove))).isEmpty();
        }
    }

    static class PickUpBlockGoal extends Goal {
        private final WatcherEntity ender_watcher;

        public PickUpBlockGoal(WatcherEntity ender_watcher) {
            this.ender_watcher = ender_watcher;
        }

        @Override
        public boolean canStart() {
            if (this.ender_watcher.getCarriedBlock() != null) {
                return false;
            }
            if (this.ender_watcher.getTarget() == null) {
                return false;
            }
            if (!this.ender_watcher.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                return false;
            }
            return this.ender_watcher.getRandom().nextInt(PickUpBlockGoal.toGoalTicks(5)) == 0;
        }

        @Override
        public void tick() {
            Random random = this.ender_watcher.random;
            World world = this.ender_watcher.getWorld();
            LivingEntity target = ender_watcher.getTarget();
            int randomOffset = random.nextInt(3);
            int i = target.getBlockX();
            int j = target.getBlockY();
            int k = target.getBlockZ();
            Vec3d vec3d = new Vec3d((double)this.ender_watcher.getBlockX() + 0.5, (double)this.ender_watcher.getEyeY() + 0.5 - randomOffset, (double)this.ender_watcher.getBlockZ() + 0.5);
            Vec3d vec3d2 = new Vec3d((double)i + 0.5, (double)j + 0.5, (double)k + 0.5);
            BlockHitResult blockHitResult = world.raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.ender_watcher));
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            BlockState targetBlockState = world.getBlockState(targetBlockPos);
            if (!targetBlockState.isIn(ModTags.ENDER_WATCHER_UNHOLDABLE) &&
                    this.ender_watcher.squaredDistanceTo(
                            targetBlockPos.getX(),
                            targetBlockPos.getY(),
                            targetBlockPos.getZ()) < 10) {
                world.removeBlock(targetBlockPos, false);
                world.emitGameEvent(GameEvent.BLOCK_DESTROY, targetBlockPos, GameEvent.Emitter.of(this.ender_watcher, targetBlockState));
                this.ender_watcher.setCarriedBlock(targetBlockState.getBlock().getDefaultState());
                this.ender_watcher.getWorld().playSound(null, this.ender_watcher.prevX, this.ender_watcher.prevY, this.ender_watcher.prevZ, ModSounds.WATCHER_BREAK_EVENT, this.ender_watcher.getSoundCategory(), 1.0f, 1.0f);
            }
        }
    }

    ///////////////
    // ANIMATIONS

    private AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {

        if (tAnimationState.isMoving()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.ender_watcher.walk", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.ender_watcher.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
