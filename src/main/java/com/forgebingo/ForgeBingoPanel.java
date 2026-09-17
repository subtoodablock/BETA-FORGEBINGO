package com.forgebingo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicProgressBarUI;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

final class ForgeBingoPanel extends PluginPanel
{
    private static final String WEBSITE = "https://www.forgebingo.com/plugin";
    private static final int CONTROL_HEIGHT = 26;
    private static final int STANDARD_TILE_CELL_HEIGHT = 58;
    private static final int LARGE_BOARD_TARGET_HEIGHT = 252;

    private final ForgeBingoPlugin plugin;
    private final TileArtworkProvider artworkProvider;
    private final JLabel connection = new JLabel("API key needed");
    private final JLabel boardTitle = new JLabel("Choose board");
    private final JLabel teamName = new JLabel(" ");
    private final JComboBox<ForgeBingoModels.BoardSummary> boardPicker = new JComboBox<>();
    private final JProgressBar progress = new JProgressBar();
    private final JPanel boardFrame = new JPanel(new BorderLayout());
    private final JPanel grid = new JPanel();
    private final JButton retryProof = new JButton("Retry failed proof");
    private final Map<String, BingoTileButton> tileButtons = new LinkedHashMap<>();
    private final TileDetailOverlay detailOverlay;
    private boolean updatingBoardPicker;
    private ForgeBingoModels.BoardResponse board;
    private ForgeBingoModels.Tile selectedTile;

