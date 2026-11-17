package com.parasites;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class ParasiteEntity extends HostileEntity {
    public enum Variant { NORMAL, FAST, POISON }

    private final Variant variant;
    public int cooldown = 0;

    public ParasiteEntity(EntityType<ParasiteEntity> type, World world, Variant variant) {
        super(type, world);
        this.variant = variant;
    }

    public Variant getVariant() {
        return variant;
    }

    public static DefaultAttributeContainer.Builder createAttributes(Variant v) {
        double hp, speed, dmg;
        switch (v) {
            case FAST -> { hp = 8; speed = 0.48; dmg = 2; }
            case POISON -> { hp = 14; speed = 0.27; dmg = 3; }
            default -> { hp = 10; speed = 0.30; dmg = 2; }
        }
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, hp)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, dmg)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new PounceAtTargetGoal(this, 0.45f));
        goalSelector.add(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, VillagerEntity.class, true));
        targetSelector.add(4, new ActiveTargetGoal<>(this, AnimalEntity.class, true));
    }

    // Parasite sits on top of the host's head
    @Override
    public double getHeightOffset() {
        return 0.5;
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(hasVehicle() ? null : target);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (cooldown > 0) cooldown--;
        Entity v = getVehicle();
        if (v instanceof LivingEntity host) {
            getNavigation().stop();
            if (host.isDead()) {
                stopRiding();                       // host killed -> parasite is freed
            } else if (host instanceof PlayerEntity p && !p.hasStatusEffect(ParasiteMod.INFECTION)) {
                stopRiding();                       // player cured / infection ended
            }
        }
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        cooldown = 100;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity le && !hasVehicle()) {
            if (variant == Variant.POISON) {
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 0), this);
            }
            if (random.nextFloat() < 0.5f) {
                ParasiteMod.infect(this, le);
            }
        }
        return hit;
    }

    @Override
    public void onDeath(DamageSource source) {
        Entity v = getVehicle();
        super.onDeath(source);
        if (!getWorld().isClient && v instanceof LivingEntity host) {
            if (host instanceof PlayerEntity p) {
                p.removeStatusEffect(ParasiteMod.INFECTION);   // player is freed
            } else {
                host.damage(getDamageSources().genericKill(), 1000f); // zombie dies with the parasite
            }
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SILVERFISH_DEATH; }
}
