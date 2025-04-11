package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestMobDropsPayload(Identifier mobId) implements CustomPayload {

    public static final CustomPayload.Id<RequestMobDropsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "request_mob_drops"));

    public static final PacketCodec<RegistryByteBuf, RequestMobDropsPayload> CODEC =
            PacketCodec.of(RequestMobDropsPayload::write, RequestMobDropsPayload::read);

    public static RequestMobDropsPayload read(RegistryByteBuf buf) {
        return new RequestMobDropsPayload(buf.readIdentifier());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(mobId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
