package at.hannibal2.skyhanni.utils.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class WrappedAbstractedContainerScreen<T extends AbstractContainerMenu>
    extends Screen
    implements MenuAccess<T> {

    protected final AbstractContainerScreen<T> delegate;

    public WrappedAbstractedContainerScreen(AbstractContainerScreen<T> delegate) {
        super(delegate.getTitle());
        this.delegate = delegate;
    }

    @Override
    public @NonNull T getMenu() {
        return delegate.getMenu();
    }

    @Override
    public void init() {
        delegate.init();
    }

    @Override
    public void tick() {
        delegate.tick();
    }

    @Override
    public void removed() {
        delegate.removed();
    }

    @Override
    public void onClose() {
        delegate.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return delegate.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return delegate.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return delegate.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(
        double x,
        double y,
        double scrollX,
        double scrollY
    ) {
        return delegate.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return delegate.keyPressed(event);
    }
    //? if >= 26.1 {
@Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        delegate.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
//?} else {

    /*public void renderBg(@NotNull GuiGraphicsExtractor drawContext, float partialTicks, int originalMouseX, int originalMouseY) {
        delegate.renderBg(drawContext, partialTicks, originalMouseX, originalMouseY);
    }
    *///?}

    public void extractContents(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        delegate.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    /*
    public void extractCarriedItem(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY
    ) {
        delegate.extractCarriedItem(graphics, mouseX, mouseY);
    }


    public void extractSnapbackItem(GuiGraphicsExtractor graphics) {
        delegate.extractSnapbackItem(graphics);
    }
     */

/*
    protected void extractSlots(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY
    ) {
        delegate.extractSlots(graphics, mouseX, mouseY);
    }

    protected void extractTooltip(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY
    ) {
        delegate.extractTooltip(graphics, mouseX, mouseY);
    }

    protected void extractLabels(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY
    ) {
        delegate.extractLabels(graphics, mouseX, mouseY);
    }

    protected void extractSlot(
        GuiGraphicsExtractor graphics,
        Slot slot,
        int mouseX,
        int mouseY
    ) {
        delegate.extractSlot(graphics, slot, mouseX, mouseY);
    }
*/
    public void clearDraggingState() {
        delegate.clearDraggingState();
    }

    public void slotClicked(
        Slot slot,
        int slotId,
        int button,
        ContainerInput input
    ) {
        delegate.slotClicked(slot, slotId, button, input);
    }
/*
    protected void handleSlotStateChanged(
        int slotId,
        int containerId,
        boolean newState
    ) {
        delegate.handleSlotStateChanged(slotId, containerId, newState);
    }

    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        return delegate.checkHotbarKeyPressed(event);
    }

    protected boolean hasClickedOutside(
        double mx,
        double my,
        int xo,
        int yo
    ) {
        return delegate.hasClickedOutside(mx, my, xo, yo);
    }

    protected boolean isHovering(
        int left,
        int top,
        int width,
        int height,
        double mouseX,
        double mouseY
    ) {
        return delegate.isHovering(
            left,
            top,
            width,
            height,
            mouseX,
            mouseY
        );
    }

    protected void containerTick() {
        delegate.containerTick();
    }
*/

    @Override
    public boolean isPauseScreen() {
        return delegate.isPauseScreen();
    }

    @Override
    public boolean isInGameUi() {
        return delegate.isInGameUi();
    }

    // ---- field accessors ----

    public Slot getHoveredSlot() {
        return delegate.hoveredSlot;
    }

    public int getLeftPos() {
        return delegate.leftPos;
    }

    public int getTopPos() {
        return delegate.topPos;
    }

    public int getImageWidth() {
        return delegate.imageWidth;
    }

    public int getImageHeight() {
        return delegate.imageHeight;
    }

    /*
    public Component getPlayerInventoryTitle() {
        return delegate.playerInventoryTitle;
    }
     */

    public AbstractContainerScreen<T> getDelegate() {
        return delegate;
    }
}
