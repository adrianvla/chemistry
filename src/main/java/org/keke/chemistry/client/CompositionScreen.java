package org.keke.chemistry.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
import org.keke.chemistry.screen.CompositionScreenHandler;
import org.keke.chemistry.utils.Compound;
import org.keke.chemistry.utils.FormulaParser;

/**
 * Client-side GUI for the Chemical Composer block.
 * Contains a text field for formula input, amount input,
 * a moles/grams toggle, and an Add button.
 */
@Environment(EnvType.CLIENT)
public class CompositionScreen extends HandledScreen<CompositionScreenHandler> {

    private TextFieldWidget formulaField;
    private TextFieldWidget amountField;
    private boolean useGrams = false;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;

    public CompositionScreen(CompositionScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 200;
        this.backgroundHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        // Formula input field
        formulaField = new TextFieldWidget(this.textRenderer, cx + 10, cy + 25, 180, 16, Text.literal("Formula"));
        formulaField.setMaxLength(128);
        formulaField.setPlaceholder(Text.literal("e.g. H2SO4, (NH4)2[Ce(NO3)6]").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(formulaField);

        // Amount input field
        amountField = new TextFieldWidget(this.textRenderer, cx + 10, cy + 55, 120, 16, Text.literal("Amount"));
        amountField.setMaxLength(16);
        amountField.setPlaceholder(Text.literal("e.g. 1.0").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(amountField);

        // Moles / Grams toggle
        CyclingButtonWidget<Boolean> unitToggle = CyclingButtonWidget.onOffBuilder(
                Text.literal("g"), Text.literal("mol"))
                .initially(false)
                .build(cx + 135, cy + 55, 55, 16, Text.literal("Unit"),
                        (button, value) -> useGrams = value);
        this.addDrawableChild(unitToggle);

        // Add button
        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Add to Beaker"), button -> {
            onAddClicked();
        }).dimensions(cx + 10, cy + 85, 180, 20).build();
        this.addDrawableChild(addButton);

        setInitialFocus(formulaField);
    }

    private void onAddClicked() {
        String formula = formulaField.getText().trim();
        String amountStr = amountField.getText().trim();

        if (formula.isEmpty()) {
            statusMessage = "Enter a formula";
            statusColor = 0xFF5555;
            return;
        }

        // Validate formula
        try {
            var parsed = FormulaParser.parse(formula);
            if (parsed.isEmpty()) {
                statusMessage = "Invalid formula";
                statusColor = 0xFF5555;
                return;
            }
        } catch (Exception e) {
            statusMessage = "Invalid formula: " + e.getMessage();
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

        // Send packet to server
        sendCompositionPacket(handler.getBlockPos(), formula, amount, useGrams);

        double molarMass = Compound.computeMolarMass(formula);
        double molesDisplay = useGrams ? amount / molarMass : amount;
        double gramsDisplay = useGrams ? amount : amount * molarMass;
        statusMessage = String.format("Sent: %.3f mol (%.2f g) of %s", molesDisplay, gramsDisplay, formula);
        statusColor = 0x55FF55;
    }

    private void sendCompositionPacket(BlockPos pos, String formula, double amount, boolean isGrams) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeString(formula);
        buf.writeDouble(amount);
        buf.writeBoolean(isGrams);
        ClientPlayNetworking.send(ModNetworking.COMPOSITION_ADD, buf);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        // Dark background panel
        context.fill(cx, cy, cx + this.backgroundWidth, cy + this.backgroundHeight, 0xCC1A1A2E);
        // Border
        context.drawBorder(cx, cy, this.backgroundWidth, this.backgroundHeight, 0xFF4A4A6A);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Title
        context.drawText(this.textRenderer, this.title, 10, 6, 0x00FFAA, true);

        // Labels
        context.drawText(this.textRenderer, Text.literal("Formula:"), 10, 16, 0xCCCCCC, false);
        context.drawText(this.textRenderer, Text.literal("Amount:"), 10, 46, 0xCCCCCC, false);

        // Status message
        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(statusMessage), 10, 112, statusColor, false);
        }

        // Show computed molar mass for current formula
        String formula = formulaField.getText().trim();
        if (!formula.isEmpty()) {
            try {
                var parsed = FormulaParser.parse(formula);
                if (!parsed.isEmpty()) {
                    double mm = Compound.computeMolarMass(formula);
                    context.drawText(this.textRenderer,
                            Text.literal(String.format("M = %.2f g/mol", mm)),
                            10, 126, 0x888888, false);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow typing in text fields without closing the screen
        if (formulaField.isFocused() || amountField.isFocused()) {
            if (keyCode == 256) { // Escape
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
