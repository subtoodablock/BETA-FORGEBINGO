package com.forgebingo;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ForgeBingoTheme
{
    static final Color BACKGROUND = new Color(14, 15, 18);
    static final Color CARD = new Color(23, 24, 29);
    static final Color CARD_DARK = new Color(16, 17, 21);
    static final Color BORDER = new Color(73, 61, 46);
    static final Color GOLD = new Color(255, 190, 31);
    static final Color GOLD_DARK = new Color(143, 99, 28);
    static final Color TEXT = new Color(226, 221, 211);
    static final Color MUTED = new Color(139, 137, 132);
    static final Color GREEN = new Color(67, 177, 112);
    static final Color RED = new Color(196, 65, 55);

    private static final Pattern HEX = Pattern.compile("^#([0-9a-fA-F]{6})$");
    private static final Pattern HSL = Pattern.compile(
        "^hsl\\(\\s*([+-]?[0-9]+(?:\\.[0-9]+)?)\\s*[, ]\\s*([0-9]+(?:\\.[0-9]+)?)%\\s*[, ]\\s*([0-9]+(?:\\.[0-9]+)?)%\\s*\\)$",
        Pattern.CASE_INSENSITIVE);

    private ForgeBingoTheme()
    {
    }

    static Color parseTileColor(String value)
    {
        if (value == null)
        {
            return null;
        }
        Matcher hex = HEX.matcher(value.trim());
        if (hex.matches())
        {
            return new Color(Integer.parseInt(hex.group(1), 16));
        }
        Matcher hsl = HSL.matcher(value.trim());
        if (!hsl.matches())
        {
            return null;
        }
        float hue = (float) (((Double.parseDouble(hsl.group(1)) % 360.0) + 360.0) % 360.0 / 360.0);
        float saturation = clamp((float) (Double.parseDouble(hsl.group(2)) / 100.0));
        float lightness = clamp((float) (Double.parseDouble(hsl.group(3)) / 100.0));
        float brightness = lightness + saturation * Math.min(lightness, 1.0f - lightness);
        float adjustedSaturation = brightness == 0 ? 0 : 2.0f * (1.0f - lightness / brightness);
        return Color.getHSBColor(hue, adjustedSaturation, brightness);
    }

    static Color mix(Color first, Color second, float amount)
    {
        float ratio = clamp(amount);
        return new Color(
            Math.round(first.getRed() * (1 - ratio) + second.getRed() * ratio),
            Math.round(first.getGreen() * (1 - ratio) + second.getGreen() * ratio),
            Math.round(first.getBlue() * (1 - ratio) + second.getBlue() * ratio));
    }

    private static float clamp(float value)
    {
        return Math.max(0, Math.min(1, value));
    }
}
