package com.ziondev.experiencetweaks.mixin;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.ziondev.experiencetweaks.Config;
import com.ziondev.experiencetweaks.EnchantmentConfigHandler;
import com.ziondev.experiencetweaks.network.ClientEnchantLevelCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Shadow
    @Final
    private static Identifier ENCHANTMENT_SLOT_DISABLED_SPRITE;

    @Shadow
    @Final
    private static Identifier ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE;

    @Shadow
    @Final
    private static Identifier ENCHANTMENT_SLOT_SPRITE;

    @Shadow
    @Final
    private static Identifier ENCHANTING_TABLE_LOCATION;

    public EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow
    private void extractBook(GuiGraphicsExtractor graphics, int left, int top) {
        throw new AssertionError();
    }

    @Inject(
            method = "extractBackground",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float a,
            CallbackInfo ci
    ) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, ENCHANTING_TABLE_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractBook(graphics, xo, yo);
        EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());

        for (int buttonId = 0; buttonId < 3; buttonId++) {
            int leftPos = xo + 60;
            int leftPosText = leftPos + 20;
            int requiredLevel = this.menu.costs[buttonId];
            if (requiredLevel == 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_DISABLED_SPRITE, leftPos, yo + 14 + 19 * buttonId, 108, 19);
                continue;
            }

            // Item cost is based on button index so it is always ordered 1x/2x/3x the multiplier
            int itemCost = experienceTweaks$getItemCost(buttonId);
            int personalRequiredLevel = ClientEnchantLevelCache.getRequiredLevel(buttonId);
            String levelText = Integer.toString(personalRequiredLevel);
            int textWidth = 86 - this.font.width(levelText);
            FormattedText message = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
            int textColor = -9937334;
            boolean affordable = this.minecraft.player.getAbilities().instabuild
                    || this.minecraft.player.experienceLevel >= personalRequiredLevel
                    && experienceTweaks$getCurrencyCount() >= itemCost;

            if (!affordable || this.menu.enchantClue[buttonId] == -1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_DISABLED_SPRITE, leftPos, yo + 14 + 19 * buttonId, 108, 19);
                experienceTweaks$drawCurrencyCost(graphics, leftPos + 1, yo + 15 + 19 * buttonId, itemCost, false);
                graphics.textWithWordWrap(this.font, message, leftPosText, yo + 16 + 19 * buttonId, textWidth, ARGB.opaque((textColor & 16711422) >> 1), false);
                textColor = -12550384;
            } else {
                int xx = mouseX - (xo + 60);
                int yy = mouseY - (yo + 14 + 19 * buttonId);
                if (xx >= 0 && yy >= 0 && xx < 108 && yy < 19) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE, leftPos, yo + 14 + 19 * buttonId, 108, 19);
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                    textColor = -128;
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_SPRITE, leftPos, yo + 14 + 19 * buttonId, 108, 19);
                }

                experienceTweaks$drawCurrencyCost(graphics, leftPos + 1, yo + 15 + 19 * buttonId, itemCost, true);
                graphics.textWithWordWrap(this.font, message, leftPosText, yo + 16 + 19 * buttonId, textWidth, textColor, false);
                textColor = -8323296;
            }

            graphics.text(this.font, levelText, leftPosText + 86 - this.font.width(levelText), yo + 16 + 19 * buttonId + 7, textColor);
        }

        ci.cancel();
    }

    @Inject(
            method = "extractRenderState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$extractTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float ignored,
            CallbackInfo ci
    ) {
        float partialTick = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        assert this.minecraft.player != null;
        boolean infiniteMaterials = this.minecraft.player.hasInfiniteMaterials();

        for (int buttonId = 0; buttonId < 3; buttonId++) {
            int requiredLevel = this.menu.costs[buttonId];
            assert this.minecraft
                    .level != null;
            Optional<Holder.Reference<Enchantment>> enchantment = this.minecraft
                    .level
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(this.menu.enchantClue[buttonId]);

            if (this.isHovering(60, 14 + 19 * buttonId, 108, 17, mouseX, mouseY) && requiredLevel > 0) {
                int enchantmentLevel = this.menu.levelClue[buttonId];
                // Item cost based on button index, not vanilla required level
                int itemCost = experienceTweaks$getItemCost(buttonId);
                List<Component> texts = Lists.newArrayList();
                texts.add(Component.translatable(
                        "container.enchant.clue",
                        enchantment.isEmpty() ? "" : Enchantment.getFullname(enchantment.get(), enchantmentLevel)
                ).withStyle(ChatFormatting.WHITE));

                if (enchantment.isEmpty()) {
                    texts.add(Component.literal(""));
                    texts.add(Component.translatable("neoforge.container.enchant.limitedEnchantability").withStyle(ChatFormatting.RED));
                } else if (!infiniteMaterials) {
                    texts.add(CommonComponents.EMPTY);
                    boolean hasItems = experienceTweaks$getCurrencyCount() >= itemCost;
                    ItemStack costStack = new ItemStack(experienceTweaks$getCostItem());
                    texts.add(Component.literal(itemCost + "x ").append(costStack.getHoverName()).withStyle(hasItems ? ChatFormatting.GRAY : ChatFormatting.RED));
                    int personalRequired = ClientEnchantLevelCache.getRequiredLevel(buttonId);
                    boolean hasPersonalLevel = this.minecraft.player.experienceLevel >= personalRequired;
                    texts.add(Component.translatable("experiencetweaks.enchant.tooltip.min_level", personalRequired).withStyle(hasPersonalLevel ? ChatFormatting.GRAY : ChatFormatting.RED));
                    texts.add(Component.translatable("experiencetweaks.enchant.tooltip.no_consumption").withStyle(ChatFormatting.DARK_GRAY));
                }

                graphics.setComponentTooltipForNextFrame(this.font, texts, mouseX, mouseY);
                break;
            }
        }

        ci.cancel();
    }

    @Unique
    private void experienceTweaks$drawCurrencyCost(GuiGraphicsExtractor graphics, int x, int y, int itemCost, boolean enabled) {
        ItemStack stack = new ItemStack(experienceTweaks$getCostItem());
        graphics.item(stack, x, y);
        String label = Integer.toString(itemCost);
        int labelColor = enabled ? -1 : -12550384;
        graphics.text(this.font, label, x + 17 - this.font.width(label), y + 9, labelColor, true);
    }

    @Unique
    private int experienceTweaks$getCurrencyCount() {
        ItemStack currency = this.menu.getSlot(1).getItem();
        return currency.is(experienceTweaks$getCostItem()) ? currency.getCount() : 0;
    }

    @Unique
    private static Item experienceTweaks$getCostItem() {
        return EnchantmentConfigHandler.getConfiguredItem();
    }

    /**
     * Item cost is based on button index (0, 1, 2), so the cost is always ordered: button 1 < button 2 < button 3, 
     * regardless of what level values the vanilla enchantment system rolls into costs[].
     */
    @Unique
    private static int experienceTweaks$getItemCost(int buttonId) {
        double multiplier = Config.ENCHANTMENT_COST_MULTIPLIER.get();
        if (multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil((buttonId + 1) * multiplier));
    }
}
