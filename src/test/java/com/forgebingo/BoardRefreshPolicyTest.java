package com.forgebingo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoardRefreshPolicyTest
{
    @Test
    public void pollsForRevisionsEveryFiveSeconds()
    {
        assertTrue(BoardRefreshPolicy.CHANGE_POLL_SECONDS == 5L);
    }

    @Test
    public void reloadsImmediatelyWhenTheTeamBoardRevisionChanges()
    {
        ForgeBingoModels.BoardResponse current = currentBoard("old");
        ForgeBingoModels.BoardSummary summary = summary("new");

        assertTrue(BoardRefreshPolicy.needsFullRefresh(summary, current, 1_000L, 2_000L));
    }

    @Test
    public void skipsTheHeavyBoardDownloadWhenNothingChanged()
    {
        ForgeBingoModels.BoardResponse current = currentBoard("same");
        ForgeBingoModels.BoardSummary summary = summary("same");

        assertFalse(BoardRefreshPolicy.needsFullRefresh(summary, current, 1_000L, 2_000L));
    }

    @Test
    public void periodicallyRefreshesEvenWhenTheRevisionIsUnchanged()
    {
        ForgeBingoModels.BoardResponse current = currentBoard("same");
        ForgeBingoModels.BoardSummary summary = summary("same");

        assertTrue(BoardRefreshPolicy.needsFullRefresh(summary, current, 1_000L,
            1_000L + BoardRefreshPolicy.FULL_REFRESH_MILLIS));
    }

    private static ForgeBingoModels.BoardSummary summary(String updatedAt)
    {
        ForgeBingoModels.BoardSummary summary = new ForgeBingoModels.BoardSummary();
        summary.teamBoardId = "board";
        summary.boardTitle = "Forge Bingo";
        summary.boardSize = 1;
        summary.gameType = "classic";
        summary.teamName = "Team";
        summary.active = true;
        summary.totalTiles = 1;
        summary.completedCount = 0;
        summary.updatedAt = updatedAt;
        return summary;
    }

    private static ForgeBingoModels.BoardResponse currentBoard(String updatedAt)
    {
        ForgeBingoModels.BoardResponse board = new ForgeBingoModels.BoardResponse();
        board.teamBoardId = "board";
        board.teamName = "Team";
        board.updatedAt = updatedAt;
        board.board = new ForgeBingoModels.BoardMetadata();
        board.board.title = "Forge Bingo";
        board.board.size = 1;
        board.board.gameType = "classic";
        board.board.active = true;
        board.tiles.add(new ForgeBingoModels.Tile());
        return board;
    }
}
