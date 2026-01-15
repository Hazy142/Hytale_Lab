/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.plugin.PluginInit;
import java.nio.file.Path;
import javax.annotation.Nonnull;

public class JavaPluginInit
extends PluginInit {
    @Nonnull
    private final Path file;
    @Nonnull
    private final PluginClassLoader classLoader;

    public JavaPluginInit(@Nonnull PluginManifest pluginManifest, @Nonnull Path dataDirectory, @Nonnull Path file, @Nonnull PluginClassLoader classLoader) {
        super(pluginManifest, dataDirectory);
        this.file = file;
        this.classLoader = classLoader;
    }

    @Nonnull
    public Path getFile() {
        return this.file;
    }

    @Nonnull
    public PluginClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public boolean isInServerClassPath() {
        return this.classLoader.isInServerClassPath();
    }
}

