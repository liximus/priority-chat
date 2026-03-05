package com.listraind.prioritychat.mixin.client;

import com.listraind.prioritychat.PriorityChatConfig;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;


@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

private static final String MARKER_BIG  = "{PC_BIG}";
@Unique
private static final java.util.Map<Integer, Integer> lineWidthsByTop = new java.util.HashMap<>();


@Unique
private static final boolean CHAT_HEADS_LOADED =  net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("chat_heads");

@Unique PriorityChatConfig configInstance = PriorityChatConfig.getInstance();

@Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "method_71991",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V")
    )
    private void modifyTextSize(
            GuiGraphics graphics, Font font, FormattedCharSequence text,
            int x, int y, int color) {

        boolean isBig = isBigLine(text);
        float scale = isBig ? configInstance.getTextScale() : 1.0f;

        FormattedCharSequence cleanText = isBig ? removeMarker(text, MARKER_BIG) : text;

        if (scale != 1.0f) {
            int yOffset = (int)(8 * (scale - 1));
            int correctedY = y - yOffset;

            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, cleanText,
                    (int)(x / scale),
                    (int)(correctedY / scale),
                    color);
            graphics.pose().popMatrix();
        } else {
            graphics.drawString(font, cleanText, x, y, color);
        }
    }




    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow protected abstract int getLineHeight();

    @Shadow
    private static double getTimeFactor(int age) {
        throw new AssertionError();
    }

    @Shadow public abstract void clearMessages(boolean bl);

    @Inject(method = "forEachLine", at = @At("HEAD"), cancellable = true)
    private void customForEachLine(int maxLines, int tickCount, boolean focused,
                                   int startY, ChatComponent.LineConsumer consumer,
                                   CallbackInfoReturnable<Integer> cir) {
        int baseHeight = this.getLineHeight();
        int count = 0;
        int maxIndex = Math.min(this.trimmedMessages.size() - chatScrollbarPos, maxLines);
        int currentBottom = startY;
        if(this.lineWidthsByTop!=null) lineWidthsByTop.clear();

        for (int n = 0; n < maxIndex; n++) {
            int index = n + chatScrollbarPos;
            GuiMessage.Line line = this.trimmedMessages.get(index);
            if (line == null) {
                currentBottom -= baseHeight;
                continue;
            }

            boolean isBig = isBigLine(line.content());
            float msgScale = isBig ? configInstance.getTextScale() : 1f;
            int lineHeight = Math.max(1, (int)(baseHeight * msgScale));
            FormattedCharSequence cleanText = isBig ? removeMarker(line.content(), MARKER_BIG) : line.content();
            int lineWidth = (int)(this.minecraft.font.width(cleanText) * msgScale);
            String text = extractText(cleanText);
            if (CHAT_HEADS_LOADED && ((text.contains("<") && text.contains(">")) || text.contains(": "))) {
                lineWidth += (int)(10*msgScale);
            }

            int bottom = currentBottom;
            int top = bottom - lineHeight;

            int age = tickCount - line.addedTime();
            float opacity = focused ? 1.0F : (float) getTimeFactor(age);

            if (opacity > 1.0E-5F) {
                count++;
                lineWidthsByTop.put(top, lineWidth); // сохраняем ширину
                consumer.accept(0, top, bottom, line, n, opacity);
            }

            currentBottom = top;
        }

        cir.setReturnValue(count);
    }

    private static String extractText(FormattedCharSequence sequence) {
        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }


    @Redirect(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"
                    )

    )
    private List<FormattedCharSequence> modifyWrap(FormattedText content, int width, Font font) {
        String text = content.getString();

        if (configInstance.getIsPersonFavourite(extractNickName(text))) {
            width = (int)(width / 1.6f);
            List<FormattedCharSequence> lines = ComponentRenderUtils.wrapComponents(content, width, font);
            FormattedCharSequence marker = FormattedCharSequence.forward(
                    MARKER_BIG, Style.EMPTY
            );

            List<FormattedCharSequence> marked = new ArrayList<>();
            for (FormattedCharSequence line : lines) {
                marked.add(FormattedCharSequence.composite(line, marker));
            }
            return marked;
        }

        return ComponentRenderUtils.wrapComponents(content, width, font);
    }

    @Unique
    private static boolean isBigLine(FormattedCharSequence sequence) {
        String text = extractText(sequence);
        return text.contains(MARKER_BIG);
    }


    @Unique
    private static FormattedCharSequence removeMarker(FormattedCharSequence original, String marker) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        original.accept((index, style, codePoint) -> {
            fullText.appendCodePoint(codePoint);
            parts.add(FormattedCharSequence.forward(
                    String.valueOf(Character.toChars(codePoint)), style
            ));
            return true;
        });

        String text = fullText.toString();
        if (!text.contains(marker)) {
            return original;
        }

        int markerStart = text.indexOf(marker);
        int markerEnd = markerStart + marker.length();

        List<FormattedCharSequence> filtered = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            if (i < markerStart || i >= markerEnd) {
                filtered.add(parts.get(i));
            }
        }

        return FormattedCharSequence.composite(filtered);
    }

    @Unique
    private static String extractNickName(String text) {

        if (text.contains("<") && text.contains(">")) {
            int start = text.indexOf('<') + 1;
            int end = text.indexOf('>');
            if (start < end && end - start <= 16) {
                return text.substring(start, end);
            }
        }

        if (text.contains("[") && text.contains("]")) {
            int start = text.indexOf('[') + 1;
            int end = text.indexOf(']');
            if (start < end && end - start <= 16) {
                return text.substring(start, end);
            }
        }

        if (text.contains(": ")) {
            String[] parts = text.split(": ", 2);
            if (parts.length > 0 && parts[0].length() <= 16) {
                String nick = parts[0].replaceAll("\\[.*?\\]\\s*", "").trim();
                if (!nick.isEmpty() && nick.length() <= 16) {
                    return nick;
                }
            }
        }

        if (text.contains(" ")) {
            String[] parts = text.split(" ", 2);
            if (parts.length > 0 && parts[0].length() <= 16) {
                String nick = parts[0].replaceAll("\\[.*?\\]\\s*", "").trim();
                if (!nick.isEmpty() && nick.length() <= 16) {
                    return nick;
                }
            }
        }

        return "";
    }

    @Redirect(
            method = "method_71992",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0)
    )
    private void modifyBackgroundWidth(
            GuiGraphics graphics, int left, int top, int right, int bottom, int color) {

        Integer textWidth = 0;

        if(configInstance.getIsMinimalBackground()) textWidth = lineWidthsByTop.get(top);

        if (textWidth !=  null && configInstance.getIsMinimalBackground()) {
            int newRight = left + textWidth + 8;
            graphics.fill(left, top, newRight, bottom, color);
        } else {
            graphics.fill(left, top, right, bottom, color);
        }
    }
}

