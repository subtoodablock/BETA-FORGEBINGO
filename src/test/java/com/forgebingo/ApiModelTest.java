package com.forgebingo;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiModelTest
{
    private final Gson gson = new Gson();

    @Test
    public void serializesLootEventContract()
    {
        ForgeBingoModels.LootEventRequest request = new ForgeBingoModels.LootEventRequest(
            "11111111-1111-4111-8111-111111111111", 42, "Goblin", Instant.EPOCH.toString(),
            Arrays.asList(new ForgeBingoModels.LootItem(995, 100)));
        String json = gson.toJson(request);
        assertTrue(json.contains("\"eventId\""));
        assertTrue(json.contains("\"itemId\":995"));
        assertFalse(json.contains("apiKey"));
    }

    @Test
    public void parsesBoardRulesAndTeamProgress()
    {
        String json = "{\"teamBoardId\":\"board\",\"board\":{\"size\":3,\"game_type\":\"battleship\",\"is_active\":true},\"tiles\":[{\"id\":\"tile\",\"completed\":false,\"backgroundColor\":\"hsl(45, 80%, 25%)\",\"automationRule\":{\"type\":\"npc_loot\",\"itemIds\":[4151,12004],\"npcIds\":[247],\"requiredQuantity\":2},\"automationProgress\":{\"currentQuantity\":1}}]}";
        ForgeBingoModels.BoardResponse board = gson.fromJson(json, ForgeBingoModels.BoardResponse.class);
        assertEquals(3, board.board.size);
        assertEquals("battleship", board.board.gameType);
        assertTrue(board.board.active);
        assertEquals(2, board.tiles.get(0).automationRule.itemIds.size());
        assertEquals(Integer.valueOf(247), board.tiles.get(0).automationRule.npcIds.get(0));
        assertEquals(1, board.tiles.get(0).currentQuantity());
        assertEquals("hsl(45, 80%, 25%)", board.tiles.get(0).backgroundColor);
    }

    @Test
    public void parsesClassicAndBattleshipBoardSummaries()
    {
        String json = "{\"boards\":["
            + "{\"teamBoardId\":\"classic\",\"gameType\":\"classic\"},"
            + "{\"teamBoardId\":\"battle\",\"gameType\":\"battleship\",\"updatedAt\":\"2026-09-16T00:00:00Z\"}]}";
        ForgeBingoModels.BoardsResponse response = gson.fromJson(json, ForgeBingoModels.BoardsResponse.class);

        assertEquals(2, response.boards.size());
        assertEquals("classic", response.boards.get(0).gameType);
        assertEquals("battleship", response.boards.get(1).gameType);
        assertEquals("2026-09-16T00:00:00Z", response.boards.get(1).updatedAt);
    }

    @Test
    public void ignoresOpponentBoardPayloads()
    {
        String json = "{\"teamBoardId\":\"mine\",\"tiles\":[{\"id\":\"mine-tile\"}],"
            + "\"opponent\":{\"teamBoardId\":\"theirs\",\"teamName\":\"Yama\","
            + "\"tiles\":[{\"id\":\"opponent-tile\",\"completed\":true}]}}";
        ForgeBingoModels.BoardResponse response = gson.fromJson(json, ForgeBingoModels.BoardResponse.class);

        assertEquals(1, response.safeTiles().size());
        assertEquals("mine-tile", response.safeTiles().get(0).id);
        assertFalse(Arrays.stream(ForgeBingoModels.BoardResponse.class.getDeclaredFields())
            .anyMatch(field -> "opponent".equals(field.getName())));
    }

    @Test
    public void parsesUnrestrictedRuleWithoutNpcIds()
    {
        String json = "{\"tiles\":[{\"automationRule\":{\"type\":\"npc_loot\",\"itemIds\":[4151],\"requiredQuantity\":1}}]}";
        ForgeBingoModels.BoardResponse board = gson.fromJson(json, ForgeBingoModels.BoardResponse.class);

        assertTrue(board.tiles.get(0).automationRule.npcIds.isEmpty());
        assertTrue(board.tiles.get(0).automationRule.allowsNpc(999));
    }

    @Test
    public void parsesSubTasksAndAutomationPauseSignal()
    {
        String boardJson = "{\"tiles\":[{\"subTasks\":[{\"id\":\"sub\",\"title\":\"Defeat the boss\",\"completed\":true}]}]}";
        ForgeBingoModels.BoardResponse board = gson.fromJson(boardJson, ForgeBingoModels.BoardResponse.class);
        assertEquals(1, board.tiles.get(0).safeSubTasks().size());
        assertTrue(board.tiles.get(0).safeSubTasks().get(0).completed);

        ForgeBingoModels.LootEventResponse response = gson.fromJson("{\"automationDisabled\":true}",
            ForgeBingoModels.LootEventResponse.class);
        assertTrue(response.automationDisabled);
    }

    @Test
    public void preservesUpdatedDescriptionsTasksAndDropRules()
    {
        String json = "{\"teamBoardId\":\"board\",\"updatedAt\":\"2026-09-17T01:02:03Z\",\"tiles\":[{"
            + "\"id\":\"tile\",\"title\":\"Updated title\",\"description\":\"Updated description\","
            + "\"iconUrl\":\"https://cdn.example.com/tile.png\",\"position\":7,"
            + "\"subTasks\":[{\"id\":\"one\",\"title\":\"First task\",\"completed\":true}],"
            + "\"automationRule\":{\"type\":\"npc_loot\",\"itemIds\":[4151,12004],"
            + "\"npcIds\":[415,416],\"requiredQuantity\":3},"
            + "\"automationProgress\":{\"currentQuantity\":2}}]}";

        ForgeBingoModels.BoardResponse board = gson.fromJson(json, ForgeBingoModels.BoardResponse.class);
        ForgeBingoModels.Tile tile = board.tiles.get(0);

        assertEquals("2026-09-17T01:02:03Z", board.updatedAt);
        assertEquals("Updated title", tile.title);
        assertEquals("Updated description", tile.description);
        assertEquals("https://cdn.example.com/tile.png", tile.iconUrl);
        assertEquals(7, tile.position);
        assertEquals(1, tile.safeSubTasks().size());
        assertEquals("First task", tile.safeSubTasks().get(0).title);
        assertTrue(tile.safeSubTasks().get(0).completed);
        assertEquals(Arrays.asList(4151, 12004), tile.automationRule.itemIds);
        assertEquals(Arrays.asList(415, 416), tile.automationRule.npcIds);
        assertEquals(3, tile.automationRule.requiredQuantity);
        assertEquals(2, tile.currentQuantity());
    }
}
