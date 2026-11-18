package com.autotrade.mixin;

import com.autotrade.AutoTradeClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends Screen {
    @Unique
    private Button autotrade$toggleButton;

    protected MerchantScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void autotrade$addToggleButton(CallbackInfo ci) {
        int x = (this.width - 276) / 2 + 180;
        int y = (this.height - 166) / 2 + 6;

        this.autotrade$toggleButton = Button.builder(autotrade$buttonLabel(), btn -> {
            AutoTradeClient.toggleAutoTrade();
            btn.setMessage(autotrade$buttonLabel());
        }).bounds(x, y, 90, 18).build();

        this.addRenderableWidget(this.autotrade$toggleButton);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void autotrade$syncButtonLabel(CallbackInfo ci) {
        if (this.autotrade$toggleButton != null) {
            this.autotrade$toggleButton.setMessage(autotrade$buttonLabel());
        }
    }

    @Unique
    private static Component autotrade$buttonLabel() {
        return Component.translatable(
                AutoTradeClient.autoTradeEnabled ? "gui.autotrade.on" : "gui.autotrade.off"
        );
    }
}
