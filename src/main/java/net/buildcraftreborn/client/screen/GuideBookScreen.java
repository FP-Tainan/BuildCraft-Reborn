package net.buildcraftreborn.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Livro guia: capítulos à esquerda, páginas à direita. O texto vem das traduções
 * {@code guide.buildcraftreborn.<capítulo>.<página>}, contadas a partir de 1 enquanto existirem.
 */
public class GuideBookScreen extends Screen {
    /** Capítulo e o item que aparece como ícone. */
    private static final String[][] CHAPTERS = {
            {"intro", "buildcraftreborn:guide"},
            {"tools", "buildcraftreborn:wrench"},
            {"engines", "buildcraftreborn:stirling_engine"},
            {"factory", "buildcraftreborn:pump"},
            {"quarry", "buildcraftreborn:quarry"},
            {"pipes", "buildcraftreborn:pipe_items_wood"},
            {"fluid_pipes", "buildcraftreborn:pipe_fluids_wood"},
            {"special_pipes", "buildcraftreborn:pipe_items_lapis"},
            {"oil", "buildcraftreborn:oil_bucket"},
            {"silicon", "buildcraftreborn:laser"},
            {"gates", "buildcraftreborn:gate"},
            {"builders", "buildcraftreborn:architect_table"},
    };
    private static final int WIDTH = 330;
    private static final int HEIGHT = 200;
    private static final int LIST_WIDTH = 110;
    private static final int PAPER = 0xFFEFE6CF;
    private static final int INK = 0xFF3A2E1F;

    private int chapter;
    private int page;
    private int left;
    private int top;

    public GuideBookScreen() {
        super(Component.translatable("gui.buildcraftreborn.guide.title"));
    }

    private static String key(int chapter, String suffix) {
        return "guide.buildcraftreborn." + CHAPTERS[chapter][0] + "." + suffix;
    }

    private static int pages(int chapter) {
        int count = 0;
        while (Language.getInstance().has(key(chapter, String.valueOf(count + 1)))) count++;
        return Math.max(1, count);
    }

    private static ItemStack icon(int chapter) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(CHAPTERS[chapter][1])).orElse(Items.BOOK));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        for (int i = 0; i < CHAPTERS.length; i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.translatableWithFallback(key(i, "title"), CHAPTERS[i][0]), button -> {
                this.chapter = index;
                this.page = 0;
            }).bounds(this.left + 6, this.top + 20 + i * 12, LIST_WIDTH - 6, 12).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> this.page = Math.max(0, this.page - 1))
                .bounds(this.left + WIDTH - 62, this.top + HEIGHT - 20, 24, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> this.page = Math.min(pages(this.chapter) - 1, this.page + 1))
                .bounds(this.left + WIDTH - 32, this.top + HEIGHT - 20, 24, 14).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xF0262B30);
        graphics.fill(this.left + LIST_WIDTH + 4, this.top + 18, this.left + WIDTH - 6, this.top + HEIGHT - 24, PAPER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.left + 8, this.top + 6, 0xFFF2C94C, true);
        int pageLeft = this.left + LIST_WIDTH + 10;
        int pageWidth = WIDTH - LIST_WIDTH - 22;
        graphics.item(icon(this.chapter), pageLeft, this.top + 22);
        graphics.text(this.font, Component.translatableWithFallback(key(this.chapter, "title"), CHAPTERS[this.chapter][0]),
                pageLeft + 20, this.top + 26, INK, false);
        graphics.textWithWordWrap(this.font, Component.translatable(key(this.chapter, String.valueOf(this.page + 1))),
                pageLeft, this.top + 44, pageWidth, INK, false);
        graphics.text(this.font, Component.literal((this.page + 1) + " / " + pages(this.chapter)),
                this.left + LIST_WIDTH + 10, this.top + HEIGHT - 17, 0xFFD8D0C0, false);
    }
}
