package net.buildcraftreborn.transport.plug;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.registry.BCItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Encaixes simples (sem tela): pulsar, sensor de luz e temporizador. Tapam a face como a porta lógica. */
public enum PlugKind {
    PULSAR("pulsar_static"),
    LIGHT_SENSOR("daylight_sensor"),
    TIMER("timer");

    public final Identifier texture;

    PlugKind(String texture) {
        this.texture = BuildCraftReborn.id("textures/entity/plug/" + texture + ".png");
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ItemStack stack() {
        return new ItemStack(switch (this) {
            case PULSAR -> BCItems.PLUG_PULSAR.get();
            case LIGHT_SENSOR -> BCItems.PLUG_LIGHT_SENSOR.get();
            case TIMER -> BCItems.PLUG_TIMER.get();
        });
    }

    public static @Nullable PlugKind byId(String id) {
        for (PlugKind kind : values()) {
            if (kind.id().equals(id)) return kind;
        }
        return null;
    }
}
