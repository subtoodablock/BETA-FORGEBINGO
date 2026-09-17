package com.forgebingo;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class TileDetailOverlay extends JLayeredPane
{
    private static final int ANIMATION_MILLIS = 320;
    private static final int CARD_MARGIN = 8;
    private static final int MAX_CARD_HEIGHT = 350;

    private final JComponent baseContent;
    private final TileArtworkProvider artworkProvider;
    private final Runnable onClosed;
    private final ModalBackdrop backdrop = new ModalBackdrop();
    private final RoundedPanel detailCard = new RoundedPanel(new BorderLayout(0, 8), 14);
    private final ZoomSurface zoomSurface = new ZoomSurface();
    private final JLabel artwork = new JLabel();
    private final JPanel artworkBox = new RoundedPanel(new BorderLayout(), 10);
    private final JTextArea title = textArea(Font.BOLD, 16f, ForgeBingoTheme.GOLD);
    private final JLabel meta = new JLabel();
    private final JTextArea description = textArea(Font.PLAIN, 12f, ForgeBingoTheme.TEXT);
    private final JLabel descriptionHeading = sectionHeading("DESCRIPTION");
    private final JLabel tasksHeading = sectionHeading("TASKS");
    private final JPanel tasks = new JPanel();
    private final JButton close = new JButton("×");
    private Timer animation;
    private long animationStarted;
    private Rectangle animationFrom;
    private Rectangle animationTo;
    private boolean opening;
    private ForgeBingoModels.Tile tile;
    private JComponent sourceTile;
    private int artworkRequest;

    TileDetailOverlay(JComponent baseContent, TileArtworkProvider artworkProvider, Runnable onClosed)
    {
        this.baseContent = baseContent;
        this.artworkProvider = artworkProvider;
        this.onClosed = onClosed;
        setOpaque(false);

        add(baseContent, JLayeredPane.DEFAULT_LAYER);
        add(backdrop, JLayeredPane.MODAL_LAYER);
        add(detailCard, JLayeredPane.POPUP_LAYER);
        add(zoomSurface, JLayeredPane.DRAG_LAYER);
        backdrop.setVisible(false);
        detailCard.setVisible(false);
        zoomSurface.setVisible(false);

        buildCard();
        backdrop.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                closeAnimated();
            }
        });
        close.addActionListener(event -> closeAnimated());
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeTile");
        getActionMap().put("closeTile", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                closeAnimated();
            }
        });
    }

    private void buildCard()
    {
        detailCard.setBackground(ForgeBingoTheme.CARD);
        detailCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ForgeBingoTheme.GOLD_DARK),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel header = new JPanel(new BorderLayout(9, 0));
        header.setOpaque(false);
        artworkBox.setBackground(ForgeBingoTheme.CARD_DARK);
        artworkBox.setBorder(BorderFactory.createLineBorder(ForgeBingoTheme.BORDER));
        artworkBox.setPreferredSize(new Dimension(56, 56));
        artworkBox.setMinimumSize(new Dimension(56, 56));
        artwork.setHorizontalAlignment(SwingConstants.CENTER);
        artwork.setVerticalAlignment(SwingConstants.CENTER);
        artworkBox.add(artwork, BorderLayout.CENTER);
        header.add(artworkBox, BorderLayout.WEST);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        title.setRows(2);
        title.setBorder(null);
        meta.setForeground(ForgeBingoTheme.MUTED);
        meta.setFont(meta.getFont().deriveFont(Font.BOLD, 10f));
        names.add(title);
        names.add(Box.createRigidArea(new Dimension(0, 3)));
        names.add(meta);
        header.add(names, BorderLayout.CENTER);

        close.setToolTipText("Close tile");
        close.setFocusable(false);
        close.setFont(close.getFont().deriveFont(Font.BOLD, 18f));
        close.setForeground(ForgeBingoTheme.TEXT);
        close.setBackground(ForgeBingoTheme.CARD_DARK);
        close.setBorder(BorderFactory.createLineBorder(ForgeBingoTheme.BORDER));
        close.setPreferredSize(new Dimension(28, 28));
        JPanel closeCorner = new JPanel(new BorderLayout());
        closeCorner.setOpaque(false);
        closeCorner.add(close, BorderLayout.NORTH);
        header.add(closeCorner, BorderLayout.EAST);
        detailCard.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        descriptionHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        tasksHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        tasks.setOpaque(false);
        tasks.setLayout(new BoxLayout(tasks, BoxLayout.Y_AXIS));
        tasks.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(descriptionHeading);
        body.add(Box.createRigidArea(new Dimension(0, 4)));
        body.add(description);
        body.add(Box.createRigidArea(new Dimension(0, 12)));
        body.add(tasksHeading);
        body.add(Box.createRigidArea(new Dimension(0, 5)));
        body.add(tasks);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(null);
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(14);
        detailCard.add(bodyScroll, BorderLayout.CENTER);
    }

    void open(ForgeBingoModels.Tile tile, JComponent sourceTile)
    {
        if (tile == null)
        {
            return;
        }
        this.tile = tile;
        this.sourceTile = sourceTile;
        populate(tile);
        Rectangle target = targetBounds();
        detailCard.setBounds(target);
        layoutTree(detailCard);

        if (!isShowing() || sourceTile == null || sourceTile.getWidth() < 2 || sourceTile.getHeight() < 2)
        {
            stopAnimation();
            backdrop.setAlpha(1f);
            backdrop.setVisible(true);
            zoomSurface.setVisible(false);
            detailCard.setVisible(true);
            repaint();
            return;
        }

        Rectangle source = sourceBounds();
        BufferedImage tileImage = snapshot(sourceTile);
        BufferedImage cardImage = snapshot(detailCard);
        stopAnimation();
        backdrop.setAlpha(0f);
        backdrop.setVisible(true);
        detailCard.setVisible(false);
        zoomSurface.setTransition(tileImage, cardImage);
        zoomSurface.setProgress(0f);
        zoomSurface.setBounds(source);
        zoomSurface.setVisible(true);
        startAnimation(source, target, true);
    }

    void refresh(ForgeBingoModels.Tile tile, JComponent sourceTile)
    {
        if (tile == null || this.tile == null || !Objects.equals(tile.id, this.tile.id))
        {
            return;
        }
        this.tile = tile;
        this.sourceTile = sourceTile;
        populate(tile);
        detailCard.revalidate();
        detailCard.repaint();
    }

    void closeAnimated()
    {
        if (!isDetailOpen())
        {
            return;
        }
        stopAnimation();
        Rectangle from = detailCard.isVisible() ? detailCard.getBounds() : targetBounds();
        detailCard.setBounds(from);
        detailCard.setVisible(true);
        layoutTree(detailCard);
        BufferedImage cardImage = snapshot(detailCard);
        BufferedImage tileImage = sourceTile == null ? null : snapshot(sourceTile);
        detailCard.setVisible(false);
        Rectangle target = sourceBounds();
        zoomSurface.setTransition(cardImage, tileImage);
        zoomSurface.setProgress(0f);
        zoomSurface.setBounds(from);
        zoomSurface.setVisible(true);
        startAnimation(from, target, false);
    }

    void closeImmediately()
    {
        boolean wasOpen = isDetailOpen();
        stopAnimation();
        backdrop.setVisible(false);
        detailCard.setVisible(false);
        zoomSurface.setVisible(false);
        tile = null;
        sourceTile = null;
        if (wasOpen && onClosed != null)
        {
            onClosed.run();
        }
    }

    boolean isDetailOpen()
    {
        return tile != null && (backdrop.isVisible() || detailCard.isVisible() || zoomSurface.isVisible());
    }

    String titleText()
    {
        return title.getText();
    }

    String descriptionText()
    {
        return description.getText();
    }

    String taskText()
    {
        StringBuilder text = new StringBuilder();
        for (Component component : tasks.getComponents())
        {
            if (component instanceof TaskRow)
            {
                if (text.length() > 0) text.append('\n');
                text.append(((TaskRow) component).text());
            }
        }
        return text.toString();
    }

    int taskProgressBarCount()
    {
        int count = 0;
        for (Component component : tasks.getComponents())
        {
            if (component instanceof TaskRow)
            {
                count++;
            }
        }
        return count;
    }

    int widestTaskProgressBar()
    {
        int width = 0;
        for (Component component : tasks.getComponents())
        {
            if (component instanceof TaskRow)
            {
                width = Math.max(width, ((TaskRow) component).progressWidth());
            }
        }
        return width;
    }

    boolean descriptionVisible()
    {
        return description.isVisible();
    }

    float titleFontSize()
    {
        return title.getFont().getSize2D();
    }

    float descriptionFontSize()
    {
        return description.getFont().getSize2D();
    }

    String layoutSummary()
    {
        return "overlay=" + getBounds() + "; card=" + detailCard.getBounds()
            + "; visible=" + detailCard.isVisible() + "; tasks=" + tasks.getComponentCount();
    }

    int detailCardHeight()
    {
        return detailCard.getHeight();
    }

    @Override
    public void doLayout()
    {
        baseContent.setBounds(0, 0, getWidth(), getHeight());
        backdrop.setBounds(0, 0, getWidth(), getHeight());
        if (detailCard.isVisible() && animation == null)
        {
            detailCard.setBounds(targetBounds());
        }
    }

    private void populate(ForgeBingoModels.Tile tile)
    {
        title.setText(empty(tile.title) ? "Untitled tile" : tile.title.trim());
        title.setCaretPosition(0);
        String status = tile.verified ? "VERIFIED" : tile.completed ? "COMPLETED" : "NOT COMPLETED";
        int proofCount = tile.proofMedia == null ? 0 : tile.proofMedia.size();
        meta.setText(status + "  ·  " + Math.max(0, tile.points) + " pts"
            + (proofCount == 0 ? "" : "  ·  " + proofCount + (proofCount == 1 ? " proof" : " proofs")));
        meta.setForeground(tile.verified ? ForgeBingoTheme.GREEN
            : tile.completed ? ForgeBingoTheme.GOLD : ForgeBingoTheme.MUTED);

        boolean hasDescription = !empty(tile.description);
        description.setText(hasDescription ? tile.description.trim() : "");
        description.setCaretPosition(0);
        description.setVisible(hasDescription);
        descriptionHeading.setVisible(hasDescription);

        tasks.removeAll();
        if (tile.safeSubTasks().isEmpty())
        {
            double progress = tile.completed ? 1.0 : tile.automationRule != null && tile.automationRule.isNpcLoot()
                ? Math.min(1.0, tile.currentQuantity() / (double) tile.automationRule.requiredQuantity) : 0.0;
            tasks.add(new TaskRow(tile.title == null ? "Tile objective" : tile.title, tile.completed, progress));
        }
        else
        {
            for (ForgeBingoModels.SubTask subTask : tile.safeSubTasks())
            {
                if (subTask != null && !empty(subTask.title))
                {
                    tasks.add(new TaskRow(subTask.title.trim(), subTask.completed, subTask.completed ? 1.0 : 0.0));
                    tasks.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }
        }

        setArtwork(tile);
    }

    private void setArtwork(ForgeBingoModels.Tile tile)
    {
        int request = ++artworkRequest;
        artwork.setIcon(null);
        boolean hasArtwork = TileArtworkLoader.websiteIconUrl(tile) != null
            || TileArtworkLoader.fallbackItemId(tile) != null;
        artworkBox.setVisible(hasArtwork);
        if (!hasArtwork || artworkProvider == null)
        {
            return;
        }
        artworkProvider.load(tile, image -> SwingUtilities.invokeLater(() -> {
            if (request != artworkRequest || image.getWidth() < 1 || image.getHeight() < 1) return;
            double scale = Math.min(46.0 / image.getWidth(), 46.0 / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            artwork.setIcon(new ImageIcon(scaled));
        }));
    }

    private void startAnimation(Rectangle from, Rectangle to, boolean opening)
    {
        this.opening = opening;
        animationFrom = new Rectangle(from);
        animationTo = new Rectangle(to);
        animationStarted = System.nanoTime();
        animation = new Timer(16, event -> animateFrame());
        animation.setCoalesce(true);
        animation.start();
    }

    private void animateFrame()
    {
        float elapsed = (System.nanoTime() - animationStarted) / 1_000_000f;
        float raw = Math.min(1f, elapsed / ANIMATION_MILLIS);
        float eased = raw < .5f
            ? 4f * raw * raw * raw
            : 1f - (float) Math.pow(-2f * raw + 2f, 3) / 2f;
        zoomSurface.setBounds(interpolate(animationFrom, animationTo, eased));
        zoomSurface.setProgress(eased);
        backdrop.setAlpha(opening ? eased : 1f - eased);
        if (raw >= 1f)
        {
            stopAnimation();
            zoomSurface.setVisible(false);
            if (opening)
            {
                detailCard.setBounds(targetBounds());
                detailCard.setVisible(true);
                detailCard.revalidate();
                detailCard.repaint();
            }
            else
            {
                backdrop.setVisible(false);
                tile = null;
                sourceTile = null;
                if (onClosed != null) onClosed.run();
            }
        }
    }

    private void stopAnimation()
    {
        if (animation != null)
        {
            animation.stop();
            animation = null;
        }
    }

    private Rectangle targetBounds()
    {
        int width = Math.max(1, getWidth() - CARD_MARGIN * 2);
        int availableHeight = Math.max(1, getHeight() - 28);
        int desiredHeight = Math.max(220, getHeight() / 2);
        int height = Math.min(MAX_CARD_HEIGHT, Math.min(desiredHeight, availableHeight));
        int x = Math.max(0, (getWidth() - width) / 2);
        int y = Math.max(8, (getHeight() - height) / 2);
        return new Rectangle(x, y, width, height);
    }

    private Rectangle sourceBounds()
    {
        if (sourceTile != null && sourceTile.getParent() != null && sourceTile.getWidth() > 0 && sourceTile.getHeight() > 0)
        {
            return SwingUtilities.convertRectangle(sourceTile.getParent(), sourceTile.getBounds(), this);
        }
        Rectangle target = targetBounds();
        return new Rectangle(target.x + target.width / 2, target.y + target.height / 2, 1, 1);
    }

    private static Rectangle interpolate(Rectangle from, Rectangle to, float amount)
    {
        return new Rectangle(
            Math.round(from.x + (to.x - from.x) * amount),
            Math.round(from.y + (to.y - from.y) * amount),
            Math.max(1, Math.round(from.width + (to.width - from.width) * amount)),
            Math.max(1, Math.round(from.height + (to.height - from.height) * amount)));
    }

    private static BufferedImage snapshot(Component component)
    {
        int width = Math.max(1, component.getWidth());
        int height = Math.max(1, component.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        return image;
    }

    private static void layoutTree(Component component)
    {
        if (component instanceof java.awt.Container)
        {
            java.awt.Container container = (java.awt.Container) component;
            container.doLayout();
            for (Component child : container.getComponents()) layoutTree(child);
        }
    }

    private static JTextArea textArea(int style, float size, Color color)
    {
        JTextArea area = new WrappingTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(area.getFont().deriveFont(style, size));
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    private static JLabel sectionHeading(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(ForgeBingoTheme.GOLD_DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        return label;
    }

    private static boolean empty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private static final class TaskRow extends JPanel
    {
        private static final int PROGRESS_WIDTH = 132;
        private final String value;
        private final boolean completed;
        private final TaskProgressBar progress;

        private TaskRow(String value, boolean completed, double progressValue)
        {
            super(new BorderLayout(7, 0));
            this.value = value;
            this.completed = completed;
            setOpaque(false);
            JLabel state = new JLabel(completed ? "✓" : "○");
            state.setForeground(completed ? ForgeBingoTheme.GREEN : ForgeBingoTheme.MUTED);
            state.setFont(state.getFont().deriveFont(Font.BOLD, 15f));
            state.setVerticalAlignment(SwingConstants.TOP);
            add(state, BorderLayout.WEST);

            JPanel detail = new JPanel();
            detail.setOpaque(false);
            detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
            JTextArea task = textArea(Font.PLAIN, 12f, completed ? new Color(176, 205, 186) : ForgeBingoTheme.TEXT);
            task.setText(value);
            task.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(task);
            detail.add(Box.createRigidArea(new Dimension(0, 5)));
            progress = new TaskProgressBar(progressValue, completed, PROGRESS_WIDTH);
            progress.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(progress);
            add(detail, BorderLayout.CENTER);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            int height = Math.max(34, task.getPreferredSize().height + 12);
            setPreferredSize(new Dimension(1, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        private String text()
        {
            return (completed ? "✓ " : "○ ") + value;
        }

        private int progressWidth()
        {
            return progress.getPreferredSize().width;
        }
    }

    private static final class TaskProgressBar extends JComponent
    {
        private static final int HEIGHT = 5;
        private static final Color TRACK = new Color(49, 50, 57);
        private static final Color GOLD_FILL = new Color(218, 171, 70);
        private static final Color COMPLETE_FILL = new Color(76, 190, 132);
        private final double progress;
        private final boolean completed;

        private TaskProgressBar(double progress, boolean completed, int width)
        {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            this.completed = completed;
            Dimension size = new Dimension(width, HEIGHT);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setToolTipText(completed ? "Task complete" : Math.round(this.progress * 100) + "% complete");
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = HEIGHT;
            g.setColor(TRACK);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            int fillWidth = (int) Math.round(getWidth() * progress);
            if (fillWidth > 0)
            {
                g.setColor(completed ? COMPLETE_FILL : GOLD_FILL);
                g.fillRoundRect(0, 0, fillWidth, getHeight(), arc, arc);
            }
            g.dispose();
        }
    }

    private static final class WrappingTextArea extends JTextArea
    {
        @Override
        public Dimension getMaximumSize()
        {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }
    }

    private static final class ModalBackdrop extends JComponent
    {
        private float alpha;

        private void setAlpha(float alpha)
        {
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.SrcOver.derive(.74f * alpha));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static final class ZoomSurface extends JComponent
    {
        private BufferedImage fromImage;
        private BufferedImage toImage;
        private float progress;

        private void setTransition(BufferedImage fromImage, BufferedImage toImage)
        {
            this.fromImage = fromImage;
            this.toImage = toImage;
            repaint();
        }

        private void setProgress(float progress)
        {
            this.progress = Math.max(0f, Math.min(1f, progress));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            if (fromImage == null && toImage == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (fromImage != null && progress < 1f)
            {
                g.setComposite(AlphaComposite.SrcOver.derive(1f - progress));
                g.drawImage(fromImage, 0, 0, getWidth(), getHeight(), null);
            }
            if (toImage != null && progress > 0f)
            {
                g.setComposite(AlphaComposite.SrcOver.derive(progress));
                g.drawImage(toImage, 0, 0, getWidth(), getHeight(), null);
            }
            g.dispose();
        }
    }

    private static final class RoundedPanel extends JPanel
    {
        private final int arc;

        private RoundedPanel(java.awt.LayoutManager layout, int arc)
        {
            super(layout);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
