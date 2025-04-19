package net.pixeldreamstudios.journal.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.pixeldreamstudios.journal.registry.ModBlockEntities;

public class MobDisplayBlockEntity extends BlockEntity {
    private Identifier mobId = null;
    private Entity cachedMob = null;

    public MobDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOB_DISPLAY_BLOCK_ENTITY, pos, state);
    }

    public void setMobId(Identifier mobId) {
        this.mobId = mobId;
        this.cachedMob = null;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
        System.out.println("[MobDisplayBlockEntity] setMobId: " + mobId);
    }

    public Identifier getMobId() {
        return mobId;
    }

    public Entity getOrCreateMob() {
        if (cachedMob == null && mobId != null && world != null && world.isClient()) {
            System.out.println("[MobDisplayBlockEntity] Attempting to create mob: " + mobId);

            EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(mobId).orElse(null);
            if (type == null) {
                System.out.println("[MobDisplayBlockEntity] Unknown EntityType: " + mobId);
                return null;
            }

            // Create a fake mob in the client world
            cachedMob = type.create(world);
            if (cachedMob == null) {
                System.out.println("[MobDisplayBlockEntity] Failed to create mob entity for: " + mobId);
                return null;
            }

            cachedMob.updatePosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            System.out.println("[MobDisplayBlockEntity] Created mob entity for render: " + mobId);
        }
        return cachedMob;
    }


    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("MobId")) {
            mobId = Identifier.tryParse(nbt.getString("MobId"));
            System.out.println("[MobDisplayBlockEntity] readNbt MobId: " + mobId);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (mobId != null) {
            nbt.putString("MobId", mobId.toString());
            System.out.println("[MobDisplayBlockEntity] writeNbt MobId: " + mobId);
        }
    }
}
