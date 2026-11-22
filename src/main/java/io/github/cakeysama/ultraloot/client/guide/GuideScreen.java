package io.github.cakeysama.ultraloot.client.guide;

import io.github.cakeysama.ultraloot.client.screen.GuiDraw;
import io.github.cakeysama.ultraloot.guide.GuideBookData;
import io.github.cakeysama.ultraloot.guide.GuideBookData.Chapter;
import io.github.cakeysama.ultraloot.guide.GuideBookData.ItemSpec;
import io.github.cakeysama.ultraloot.guide.GuideBookData.Page;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Ultra Loot Guide screen: a chapter list on the left and a paper page on the right.
 * <p>
 * Authored pages ({@link GuideBookData}) are laid out into "views" when the screen opens: heading, then a recipe grid
 * or an icon row, then the wrapped text. Text that does not fit continues on the next view, so nothing is ever cut,
 * whatever the language or font. Recipes come from the client's recipe manager (the recipes the server sent), so
 * datapack changes show up and a removed recipe is reported instead of drawn wrong. Read-only: no packets.
 */
public class GuideScreen extends Screen {
    private static final int WIDTH = 316;
    private static final int HEIGHT = 204;
    private static final int HEADER_H = 20;

    private static final int NAV_X = 6;
    private static final int NAV_W = 104;
    private static final int ROW_H = 18;

    private static final int PAGE_X = 116;
    private static final int PAGE_W = 194;
    private static final int PAD = 8;
    private static final int TEXT_W = PAGE_W - 2 * PAD;
    private static final int FOOTER_H = 16;
    private static final int LINE_H = 10;
    private static final int HEADING_H = 14;
    private static final int RECIPE_H = 62;
    private static final int ITEMS_H = 24;

    // Frame (matches the machine GUIs) and paper palette.
    private static final int FRAME_EDGE = 0xFF0E1013;
    private static final int FRAME_FILL = 0xFF1E2228;
    private static final int FRAME_LIGHT = 0xFF3A414B;
    private static final int PAPER = 0xFFF4ECD8;
    private static final int PAPER_EDGE = 0xFFB9A57E;
    private static final int PAPER_SHADE = 0xFFE6DABE;
    private static final int INK = 0xFF2B2520;
    private static final int INK_HEADING = 0xFF6B4A12;
    private static final int INK_DIM = 0xFF8A7A5C;
    private static final int INK_BAD = 0xFFA23A2A;
    private static final int SLOT_FILL = 0xFFDCCDA8;
    private static final int SLOT_DARK = 0xFF9C8A62;
    private static final int SLOT_LIGHT = 0xFFFBF6EA;
    private static final int SELECTED = 0x40E8B23A;

    /** Where the reader was, kept for the session so the book reopens on the same page. */
    private static int lastChapter;
    private static int lastView;

    private final GuideBookData book;
    private final List<List<View>> views = new ArrayList<>();
    /** Page icons are built once per screen, not every frame (a Mob Card stack carries components). */
    private final Map<ItemSpec, ItemStack> stacks = new HashMap<>();
    private int chapter;
    private int view;
    private int navScroll;
    private int left;
    private int top;
    @Nullable
    private ItemStack hovered;

    /** One screenful: optional heading, optional block (recipe or icons) from its page, and some text lines. */
    private record View(@Nullable Component heading, @Nullable Page block, List<FormattedCharSequence> lines) {}

    public GuideScreen(GuideBookData book) {
        super(Component.translatable("ultraloot.guide.title"));
        this.book = book;
    }

