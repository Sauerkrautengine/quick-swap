package com.akahotkeymanager;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class AkaHotkeyManagerClient implements ClientModInitializer {

    public static final String MOD_ID = "aka_hotkey_manager";

    private static final int CHEST_SLOT = 6;
    private static final int INV_START = 9;
    private static final int HOTBAR_END = 44;
    private static final int OFFHAND_SLOT = 45;

    private static KeyMapping totemKey;
    private static KeyMapping elytraKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        totemKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aka_hotkey_manager.totem", InputConstants.KEY_X, category));
        elytraKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aka_hotkey_manager.elytra", InputConstants.KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (totemKey.consumeClick()) {
                if (ready(client)) equipTotem(client, client.player);
            }
            while (elytraKey.consumeClick()) {
                if (ready(client)) swapChest(client, client.player);
            }
        });
    }

    private static boolean ready(Minecraft mc) {
        return mc.player != null && mc.gameMode != null && !mc.player.isSpectator();
    }

    private static void equipTotem(Minecraft mc, LocalPlayer player) {
        InventoryMenu menu = player.inventoryMenu;
        if (menu.getSlot(OFFHAND_SLOT).getItem().is(Items.TOTEM_OF_UNDYING)) return;

        for (int i = INV_START; i <= HOTBAR_END; i++) {
            if (menu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
                // Button 40 tauscht mit der Offhand
                mc.gameMode.handleContainerInput(menu.containerId, i, 40, ContainerInput.SWAP, player);
                return;
            }
        }
    }

    private static void swapChest(Minecraft mc, LocalPlayer player) {
        InventoryMenu menu = player.inventoryMenu;
        boolean wantChestplate = menu.getSlot(CHEST_SLOT).getItem().is(Items.ELYTRA);

        int best = -1;
        boolean bestOff = false;
        int bestDur = -1;
        int bestEnch = -1;

        for (int i = INV_START; i <= OFFHAND_SLOT; i++) {
            ItemStack s = menu.getSlot(i).getItem();
            if (!matches(s, wantChestplate)) continue;

            boolean off = i == OFFHAND_SLOT;
            int dur = s.getMaxDamage() - s.getDamageValue();
            int ench = enchantLevels(s);

            if (best == -1 || isBetter(off, dur, ench, bestOff, bestDur, bestEnch)) {
                best = i;
                bestOff = off;
                bestDur = dur;
                bestEnch = ench;
            }
        }
        if (best == -1) return;

        int id = menu.containerId;
        mc.gameMode.handleContainerInput(id, best, 0, ContainerInput.PICKUP, player);
        mc.gameMode.handleContainerInput(id, CHEST_SLOT, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) {
            mc.gameMode.handleContainerInput(id, best, 0, ContainerInput.PICKUP, player);
        }
    }

    private static boolean matches(ItemStack s, boolean wantChestplate) {
        if (s.isEmpty()) return false;
        if (wantChestplate) return s.is(ItemTags.CHEST_ARMOR) && !s.is(Items.ELYTRA);
        return s.is(Items.ELYTRA) && s.getDamageValue() < s.getMaxDamage() - 1;
    }

    private static boolean isBetter(boolean off, int dur, int ench, boolean bOff, int bDur, int bEnch) {
        if (off != bOff) return off;
        if (dur != bDur) return dur > bDur;
        return ench > bEnch;
    }

    private static int enchantLevels(ItemStack s) {
        ItemEnchantments e = s.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int sum = 0;
        for (Holder<Enchantment> h : e.keySet()) {
            sum += e.getLevel(h);
        }
        return sum;
    }
}
