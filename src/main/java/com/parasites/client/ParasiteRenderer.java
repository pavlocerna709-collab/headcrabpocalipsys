package com.parasites.client;

import com.parasites.ParasiteEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ParasiteRenderer extends MobEntityRenderer<ParasiteEntity, ParasiteModel> {
    private static final Identifier NORMAL = new Identifier("parasites", "textures/entity/parasite.png");
    private static final Identifier FAST = new Identifier("parasites", "textures/entity/parasite_fast.png");
    private static final Identifier POISON = new Identifier("parasites", "textures/entity/parasite_poison.png");

    public ParasiteRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ParasiteModel(ctx.getPart(ParasiteModel.LAYER)), 0.25f);
    }

    @Override
    public Identifier getTexture(ParasiteEntity e) {
        return switch (e.getVariant()) {
            case FAST -> FAST;
            case POISON -> POISON;
            default -> NORMAL;
        };
    }
}