    ForgeBingoPanel(ForgeBingoPlugin plugin, TileArtworkProvider artworkProvider)
    {
        super(false);
        this.plugin = plugin;
        this.artworkProvider = artworkProvider;
        setLayout(new BorderLayout());
        setBackground(ForgeBingoTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        JPanel content = new WidthTrackingPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ForgeBingoTheme.BACKGROUND);

        content.add(buildHeader());
        content.add(Box.createRigidArea(new Dimension(0, 5)));

        connection.setHorizontalAlignment(SwingConstants.CENTER);
        connection.setForeground(ForgeBingoTheme.MUTED);
        connection.setFont(connection.getFont().deriveFont(Font.PLAIN, 11f));
        fillWidth(connection, 20);
        content.add(connection);
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        stylePicker();
        content.add(boardPicker);
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        progress.setStringPainted(true);
        progress.setString("No board selected");
        progress.setForeground(ForgeBingoTheme.GOLD);
        progress.setBackground(ForgeBingoTheme.CARD_DARK);
        progress.setUI(new BasicProgressBarUI()
        {
            @Override
            protected Color getSelectionForeground()
            {
                return new Color(37, 29, 12);
            }

            @Override
            protected Color getSelectionBackground()
            {
                return new Color(225, 214, 186);
            }
        });
        progress.setBorder(BorderFactory.createLineBorder(ForgeBingoTheme.BORDER));
        fillWidth(progress, 18);
        content.add(progress);
        content.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel actions = new JPanel(new GridLayout(1, 2, 5, 0));
        actions.setOpaque(false);
        JButton refresh = actionButton("Refresh");
        refresh.addActionListener(event -> plugin.manualRefresh());
        JButton website = actionButton("Full website");
        website.addActionListener(event -> LinkBrowser.browse(WEBSITE));
        actions.add(refresh);
        actions.add(website);
        fillWidth(actions, CONTROL_HEIGHT);
        content.add(actions);
        content.add(Box.createRigidArea(new Dimension(0, 7)));

        boardFrame.setBackground(ForgeBingoTheme.CARD_DARK);
        boardFrame.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ForgeBingoTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        grid.setBackground(ForgeBingoTheme.CARD_DARK);
        boardFrame.add(grid, BorderLayout.CENTER);
        fillWidth(boardFrame, 12);
        content.add(boardFrame);
        content.add(Box.createRigidArea(new Dimension(0, 7)));

        retryProof.setVisible(false);
        retryProof.addActionListener(event -> plugin.retryFailedProof());
        stylePrimaryButton(retryProof);
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        content.add(retryProof);

        JLabel privacy = new JLabel("Proof uploads: off by default");
        privacy.setForeground(new Color(105, 105, 105));
        privacy.setHorizontalAlignment(SwingConstants.CENTER);
        privacy.setFont(privacy.getFont().deriveFont(Font.PLAIN, 9f));
        fillWidth(privacy, 18);
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        content.add(privacy);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(ForgeBingoTheme.BACKGROUND);
        scrollPane.getViewport().setBackground(ForgeBingoTheme.BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        detailOverlay = new TileDetailOverlay(scrollPane, artworkProvider, this::onDetailClosed);
        add(detailOverlay, BorderLayout.CENTER);
    }

    private JComponent buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(9, 0));
        header.setBackground(ForgeBingoTheme.CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ForgeBingoTheme.BORDER),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)));

        Image logo = ImageUtil.loadImageResource(getClass(), "/forgebingo-icon.png")
            .getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logo));
        logoLabel.setPreferredSize(new Dimension(42, 42));
        header.add(logoLabel, BorderLayout.WEST);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel product = new JLabel("FORGEBINGO");
        product.setForeground(ForgeBingoTheme.GOLD);
        product.setFont(product.getFont().deriveFont(Font.BOLD, 14f));
        boardTitle.setForeground(ForgeBingoTheme.TEXT);
        boardTitle.setFont(boardTitle.getFont().deriveFont(Font.BOLD, 11f));
        teamName.setForeground(ForgeBingoTheme.MUTED);
        teamName.setFont(teamName.getFont().deriveFont(Font.PLAIN, 9f));
        names.add(product);
        names.add(boardTitle);
        names.add(teamName);
        header.add(names, BorderLayout.CENTER);
        fillWidth(header, 56);
        return header;
    }

    private void stylePicker()
    {
        boardPicker.setBackground(ForgeBingoTheme.CARD);
        boardPicker.setForeground(ForgeBingoTheme.TEXT);
        boardPicker.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBackground(isSelected ? new Color(70, 55, 25) : ForgeBingoTheme.CARD);
                label.setForeground(isSelected ? new Color(255, 228, 151) : ForgeBingoTheme.TEXT);
                label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                return label;
            }
        });
        fillWidth(boardPicker, CONTROL_HEIGHT);
        boardPicker.addActionListener(event -> {
            if (!updatingBoardPicker)
            {
                ForgeBingoModels.BoardSummary selected = (ForgeBingoModels.BoardSummary) boardPicker.getSelectedItem();
                if (selected != null)
                {
                    plugin.selectBoard(selected.teamBoardId);
                }
            }
        });
    }

    void showConnecting()
    {
        onEdt(() -> {
            connection.setText("● Connecting");
            connection.setToolTipText(null);
            connection.setForeground(ForgeBingoTheme.GOLD);
        });
    }

    void showConnected(String displayName)
    {
        onEdt(() -> {
            connection.setText("● Connected");
            connection.setToolTipText(displayName == null ? null : "Connected as " + displayName);
            connection.setForeground(ForgeBingoTheme.GREEN);
        });
    }

    void showError(String message)
    {
        onEdt(() -> {
            String shortMessage = message != null && message.toLowerCase().contains("api key")
                ? "API key needed" : shorten(message, 24);
            connection.setText("● " + shortMessage + (board == null ? "" : " · cached"));
            connection.setToolTipText(message);
            connection.setForeground(new Color(230, 120, 100));
        });
    }

    void showAutomationPaused()
    {
        onEdt(() -> {
            connection.setText("● Automation paused");
            connection.setToolTipText("NPC-loot automation is temporarily paused by ForgeBingo.");
            connection.setForeground(ForgeBingoTheme.GOLD);
        });
    }

    void setBoards(List<ForgeBingoModels.BoardSummary> boards, String selectedBoardId)
    {
        onEdt(() -> {
            updatingBoardPicker = true;
            boardPicker.removeAllItems();
            ForgeBingoModels.BoardSummary selected = null;
            for (ForgeBingoModels.BoardSummary summary : boards == null
                ? Collections.<ForgeBingoModels.BoardSummary>emptyList() : boards)
            {
                boardPicker.addItem(summary);
                if (Objects.equals(summary.teamBoardId, selectedBoardId))
                {
                    selected = summary;
                }
            }
            if (selected != null)
            {
                boardPicker.setSelectedItem(selected);
            }
            else if (boardPicker.getItemCount() > 0)
            {
                boardPicker.setSelectedIndex(0);
            }
            updatingBoardPicker = false;
        });
    }

    void setBoard(ForgeBingoModels.BoardResponse board)
    {
        onEdt(() -> {
            this.board = board;
            renderBoardView();
        });
    }

    private void renderBoardView()
    {
            List<ForgeBingoModels.Tile> tiles = board.safeTiles();
            boardTitle.setText(board.board == null || board.board.title == null ? "Bingo board" : board.board.title);
            String gameType = board.board == null || board.board.gameType == null
                ? "classic" : board.board.gameType;
            String team = board.teamName;
            if (team == null) team = "Your team";
            teamName.setText("battleship".equals(gameType)
                ? team + "  ·  YOUR TASKS"
                : team + "  ·  CLASSIC");

            int completedCount = 0;
            for (ForgeBingoModels.Tile tile : tiles)
            {
                if (tile.completed)
                {
                    completedCount++;
                }
            }
            progress.setMaximum(Math.max(1, tiles.size()));
            progress.setValue(completedCount);
            progress.setString(completedCount + " / " + tiles.size() + " complete");

            int size = board.board == null || board.board.size < 1 ? 5 : Math.min(10, board.board.size);
            int gap = tileGap(size);
            int cellHeight = tileCellHeight(size);
            int gridHeight = size * cellHeight + Math.max(0, size - 1) * gap;
            grid.removeAll();
            tileButtons.clear();
            grid.setLayout(new GridLayout(size, size, gap, gap));
            grid.setPreferredSize(new Dimension(1, gridHeight));
            grid.setMinimumSize(new Dimension(0, gridHeight));
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridHeight));
            for (ForgeBingoModels.Tile tile : tiles)
            {
                BingoTileButton button = new BingoTileButton(tile,
                    selectedTile != null && Objects.equals(tile.id, selectedTile.id));
                button.setMinimumSize(new Dimension(0, 0));
                button.setPreferredSize(new Dimension(1, cellHeight));
                button.addActionListener(event -> selectTile(tile));
                grid.add(button);
                if (tile.id != null)
                {
                    tileButtons.put(tile.id, button);
                }
                addTileIcon(button, tile);
            }
            int frameHeight = gridHeight + 12;
            boardFrame.setPreferredSize(new Dimension(1, frameHeight));
            boardFrame.setMaximumSize(new Dimension(Integer.MAX_VALUE, frameHeight));
            grid.revalidate();
            boardFrame.revalidate();
            boardFrame.repaint();

            ForgeBingoModels.Tile refreshedSelection = null;
            if (selectedTile != null)
            {
                for (ForgeBingoModels.Tile tile : tiles)
                {
                    if (Objects.equals(tile.id, selectedTile.id))
                    {
                        refreshedSelection = tile;
                        break;
                    }
                }
            }
            if (refreshedSelection != null)
            {
                selectedTile = refreshedSelection;
                BingoTileButton source = tileButtons.get(refreshedSelection.id);
                for (Map.Entry<String, BingoTileButton> entry : tileButtons.entrySet())
                {
                    entry.getValue().setTileSelected(Objects.equals(entry.getKey(), refreshedSelection.id));
                }
                detailOverlay.refresh(refreshedSelection, source);
            }
            else if (selectedTile != null || detailOverlay.isDetailOpen())
            {
                clearTileSelection();
            }
    }

    private void addTileIcon(BingoTileButton button, ForgeBingoModels.Tile tile)
    {
        if (artworkProvider == null)
        {
            return;
        }
        artworkProvider.load(tile, image -> SwingUtilities.invokeLater(() -> {
            if (button.getParent() == grid && (tile.id == null || tileButtons.get(tile.id) == button))
            {
                button.useTileIcon(image);
            }
        }));
    }

    void showProofFailure(String message)
    {
        onEdt(() -> {
            retryProof.setText("Retry proof: " + message);
            retryProof.setVisible(true);
        });
    }

    void clearProofFailure()
    {
        onEdt(() -> retryProof.setVisible(false));
    }

    void selectTile(ForgeBingoModels.Tile tile)
    {
        selectedTile = tile;
        for (Map.Entry<String, BingoTileButton> entry : tileButtons.entrySet())
        {
            entry.getValue().setTileSelected(Objects.equals(entry.getKey(), tile.id));
        }
        detailOverlay.open(tile, tile.id == null ? null : tileButtons.get(tile.id));
    }

    private void clearTileSelection()
    {
        detailOverlay.closeImmediately();
        onDetailClosed();
    }

    private void onDetailClosed()
    {
        selectedTile = null;
        for (BingoTileButton button : tileButtons.values())
        {
            button.setTileSelected(false);
        }
    }

    int displayedTileCount()
    {
        return tileButtons.size();
    }

    String displayedBoardTitle()
    {
        return boardTitle.getText();
    }

    String displayedTeamName()
    {
        return teamName.getText();
    }

    String displayedTileTitle()
    {
        return detailOverlay.titleText();
    }

    String displayedTaskText()
    {
        String description = detailOverlay.descriptionText();
        String tasks = detailOverlay.taskText();
        return description.isEmpty() ? tasks : description + "\n" + tasks;
    }

    int displayedTaskProgressBarCount()
    {
        return detailOverlay.taskProgressBarCount();
    }

    int widestDisplayedTaskProgressBar()
    {
        return detailOverlay.widestTaskProgressBar();
    }

    int displayedGridHeight()
    {
        return grid.getPreferredSize().height;
    }

    float displayedTitleFontSize()
    {
        return detailOverlay.titleFontSize();
    }

    float displayedDescriptionFontSize()
    {
        return detailOverlay.descriptionFontSize();
    }

    boolean detailChildrenFit()
    {
        return true;
    }

    String detailLayoutSummary()
    {
        return detailOverlay.layoutSummary();
    }

    boolean detailOverlayVisible()
    {
        return detailOverlay.isDetailOpen();
    }

    int displayedDetailCardHeight()
    {
        return detailOverlay.detailCardHeight();
    }

    boolean detailDescriptionVisible()
    {
        return detailOverlay.descriptionVisible();
    }

    void closeDetailForTest()
    {
        detailOverlay.closeImmediately();
    }

    static int tileGap(int size)
    {
        return size <= 5 ? 4 : size <= 7 ? 3 : 2;
    }

    static int tileCellHeight(int size)
    {
        if (size <= 5)
        {
            return STANDARD_TILE_CELL_HEIGHT;
        }
        int gap = tileGap(size);
        return Math.max(20, (LARGE_BOARD_TARGET_HEIGHT - Math.max(0, size - 1) * gap) / size);
    }

    private static JButton actionButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(ForgeBingoTheme.CARD);
        button.setForeground(ForgeBingoTheme.TEXT);
        button.setBorder(BorderFactory.createLineBorder(ForgeBingoTheme.BORDER));
        return button;
    }

    private static void stylePrimaryButton(JButton button)
    {
        button.setFocusPainted(false);
        button.setBackground(new Color(115, 79, 21));
        button.setForeground(new Color(255, 235, 184));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ForgeBingoTheme.GOLD_DARK),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        fillWidth(button, CONTROL_HEIGHT);
    }

    private static void onEdt(Runnable runnable)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static void fillWidth(JComponent component, int height)
    {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMinimumSize(new Dimension(0, height));
        component.setPreferredSize(new Dimension(1, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private static String shorten(String value, int maxLength)
    {
        if (value == null || value.trim().isEmpty())
        {
            return "Unavailable";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 1).trim() + "…";
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable
    {
        private WidthTrackingPanel()
        {
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }
}
