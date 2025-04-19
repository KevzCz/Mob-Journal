    package net.pixeldreamstudios.journal.network;

    import net.minecraft.network.RegistryByteBuf;
    import net.minecraft.network.codec.PacketCodec;
    import net.minecraft.network.packet.CustomPayload;
    import net.minecraft.util.Identifier;
    import net.minecraft.util.math.BlockPos;

    public record SetMobIdPayload(BlockPos pos, Identifier mobId) implements CustomPayload {
        public static final Id<SetMobIdPayload> ID = new Id<>(Identifier.of("journal", "set_mob_id"));
        public static final PacketCodec<RegistryByteBuf, SetMobIdPayload> CODEC =
                PacketCodec.of(SetMobIdPayload::write, SetMobIdPayload::read);

        public static SetMobIdPayload read(RegistryByteBuf buf) {
            return new SetMobIdPayload(buf.readBlockPos(), buf.readIdentifier());
        }

        public void write(RegistryByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeIdentifier(mobId);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
