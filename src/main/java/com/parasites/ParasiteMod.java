package com.parasites;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

public class ParasiteMod implements ModInitializer {
    public static final String MOD_ID = "parasites";

    public static final StatusEffect INFECTION = Registry.register(
            Registries.STATUS_EFFECT, new Identifier(MOD_ID, "infection"), new InfectionEffect());

    public static final EntityType<ParasiteEntity> PARASITE = reg("parasite", ParasiteEntity.Variant.NORMAL);
    public static final EntityType<ParasiteEntity> FAST_PARASITE = reg("fast_parasite", ParasiteEntity.Variant.FAST);
    public static final EntityType<ParasiteEntity> POISON_PARASITE = reg("poison_parasite", ParasiteEntity.Variant.POISON);

    private static EntityType<ParasiteEntity> reg(String name, ParasiteEntity.Variant v) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, name),
                FabricEntityTypeBuilder.<ParasiteEntity>create(SpawnGroup.MONSTER, (t, w) -> new ParasiteEntity(t, w, v))
                        .dimensions(EntityDimensions.fixed(0.5f, 0.4f))
                        .trackRangeBlocks(64)
                        .build());
    }

    @Override
    public void onInitialize() {
        setup(PARASITE, ParasiteEntity.Variant.NORMAL, 60);
        setup(FAST_PARASITE, ParasiteEntity.Variant.FAST, 35);
        setup(POISON_PARASITE, ParasiteEntity.Variant.POISON, 25);

        Item egg1 = egg("parasite_spawn_egg", PARASITE, 0xC8967A, 0x7A3B2E);
        Item egg2 = egg("fast_parasite_spawn_egg", FAST_PARASITE, 0xD9D38C, 0x8C7A2E);
        Item egg3 = egg("poison_parasite_spawn_egg", POISON_PARASITE, 0x7DBA4A, 0x2E5A1F);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> {
            e.add(egg1); e.add(egg2); e.add(egg3);
        });

        // Player dies while infected -> turns into a zombie with the parasite on its head
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity pl && pl.hasStatusEffect(INFECTION)) {
                ServerWorld w = pl.getServerWorld();
                ParasiteEntity para = pl.getPassengerList().stream()
                        .filter(e -> e instanceof ParasiteEntity)
                        .map(e -> (ParasiteEntity) e).findFirst().orElse(null);
                ZombieEntity z = EntityType.ZOMBIE.create(w);
                if (z == null) return;
                z.refreshPositionAndAngles(pl.getX(), pl.getY(), pl.getZ(), pl.getYaw(), 0);
                z.setCustomName(pl.getName());
                z.setPersistent();
                w.spawnEntity(z);
                if (para != null) {
                    para.stopRiding();
                    para.startRiding(z, true);
                }
            }
        });

        // /apocalypse -> spawns a swarm around you
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("apocalypse")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            ServerWorld w = src.getWorld();
                            Vec3d pos = src.getPosition();
                            for (int i = 0; i < 24; i++) {
                                EntityType<ParasiteEntity> t = i % 3 == 0 ? FAST_PARASITE : (i % 3 == 1 ? POISON_PARASITE : PARASITE);
                                ParasiteEntity e = t.create(w);
                                if (e == null) continue;
                                e.refreshPositionAndAngles(pos.x + w.random.nextInt(17) - 8, pos.y + 1,
                                        pos.z + w.random.nextInt(17) - 8, w.random.nextFloat() * 360f, 0);
                                w.spawnEntity(e);
                            }
                            src.sendFeedback(() -> Text.literal("The apocalypse has begun..."), true);
                            return 1;
                        })));
    }

    private static void setup(EntityType<ParasiteEntity> type, ParasiteEntity.Variant v, int weight) {
        FabricDefaultAttributeRegistry.register(type, ParasiteEntity.createAttributes(v));
        SpawnRestriction.register(type, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, type, weight, 2, 4);
    }

    private static Item egg(String name, EntityType<? extends MobEntity> type, int c1, int c2) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name),
                new SpawnEggItem(type, c1, c2, new Item.Settings()));
    }

    /** Parasite latches on to the target. */
    public static void infect(ParasiteEntity p, LivingEntity t) {
        if (!(p.getWorld() instanceof ServerWorld) || p.hasVehicle() || p.cooldown > 0) return;

        if (t instanceof PlayerEntity pl) {
            if (pl.isCreative() || pl.isSpectator() || pl.hasStatusEffect(INFECTION) || pl.hasPassengers()) return;
            pl.addStatusEffect(new StatusEffectInstance(INFECTION, 20 * 90, 0));
            p.startRiding(pl, true);
            p.setTarget(null);
        } else if (t instanceof VillagerEntity || t instanceof AnimalEntity) {
            MobEntity m = (MobEntity) t;
            boolean baby = m.isBaby();
            MobEntity z = (m instanceof VillagerEntity)
                    ? m.convertTo(EntityType.ZOMBIE_VILLAGER, false)
                    : m.convertTo(EntityType.ZOMBIE, false);
            if (z == null) return;
            z.setPersistent();
            if (z instanceof ZombieEntity ze) ze.setBaby(baby);
            p.startRiding(z, true);
            p.setTarget(null);
        }
    }
}
