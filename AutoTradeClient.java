package com.autotrade;

import com.autotrade.mixin.MerchantScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.lwjgl.glfw.GLFW;

public class AutoTradeClient implements ClientModInitializer {
    public static boolean autoTradeEnabled = false;
    private static int tickCooldown = 0;

    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autotrade.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.autotrade"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(AutoTradeClient::onClientTick);
    }

    public static void toggleAutoTrade() {
        autoTradeEnabled = !autoTradeEnabled;
        tickCooldown = 0;
    }

    private static void onClientTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            if (client.screen instanceof MerchantScreen) {
                toggleAutoTrade();
            }
        }

        if (!(client.screen instanceof MerchantScreen screen) || client.player == null || client.gameMode == null) {
            autoTradeEnabled = false;
            tickCooldown = 0;
            return;
        }

        if (!autoTradeEnabled) {
            return;
        }

        if (tickCooldown > 0) {
            tickCooldown--;
            return;
        }

        MerchantMenu menu = screen.getMenu();
        int selectedIndex = ((MerchantScreenAccessor) screen).getShopItem();
        MerchantOffers offers = menu.getOffers();

        if (selectedIndex < 0 || selectedIndex >= offers.size()) {
            autoTradeEnabled = false;
            return;
        }

        MerchantOffer offer = offers.get(selectedIndex);
        if (offer.isOutOfStock()) {
            autoTradeEnabled = false;
            client.player.displayClientMessage(Component.literal("[Auto Trade] Stopped: trade is out of stock."), true);
            return;
        }

        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundSelectTradePacket(selectedIndex));
        }

        // Shift-click the merchant output slot (slot 2) to perform the trade.
        client.gameMode.handleInventoryMouseClick(
                menu.containerId,
                2,
                0,
                ClickType.QUICK_MOVE,
                client.player
        );

        // Wait 2 ticks between trades so inventory & offer stock stay in sync.
        tickCooldown = 2;
    }
}
