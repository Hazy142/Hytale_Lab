/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.plugin.PluginType;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public abstract class JavaPlugin
extends PluginBase {
    @Nonnull
    private final Path file;
    @Nonnull
    private final PluginClassLoader classLoader;

    public JavaPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.file = init.getFile();
        this.classLoader = init.getClassLoader();
        this.classLoader.setPlugin(this);
    }

    @Nonnull
    public Path getFile() {
        return this.file;
    }

    @Override
    protected void start0() {
        super.start0();
        if (this.getManifest().includesAssetPack()) {
            String id;
            AssetModule assetModule = AssetModule.get();
            AssetPack existing = assetModule.getAssetPack(id = new PluginIdentifier(this.getManifest()).toString());
            if (existing != null) {
                this.getLogger().at(Level.WARNING).log("Asset pack %s already exists, skipping embedded pack", id);
                return;
            }
            assetModule.registerPack(id, this.file, this.getManifest());
        }
    }

    @Nonnull
    public PluginClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    @Nonnull
    public final PluginType getType() {
        return PluginType.PLUGIN;
    }
}

