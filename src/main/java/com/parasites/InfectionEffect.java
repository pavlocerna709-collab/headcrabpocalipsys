package com.parasites;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class InfectionEffect extends StatusEffect {
    public InfectionEffect() {
        super(StatusEffectCategory.HARMFUL, 0x6B8E23);
        addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                "7107DE5E-7CE8-4030-940E-514C1F160890", -0.15,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.damage(entity.getDamageSources().magic(), 1.0f);
    }
}
