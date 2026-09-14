package net.buildcraftreborn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuração do BuildCraft Reborn, num JSON simples. Campos que faltam no arquivo ficam com o
 * padrão, e o arquivo é regravado com todos os campos para o jogador enxergar as opções.
 */
public final class BCConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Distância máxima entre marcadores ligados (BuildCraft: 64). */
    public int markerMaxDistance = 64;
    /** Distância máxima que a bomba procura fluido (BuildCraft: 64). */
    public int pumpMaxDistance = 64;
    /** A bomba consome fontes de água infinitas. */
    public boolean pumpsConsumeWater = false;
    /** Profundidade máxima do poço de mineração e da pedreira, em blocos abaixo da máquina. */
    public int miningMaxDepth = 512;
    /** Multiplicador da energia para quebrar blocos (pedreira, poço, construtor). */
    public double miningMultiplier = 1.0;
    /** Tarefas da pedreira por tick (BuildCraft: 4, de 1 a 20). */
    public int quarryMaxTasksPerTick = 4;
    /** Gera fontes de água na rocha-mãe (BuildCraft: 2,5% dos chunks). */
    public boolean waterSprings = true;
    /** Gera poços, lagos e biomas de petróleo no mundo. */
    public boolean oilWorldgen = true;
    /** Desenha os feixes dos lasers. */
    public boolean renderLaserBeams = true;

    public static BCConfig defaults() {
        return new BCConfig();
    }

    public static BCConfig load(Path file) {
        BCConfig config = defaults();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                BCConfig read = GSON.fromJson(reader, BCConfig.class);
                if (read != null) config = read;
            } catch (IOException | JsonParseException e) {
                BuildCraftReborn.LOGGER.warn("Configuração inválida em {}, usando os padrões", file, e);
                return config;
            }
        }
        config.clamp();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            BuildCraftReborn.LOGGER.warn("Não deu para gravar {}", file, e);
        }
        return config;
    }

    private void clamp() {
        this.markerMaxDistance = Math.clamp(this.markerMaxDistance, 8, 256);
        this.pumpMaxDistance = Math.clamp(this.pumpMaxDistance, 8, 256);
        this.miningMaxDepth = Math.clamp(this.miningMaxDepth, 8, 4096);
        this.miningMultiplier = Math.clamp(this.miningMultiplier, 0.1, 100.0);
        this.quarryMaxTasksPerTick = Math.clamp(this.quarryMaxTasksPerTick, 1, 20);
    }
}
