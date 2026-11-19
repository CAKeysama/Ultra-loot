package io.github.cakeysama.ultraloot.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads loot profiles from {@code data/<ns>/ultraloot/loot_profiles/*.json} (reloadable with /reload) and merges them
 * with auto-discovered {@code chests/*} tables of every namespace (vanilla and other mods).
 * <p>
 * Tables that use {@code minecraft:exploration_map} are rejected: that function runs a structure search every
 * time it rolls, which is far too expensive to repeat on a timer.
 */
@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public final class LootProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "ultraloot/loot_profiles";
    private static final Gson GSON = new GsonBuilder().create();
    private static final LootProfileManager INSTANCE = new LootProfileManager();

    private Map<ResourceLocation, LootProfile> explicit = Map.of();
    /** Resolved view (explicit + auto), rebuilt lazily after each reload because loot tables reload too. */
    @Nullable
    private Map<ResourceLocation, LootProfile> resolved;
    /** Loot tables the resolved view was built from; a /reload swaps this instance, which forces a rebuild. */
    @Nullable
    private Object resolvedFor;
    private final Map<ResourceLocation, Boolean> safeTables = new HashMap<>();

    private LootProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, LootProfile> loaded = new LinkedHashMap<>();
        files.forEach((id, json) -> LootProfile.Definition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> UltraLoot.LOGGER.error("Invalid Ultra Loot profile {}: {}", id, error))
                .ifPresent(definition -> loaded.put(id, definition.toProfile(id))));
        explicit = Map.copyOf(loaded);
        synchronized (this) {
            resolved = null;
            safeTables.clear();
        }
        UltraLoot.LOGGER.info("Loaded {} Ultra Loot loot profiles", loaded.size());
    }

    public static LootProfileManager get() {
        return INSTANCE;
    }

    /** All usable profiles, sorted by category then id. */
    public synchronized Collection<LootProfile> all(MinecraftServer server) {
        return resolve(server).values();
    }

    @Nullable
    public synchronized LootProfile get(MinecraftServer server, ResourceLocation id) {
        return resolve(server).get(id);
    }

    private Map<ResourceLocation, LootProfile> resolve(MinecraftServer server) {
        if (resolved != null && resolvedFor == server.reloadableRegistries()) return resolved;
        if (resolvedFor != server.reloadableRegistries()) safeTables.clear();
        resolvedFor = server.reloadableRegistries();
        Collection<ResourceLocation> tables = server.reloadableRegistries().getKeys(Registries.LOOT_TABLE);
        Set<ResourceLocation> tableSet = Set.copyOf(tables);
        Map<ResourceLocation, LootProfile> map = new LinkedHashMap<>();
        for (LootProfile profile : explicit.values()) {
            ResourceLocation table = profile.table().location();
            if (!tableSet.contains(table)) {
                UltraLoot.LOGGER.warn("Ultra Loot profile {} points to missing loot table {}", profile.id(), table);
                continue;
            }
            if (isSafe(server, table)) map.put(profile.id(), profile);
        }
        if (Balance.autoDiscoverChestTables) {
            Set<ResourceLocation> covered = explicit.values().stream().map(p -> p.table().location()).collect(Collectors.toSet());
            for (ResourceLocation table : tables) {
                if (table.getPath().startsWith("chests/") && !table.getPath().contains("inject") && !covered.contains(table) && isSafe(server, table)) {
                    LootProfile auto = LootProfile.autoChest(table);
                    map.putIfAbsent(auto.id(), auto);
                }
            }
        }
        List<LootProfile> sorted = map.values().stream()
                .sorted(Comparator.comparing(LootProfile::autoDiscovered)
                        .thenComparing(LootProfile::category)
                        .thenComparing(p -> p.id().toString()))
                .toList();
        Map<ResourceLocation, LootProfile> ordered = new LinkedHashMap<>();
        sorted.forEach(p -> ordered.put(p.id(), p));
        resolved = ordered;
        return resolved;
    }

    /**
     * Encodes the table (and every table it references through {@code minecraft:loot_table} entries) once and
     * rejects it if any of them uses an exploration map. Tables that cannot be encoded are treated as unsafe.
     */
    private boolean isSafe(MinecraftServer server, ResourceLocation tableId) {
        Boolean known = safeTables.get(tableId);
        if (known != null) return known;
        safeTables.put(tableId, true); // cycle guard: a table referencing itself is judged by its own content
        boolean safe = checkTable(server, tableId);
        safeTables.put(tableId, safe);
        return safe;
    }

    private boolean checkTable(MinecraftServer server, ResourceLocation tableId) {
        LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, tableId));
        if (table == LootTable.EMPTY) return false;
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var encoded = LootTable.DIRECT_CODEC.encodeStart(ops, table).result();
        if (encoded.isEmpty()) {
            UltraLoot.LOGGER.warn("Ultra Loot could not inspect loot table {}; it will not be offered as a profile", tableId);
            return false;
        }
        JsonElement json = encoded.get();
        if (json.toString().contains("exploration_map")) return false;
        List<ResourceLocation> references = new ArrayList<>();
        collectReferences(json, references);
        for (ResourceLocation reference : references) {
            if (!isSafe(server, reference)) return false;
        }
        return true;
    }

    /** Finds {@code {"type": "minecraft:loot_table", "value": "<id>"}} entries (references to other tables). */
    private static void collectReferences(JsonElement json, List<ResourceLocation> out) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(e -> collectReferences(e, out));
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            JsonElement type = object.get("type");
            JsonElement value = object.get("value");
            if (type != null && type.isJsonPrimitive() && "minecraft:loot_table".equals(type.getAsString())
                    && value != null && value.isJsonPrimitive()) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id != null) out.add(id);
            }
            object.entrySet().forEach(e -> collectReferences(e.getValue(), out));
        }
    }
}
