package com.forgebingo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProofUploadPolicyTest
{
    @Test
    public void waitsOneSecondAfterTheMatchedDropBeforeCapturingProof()
    {
        assertEquals(1_000L, ProofUploadPolicy.CAPTURE_DELAY_MILLIS);
    }

    @Test
    public void screenshotCaptureIsOptIn()
    {
        List<ForgeBingoModels.LootItem> match = Collections.singletonList(new ForgeBingoModels.LootItem(4151, 1));
        assertFalse(ProofUploadPolicy.shouldCapture(false, match));
        assertTrue(ProofUploadPolicy.shouldCapture(true, match));
        assertFalse(ProofUploadPolicy.shouldCapture(true, Collections.emptyList()));
    }

    @Test
    public void uploadsForEveryServerConfirmedMatch()
    {
        ForgeBingoModels.LootMatch complete = new ForgeBingoModels.LootMatch();
        complete.tileId = "complete";
        complete.completed = true;
        ForgeBingoModels.LootMatch progressOnly = new ForgeBingoModels.LootMatch();
        progressOnly.tileId = "progress";
        progressOnly.completed = false;
        ForgeBingoModels.LootEventResponse response = new ForgeBingoModels.LootEventResponse();
        response.matchedTiles = Arrays.asList(complete, progressOnly);

        assertEquals(Arrays.asList("complete", "progress"), ProofUploadPolicy.matchedTileIds(response));
    }

    @Test
    public void ignoresInvalidAndDuplicateServerMatches()
    {
        ForgeBingoModels.LootMatch first = new ForgeBingoModels.LootMatch();
        first.tileId = "tile";
        ForgeBingoModels.LootMatch duplicate = new ForgeBingoModels.LootMatch();
        duplicate.tileId = "tile";
        ForgeBingoModels.LootMatch missing = new ForgeBingoModels.LootMatch();
        missing.tileId = "";
        ForgeBingoModels.LootEventResponse response = new ForgeBingoModels.LootEventResponse();
        response.matchedTiles = Arrays.asList(first, duplicate, null, missing);

        assertEquals(Collections.singletonList("tile"), ProofUploadPolicy.matchedTileIds(response));
        assertEquals(Collections.emptyList(), ProofUploadPolicy.matchedTileIds(null));
    }
}
