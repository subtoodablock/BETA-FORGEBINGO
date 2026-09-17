package com.forgebingo;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BoardSelectionTest
{
    @Test
    public void keepsAConfiguredBoardThatIsStillAuthorized()
    {
        assertEquals("second", BoardSelection.select(Arrays.asList(board("first"), board("second")), "second"));
    }

    @Test
    public void fallsBackToTheFirstAuthorizedBoard()
    {
        assertEquals("first", BoardSelection.select(Arrays.asList(board("first"), board("second")), "removed"));
    }

    @Test
    public void clearsSelectionWhenNoBoardsAreAuthorized()
    {
        assertEquals("", BoardSelection.select(Collections.emptyList(), "removed"));
    }

    private static ForgeBingoModels.BoardSummary board(String id)
    {
        ForgeBingoModels.BoardSummary board = new ForgeBingoModels.BoardSummary();
        board.teamBoardId = id;
        return board;
    }
}
