package net.buildcraftreborn.builders.snapshot;

import net.buildcraftreborn.BuildCraftReborn;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Moldes e plantas salvos no mundo, um arquivo compactado por hash em {@code data/buildcraftreborn/snapshots}.
 * O item só guarda o hash; assim uma planta grande não pesa no inventário nem nos pacotes.
 */
public final class SnapshotStore {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final int CACHE_SIZE = 16;
    private static final Map<String, Snapshot> CACHE = new LinkedHashMap<>(CACHE_SIZE, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Snapshot> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private SnapshotStore() {}

    public static Path directory(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.DATA).resolve(BuildCraftReborn.MODID).resolve("snapshots");
    }

    /** Salva (se ainda não existe) e devolve o hash. */
    public static String put(ServerLevel level, Snapshot snapshot) {
        String hash = snapshot.hash();
        Path file = directory(level).resolve(hash + ".nbt");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                NbtIo.writeCompressed(snapshot.save(), file);
            }
        } catch (IOException exception) {
            BuildCraftReborn.LOGGER.error("Não deu para salvar a planta {}", hash, exception);
        }
        synchronized (CACHE) {
            CACHE.put(file.toAbsolutePath().toString(), snapshot);
        }
        return hash;
    }

    public static @Nullable Snapshot get(ServerLevel level, String hash) {
        if (!HASH.matcher(hash).matches()) return null;
        Path file = directory(level).resolve(hash + ".nbt");
        String key = file.toAbsolutePath().toString();
        synchronized (CACHE) {
            Snapshot cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        if (!Files.exists(file)) return null;
        try {
            Snapshot snapshot = Snapshot.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), level.holderLookup(Registries.BLOCK));
            synchronized (CACHE) {
                CACHE.put(key, snapshot);
            }
            return snapshot;
        } catch (IOException exception) {
            BuildCraftReborn.LOGGER.error("Não deu para ler a planta {}", hash, exception);
            return null;
        }
    }
}
