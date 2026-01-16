package net.pixeldreamstudios.journal.mixin;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {
    @Accessor("pos")
    void setPos(float pos);

    @Accessor("speed")
    void setSpeed(float speed);

    @Accessor("prevSpeed")
    void setPrevSpeed(float prevSpeed);

    @Accessor("pos")
    float getPos();

    @Accessor("speed")
    float getSpeed();
}