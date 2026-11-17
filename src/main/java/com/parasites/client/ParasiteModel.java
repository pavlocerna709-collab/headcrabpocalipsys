package com.parasites.client;

import com.parasites.ParasiteEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ParasiteModel extends EntityModel<ParasiteEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(new Identifier("parasites", "parasite"), "main");
    private final ModelPart root;
    private final ModelPart[] legs = new ModelPart[4];

    public ParasiteModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg" + i);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot();
        r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -4f, -4f, 8f, 4f, 8f),
                ModelTransform.pivot(0f, 22f, 0f));
        float[][] p = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int i = 0; i < 4; i++) {
            r.addChild("leg" + i, ModelPartBuilder.create().uv(0, 12).cuboid(-1f, 0f, -1f, 2f, 2f, 2f),
                    ModelTransform.pivot(p[i][0], 22f, p[i][1]));
        }
        return TexturedModelData.of(md, 32, 32);
    }

    @Override
    public void setAngles(ParasiteEntity e, float limbAngle, float limbDistance, float anim, float headYaw, float headPitch) {
        for (int i = 0; i < 4; i++) {
            legs[i].pitch = MathHelper.cos(limbAngle * 1.2f + i * (float) Math.PI / 2f) * 1.4f * limbDistance;
        }
    }

    @Override
    public void render(MatrixStack m, VertexConsumer v, int light, int overlay, float r, float g, float b, float a) {
        root.render(m, v, light, overlay, r, g, b, a);
    }
}
