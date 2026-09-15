package net.buildcraftreborn.builders.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.buildcraftreborn.BuildCraftReborn;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Biblioteca eletrônica: pasta {@code buildcraftreborn/library} na pasta do jogo (ou do servidor), a mesma para
 * todos os mundos, como os {@code snapshots-server} do BuildCraft. Cada arquivo guarda a planta com nome e autor.
 */
public final class LibraryStore {
    public static final int MAX_ENTRIES = 256;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    public record Entry(String hash, String type, String name, String author) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("hash").forGetter(Entry::hash),
                Codec.STRING.fieldOf("type").forGetter(Entry::type),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.STRING.fieldOf("author").forGetter(Entry::author)
        ).apply(instance, Entry::new));
    }

    private record Cached(long modified, Entry entry) {}

    private static final Map<Path, Cached> HEADERS = new HashMap<>();

    private LibraryStore() {}

    public static Path directory() {
        return FabricLoader.getInstance().getGameDir().resolve(BuildCraftReborn.MODID).resolve("library");
    }

    public static void save(Snapshot snapshot, String name, String author) {
        Path file = directory().resolve(snapshot.hash() + ".nbt");
        CompoundTag tag = new CompoundTag();
        tag.put("Snapshot", snapshot.save());
        tag.putString("Name", name);
        tag.putString("Author", author);
        try {
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(tag, file);
        } catch (IOException exception) {
            BuildCraftReborn.LOGGER.error("Não deu para salvar {} na biblioteca", snapshot.hash(), exception);
        }
    }

    public static @Nullable Snapshot load(ServerLevel level, String hash) {
        CompoundTag tag = read(hash);
        return tag == null ? null : Snapshot.load(tag.getCompoundOrEmpty("Snapshot"), level.holderLookup(Registries.BLOCK));
    }

    public static @Nullable Entry entry(String hash) {
        for (Entry entry : list()) {
            if (entry.hash().equals(hash)) return entry;
        }
        return null;
    }

    public static boolean delete(String hash) {
        if (!HASH.matcher(hash).matches()) return false;
        try {
            return Files.deleteIfExists(directory().resolve(hash + ".nbt"));
        } catch (IOException exception) {
            return false;
        }
    }

    /** Entradas por nome; os cabeçalhos ficam guardados enquanto o arquivo não muda. */
    public static synchronized List<Entry> list() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) return List.of();
        List<Entry> entries = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".nbt")).limit(MAX_ENTRIES).toList()) {
                String hash = file.getFileName().toString().replace(".nbt", "");
                if (!HASH.matcher(hash).matches()) continue;
                long modified = Files.getLastModifiedTime(file).toMillis();
                Cached cached = HEADERS.get(file);
                if (cached == null || cached.modified() != modified) {
                    CompoundTag tag = read(hash);
                    if (tag == null) continue;
                    CompoundTag snapshot = tag.getCompoundOrEmpty("Snapshot");
                    cached = new Cached(modified, new Entry(hash, snapshot.getStringOr("Type", "template"), tag.getStringOr("Name", ""),
                            tag.getStringOr("Author", "")));
                    HEADERS.put(file, cached);
                }
                entries.add(cached.entry());
            }
        } catch (IOException exception) {
            BuildCraftReborn.LOGGER.error("Não deu para listar a biblioteca", exception);
        }
        entries.sort(Comparator.comparing(Entry::name).thenComparing(Entry::hash));
        return entries;
    }

    private static @Nullable CompoundTag read(String hash) {
        if (!HASH.matcher(hash).matches()) return null;
        Path file = directory().resolve(hash + ".nbt");
        if (!Files.exists(file)) return null;
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException exception) {
            BuildCraftReborn.LOGGER.error("Não deu para ler {} da biblioteca", hash, exception);
            return null;
        }
    }
}
