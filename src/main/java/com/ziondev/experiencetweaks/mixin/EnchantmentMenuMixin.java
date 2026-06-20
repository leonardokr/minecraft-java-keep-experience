package com.ziondev.experiencetweaks.mixin;

import com.ziondev.experiencetweaks.ExperienceTweaksMod;
import com.ziondev.experiencetweaks.ModConfig;
import com.ziondev.experiencetweaks.PlayerEnchantData;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private DataSlot enchantmentSeed;

    @Shadow
    @Final
    public int[] costs;

    protected EnchantmentMenuMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Shadow
    public abstract void slotsChanged(@NonNull Container container);

    @Invoker("getEnchantmentList")
    protected abstract List<EnchantmentInstance> experienceTweaks$getEnchantmentList(
            RegistryAccess access,
            ItemStack itemStack,
            int slot,
            int enchantmentCost
    );

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN")
    )
    private void experienceTweaks$replaceCurrencySlot(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            CallbackInfo ci
    ) {
        Slot originalSlot = this.slots.get(1);
        Slot replacementSlot = new Slot(this.enchantSlots, 1, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(experienceTweaks$getCostItem());
            }

            @Override
            public Identifier getNoItemIcon() {
                return null;
            }
        };
        replacementSlot.index = originalSlot.index;
        this.slots.set(1, replacementSlot);
    }

    @Inject(
            method = "clickMenuButton",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$enchantWithConfiguredItem(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (buttonId < 0 || buttonId >= this.costs.length) {
            return;
        }

        ItemStack itemStack = this.enchantSlots.getItem(0);
        ItemStack currency = this.enchantSlots.getItem(1);
        int requiredLevel = this.costs[buttonId];
        int vanillaLevelCost = buttonId + 1;
        // Item cost is based on button index (1, 2, 3), so it is always ordered
        int itemCost = experienceTweaks$getItemCost(buttonId);

        if (requiredLevel <= 0 || itemStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            // Check the per-player cooldown level
            int personalRequiredLevel = experienceTweaks$getPersonalRequiredLevel(player, buttonId);
            if (player.experienceLevel < personalRequiredLevel) {
                cir.setReturnValue(false);
                return;
            }

            if (currency.isEmpty()
                    || !currency.is(experienceTweaks$getCostItem())
                    || currency.getCount() < itemCost) {
                cir.setReturnValue(false);
                return;
            }
        }

        this.access.execute((level, pos) -> {
            ItemStack enchantmentItem;
            List<EnchantmentInstance> enchantments = this.experienceTweaks$getEnchantmentList(
                    level.registryAccess(),
                    itemStack,
                    buttonId,
                    requiredLevel
            );

            if (!enchantments.isEmpty()) {
                // Record the enchanting BEFORE giveExperienceLevels changes the level
                int levelBeforeEnchant = player.experienceLevel;

                player.onEnchantmentPerformed(itemStack, vanillaLevelCost);
                player.giveExperienceLevels(vanillaLevelCost);

                enchantmentItem = itemStack.getItem().applyEnchantments(itemStack, enchantments);
                this.enchantSlots.setItem(0, enchantmentItem);
                net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, enchantmentItem, enchantments);

                if (!player.hasInfiniteMaterials() && itemCost > 0) {
                    currency.consume(itemCost, player);
                    if (currency.isEmpty()) {
                        this.enchantSlots.setItem(1, ItemStack.EMPTY);
                    }
                }

                // Update the per-player required level for next use of this button.
                if (!player.hasInfiniteMaterials()) {
                    PlayerEnchantData enchantData = ExperienceTweaksMod.getEnchantData();
                    if (enchantData != null) {
                        String cooldownType = ModConfig.getEnchantmentCooldownType();
                        if (cooldownType.equalsIgnoreCase("current_level")) {
                            enchantData.recordEnchant(player.getUUID(), levelBeforeEnchant);
                        } else if (cooldownType.equalsIgnoreCase("last_level")) {
                            enchantData.recordEnchant(player.getUUID(), requiredLevel);
                        }
                    }
                    if (player instanceof ServerPlayer serverPlayer) {
                        ExperienceTweaksMod.syncEnchantLevels(serverPlayer);
                    }
                }

                player.awardStat(Stats.ENCHANT_ITEM);
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchantmentItem, vanillaLevelCost);
                }

                this.enchantSlots.setChanged();
                this.enchantmentSeed.set(player.getEnchantmentSeed());
                this.slotsChanged(this.enchantSlots);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
        });

        cir.setReturnValue(true);
    }

    @Unique
    private static int experienceTweaks$getPersonalRequiredLevel(Player player, int buttonId) {
        PlayerEnchantData enchantData = ExperienceTweaksMod.getEnchantData();
        if (enchantData != null) {
            return enchantData.getRequiredLevel(player.getUUID(), buttonId, player.experienceLevel);
        }
        // Fallback to base level from config when SavedData is not available
        return ModConfig.getEnchantmentBaseRequiredLevel(buttonId);
    }

    @Inject(
            method = "quickMoveStack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$quickMoveConfiguredItem(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem() || slotIndex < 2) {
            return;
        }

        ItemStack stack = slot.getItem();
        if (stack.is(experienceTweaks$getCostItem()) && !stack.is(Items.LAPIS_LAZULI)) {
            ItemStack clicked = stack.copy();
            if (!this.moveItemStackTo(stack, 1, 2, true)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            slot.onTake(player, stack);
            cir.setReturnValue(clicked);
        }
    }

    @Inject(
            method = "getGoldCount",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$getAffordableEnchantingButtonCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack currency = this.enchantSlots.getItem(1);
        if (currency.isEmpty() || !currency.is(experienceTweaks$getCostItem())) {
            cir.setReturnValue(0);
            return;
        }

        int affordableButtons = 0;
        for (int buttonId = 0; buttonId < this.costs.length; buttonId++) {
            int requiredLevel = this.costs[buttonId];
            if (requiredLevel > 0 && currency.getCount() >= experienceTweaks$getItemCost(buttonId)) {
                affordableButtons = buttonId + 1;
            }
        }

        cir.setReturnValue(affordableButtons);
    }

    @Unique
    private static Item experienceTweaks$getCostItem() {
        return ModConfig.getEnchantmentCostItem();
    }

    @Unique
    private static int experienceTweaks$getItemCost(int buttonId) {
        double multiplier = ModConfig.getEnchantmentCostMultiplier();
        if (multiplier <= 0.0D) {
            return 0;
        }
        // Cost scales with button index (1, 2, 3) so buttons are always ordered
        return Math.max(1, (int) Math.ceil((buttonId + 1) * multiplier));
    }
}
