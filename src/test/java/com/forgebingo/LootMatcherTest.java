package com.forgebingo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.game.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LootMatcherTest
{
    @Test
    public void sendsOnlyAcceptedNpcLoot()
    {
        ForgeBingoModels.BoardResponse board = boardWithRule(false, 100, 101);
        List<ForgeBingoModels.LootItem> matched = LootMatcher.matchedItems(board, 42,
            Arrays.asList(new ItemStack(100, 3), new ItemStack(999, 50), new ItemStack(101, 2)));

        assertEquals(2, matched.size());
        assertEquals(100, matched.get(0).itemId);
        assertEquals(3, matched.get(0).quantity);
        assertEquals(101, matched.get(1).itemId);
    }

    @Test
    public void ignoresCompletedAndManualTiles()
    {
        ForgeBingoModels.BoardResponse board = boardWithRule(true, 100);
        ForgeBingoModels.Tile manual = new ForgeBingoModels.Tile();
        manual.id = "manual";
        board.tiles.add(manual);

        assertTrue(LootMatcher.matchedItems(board, 42,
            Collections.singletonList(new ItemStack(100, 1))).isEmpty());
    }

    @Test
    public void unrestrictedRuleAcceptsTheItemFromAnyNpc()
    {
        ForgeBingoModels.BoardResponse board = boardWithRule(false, 100);

        assertEquals(1, LootMatcher.matchedItems(board, 7,
            Collections.singletonList(new ItemStack(100, 1))).size());
        assertEquals(1, LootMatcher.matchedItems(board, 9999,
            Collections.singletonList(new ItemStack(100, 1))).size());
    }

    @Test
    public void restrictedRuleRequiresAnAllowedNpc()
    {
        ForgeBingoModels.BoardResponse board = boardWithRule(false, 100);
        board.tiles.get(0).automationRule.npcIds = Arrays.asList(247, 248);

        assertEquals(1, LootMatcher.matchedItems(board, 247,
            Collections.singletonList(new ItemStack(100, 1))).size());
        assertTrue(LootMatcher.matchedItems(board, 999,
            Collections.singletonList(new ItemStack(100, 1))).isEmpty());
    }

    @Test
    public void unrestrictedTileStillMatchesOnMixedBoard()
    {
        ForgeBingoModels.BoardResponse board = boardWithRule(false, 100);
        ForgeBingoModels.BoardResponse restricted = boardWithRule(false, 101);
        restricted.tiles.get(0).automationRule.npcIds = Collections.singletonList(247);
        board.tiles.add(restricted.tiles.get(0));

        List<ForgeBingoModels.LootItem> matched = LootMatcher.matchedItems(board, 999,
            Arrays.asList(new ItemStack(100, 2), new ItemStack(101, 3)));

        assertEquals(1, matched.size());
        assertEquals(100, matched.get(0).itemId);
    }

    private static ForgeBingoModels.BoardResponse boardWithRule(boolean completed, Integer... itemIds)
    {
        ForgeBingoModels.LootRule rule = new ForgeBingoModels.LootRule();
        rule.type = "npc_loot";
        rule.itemIds = Arrays.asList(itemIds);
        rule.requiredQuantity = 5;
        ForgeBingoModels.Tile tile = new ForgeBingoModels.Tile();
        tile.id = "tile";
        tile.completed = completed;
        tile.automationRule = rule;
        ForgeBingoModels.BoardResponse board = new ForgeBingoModels.BoardResponse();
        board.tiles.add(tile);
        return board;
    }
}
