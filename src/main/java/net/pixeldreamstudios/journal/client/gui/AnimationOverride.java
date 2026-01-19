package net.pixeldreamstudios.journal.client.gui;

import net.minecraft.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;

public class AnimationOverride {
    private static final Map<LivingEntity, PoseData> OVERRIDES = new WeakHashMap<>();

    public static class PoseData {
        public float currentPos;
        public float prevPos;
        public float speed;

        public PoseData(float currentPos, float prevPos, float speed) {
            this.currentPos = currentPos;
            this.prevPos = prevPos;
            this.speed = speed;
        }
    }

    public static void set(LivingEntity entity, float limbSwing, float prevLimbSwing, float speed) {
        OVERRIDES.put(entity, new PoseData(limbSwing, prevLimbSwing, speed));
    }

    public static PoseData getPoseData(LivingEntity entity) {
        return OVERRIDES.get(entity);
    }

    public static void clear(LivingEntity entity) {
        OVERRIDES.remove(entity);
    }
}