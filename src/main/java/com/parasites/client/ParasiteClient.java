package com.parasites.client;

import com.parasites.ParasiteMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ParasiteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(ParasiteModel.LAYER, ParasiteModel::getTexturedModelData);
        EntityRendererRegistry.register(ParasiteMod.PARASITE, ParasiteRenderer::new);
        EntityRendererRegistry.register(ParasiteMod.FAST_PARASITE, ParasiteRenderer::new);
        EntityRendererRegistry.register(ParasiteMod.POISON_PARASITE, ParasiteRenderer::new);
    }
}
