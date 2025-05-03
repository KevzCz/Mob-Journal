package net.pixeldreamstudios.journal.data;

import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class JournalComponents implements EntityComponentInitializer {

    public static final ComponentKey<JournalComponent> JOURNAL =
            ComponentRegistry.getOrCreate(Identifier.of("journal", "journal"), JournalComponent.class);

    public static final ComponentKey<MobStatTrackerComponent> MOB_STATS =
            ComponentRegistry.getOrCreate(Identifier.of("journal", "mob_stats"), MobStatTrackerComponent.class);
    public static final ComponentKey<FavoriteMobsComponent> FAVORITES =
            ComponentRegistry.getOrCreate(Identifier.of("journal", "favorites"), FavoriteMobsComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(JOURNAL, JournalComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(MOB_STATS, MobStatTrackerComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(FAVORITES, FavoriteMobsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);

    }
}
