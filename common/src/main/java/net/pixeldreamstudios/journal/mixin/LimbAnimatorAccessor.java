package net.pixeldreamstudios.journal.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface LimbAnimatorAccessor {
    @Accessor("position")
    void setPos(float pos);

    @Accessor("speed")
    void setSpeed(float speed);

    @Accessor("speedOld")
    void setPrevSpeed(float prevSpeed);

    @Accessor("position")
    float getPos();

    @Accessor("speed")
    float getSpeed();
}
