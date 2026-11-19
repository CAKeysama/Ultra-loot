package io.github.cakeysama.ultraloot.client.screen.widget;

import io.github.cakeysama.ultraloot.client.screen.GuiDraw;
import io.github.cakeysama.ultraloot.client.screen.GuiSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Row-based scroll state for a fixed-height list plus its scrollbar. Scrolls by whole rows so no clipping is needed.
 * All coordinates are relative to the GUI panel.
 */
public class ScrollableList {
    public static final int ROW_HEIGHT = 18;
    private static final int TRACK_WIDTH = 10;

    private final int x;
    private final int y;
    private final int width;
    private final int visibleRows;
    private int count;
    private int offset;
    private boolean dragging;

    /** @param width width of the row area; the scrollbar track sits 2 px to its right */
    public ScrollableList(int x, int y, int width, int visibleRows) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.visibleRows = visibleRows;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return visibleRows * ROW_HEIGHT + 2;
    }

    public int visibleRows() {
        return visibleRows;
    }

    public int offset() {
        return offset;
    }

    /** Updates the row count (synced data may shrink) and keeps the offset in range. */
    public void setCount(int count) {
        this.count = count;
        this.offset = Mth.clamp(offset, 0, maxOffset());
    }

    private int maxOffset() {
        return Math.max(0, count - visibleRows);
    }

    public int rowY(int visibleIndex) {
        return y + 1 + visibleIndex * ROW_HEIGHT;
    }

    /** @return absolute row index under the point, or -1 */
    public int rowAt(double mx, double my) {
        if (mx < x + 1 || mx >= x + width - 1 || my < y + 1 || my >= y + 1 + visibleRows * ROW_HEIGHT) return -1;
        int index = offset + (int) ((my - y - 1) / ROW_HEIGHT);
        return index < count ? index : -1;
    }

    public boolean isOver(double mx, double my) {
        return mx >= x && mx < trackX() + TRACK_WIDTH && my >= y && my < y + height();
    }

    public boolean scroll(double mx, double my, double delta) {
        if (!isOver(mx, my) || delta == 0) return false;
        offset = Mth.clamp(offset - (int) Math.signum(delta), 0, maxOffset());
        return true;
    }

    private int trackX() {
        return x + width + 2;
    }

    private int thumbY() {
        int travel = height() - 2 - GuiSprites.THUMB.height();
        int max = maxOffset();
        return y + 1 + (max == 0 ? 0 : Math.round((float) offset / max * travel));
    }

    /** Starts a scrollbar drag. @return true if the click hit the track */
    public boolean mouseClicked(double mx, double my) {
        int tx = trackX();
        if (mx < tx || mx >= tx + TRACK_WIDTH || my < y || my >= y + height() || maxOffset() == 0) return false;
        dragging = true;
        dragTo(my);
        return true;
    }

    public boolean mouseDragged(double my) {
        if (!dragging) return false;
        dragTo(my);
        return true;
    }

    public boolean mouseReleased() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    private void dragTo(double my) {
        int travel = height() - 2 - GuiSprites.THUMB.height();
        double t = (my - y - 1 - GuiSprites.THUMB.height() / 2.0) / Math.max(1, travel);
        offset = Mth.clamp((int) Math.round(t * maxOffset()), 0, maxOffset());
    }

    /** Draws the list inset and the scrollbar; rows are drawn by the owner. */
    public void renderFrame(GuiGraphics graphics, double mx, double my) {
        GuiDraw.inset(graphics, x, y, width, height());
        int tx = trackX();
        GuiDraw.inset(graphics, tx, y, TRACK_WIDTH, height());
        GuiSprites.Sprite thumb = maxOffset() == 0 ? GuiSprites.THUMB_DISABLED
                : dragging || (mx >= tx && mx < tx + TRACK_WIDTH && my >= thumbY() && my < thumbY() + GuiSprites.THUMB.height())
                ? GuiSprites.THUMB_HOVER : GuiSprites.THUMB;
        thumb.draw(graphics, tx + 1, thumbY());
    }
}
