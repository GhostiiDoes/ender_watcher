package ghostiidoes.ender_watcher.entity.client;

import ghostiidoes.ender_watcher.EnderWatcherMod;
import ghostiidoes.ender_watcher.entity.custom.WatcherEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class WatcherModel extends GeoModel<WatcherEntity> {
    @Override
    public Identifier getModelResource(WatcherEntity animatable) {
        return new Identifier(EnderWatcherMod.MOD_ID, "geo/ender_watcher.geo.json");
    }

    @Override
    public Identifier getTextureResource(WatcherEntity animatable) {
        return new Identifier(EnderWatcherMod.MOD_ID, "textures/entity/ender_watcher.png");
    }

    @Override
    public Identifier getAnimationResource(WatcherEntity animatable) {
        return new Identifier(EnderWatcherMod.MOD_ID, "animations/ender_watcher.animation.json");
    }

    @Override
    public void setCustomAnimations(WatcherEntity animatable, long instanceId, AnimationState<WatcherEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
            head.setRotY(entityData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
        }

        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
