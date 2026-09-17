package com.forgebingo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.SwingConstants;

final class BingoTileButton extends JButton
{
    private static final int ARC = 9;
    private final ForgeBingoModels.Tile tile;
    private boolean selected;
    private BufferedImage tileImage;

    BingoTileButton(ForgeBingoModels.Tile tile, boolean selected)
    {
        this.tile = tile;
        this.selected = selected;
        setText("");
        getAccessibleContext().setAccessibleName(tile.title == null ? "Bingo tile" : tile.title);
        getAccessibleContext().setAccessibleDescription("Open this tile's task details");
        setFont(getFont().deriveFont(Font.BOLD, 8f));
        setForeground(textColor());
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(0);
        setMargin(new java.awt.Insets(2, 1, 3, 1));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    void useTileIcon(BufferedImage image)
    {
        tileImage = image;
        repaint();
    }

    void setTileSelected(boolean selected)
    {
        this.selected = selected;
        repaint();
    }

    private Color textColor()
    {
        if (tile.verified)
        {
            return new Color(181, 240, 205);
        }
        return tile.completed ? new Color(255, 225, 135) : ForgeBingoTheme.TEXT;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color top;
        Color bottom;
        Color border;
        if (tile.verified)
        {
            top = new Color(29, 88, 59);
            bottom = new Color(13, 43, 31);
            border = ForgeBingoTheme.GREEN;
        }
        else if (tile.completed)
        {
            top = new Color(104, 75, 23);
            bottom = new Color(43, 34, 18);
            border = ForgeBingoTheme.GOLD;
        }
        else
        {
            Color custom = ForgeBingoTheme.parseTileColor(tile.backgroundColor);
            top = custom == null ? new Color(39, 40, 46) : ForgeBingoTheme.mix(custom, Color.WHITE, 0.08f);
            bottom = custom == null ? new Color(22, 23, 28) : ForgeBingoTheme.mix(custom, Color.BLACK, 0.24f);
            border = getModel().isRollover() ? ForgeBingoTheme.GOLD_DARK : ForgeBingoTheme.BORDER;
        }
        if (getModel().isPressed())
        {
            top = ForgeBingoTheme.mix(top, Color.BLACK, 0.18f);
            bottom = ForgeBingoTheme.mix(bottom, Color.BLACK, 0.18f);
        }

        g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
        g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC, ARC);
        g.setColor(selected ? ForgeBingoTheme.GOLD : border);
        g.setStroke(new BasicStroke(selected ? 2f : 1f));
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC, ARC);
        g.dispose();

        super.paintComponent(graphics);

        Graphics2D overlay = (Graphics2D) graphics.create();
        overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (tileImage != null)
        {
            int available = Math.max(6, Math.min(32, Math.min(getWidth() - 6, getHeight() - 8)));
            double scale = Math.min(available / (double) tileImage.getWidth(), available / (double) tileImage.getHeight());
            int iconWidth = Math.max(1, (int) Math.round(tileImage.getWidth() * scale));
            int iconHeight = Math.max(1, (int) Math.round(tileImage.getHeight() * scale));
            int iconX = (getWidth() - iconWidth) / 2;
            int iconY = (getHeight() - iconHeight) / 2;
            overlay.drawImage(tileImage, iconX, iconY, iconWidth, iconHeight, null);
        }

        if (tile.completed || tile.verified)
        {
            int diameter = Math.max(7, Math.min(14, Math.min(getWidth(), getHeight()) - 5));
            int x = getWidth() - diameter - 4;
            int y = 3;
            overlay.setColor(tile.verified ? ForgeBingoTheme.GREEN : ForgeBingoTheme.GOLD);
            overlay.fillOval(x, y, diameter, diameter);
            overlay.setColor(tile.verified ? Color.WHITE : new Color(35, 26, 8));
            overlay.setStroke(new BasicStroke(Math.max(1f, diameter / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            overlay.drawLine(x + diameter / 4, y + diameter / 2,
                x + diameter / 2 - 1, y + diameter - diameter / 4);
            overlay.drawLine(x + diameter / 2 - 1, y + diameter - diameter / 4,
                x + diameter - diameter / 5, y + diameter / 4);
        }

        if (!tile.completed && tile.automationRule != null && tile.automationRule.isNpcLoot())
        {
            int required = Math.max(1, tile.automationRule.requiredQuantity);
            double progress = Math.min(1.0, tile.currentQuantity() / (double) required);
            int width = Math.max(0, (int) Math.round((getWidth() - 6) * progress));
            overlay.setColor(new Color(52, 47, 36));
            overlay.fillRoundRect(3, getHeight() - 5, getWidth() - 6, 3, 3, 3);
            overlay.setColor(ForgeBingoTheme.GOLD);
            overlay.fillRoundRect(3, getHeight() - 5, width, 3, 3, 3);
        }
        overlay.dispose();
    }

}
