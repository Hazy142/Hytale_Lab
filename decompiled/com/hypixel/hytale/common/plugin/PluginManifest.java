/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.common.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.ObjectMapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.common.util.java.ManifestUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PluginManifest {
    @Nonnull
    private static final BuilderCodec.Builder<PluginManifest> BUILDER = BuilderCodec.builder(PluginManifest.class, PluginManifest::new);
    @Nonnull
    public static final Codec<PluginManifest> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BUILDER.append(new KeyedCodec<String>("Group", Codec.STRING), (manifest, o) -> {
        manifest.group = o;
    }, manifest -> manifest.group).add()).append(new KeyedCodec<String>("Name", Codec.STRING), (manifest, o) -> {
        manifest.name = o;
    }, manifest -> manifest.name).addValidator(Validators.nonNull()).add()).append(new KeyedCodec<Semver>("Version", Semver.CODEC), (manifest, o) -> {
        manifest.version = o;
    }, manifest -> manifest.version).add()).append(new KeyedCodec<String>("Description", Codec.STRING), (manifest, o) -> {
        manifest.description = o;
    }, manifest -> manifest.description).add()).append(new KeyedCodec<T[]>("Authors", new ArrayCodec<AuthorInfo>(AuthorInfo.CODEC, AuthorInfo[]::new)), (manifest, o) -> {
        manifest.authors = List.of(o);
    }, manifest -> (AuthorInfo[])manifest.authors.toArray(AuthorInfo[]::new)).add()).append(new KeyedCodec<String>("Website", Codec.STRING), (manifest, o) -> {
        manifest.website = o;
    }, manifest -> manifest.website).add()).append(new KeyedCodec<String>("Main", Codec.STRING), (manifest, o) -> {
        manifest.main = o;
    }, manifest -> manifest.main).add()).append(new KeyedCodec<SemverRange>("ServerVersion", SemverRange.CODEC), (manifest, o) -> {
        manifest.serverVersion = o;
    }, manifest -> manifest.serverVersion).add()).append(new KeyedCodec("Dependencies", new ObjectMapCodec<PluginIdentifier, SemverRange, Object2ObjectLinkedOpenHashMap>(SemverRange.CODEC, Object2ObjectLinkedOpenHashMap::new, PluginIdentifier::toString, PluginIdentifier::fromString)), (manifest, o) -> {
        manifest.dependencies = o;
    }, manifest -> manifest.dependencies).add()).append(new KeyedCodec("OptionalDependencies", new ObjectMapCodec<PluginIdentifier, SemverRange, Object2ObjectLinkedOpenHashMap>(SemverRange.CODEC, Object2ObjectLinkedOpenHashMap::new, PluginIdentifier::toString, PluginIdentifier::fromString)), (manifest, o) -> {
        manifest.optionalDependencies = o;
    }, manifest -> manifest.optionalDependencies).add()).append(new KeyedCodec("LoadBefore", new ObjectMapCodec<PluginIdentifier, SemverRange, Object2ObjectLinkedOpenHashMap>(SemverRange.CODEC, Object2ObjectLinkedOpenHashMap::new, PluginIdentifier::toString, PluginIdentifier::fromString)), (manifest, o) -> {
        manifest.loadBefore = o;
    }, manifest -> manifest.loadBefore).add()).append(new KeyedCodec<Boolean>("DisabledByDefault", Codec.BOOLEAN), (manifest, o) -> {
        manifest.disabledByDefault = o;
    }, manifest -> manifest.disabledByDefault).add()).append(new KeyedCodec<Boolean>("IncludesAssetPack", Codec.BOOLEAN), (manifest, o) -> {
        manifest.includesAssetPack = o;
    }, o -> o.includesAssetPack).add()).build();
    @Nonnull
    public static final Codec<PluginManifest[]> ARRAY_CODEC;
    private static final String CORE_GROUP = "Hytale";
    private static final Semver CORE_VERSION;
    private String group;
    private String name;
    private Semver version;
    @Nullable
    private String description;
    @Nonnull
    private List<AuthorInfo> authors = new ObjectArrayList<AuthorInfo>();
    @Nullable
    private String website;
    @Nullable
    private String main;
    private SemverRange serverVersion;
    @Nonnull
    private Map<PluginIdentifier, SemverRange> dependencies = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();
    @Nonnull
    private Map<PluginIdentifier, SemverRange> optionalDependencies = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();
    @Nonnull
    private Map<PluginIdentifier, SemverRange> loadBefore = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();
    @Nonnull
    private List<PluginManifest> subPlugins = new ArrayList<PluginManifest>();
    private boolean disabledByDefault = false;
    private boolean includesAssetPack = false;

    public PluginManifest() {
    }

    public PluginManifest(@Nonnull String group, @Nonnull String name, @Nonnull Semver version, @Nullable String description, @Nonnull List<AuthorInfo> authors, @Nullable String website, @Nullable String main, @Nullable SemverRange serverVersion, @Nonnull Map<PluginIdentifier, SemverRange> dependencies, @Nonnull Map<PluginIdentifier, SemverRange> optionalDependencies, @Nonnull Map<PluginIdentifier, SemverRange> loadBefore, @Nonnull List<PluginManifest> subPlugins, boolean disabledByDefault) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.website = website;
        this.main = main;
        this.serverVersion = serverVersion;
        this.dependencies = dependencies;
        this.optionalDependencies = optionalDependencies;
        this.loadBefore = loadBefore;
        this.subPlugins = subPlugins;
        this.disabledByDefault = disabledByDefault;
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

    public Semver getVersion() {
        return this.version;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @Nonnull
    public List<AuthorInfo> getAuthors() {
        return Collections.unmodifiableList(this.authors);
    }

    @Nullable
    public String getWebsite() {
        return this.website;
    }

    public void setGroup(@Nonnull String group) {
        this.group = group;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    public void setVersion(@Nullable Semver version) {
        this.version = version;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setAuthors(@Nonnull List<AuthorInfo> authors) {
        this.authors = authors;
    }

    public void setWebsite(@Nullable String website) {
        this.website = website;
    }

    @Nullable
    public String getMain() {
        return this.main;
    }

    public SemverRange getServerVersion() {
        return this.serverVersion;
    }

    @Nonnull
    public Map<PluginIdentifier, SemverRange> getDependencies() {
        return Collections.unmodifiableMap(this.dependencies);
    }

    public void injectDependency(PluginIdentifier identifier, SemverRange range) {
        this.dependencies.put(identifier, range);
    }

    @Nonnull
    public Map<PluginIdentifier, SemverRange> getOptionalDependencies() {
        return Collections.unmodifiableMap(this.optionalDependencies);
    }

    @Nonnull
    public Map<PluginIdentifier, SemverRange> getLoadBefore() {
        return Collections.unmodifiableMap(this.loadBefore);
    }

    public boolean isDisabledByDefault() {
        return this.disabledByDefault;
    }

    public boolean includesAssetPack() {
        return this.includesAssetPack;
    }

    @Nonnull
    public List<PluginManifest> getSubPlugins() {
        return Collections.unmodifiableList(this.subPlugins);
    }

    public void inherit(@Nonnull PluginManifest manifest) {
        if (this.group == null) {
            this.group = manifest.group;
        }
        if (this.version == null) {
            this.version = manifest.version;
        }
        if (this.description == null) {
            this.description = manifest.description;
        }
        if (this.authors.isEmpty()) {
            this.authors = manifest.authors;
        }
        if (this.website == null) {
            this.website = manifest.website;
        }
        if (!this.disabledByDefault) {
            this.disabledByDefault = manifest.disabledByDefault;
        }
        this.dependencies.put(new PluginIdentifier(manifest), SemverRange.fromString(manifest.version.toString()));
    }

    @Nonnull
    public String toString() {
        return "PluginManifest{group='" + this.group + "', name='" + this.name + "', version='" + String.valueOf(this.version) + "', description='" + this.description + "', authors=" + String.valueOf(this.authors) + ", website='" + this.website + "', main='" + this.main + "', serverVersion=" + String.valueOf(this.serverVersion) + ", dependencies=" + String.valueOf(this.dependencies) + ", optionalDependencies=" + String.valueOf(this.optionalDependencies) + ", disabledByDefault=" + this.disabledByDefault + "}";
    }

    @Nonnull
    public static CoreBuilder corePlugin(@Nonnull Class<?> pluginClass) {
        return new CoreBuilder(CORE_GROUP, pluginClass.getSimpleName(), CORE_VERSION, pluginClass.getName());
    }

    static {
        BUILDER.append(new KeyedCodec<T[]>("SubPlugins", new ArrayCodec<PluginManifest>(CODEC, PluginManifest[]::new)), (manifest, o) -> {
            manifest.subPlugins = List.of(o);
        }, manifest -> (PluginManifest[])manifest.subPlugins.toArray(PluginManifest[]::new)).add();
        ARRAY_CODEC = new ArrayCodec<PluginManifest>(CODEC, PluginManifest[]::new);
        CORE_VERSION = ManifestUtil.getVersion() == null ? Semver.fromString("0.0.0-dev") : ManifestUtil.getVersion();
    }

    public static class CoreBuilder {
        private static final String CORE_GROUP = "Hytale";
        private static final Semver CORE_VERSION = ManifestUtil.getVersion() == null ? Semver.fromString("0.0.0-dev") : ManifestUtil.getVersion();
        @Nonnull
        private final String group;
        @Nonnull
        private final String name;
        @Nonnull
        private final Semver version;
        @Nullable
        private String description;
        @Nonnull
        private final String main;
        @Nonnull
        private final Map<PluginIdentifier, SemverRange> dependencies = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();
        @Nonnull
        private final Map<PluginIdentifier, SemverRange> optionalDependencies = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();
        @Nonnull
        private final Map<PluginIdentifier, SemverRange> loadBefore = new Object2ObjectLinkedOpenHashMap<PluginIdentifier, SemverRange>();

        @Nonnull
        public static CoreBuilder corePlugin(@Nonnull Class<?> pluginClass) {
            return new CoreBuilder("Hytale", pluginClass.getSimpleName(), CORE_VERSION, pluginClass.getName());
        }

        private CoreBuilder(@Nonnull String group, @Nonnull String name, @Nonnull Semver version, @Nonnull String main) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.main = main;
        }

        @Nonnull
        public CoreBuilder description(@Nonnull String description) {
            this.description = description;
            return this;
        }

        @Nonnull
        @SafeVarargs
        public final CoreBuilder depends(Class<?> ... dependencies) {
            for (Class<?> dependency : dependencies) {
                this.dependencies.put(new PluginIdentifier("Hytale", dependency.getSimpleName()), SemverRange.WILDCARD);
            }
            return this;
        }

        @Nonnull
        @SafeVarargs
        public final CoreBuilder optDepends(Class<?> ... dependencies) {
            for (Class<?> optionalDependency : dependencies) {
                this.optionalDependencies.put(new PluginIdentifier("Hytale", optionalDependency.getSimpleName()), SemverRange.WILDCARD);
            }
            return this;
        }

        @Nonnull
        @SafeVarargs
        public final CoreBuilder loadsBefore(Class<?> ... plugins) {
            for (Class<?> plugin : plugins) {
                this.loadBefore.put(new PluginIdentifier("Hytale", plugin.getSimpleName()), SemverRange.WILDCARD);
            }
            return this;
        }

        @Nonnull
        public PluginManifest build() {
            return new PluginManifest(this.group, this.name, this.version, this.description, Collections.emptyList(), null, this.main, null, this.dependencies, this.optionalDependencies, this.loadBefore, Collections.emptyList(), false);
        }
    }
}

