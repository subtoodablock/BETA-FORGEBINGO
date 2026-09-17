package com.forgebingo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForgeBingoPanelTest
{
    @Test
    public void transitionsFromBoardSummaryToSelectedTileDetails() throws Exception
    {
        ForgeBingoModels.BoardResponse board = new ForgeBingoModels.BoardResponse();
        board.teamBoardId = "board";
        board.teamName = "Dragon Slayers";
        board.board = new ForgeBingoModels.BoardMetadata();
        board.board.title = "God Wars Bingo";
        board.board.size = 1;
        board.board.gameType = "battleship";

        ForgeBingoModels.Tile tile = new ForgeBingoModels.Tile();
        tile.id = "tile";
        tile.title = "Abyssal whip";
        tile.position = 0;
        tile.points = 3;
        tile.automationRule = new ForgeBingoModels.LootRule();
        tile.automationRule.type = "npc_loot";
        tile.automationRule.itemIds = Arrays.asList(4151);
        tile.automationRule.requiredQuantity = 2;
        tile.automationProgress = new ForgeBingoModels.AutomationProgress();
        tile.automationProgress.currentQuantity = 1;
        ForgeBingoModels.SubTask subTask = new ForgeBingoModels.SubTask();
        subTask.id = "sub";
        subTask.title = "Defeat the Abyssal demon";
        subTask.completed = true;
        tile.subTasks.add(subTask);
        board.tiles.add(tile);

        ForgeBingoPanel[] panel = new ForgeBingoPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new ForgeBingoPanel(null, null);
            panel[0].setBoard(board);
            panel[0].selectTile(tile);
        });

        assertEquals("God Wars Bingo", panel[0].displayedBoardTitle());
        assertTrue(panel[0].displayedTeamName().contains("YOUR TASKS"));
        assertEquals(1, panel[0].displayedTileCount());
        assertTrue(panel[0].detailOverlayVisible());
        assertFalse(panel[0].detailDescriptionVisible());
        assertEquals("Abyssal whip", panel[0].displayedTileTitle());
        assertTrue(panel[0].displayedTaskText().contains("Defeat the Abyssal demon"));
        assertTrue(panel[0].displayedTaskText().contains("✓"));
        assertEquals(1, panel[0].displayedTaskProgressBarCount());
        assertTrue(panel[0].widestDisplayedTaskProgressBar() <= 132);
        assertFalse(hasTextContaining(panel[0], "drop progress"));
        assertFalse(hasTextContaining(panel[0], "any monster"));
        assertTrue(panel[0].displayedTitleFontSize() >= 15f);
        assertTrue(panel[0].displayedDescriptionFontSize() >= 12f);
        assertTrue(allBoardTilesAreTextFree(panel[0]));
        assertFalse(hasCompletionAction(panel[0]));
        assertFalse(hasButtonText(panel[0], "Opponent"));

        SwingUtilities.invokeAndWait(panel[0]::closeDetailForTest);
        assertFalse(panel[0].detailOverlayVisible());

    }

    @Test
    public void rendersAtRuneLiteSidebarWidth() throws Exception
    {
        ForgeBingoModels.BoardResponse board = previewBoard();
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() -> {
            ForgeBingoPanel panel = new ForgeBingoPanel(null, null);
            panel.setSize(242, 900);
            panel.setBoard(board);
            panel.selectTile(board.tiles.get(7));
            layoutTree(panel);
            assertTrue(panel.detailOverlayVisible());
            assertTrue(panel.detailDescriptionVisible());
            assertEquals(2, panel.displayedTaskProgressBarCount());
            assertTrue(panel.widestDisplayedTaskProgressBar() <= 132);
            assertTrue(panel.displayedDetailCardHeight() <= 350);
            assertTrue(panel.detailLayoutSummary(), panel.detailChildrenFit());
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });
        Path output = Path.of("build", "reports", "forgebingo-panel-preview.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        assertTrue(Files.size(output) > 1_000);
    }

    @Test
    public void scalesBoardsThroughTenByTen()
    {
        assertEquals(58, ForgeBingoPanel.tileCellHeight(3));
        assertEquals(58, ForgeBingoPanel.tileCellHeight(5));
        assertTrue(ForgeBingoPanel.tileCellHeight(10) >= 20);
        int tenGridHeight = ForgeBingoPanel.tileCellHeight(10) * 10 + ForgeBingoPanel.tileGap(10) * 9;
        assertTrue(tenGridHeight <= 252);
    }

    @Test
    public void laysOutOneHundredTilesAtSidebarWidth() throws Exception
    {
        ForgeBingoModels.BoardResponse board = previewBoard(10);
        ForgeBingoPanel[] panel = new ForgeBingoPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new ForgeBingoPanel(null, null);
            panel[0].setSize(242, 900);
            panel[0].setBoard(board);
            layoutTree(panel[0]);
        });
        assertEquals(100, panel[0].displayedTileCount());
        assertTrue(panel[0].displayedGridHeight() <= 252);
    }

    private static ForgeBingoModels.BoardResponse previewBoard()
    {
        return previewBoard(5);
    }

    private static ForgeBingoModels.BoardResponse previewBoard(int size)
    {
        ForgeBingoModels.BoardResponse board = new ForgeBingoModels.BoardResponse();
        board.teamBoardId = "preview";
        board.teamName = "Dragon Slayers";
        board.board = new ForgeBingoModels.BoardMetadata();
        board.board.title = "Forge Masters Bingo";
        board.board.size = size;
        for (int index = 0; index < size * size; index++)
        {
            ForgeBingoModels.Tile tile = new ForgeBingoModels.Tile();
            tile.id = "tile-" + index;
            tile.position = index;
            tile.title = index % 3 == 0 ? "Rare boss drop" : index % 3 == 1 ? "Abyssal whip" : "Complete a quest";
            tile.description = "Obtain this objective while playing Old School RuneScape with your team.";
            tile.points = index % 4 + 1;
            tile.completed = index == 0 || index == 6 || index == 12;
            tile.verified = index == 6;
            if (index == 4 || index == 9)
            {
                tile.backgroundColor = "hsl(0, 60%, 25%)";
            }
            if (index % 2 == 0)
            {
                tile.automationRule = new ForgeBingoModels.LootRule();
                tile.automationRule.type = "npc_loot";
                tile.automationRule.itemIds = Arrays.asList(4151);
                tile.automationRule.requiredQuantity = 5;
                tile.automationProgress = new ForgeBingoModels.AutomationProgress();
                tile.automationProgress.currentQuantity = Math.min(index % 6, 5);
            }
            if (index == 7)
            {
                ForgeBingoModels.SubTask first = new ForgeBingoModels.SubTask();
                first.id = "sub-1";
                first.title = "Obtain the required gear";
                first.completed = true;
                ForgeBingoModels.SubTask second = new ForgeBingoModels.SubTask();
                second.id = "sub-2";
                second.title = "Finish the encounter";
                tile.subTasks.add(first);
                tile.subTasks.add(second);
            }
            board.tiles.add(tile);
        }
        return board;
    }

    private static void layoutTree(Container container)
    {
        container.doLayout();
        for (Component component : container.getComponents())
        {
            if (component instanceof Container)
            {
                layoutTree((Container) component);
            }
        }
    }

    private static boolean hasCompletionAction(Container container)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JButton)
            {
                String text = ((JButton) component).getText();
                if (text != null && (text.toLowerCase().contains("complete") || text.toLowerCase().contains("undo")))
                {
                    return true;
                }
            }
            if (component instanceof Container && hasCompletionAction((Container) component))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasButtonText(Container container, String expected)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JButton && expected.equals(((JButton) component).getText()))
            {
                return true;
            }
            if (component instanceof Container && hasButtonText((Container) component, expected))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTextContaining(Container container, String expected)
    {
        String needle = expected.toLowerCase();
        for (Component component : container.getComponents())
        {
            String text = null;
            if (component instanceof javax.swing.JLabel)
            {
                text = ((javax.swing.JLabel) component).getText();
            }
            else if (component instanceof javax.swing.text.JTextComponent)
            {
                text = ((javax.swing.text.JTextComponent) component).getText();
            }
            else if (component instanceof JButton)
            {
                text = ((JButton) component).getText();
            }
            if (text != null && text.toLowerCase().contains(needle))
            {
                return true;
            }
            if (component instanceof Container && hasTextContaining((Container) component, expected))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean allBoardTilesAreTextFree(Container container)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof BingoTileButton && !((BingoTileButton) component).getText().isEmpty())
            {
                return false;
            }
            if (component instanceof Container && !allBoardTilesAreTextFree((Container) component))
            {
                return false;
            }
        }
        return true;
    }

}
