package com.forgebingo;

import java.util.Objects;

final class BoardRefreshPolicy
{
    static final long CHANGE_POLL_SECONDS = 5L;
    static final long FULL_REFRESH_MILLIS = 60_000L;

    private BoardRefreshPolicy()
    {
    }

    static boolean needsFullRefresh(ForgeBingoModels.BoardSummary summary,
        ForgeBingoModels.BoardResponse current, long lastFullRefreshMillis, long nowMillis)
    {
        if (summary == null || current == null || current.board == null)
        {
            return true;
        }
        if (!Objects.equals(summary.teamBoardId, current.teamBoardId)
            || !Objects.equals(summary.updatedAt, current.updatedAt)
            || !Objects.equals(summary.teamName, current.teamName)
            || !Objects.equals(summary.boardTitle, current.board.title)
            || !Objects.equals(normalizeGameType(summary.gameType), normalizeGameType(current.board.gameType))
            || summary.boardSize != current.board.size
            || summary.active != current.board.active
            || summary.totalTiles != current.safeTiles().size()
            || summary.completedCount != completedCount(current))
        {
            return true;
        }
        return lastFullRefreshMillis <= 0 || nowMillis - lastFullRefreshMillis >= FULL_REFRESH_MILLIS;
    }

    private static int completedCount(ForgeBingoModels.BoardResponse board)
    {
        int completed = 0;
        for (ForgeBingoModels.Tile tile : board.safeTiles())
        {
            if (tile != null && tile.completed)
            {
                completed++;
            }
        }
        return completed;
    }

    private static String normalizeGameType(String value)
    {
        return value == null || value.isEmpty() ? "classic" : value;
    }
}
