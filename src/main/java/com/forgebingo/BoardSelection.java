package com.forgebingo;

import java.util.Collections;
import java.util.List;

final class BoardSelection
{
    private BoardSelection()
    {
    }

    static String select(List<ForgeBingoModels.BoardSummary> boards, String configuredBoardId)
    {
        String configured = configuredBoardId == null ? "" : configuredBoardId;
        List<ForgeBingoModels.BoardSummary> safeBoards = boards == null ? Collections.emptyList() : boards;
        for (ForgeBingoModels.BoardSummary board : safeBoards)
        {
            if (board != null && configured.equals(board.teamBoardId))
            {
                return configured;
            }
        }
        for (ForgeBingoModels.BoardSummary board : safeBoards)
        {
            if (board != null && board.teamBoardId != null && !board.teamBoardId.isEmpty())
            {
                return board.teamBoardId;
            }
        }
        return "";
    }
}
