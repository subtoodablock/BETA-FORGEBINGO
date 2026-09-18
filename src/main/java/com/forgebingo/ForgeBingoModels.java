package com.forgebingo;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ForgeBingoModels
{
    private ForgeBingoModels()
    {
    }

    static final class Me
    {
        String userId;
        String displayName;
    }

    static final class BoardsResponse
    {
        List<BoardSummary> boards = new ArrayList<>();
    }

    static final class BoardSummary
    {
        String teamBoardId;
        String boardTitle;
        int boardSize;
        String gameType;
        String teamName;
        boolean active;
        int completedCount;
        int totalTiles;
        String updatedAt;

        @Override
        public String toString()
        {
            return boardTitle + " — " + teamName;
        }
    }

    static final class BoardResponse
    {
        String teamBoardId;
        String teamName;
        BoardMetadata board;
        List<Tile> tiles = new ArrayList<>();
        String updatedAt;

        List<Tile> safeTiles()
        {
            return tiles == null ? Collections.emptyList() : tiles;
        }
    }

    static final class BoardMetadata
    {
        String id;
        String title;
        String description;
        int size;
        @SerializedName("game_type")
        String gameType;
        @SerializedName("is_active")
        boolean active;
    }

    static final class Tile
    {
        String id;
        String title;
        String description;
        String iconUrl;
        Integer iconItemId;
        String backgroundColor;
        int points;
        int position;
        boolean completed;
        boolean verified;
        List<SubTask> subTasks = new ArrayList<>();
        LootRule automationRule;
        AutomationProgress automationProgress;
        List<Proof> proofMedia = new ArrayList<>();

        int currentQuantity()
        {
            return automationProgress == null ? 0 : Math.max(0, automationProgress.currentQuantity);
        }

        List<SubTask> safeSubTasks()
        {
            return subTasks == null ? Collections.emptyList() : subTasks;
        }
    }

    static final class SubTask
    {
        String id;
        String title;
        boolean completed;
    }

    static final class LootRule
    {
        String type;
        List<Integer> itemIds = new ArrayList<>();
        List<Integer> npcIds = new ArrayList<>();
        int requiredQuantity;

        boolean isNpcLoot()
        {
            return "npc_loot".equals(type) && itemIds != null && !itemIds.isEmpty() && requiredQuantity > 0;
        }

        boolean allowsNpc(int npcId)
        {
            return npcIds == null || npcIds.isEmpty() || (npcId > 0 && npcIds.contains(npcId));
        }
    }

    static final class AutomationProgress
    {
        int currentQuantity;
        String updatedAt;
    }

    static final class Proof
    {
        String id;
        String url;
        String storagePath;
        String type;
    }

    static final class LootItem
    {
        final int itemId;
        final int quantity;

        LootItem(int itemId, int quantity)
        {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    static final class LootEventRequest
    {
        final String eventId;
        final Integer npcId;
        final String npcName;
        final String occurredAt;
        final List<LootItem> items;

        LootEventRequest(String eventId, Integer npcId, String npcName, String occurredAt, List<LootItem> items)
        {
            this.eventId = eventId;
            this.npcId = npcId;
            this.npcName = npcName;
            this.occurredAt = occurredAt;
            this.items = items;
        }
    }

    static final class LootEventResponse
    {
        String eventId;
        boolean duplicate;
        boolean automationDisabled;
        List<LootMatch> matchedTiles = new ArrayList<>();

        List<LootMatch> safeMatches()
        {
            return matchedTiles == null ? Collections.emptyList() : matchedTiles;
        }
    }

    static final class LootMatch
    {
        String tileId;
        int contributedQuantity;
        int currentQuantity;
        int requiredQuantity;
        boolean completed;
    }

    static final class ApiError
    {
        String error;
    }
}
