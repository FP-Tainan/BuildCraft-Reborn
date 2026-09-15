package net.buildcraftreborn.builders.item;

import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.registry.BCComponents;
import net.buildcraftreborn.registry.BCItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Molde ou planta ({@code ItemSnapshot}): em branco empilha até 16; depois de escaneado na mesa do arquiteto
 * guarda o cabeçalho (hash, nome, autor) e fica sozinho na pilha. O nome vem do item renomeado na bigorna.
 */
public class SnapshotItem extends Item {
    private final Snapshot.Type type;

    public SnapshotItem(Properties properties, Snapshot.Type type) {
        super(properties);
        this.type = type;
    }

    public Snapshot.Type type() {
        return this.type;
    }

    public static @Nullable SnapshotHeader header(ItemStack stack) {
        return stack.getItem() instanceof SnapshotItem ? stack.get(BCComponents.SNAPSHOT.get()) : null;
    }

    public static ItemStack used(SnapshotHeader header) {
        ItemStack stack = new ItemStack(header.snapshotType() == Snapshot.Type.BLUEPRINT ? BCItems.BLUEPRINT.get() : BCItems.TEMPLATE.get());
        stack.set(BCComponents.SNAPSHOT.get(), header);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        SnapshotHeader header = stack.get(BCComponents.SNAPSHOT.get());
        if (header == null) return super.getName(stack);
        return Component.translatable("item.buildcraftreborn." + this.type.id() + ".named", header.name());
    }
}
