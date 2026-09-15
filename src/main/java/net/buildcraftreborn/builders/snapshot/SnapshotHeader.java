package net.buildcraftreborn.builders.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** O que o item de molde/planta usado guarda: o hash do arquivo, o tipo, o nome e quem escaneou. */
public record SnapshotHeader(String hash, String type, String name, String author) {
    public static final Codec<SnapshotHeader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("hash").forGetter(SnapshotHeader::hash),
            Codec.STRING.fieldOf("type").forGetter(SnapshotHeader::type),
            Codec.STRING.fieldOf("name").forGetter(SnapshotHeader::name),
            Codec.STRING.optionalFieldOf("author", "").forGetter(SnapshotHeader::author)
    ).apply(instance, SnapshotHeader::new));

    public static final StreamCodec<ByteBuf, SnapshotHeader> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SnapshotHeader::hash,
            ByteBufCodecs.STRING_UTF8, SnapshotHeader::type,
            ByteBufCodecs.STRING_UTF8, SnapshotHeader::name,
            ByteBufCodecs.STRING_UTF8, SnapshotHeader::author,
            SnapshotHeader::new);

    public Snapshot.Type snapshotType() {
        return Snapshot.Type.byId(this.type);
    }
}
