package org.keke.chemistry.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import io.netty.buffer.Unpooled;
import org.keke.chemistry.network.ModNetworking;
import org.keke.chemistry.screen.PipetteScreenHandler;
import org.keke.chemistry.utils.Compound;

/**
 * Client-side GUI for the Precision Pipette block.
 * Contains a text field for specifying which compound to extract
 * and how much to remove (moles or grams).
 */
@Environment(EnvType.CLIENT)
public class PipetteScreen extends HandledScreen<PipetteScreenHandler> {

    private TextFieldWidget formulaField;
    private TextFieldWidget amountField;
    private boolean useGrams = false;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;

    public PipetteScreen(PipetteScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 200;
        this.backgroundHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        // Formula input
        formulaField = new TextFieldWidget(this.textRenderer, cx + 10, cy + 25, 180, 16, Text.literal("Formula"));
        formulaField.setMaxLength(128);
        formulaField.setPlaceholder(Text.literal("Compound to extract").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(formulaField);

        // Amount input
        amountField = new TextFieldWidget(this.textRenderer, cx + 10, cy + 55, 120, 16, Text.literal("Amount"));
        amountField.setMaxLength(16);
        amountField.setPlaceholder(Text.literal("e.g. 0.25").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(amountField);

        // Unit toggle
        CyclingButtonWidget<Boolean> unitToggle = CyclingButtonWidget.onOffBuilder(
                Text.literal("g"), Text.literal("mol"))
                .initially(false)
                .build(cx + 135, cy + 55, 55, 16, Text.literal("Unit"),
                        (button, value) -> useGrams = value);
        this.addDrawableChild(unitToggle);

        // Extract button
        ButtonWidget extractButton = ButtonWidget.builder(Text.literal("Extract from Beaker"), button -> {
            onExtractClicked();
        }).dimensions(cx + 10, cy + 85, 180, 20).build();
        this.addDrawableChild(extractButton);

        setInitialFocus(formulaField);
    }

    private void onExtractClicked() {
        String formula = formulaField.getText().trim();
        String amountStr = amountField.getText().trim();

        if (formula.isEmpty()) {
            statusMessage = "Enter a formula to extract";
            statusColor = 0xFF5555;
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                statusMessage = "Amount must be positive";
                statusColor = 0xFF5555;
                return;
            }
        } catch (NumberFormatException e) {
            statusMessage = "Invalid number";
            statusColor = 0xFF5555;
            return;
        }

        sendPipettePacket(handler.getBlockPos(), formula, amount, useGrams);

        double molarMass = Compound.computeMolarMass(formula);
        double molesDisplay = useGrams ? amount / molarMass : amount;
        statusMessage = String.format("Extracting %.3f mol of %s", molesDisplay, formula);
        statusColor = 0x55FF55;
    }

    private void sendPipettePacket(BlockPos pos, String formula, double amount, boolean isGrams) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeString(formula);
        buf.writeDouble(amount);
        buf.writeBoolean(isGrams);
        ClientPlayNetworking.send(ModNetworking.PIPETTE_EXTRACT, buf);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        context.fill(cx, cy, cx + this.backgroundWidth, cy + this.backgroundHeight, 0xCC1A1A2E);
        context.drawBorder(cx, cy, this.backgroundWidth, this.backgroundHeight, 0xFF4A4A6A);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 10, 6, 0x00AAFF, true);

        context.drawText(this.textRenderer, Text.literal("Compound:"), 10, 16, 0xCCCCCC, false);
        context.drawText(this.textRenderer, Text.literal("Amount:"), 10, 46, 0xCCCCCC, false);

        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(statusMessage), 10, 112, statusColor, false);
        }

        String formula = formulaField.getText().trim();
        if (!formula.isEmpty()) {
            try {
                double mm = Compound.computeMolarMass(formula);
                if (mm > 0) {
                    context.drawText(this.textRenderer,
                            Text.literal(String.format("M = %.2f g/mol", mm)),
                            10, 126, 0x888888, false);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (formulaField.isFocused() || amountField.isFocused()) {
            if (keyCode == 256) {
                this.close();
                return true;
            }
            // Delegate to text fields for control keys (backspace, arrows, etc.)
            // but always return true to prevent super.keyPressed() from checking
            // the inventory keybind and closing the screen on 'e'
            formulaField.keyPressed(keyCode, scanCode, modifiers);
            amountField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
