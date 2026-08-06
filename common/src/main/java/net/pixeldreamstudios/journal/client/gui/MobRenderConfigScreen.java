package net.pixeldreamstudios.journal.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class MobRenderConfigScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation mobId;
    private final boolean isGridMode;

    private ConfigSlider scaleSlider;
    private ConfigSlider xOffsetSlider;
    private ConfigSlider yOffsetSlider;
    private ConfigSlider speedSlider;
    private ConfigSlider smoothingSlider;

    private EditBox scaleField;
    private EditBox xOffsetField;
    private EditBox yOffsetField;
    private EditBox speedField;
    private EditBox smoothingField;

    private Button animationModeButton;

    private LivingEntity previewEntity;
    private float currentScale;
    private float currentXOffset;
    private float currentYOffset;
    private float currentSpeed;
    private float currentSmoothing;
    private JournalConfig.AnimationMode currentAnimationMode;

    private final Map<ResourceLocation, CachedPose> poseCache = new HashMap<>();

    private int previewBoxSize = 0;
    private int previewBoxX = 0;
    private int previewBoxY = 0;

    private static class CachedPose {
        float yaw;
        float prevYaw;
        int age;
        long lastUpdated;
        float limbSwing;
        float limbSwingAmount;
        boolean initialized;

        CachedPose() {
            this.yaw = 0f;
            this.prevYaw = 0f;
            this.limbSwing = 0f;
            this.limbSwingAmount = 1.0f;
            this.initialized = false;
            this.lastUpdated = System.currentTimeMillis();
        }
    }

    private static class ConfigSlider extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private float currentValue;
        private final Consumer<Float> onChange;
        private boolean updatingFromExternal = false;

        public ConfigSlider(int x, int y, int width, int height, String label, float min, float max, float initial, Consumer<Float> onChange) {
            super(x, y, width, height, Component.literal(label), normalizeValue(initial, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.currentValue = initial;
            this.onChange = onChange;
            updateMessage();
        }

        private static double normalizeValue(float value, float min, float max) {
            return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
        }

        @Override
        protected void updateMessage() {
            currentValue = min + (float) (value * (max - min));
            setMessage(Component.literal(label));
        }

        @Override
        protected void applyValue() {
            if (!updatingFromExternal) {
                currentValue = min + (float) (value * (max - min));
                if (onChange != null) {
                    onChange.accept(currentValue);
                }
                updateMessage();
            }
        }

        public float getFloatValue() {
            return currentValue;
        }

        public void setValueFromExternal(float newValue) {
            updatingFromExternal = true;
            this.currentValue = newValue;
            this.value = normalizeValue(this.currentValue, min, max);
            updateMessage();
            updatingFromExternal = false;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }

    public MobRenderConfigScreen(Screen parent, ResourceLocation mobId, boolean isGridMode) {
        super(Component.literal("Mob Render Config"));
        this.parent = parent;
        this.mobId = mobId;
        this.isGridMode = isGridMode;

        JournalConfig.MobRenderConfig config = JournalConfig.getMobRenderConfig(mobId, isGridMode);
        this.currentScale = config.scale;
        this.currentXOffset = config.xOffset;
        this.currentYOffset = config.yOffset;
        this.currentSpeed = config.speed;
        this.currentSmoothing = config.smoothing;
        this.currentAnimationMode = config.animationMode;
    }

    @Override
    protected void init() {
        super.init();

        var level = Minecraft.getInstance().level;
        if (level != null) {
            this.previewEntity = MobEntityCache.get(mobId, level);
        }

        Font font = Minecraft.getInstance().font;
        int centerX = this.width / 2;

        int sliderHeight = 18;
        int sliderSpacing = 20;
        int buttonHeight = 18;
        int buttonSpacing = 3;

        int headerHeight = 30;
        int footerHeight = 15;
        int previewMinHeight = 30;

        int availableHeight = this.height - headerHeight - footerHeight;
        int sliderAreaHeight = (sliderHeight * 5) + (sliderSpacing * 4) + sliderHeight + sliderSpacing;
        int buttonAreaHeight = (buttonHeight * 2) + buttonSpacing;

        int totalNeededHeight = sliderAreaHeight + buttonAreaHeight + 10;
        int previewAreaHeight = Math.max(previewMinHeight, availableHeight - totalNeededHeight);

        if (previewAreaHeight < previewMinHeight) {
            previewAreaHeight = 0;
        }

        int startY = headerHeight + previewAreaHeight + 5;

        int maxWidth = Math.min(this.width - 20, 300);
        int fieldWidth = 40;
        int gap = 3;
        int sliderWidth = maxWidth - fieldWidth - gap;

        if (sliderWidth < 80) {
            sliderWidth = Math.max(60, this.width - 80);
            fieldWidth = Math.max(30, this.width - sliderWidth - gap - 20);
        }

        int sliderX = centerX - (sliderWidth + gap + fieldWidth) / 2;
        int fieldX = sliderX + sliderWidth + gap;

        this.scaleSlider = new ConfigSlider(
                sliderX, startY, sliderWidth, sliderHeight,
                "Scale", 0.1f, 10.0f, Math.max(0.1f, Math.min(10.0f, currentScale)),
                val -> {
                    currentScale = val;
                    if (!scaleField.isFocused()) {
                        scaleField.setValue(String.format("%.2f", val));
                    }
                }
        );
        this.addRenderableWidget(scaleSlider);

        this.scaleField = new EditBox(font, fieldX, startY, fieldWidth, sliderHeight, Component.literal(""));
        this.scaleField.setValue(String.format("%.2f", currentScale));
        this.scaleField.setMaxLength(10);
        this.scaleField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                currentScale = value;
                if (value >= scaleSlider.getMin() && value <= scaleSlider.getMax()) {
                    scaleSlider.setValueFromExternal(value);
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addWidget(scaleField);

        this.xOffsetSlider = new ConfigSlider(
                sliderX, startY + sliderSpacing, sliderWidth, sliderHeight,
                "X Offset", -100f, 100f, Math.max(-100f, Math.min(100f, currentXOffset)),
                val -> {
                    currentXOffset = val;
                    if (!xOffsetField.isFocused()) {
                        xOffsetField.setValue(String.format("%.2f", val));
                    }
                }
        );
        this.addRenderableWidget(xOffsetSlider);

        this.xOffsetField = new EditBox(font, fieldX, startY + sliderSpacing, fieldWidth, sliderHeight, Component.literal(""));
        this.xOffsetField.setValue(String.format("%.2f", currentXOffset));
        this.xOffsetField.setMaxLength(10);
        this.xOffsetField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                currentXOffset = value;
                if (value >= xOffsetSlider.getMin() && value <= xOffsetSlider.getMax()) {
                    xOffsetSlider.setValueFromExternal(value);
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addWidget(xOffsetField);

        this.yOffsetSlider = new ConfigSlider(
                sliderX, startY + sliderSpacing * 2, sliderWidth, sliderHeight,
                "Y Offset", -100f, 100f, Math.max(-100f, Math.min(100f, currentYOffset)),
                val -> {
                    currentYOffset = val;
                    if (!yOffsetField.isFocused()) {
                        yOffsetField.setValue(String.format("%.2f", val));
                    }
                }
        );
        this.addRenderableWidget(yOffsetSlider);

        this.yOffsetField = new EditBox(font, fieldX, startY + sliderSpacing * 2, fieldWidth, sliderHeight, Component.literal(""));
        this.yOffsetField.setValue(String.format("%.2f", currentYOffset));
        this.yOffsetField.setMaxLength(10);
        this.yOffsetField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                currentYOffset = value;
                if (value >= yOffsetSlider.getMin() && value <= yOffsetSlider.getMax()) {
                    yOffsetSlider.setValueFromExternal(value);
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addWidget(yOffsetField);

        this.speedSlider = new ConfigSlider(
                sliderX, startY + sliderSpacing * 3, sliderWidth, sliderHeight,
                "Speed", 0.0f, 2.0f, Math.max(0.0f, Math.min(2.0f, currentSpeed)),
                val -> {
                    currentSpeed = val;
                    if (!speedField.isFocused()) {
                        speedField.setValue(String.format("%.2f", val));
                    }
                }
        );
        this.addRenderableWidget(speedSlider);

        this.speedField = new EditBox(font, fieldX, startY + sliderSpacing * 3, fieldWidth, sliderHeight, Component.literal(""));
        this.speedField.setValue(String.format("%.2f", currentSpeed));
        this.speedField.setMaxLength(10);
        this.speedField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                currentSpeed = value;
                if (value >= speedSlider.getMin() && value <= speedSlider.getMax()) {
                    speedSlider.setValueFromExternal(value);
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addWidget(speedField);

        this.smoothingSlider = new ConfigSlider(
                sliderX, startY + sliderSpacing * 4, sliderWidth, sliderHeight,
                "Smooth", 1.0f, 50.0f, Math.max(1.0f, Math.min(50.0f, currentSmoothing)),
                val -> {
                    currentSmoothing = val;
                    if (!smoothingField.isFocused()) {
                        smoothingField.setValue(String.format("%.2f", val));
                    }
                }
        );
        this.addRenderableWidget(smoothingSlider);

        this.smoothingField = new EditBox(font, fieldX, startY + sliderSpacing * 4, fieldWidth, sliderHeight, Component.literal(""));
        this.smoothingField.setValue(String.format("%.2f", currentSmoothing));
        this.smoothingField.setMaxLength(10);
        this.smoothingField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                currentSmoothing = value;
                if (value >= smoothingSlider.getMin() && value <= smoothingSlider.getMax()) {
                    smoothingSlider.setValueFromExternal(value);
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addWidget(smoothingField);

        int animButtonY = startY + sliderSpacing * 5;
        int animButtonWidth = sliderWidth + gap + fieldWidth;
        int animButtonX = centerX - animButtonWidth / 2;

        this.animationModeButton = Button.builder(
                Component.literal("Animation: " + currentAnimationMode.name()),
                btn -> {
                    currentAnimationMode = switch (currentAnimationMode) {
                        case IDLE -> JournalConfig.AnimationMode.WALKING;
                        case WALKING -> JournalConfig.AnimationMode.STATIC;
                        case STATIC -> JournalConfig.AnimationMode.IDLE;
                    };
                    btn.setMessage(Component.literal("Animation: " + currentAnimationMode.name()));
                }
        ).bounds(animButtonX, animButtonY, animButtonWidth, sliderHeight).build();
        this.addRenderableWidget(animationModeButton);

        int buttonY = animButtonY + sliderSpacing;
        int totalButtonWidth = sliderWidth + gap + fieldWidth;
        int buttonWidth = Math.max(40, (totalButtonWidth - buttonSpacing * 4) / 5);

        int buttonRowWidth = (buttonWidth * 5) + (buttonSpacing * 4);
        int buttonStartX = centerX - buttonRowWidth / 2;

        Button saveButton = Button.builder(
                Component.literal("Save"),
                btn -> {
                    JournalConfig.setMobRenderConfig(mobId, isGridMode, currentScale, currentXOffset, currentYOffset, currentSpeed, currentSmoothing, currentAnimationMode);
                    JournalConfig.saveRenderConfigs();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).bounds(buttonStartX, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(saveButton);

        Button resetButton = Button.builder(
                Component.literal("Reset"),
                btn -> {
                    currentScale = 1.0f;
                    currentXOffset = 0.0f;
                    currentYOffset = 0.0f;
                    currentSpeed = 0.6f;
                    currentSmoothing = 12.0f;
                    currentAnimationMode = JournalConfig.AnimationMode.WALKING;
                    scaleSlider.setValueFromExternal(currentScale);
                    xOffsetSlider.setValueFromExternal(currentXOffset);
                    yOffsetSlider.setValueFromExternal(currentYOffset);
                    speedSlider.setValueFromExternal(currentSpeed);
                    smoothingSlider.setValueFromExternal(currentSmoothing);
                    scaleField.setValue(String.format("%.2f", currentScale));
                    xOffsetField.setValue(String.format("%.2f", currentXOffset));
                    yOffsetField.setValue(String.format("%.2f", currentYOffset));
                    speedField.setValue(String.format("%.2f", currentSpeed));
                    smoothingField.setValue(String.format("%.2f", currentSmoothing));
                    animationModeButton.setMessage(Component.literal("Animation: " + currentAnimationMode.name()));
                }
        ).bounds(buttonStartX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(resetButton);

        Button copyButton = Button.builder(
                Component.literal("Copy"),
                btn -> {
                    JournalConfig.MobRenderConfig otherConfig = JournalConfig.getMobRenderConfig(mobId, !isGridMode);
                    currentScale = otherConfig.scale;
                    currentXOffset = otherConfig.xOffset;
                    currentYOffset = otherConfig.yOffset;
                    currentSpeed = otherConfig.speed;
                    currentSmoothing = otherConfig.smoothing;
                    currentAnimationMode = otherConfig.animationMode;
                    scaleSlider.setValueFromExternal(currentScale);
                    xOffsetSlider.setValueFromExternal(currentXOffset);
                    yOffsetSlider.setValueFromExternal(currentYOffset);
                    speedSlider.setValueFromExternal(currentSpeed);
                    smoothingSlider.setValueFromExternal(currentSmoothing);
                    scaleField.setValue(String.format("%.2f", currentScale));
                    xOffsetField.setValue(String.format("%.2f", currentXOffset));
                    yOffsetField.setValue(String.format("%.2f", currentYOffset));
                    speedField.setValue(String.format("%.2f", currentSpeed));
                    smoothingField.setValue(String.format("%.2f", currentSmoothing));
                    animationModeButton.setMessage(Component.literal("Animation: " + currentAnimationMode.name()));
                }
        ).bounds(buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight).build();
        copyButton.setTooltip(Tooltip.create(
                Component.literal("Copy from " + (isGridMode ? "Detail" : "Grid"))
        ));
        this.addRenderableWidget(copyButton);

        Button pasteButton = Button.builder(
                Component.literal("Paste"),
                btn -> {
                    JournalConfig.setMobRenderConfig(mobId, !isGridMode, currentScale, currentXOffset, currentYOffset, currentSpeed, currentSmoothing, currentAnimationMode);
                    JournalConfig.saveRenderConfigs();

                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§aPasted to " + (isGridMode ? "Detail" : "Grid")),
                                true
                        );
                    }
                }
        ).bounds(buttonStartX + (buttonWidth + buttonSpacing) * 3, buttonY, buttonWidth, buttonHeight).build();
        pasteButton.setTooltip(Tooltip.create(
                Component.literal("Paste to " + (isGridMode ? "Detail" : "Grid"))
        ));
        this.addRenderableWidget(pasteButton);

        Button cancelButton = Button.builder(
                Component.literal("Back"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).bounds(buttonStartX + (buttonWidth + buttonSpacing) * 4, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(cancelButton);

        Button deleteButton = Button.builder(
                Component.literal("Delete"),
                btn -> {
                    JournalConfig.deleteMobRenderConfig(mobId, isGridMode);
                    JournalConfig.saveRenderConfigs();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).bounds(centerX - buttonWidth / 2, buttonY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight).build();
        deleteButton.setTooltip(Tooltip.create(
                Component.literal("Use defaults")
        ));
        this.addRenderableWidget(deleteButton);

        if (previewAreaHeight > 0 && previewEntity != null) {
            int previewY = headerHeight + previewAreaHeight / 2;
            int baseScale = Math.min(20, previewAreaHeight / 3);
            int previewScale = (int) (baseScale * Math.max(0.1f, Math.min(10.0f, currentScale)));

            previewBoxSize = Math.max(30, Math.min(previewAreaHeight - 10, (int) (previewScale * 2.0f)));
            previewBoxX = centerX - previewBoxSize / 2;
            previewBoxY = previewY - previewBoxSize / 2;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Font font = Minecraft.getInstance().font;

        String title = (isGridMode ? "Grid" : "Detail");
        int titleWidth = font.width(title);
        context.drawString(font, title, this.width / 2 - titleWidth / 2, 8, 0xFFFFFF);

        int mobNameY = 18;
        if (previewEntity != null) {
            String mobName = previewEntity.getDisplayName().getString();
            int maxWidth = this.width - 20;
            if (font.width(mobName) > maxWidth) {
                mobName = font.plainSubstrByWidth(mobName, maxWidth) + "..";
            }
            int nameWidth = font.width(mobName);
            context.drawString(font, mobName, this.width / 2 - nameWidth / 2, mobNameY, 0xFFDD66);
        }

        if (previewBoxSize > 0 && previewEntity != null) {
            context.fill(previewBoxX, previewBoxY, previewBoxX + previewBoxSize, previewBoxY + previewBoxSize, 0x40000000);
            context.renderOutline(previewBoxX, previewBoxY, previewBoxSize, previewBoxSize, 0x80FFFFFF);

            int previewX = previewBoxX + previewBoxSize / 2;
            int previewY = previewBoxY + previewBoxSize / 2;
            int baseScale = Math.min(20, previewBoxSize / 3);
            int previewScale = (int) (baseScale * Math.max(0.01f, currentScale));

            drawMob(context, previewX, previewY, previewScale, mouseX, mouseY, previewEntity, delta);
        }

        scaleField.render(context, mouseX, mouseY, delta);
        xOffsetField.render(context, mouseX, mouseY, delta);
        yOffsetField.render(context, mouseX, mouseY, delta);
        speedField.render(context, mouseX, mouseY, delta);
        smoothingField.render(context, mouseX, mouseY, delta);
    }

    private void drawMob(GuiGraphics context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity, float delta) {
        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack matrices = context.pose();
        dispatcher.setRenderShadow(false);
        matrices.pushPose();
        matrices.translate(x + currentXOffset, y + currentYOffset, 100.0);
        matrices.scale(scale, -scale, scale);
        matrices.translate(0.0, -1.5, 0.0);

        try {
            CachedPose pose = poseCache.computeIfAbsent(mobId, k -> new CachedPose());
            long now = System.currentTimeMillis();

            if (!pose.initialized) {
                pose.limbSwingAmount = currentSpeed;
                pose.initialized = true;
            }

            float limbSwing = 0f;
            float prevLimbSwing = 0f;
            float limbSwingAmount = 0f;

            switch (currentAnimationMode) {
                case IDLE -> {
                    long elapsed = now - pose.lastUpdated;
                    if (elapsed > 0) {
                        pose.limbSwing += (elapsed / 1000.0f) * (currentSmoothing * 0.3f);
                        pose.lastUpdated = now;
                    }
                    limbSwing = (float) Math.sin(pose.limbSwing * 0.3f) * 0.5f;
                    prevLimbSwing = (float) Math.sin((pose.limbSwing - 0.1f) * 0.3f) * 0.5f;
                    limbSwingAmount = 0.1f;
                }
                case WALKING -> {
                    prevLimbSwing = pose.limbSwing;
                    long elapsed = now - pose.lastUpdated;
                    if (elapsed > 0) {
                        pose.limbSwing += (elapsed / 1000.0f) * currentSmoothing;
                        pose.lastUpdated = now;
                    }
                    limbSwing = pose.limbSwing;
                    limbSwingAmount = currentSpeed;
                }
                case STATIC -> {
                    limbSwing = 0f;
                    prevLimbSwing = 0f;
                    limbSwingAmount = 0f;
                }
            }

            if (currentAnimationMode != JournalConfig.AnimationMode.STATIC) {
                pose.prevYaw = pose.yaw;
                pose.yaw = (now % 8000L) / 8000.0f * 360F;
                pose.age = (int) (now / 50L);

                entity.tickCount = pose.age;
                entity.yBodyRotO = pose.prevYaw;
                entity.yBodyRot = pose.yaw;
                entity.yRotO = pose.prevYaw;
                entity.setYRot(pose.yaw);
                entity.setXRot(0.0f);
                entity.yHeadRotO = pose.prevYaw;
                entity.yHeadRot = pose.yaw;

                if (entity instanceof EnderDragon dragon) {
                    dragon.oFlapTime = dragon.flapTime;
                    dragon.flapTime += 0.05f;
                    if (dragon.flapTime > 1.0f) {
                        dragon.flapTime = 0.0f;
                    }

                    dragon.yRotO = pose.prevYaw;
                    dragon.setYRot(pose.yaw);
                    dragon.yBodyRot = pose.yaw;
                    dragon.yBodyRotO = pose.prevYaw;
                    dragon.yHeadRot = pose.yaw;
                    dragon.yHeadRotO = pose.prevYaw;
                }
            } else {
                entity.setYRot(0f);
                entity.setXRot(0f);
                entity.yBodyRot = 0f;
                entity.yBodyRotO = 0f;
                entity.yHeadRot = 0f;
                entity.yHeadRotO = 0f;
            }

            AnimationOverride.set(entity, limbSwing, prevLimbSwing, limbSwingAmount);

            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, delta, matrices, context.bufferSource(), 0xF000F0);
        } catch (Throwable t) {
            matrices.popPose();
            return;
        }
        dispatcher.setRenderShadow(true);
        matrices.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (scaleField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (xOffsetField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (yOffsetField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (speedField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (smoothingField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
