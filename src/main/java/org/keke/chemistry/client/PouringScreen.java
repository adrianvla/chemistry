package org.keke.chemistry.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import io.netty.buffer.Unpooled;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.network.ModNetworking;
import org.keke.chemistry.screen.PouringScreenHandler;
import org.keke.chemistry.utils.Compound;
import org.keke.chemistry.utils.IUPACNamer;

import java.util.*;

/**
 * Client-side GUI for pouring chemicals between beakers.
 * Left panel shows the source beaker (held item) contents,
 * right panel shows the target beaker (placed block) contents and capacity.
 * The player can choose a compound and an amount to pour.
 */
@Environment(EnvType.CLIENT)
public class PouringScreen extends HandledScreen<PouringScreenHandler> {

    private TextFieldWidget amountField;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;

    /** Formulas available to pour from the source. */
    private List<String> sourceFormulas = new ArrayList<>();
    /** Currently selected formula index. */
    private int selectedIndex = 0;

    private final PlayerInventory playerInventory;

    public PouringScreen(PouringScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.playerInventory = inventory;
        this.backgroundWidth = 260;
        this.backgroundHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        // Refresh source contents from held item
        refreshSourceContents();

        // Amount field
        amountField = new TextFieldWidget(this.textRenderer,
                cx + 10, cy + 130, 100, 16, Text.literal("Amount"));
        amountField.setMaxLength(16);
        amountField.setPlaceholder(Text.literal("mol (0 = all)").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(amountField);

        // Prev / Next compound buttons
        ButtonWidget prevBtn = ButtonWidget.builder(Text.literal("<"), button -> {
            if (!sourceFormulas.isEmpty()) {
                selectedIndex = (selectedIndex - 1 + sourceFormulas.size()) % sourceFormulas.size();
            }
        }).dimensions(cx + 10, cy + 108, 20, 16).build();
        this.addDrawableChild(prevBtn);

        ButtonWidget nextBtn = ButtonWidget.builder(Text.literal(">"), button -> {
            if (!sourceFormulas.isEmpty()) {
                selectedIndex = (selectedIndex + 1) % sourceFormulas.size();
            }
        }).dimensions(cx + 115, cy + 108, 20, 16).build();
        this.addDrawableChild(nextBtn);

        // Pour Selected button
        ButtonWidget pourBtn = ButtonWidget.builder(Text.literal("Pour"), button -> {
            onPourClicked(false);
        }).dimensions(cx + 115, cy + 130, 50, 16).build();
        this.addDrawableChild(pourBtn);

        // Pour All button
        ButtonWidget pourAllBtn = ButtonWidget.builder(Text.literal("Pour All"), button -> {
            onPourClicked(true);
        }).dimensions(cx + 170, cy + 130, 80, 16).build();
        this.addDrawableChild(pourAllBtn);

        setInitialFocus(amountField);
    }

    private void refreshSourceContents() {
        sourceFormulas.clear();
        Map<String, Double> contents = BeakerItem.getContents(
                playerInventory.getMainHandStack());
        sourceFormulas.addAll(contents.keySet());
        if (selectedIndex >= sourceFormulas.size()) {
            selectedIndex = 0;
        }
    }

    private void onPourClicked(boolean pourAll) {
        if (sourceFormulas.isEmpty()) {
            statusMessage = "Source beaker is empty";
            statusColor = 0xFF5555;
            return;
        }

        if (pourAll) {
            // Send pour-all packet
            sendPourPacket(handler.getBlockPos(), "", 0, true);
            statusMessage = "Pouring all contents...";
            statusColor = 0x55FF55;
            return;
        }

        String formula = sourceFormulas.get(selectedIndex);
        String amountStr = amountField.getText().trim();
        double amount = 0;  // 0 means all of this compound

        if (!amountStr.isEmpty()) {
            try {
                amount = Double.parseDouble(amountStr);
                if (amount < 0) {
                    statusMessage = "Amount must be ≥ 0";
                    statusColor = 0xFF5555;
                    return;
                }
            } catch (NumberFormatException e) {
                statusMessage = "Invalid number";
                statusColor = 0xFF5555;
                return;
            }
        }

        sendPourPacket(handler.getBlockPos(), formula, amount, false);

        if (amount <= 0) {
            statusMessage = String.format("Pouring all %s", formula);
        } else {
            statusMessage = String.format("Pouring %.3f mol of %s", amount, formula);
        }
        statusColor = 0x55FF55;
    }

    private void sendPourPacket(BlockPos pos, String formula, double amount, boolean pourAll) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeString(formula);
        buf.writeDouble(amount);
        buf.writeBoolean(pourAll);
        ClientPlayNetworking.send(ModNetworking.BEAKER_POUR, buf);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        // Dark background panel
        context.fill(cx, cy, cx + backgroundWidth, cy + backgroundHeight, 0xCC1A1A2E);
        context.drawBorder(cx, cy, backgroundWidth, backgroundHeight, 0xFF4A4A6A);

        // Divider between source and target panels
        int mid = cx + backgroundWidth / 2;
        context.fill(mid, cy + 20, mid + 1, cy + 100, 0xFF4A4A6A);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Title
        context.drawText(textRenderer, Text.literal("Pour Between Beakers"),
                10, 5, 0x00FFAA, true);

        // Source header
        context.drawText(textRenderer, Text.literal("Source (held)"),
                10, 18, 0xAAAAFF, false);

        // Target header
        context.drawText(textRenderer, Text.literal("Target (placed)"),
                backgroundWidth / 2 + 5, 18, 0xFFAAAA, false);

        // === Source contents ===
        refreshSourceContents();
        Map<String, Double> sourceContents = BeakerItem.getContents(
                playerInventory.getMainHandStack());
        int y = 30;
        if (sourceContents.isEmpty()) {
            context.drawText(textRenderer, Text.literal("Empty").formatted(Formatting.GRAY),
                    10, y, 0x888888, false);
        } else {
            for (var entry : sourceContents.entrySet()) {
                String formula = entry.getKey();
                double moles = entry.getValue();
                int color = sourceFormulas.indexOf(formula) == selectedIndex
                        ? 0xFFFF55 : 0xCCCCCC;
                String line = String.format("%s: %.3f mol", formula, moles);
                context.drawText(textRenderer, Text.literal(line), 10, y, color, false);
                y += 10;
                if (y > 95) break;
            }
        }

        // === Target contents ===
        BlockPos targetPos = handler.getBlockPos();
        y = 30;
        int targetX = backgroundWidth / 2 + 5;

        if (client != null && client.world != null) {
            BlockEntity be = client.world.getBlockEntity(targetPos);
            if (be instanceof BeakerLiquidBlockEntity beaker) {
                Map<String, Double> targetContents = beaker.getContents();
                if (targetContents.isEmpty()) {
                    context.drawText(textRenderer, Text.literal("Empty").formatted(Formatting.GRAY),
                            targetX, y, 0x888888, false);
                } else {
                    for (var entry : targetContents.entrySet()) {
                        String line = String.format("%s: %.3f mol",
                                entry.getKey(), entry.getValue());
                        context.drawText(textRenderer, Text.literal(line),
                                targetX, y, 0xCCCCCC, false);
                        y += 10;
                        if (y > 85) break;
                    }
                }
                // Capacity info
                double vol = beaker.getTotalVolumeMl();
                double cap = beaker.getMaxCapacityMl();
                context.drawText(textRenderer,
                        Text.literal(String.format("%.0f / %.0f mL", vol, cap)),
                        targetX, 92, vol > cap * 0.9 ? 0xFF5555 : 0x88FF88, false);
            }
        }

        // Selected compound display
        if (!sourceFormulas.isEmpty()) {
            String sel = sourceFormulas.get(selectedIndex);
            String iupac = IUPACNamer.getName(sel);
            String display = sel.equals(iupac) ? sel : sel + " (" + iupac + ")";
            context.drawText(textRenderer, Text.literal(display),
                    35, 110, 0xFFFF55, false);
        }

        // Status message
        if (!statusMessage.isEmpty()) {
            context.drawText(textRenderer, Text.literal(statusMessage),
                    10, 155, statusColor, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (amountField.isFocused()) {
            if (keyCode == 256) {
                this.close();
                return true;
            }
            amountField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