    /** Opens the guide (installed as the item's client opener). Reads the book from the current resource packs. */
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GuideScreen(GuideBookData.load(minecraft.getResourceManager())));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        layout();
        chapter = Mth.clamp(lastChapter, 0, Math.max(0, views.size() - 1));
        view = views.isEmpty() ? 0 : Mth.clamp(lastView, 0, views.get(chapter).size() - 1);
        ensureChapterVisible();
    }

    // ---- Layout ---------------------------------------------------------------------------------

    private void layout() {
        views.clear();
        int contentH = HEIGHT - HEADER_H - 6 - 2 * 6 - FOOTER_H;
        int fullPageLines = Math.max(1, contentH / LINE_H);
        for (Chapter c : book.chapters()) {
            List<View> chapterViews = new ArrayList<>();
            for (Page page : c.pages()) {
                Component heading = page.titleKey() == null ? null : Component.translatable(page.titleKey());
                boolean hasBlock = page.recipe() != null || !page.items().isEmpty();
                int used = (heading == null ? 0 : HEADING_H) + (page.recipe() != null ? RECIPE_H : page.items().isEmpty() ? 0 : ITEMS_H);
                int firstLines = Math.max(0, (contentH - used) / LINE_H);
                List<FormattedCharSequence> lines = font.split(Component.translatable(page.textKey()), TEXT_W);
                int from = Math.min(firstLines, lines.size());
                chapterViews.add(new View(heading, hasBlock ? page : null, lines.subList(0, from)));
                while (from < lines.size()) {
                    while (from < lines.size() && font.width(lines.get(from)) == 0) from++; // no blank first line
                    if (from >= lines.size()) break;
                    int to = Math.min(lines.size(), from + fullPageLines);
                    chapterViews.add(new View(null, null, lines.subList(from, to)));
                    from = to;
                }
            }
            if (chapterViews.isEmpty()) chapterViews.add(new View(null, null, List.of()));
            views.add(chapterViews);
        }
    }

    // ---- Rendering ------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hovered = null;
        renderFrame(graphics);
        renderNav(graphics, mouseX, mouseY);
        renderPage(graphics, mouseX, mouseY);
        if (hovered != null && !hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    private void renderFrame(GuiGraphics g) {
        g.fill(left, top, left + WIDTH, top + HEIGHT, FRAME_EDGE);
        g.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, FRAME_FILL);
        g.fill(left + 1, top + 1, left + WIDTH - 1, top + 2, FRAME_LIGHT);
        g.fill(left + 1, top + HEADER_H, left + WIDTH - 1, top + HEADER_H + 1, GuiDraw.SEPARATOR);
        g.drawString(font, title, left + 8, top + 6, GuiDraw.ACCENT_LOOT, false);
        if (!book.chapters().isEmpty()) {
            Component name = Component.translatable(book.chapters().get(chapter).titleKey());
            int x = left + PAGE_X;
            GuiDraw.trimmed(g, font, name, x, top + 6, PAGE_W, GuiDraw.TEXT);
        }
    }

    private int navTop() {
        return top + HEADER_H + 6;
    }

    private int navHeight() {
        return HEIGHT - HEADER_H - 12;
    }

    private int visibleRows() {
        return navHeight() / ROW_H;
    }

    private void renderNav(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + NAV_X;
        int y = navTop();
        GuiDraw.inset(g, x, y, NAV_W, navHeight());
        List<Chapter> chapters = book.chapters();
        int rows = Math.min(visibleRows(), chapters.size() - navScroll);
        for (int r = 0; r < rows; r++) {
            int index = navScroll + r;
            int ry = y + 1 + r * ROW_H;
            boolean over = mouseX >= x && mouseX < x + NAV_W && mouseY >= ry && mouseY < ry + ROW_H;
            if (index == chapter) {
                g.fill(x + 1, ry, x + NAV_W - 1, ry + ROW_H, SELECTED);
                g.fill(x + 1, ry, x + 3, ry + ROW_H, GuiDraw.ACCENT_LOOT);
            } else if (over) {
                g.fill(x + 1, ry, x + NAV_W - 1, ry + ROW_H, GuiDraw.ROW_HOVER);
            }
            Chapter c = chapters.get(index);
            g.renderItem(c.iconStack(), x + 4, ry + 1);
            int color = index == chapter ? GuiDraw.TEXT : GuiDraw.LABEL;
            GuiDraw.trimmed(g, font, Component.translatable(c.titleKey()), x + 23, ry + 5, NAV_W - 28, color);
        }
        if (chapters.size() > visibleRows()) {
            int trackH = navHeight() - 2;
            int thumbH = Math.max(8, trackH * visibleRows() / chapters.size());
            int maxScroll = chapters.size() - visibleRows();
            int thumbY = y + 1 + (trackH - thumbH) * navScroll / Math.max(1, maxScroll);
            g.fill(x + NAV_W - 3, thumbY, x + NAV_W - 1, thumbY + thumbH, GuiDraw.INACTIVE);
        }
    }

    private void renderPage(GuiGraphics g, int mouseX, int mouseY) {
        int px = left + PAGE_X;
        int py = navTop();
        int ph = navHeight();
        g.fill(px - 1, py - 1, px + PAGE_W + 1, py + ph + 1, PAPER_EDGE);
        g.fill(px, py, px + PAGE_W, py + ph, PAPER);
        g.fill(px, py + ph - FOOTER_H, px + PAGE_W, py + ph - FOOTER_H + 1, PAPER_SHADE);
        if (views.isEmpty()) {
            g.drawString(font, Component.translatable("ultraloot.guide.recipe_missing"), px + PAD, py + PAD, INK_BAD, false);
            return;
        }
        View v = views.get(chapter).get(view);
        int x = px + PAD;
        int y = py + 6;
        if (v.heading() != null) {
            g.drawString(font, v.heading().copy().withStyle(s -> s.withBold(true)), x, y + 1, INK_HEADING, false);
            g.fill(x, y + 11, x + TEXT_W, y + 12, PAPER_EDGE);
            y += HEADING_H;
        }
        if (v.block() != null) {
            if (v.block().recipe() != null) {
                renderRecipe(g, v.block().recipe(), x, y, mouseX, mouseY);
                y += RECIPE_H;
            } else {
                renderItems(g, v.block().items(), x, y, mouseX, mouseY);
                y += ITEMS_H;
            }
        }
        for (FormattedCharSequence line : v.lines()) {
            g.drawString(font, line, x, y, INK, false);
            y += LINE_H;
        }
        renderFooter(g, px, py + ph - FOOTER_H, mouseX, mouseY);
    }

    private void renderFooter(GuiGraphics g, int px, int fy, int mouseX, int mouseY) {
        int total = 0;
        int current = 0;
        for (int c = 0; c < views.size(); c++) {
            if (c == chapter) current = total + view + 1;
            total += views.get(c).size();
        }
        Component counter = Component.translatable("ultraloot.guide.page", current, total);
        g.drawString(font, counter, px + (PAGE_W - font.width(counter)) / 2, fy + 4, INK_DIM, false);
        boolean canBack = chapter > 0 || view > 0;
        boolean canNext = chapter < views.size() - 1 || view < views.get(chapter).size() - 1;
        drawArrow(g, px + 6, fy + 3, "<", canBack, overBack(mouseX, mouseY));
        drawArrow(g, px + PAGE_W - 6 - font.width(">"), fy + 3, ">", canNext, overNext(mouseX, mouseY));
    }

    private void drawArrow(GuiGraphics g, int x, int y, String arrow, boolean enabled, boolean over) {
        if (!enabled) return;
        if (over) g.fill(x - 3, y - 2, x + font.width(arrow) + 3, y + 10, SLOT_FILL);
        g.drawString(font, arrow, x, y + 1, over ? INK_HEADING : INK, false);
    }

    private void renderItems(GuiGraphics g, List<ItemSpec> items, int x, int y, int mouseX, int mouseY) {
        int count = items.size();
        int sx = x + (TEXT_W - count * 20 + 2) / 2;
        for (int i = 0; i < count; i++) {
            slot(g, stacks.computeIfAbsent(items.get(i), ItemSpec::toStack), sx + i * 20, y + 2, mouseX, mouseY);
        }
    }

    private void renderRecipe(GuiGraphics g, ResourceLocation id, int x, int y, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<RecipeHolder<?>> holder = minecraft.level == null ? Optional.empty()
                : minecraft.level.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            g.drawString(font, Component.translatable("ultraloot.guide.recipe_missing"), x, y + 24, INK_BAD, false);
            return;
        }
        Recipe<?> recipe = holder.get().value();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int w = 3;
        if (recipe instanceof ShapedRecipe shaped) w = shaped.getWidth();
        int gridX = x + (TEXT_W - 112) / 2;
        int gridY = y + 2;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col < w ? row * w + col : -1;
                ItemStack stack = index >= 0 && index < ingredients.size() ? cycle(ingredients.get(index)) : ItemStack.EMPTY;
                slot(g, stack, gridX + col * 18, gridY + row * 18, mouseX, mouseY);
            }
        }
        int ax = gridX + 60;
        int ay = gridY + 23;
        g.fill(ax, ay + 3, ax + 16, ay + 5, SLOT_DARK);
        for (int i = 0; i < 5; i++) g.fill(ax + 15 + i, ay - 1 + i, ax + 16 + i, ay + 9 - i, SLOT_DARK);
        ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
        int rx = gridX + 84;
        int ry = gridY + 14;
        g.fill(rx - 1, ry - 1, rx + 27, ry + 27, SLOT_DARK);
        g.fill(rx, ry, rx + 27, ry + 27, SLOT_LIGHT);
        g.fill(rx, ry, rx + 26, ry + 26, SLOT_FILL);
        itemAt(g, result, rx + 5, ry + 5, mouseX, mouseY);
    }

    /** Tag ingredients show each matching item in turn, one per second. */
    private static ItemStack cycle(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(int) ((System.currentTimeMillis() / 1000L) % items.length)];
    }

    private void slot(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        g.fill(x, y, x + 18, y + 18, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, SLOT_LIGHT);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
        itemAt(g, stack, x + 1, y + 1, mouseX, mouseY);
    }

    private void itemAt(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            g.fill(x, y, x + 16, y + 16, 0x60FFFFFF); // under the item: items render above flat fills
            hovered = stack;
        }
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
    }

    // ---- Navigation -----------------------------------------------------------------------------

    private boolean overBack(double mouseX, double mouseY) {
        int fy = navTop() + navHeight() - FOOTER_H;
        int x = left + PAGE_X;
        return mouseX >= x && mouseX < x + 24 && mouseY >= fy && mouseY < fy + FOOTER_H;
    }

    private boolean overNext(double mouseX, double mouseY) {
        int fy = navTop() + navHeight() - FOOTER_H;
        int x = left + PAGE_X + PAGE_W - 24;
        return mouseX >= x && mouseX < x + 24 && mouseY >= fy && mouseY < fy + FOOTER_H;
    }

    private boolean overNav(double mouseX, double mouseY) {
        int x = left + NAV_X;
        return mouseX >= x && mouseX < x + NAV_W && mouseY >= navTop() && mouseY < navTop() + navHeight();
    }

    private void turn(int delta) {
        if (views.isEmpty()) return;
        int c = chapter;
        int v = view + delta;
        if (v < 0) {
            if (c == 0) return;
            c--;
            v = views.get(c).size() - 1;
        } else if (v >= views.get(c).size()) {
            if (c >= views.size() - 1) return;
            c++;
            v = 0;
        }
        goTo(c, v);
    }

    private void goTo(int newChapter, int newView) {
        if (newChapter == chapter && newView == view) return;
        chapter = newChapter;
        view = newView;
        lastChapter = chapter;
        lastView = view;
        ensureChapterVisible();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void ensureChapterVisible() {
        int rows = visibleRows();
        if (chapter < navScroll) navScroll = chapter;
        if (chapter >= navScroll + rows) navScroll = chapter - rows + 1;
        navScroll = Mth.clamp(navScroll, 0, Math.max(0, book.chapters().size() - rows));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (overNav(mouseX, mouseY)) {
                int row = (int) ((mouseY - navTop() - 1) / ROW_H);
                int index = navScroll + row;
                if (row >= 0 && row < visibleRows() && index < book.chapters().size()) {
                    goTo(index, 0);
                    return true;
                }
            }
            if (overBack(mouseX, mouseY)) {
                turn(-1);
                return true;
            }
            if (overNext(mouseX, mouseY)) {
                turn(1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        if (overNav(mouseX, mouseY)) {
            int max = Math.max(0, book.chapters().size() - visibleRows());
            navScroll = Mth.clamp(navScroll - (int) Math.signum(scrollY), 0, max);
        } else {
            turn(scrollY < 0 ? 1 : -1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                turn(1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                turn(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (chapter < views.size() - 1) goTo(chapter + 1, 0);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (chapter > 0) goTo(chapter - 1, 0);
                return true;
            }
            default -> {
                if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                    onClose();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }
}
