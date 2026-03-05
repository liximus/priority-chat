package com.listraind.prioritychat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChatConfigScreen {

    public static ConfigScreenFactory<?> create() {
        return ChatConfigScreen::buildScreen;
    }

    private static Screen buildScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("PriorityChat Config"))
                .category(buildGeneralCategory())
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory buildGeneralCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.literal("General"))
                .group(buildOptionsGroup())
                .build();
    }


    private static OptionGroup buildOptionsGroup() {
        return OptionGroup.createBuilder()
                .name(Component.literal("Options"))
                .description(OptionDescription.of(Component.literal("Main options")))
                .option(buildTextSizeOption())
                .option(buildMinimalBackgroundOption())
                .build();
    }


    private static Option<Float> buildTextSizeOption() {
        return Option.<Float>createBuilder()
                .name(Component.literal("Text size"))
                .description(OptionDescription.of(Component.literal("text size from favorite players (relative to normal)")))
                .binding(1.5f, PriorityChatConfig.getInstance()::getTextScale, PriorityChatConfig.getInstance()::setTextScale)
                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                        .range(1f, 3f)
                        .step(0.05f))
                .build();
    }

    private static Option<Boolean> buildMinimalBackgroundOption() {
        return Option.<Boolean>createBuilder()
                .name(Component.literal("Minimal background"))
                .description(OptionDescription.of(Component.literal("Use minimal chat background")))
                .binding(true, PriorityChatConfig.getInstance()::getIsMinimalBackground, PriorityChatConfig.getInstance()::setIsMinimalBackground)
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}