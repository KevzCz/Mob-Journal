package net.pixeldreamstudios.journal.neoforge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.data.FavoriteMobsData;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.MobStatsData;

import java.util.function.Supplier;

public class JournalAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Journal.MOD_ID);

    public static final Supplier<AttachmentType<JournalData>> JOURNAL =
            ATTACHMENTS.register("journal", () -> AttachmentType
                    .builder(JournalData::new)
                    .serialize(new JournalDataSerializer())
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<MobStatsData>> MOB_STATS =
            ATTACHMENTS.register("mob_stats", () -> AttachmentType
                    .builder(MobStatsData::new)
                    .serialize(new MobStatsDataSerializer())
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<FavoriteMobsData>> FAVORITES =
            ATTACHMENTS.register("favorites", () -> AttachmentType
                    .builder(FavoriteMobsData::new)
                    .serialize(new FavoriteMobsDataSerializer())
                    .copyOnDeath()
                    .build());

    private static class JournalDataSerializer implements IAttachmentSerializer<CompoundTag, JournalData> {
        @Override
        public JournalData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            JournalData data = new JournalData();
            data.readFrom(tag);
            return data;
        }

        @Override
        public CompoundTag write(JournalData data, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            data.writeTo(tag);
            return tag;
        }
    }

    private static class MobStatsDataSerializer implements IAttachmentSerializer<CompoundTag, MobStatsData> {
        @Override
        public MobStatsData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            MobStatsData data = new MobStatsData();
            data.readFrom(tag);
            return data;
        }

        @Override
        public CompoundTag write(MobStatsData data, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            data.writeTo(tag);
            return tag;
        }
    }

    private static class FavoriteMobsDataSerializer implements IAttachmentSerializer<CompoundTag, FavoriteMobsData> {
        @Override
        public FavoriteMobsData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            FavoriteMobsData data = new FavoriteMobsData();
            data.readFrom(tag);
            return data;
        }

        @Override
        public CompoundTag write(FavoriteMobsData data, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            data.writeTo(tag);
            return tag;
        }
    }
}
