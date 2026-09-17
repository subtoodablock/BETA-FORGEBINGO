package com.forgebingo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ProofUploadPolicy
{
    static final long CAPTURE_DELAY_MILLIS = 1_000L;

    private ProofUploadPolicy()
    {
    }

    static boolean shouldCapture(boolean uploadEnabled, List<ForgeBingoModels.LootItem> matchedItems)
    {
        return uploadEnabled && matchedItems != null && !matchedItems.isEmpty();
    }

    static List<String> matchedTileIds(ForgeBingoModels.LootEventResponse response)
    {
        if (response == null)
        {
            return Collections.emptyList();
        }
        Set<String> tileIds = new LinkedHashSet<>();
        for (ForgeBingoModels.LootMatch match : response.safeMatches())
        {
            if (match != null && match.tileId != null && !match.tileId.isEmpty())
            {
                tileIds.add(match.tileId);
            }
        }
        return new ArrayList<>(tileIds);
    }
}
