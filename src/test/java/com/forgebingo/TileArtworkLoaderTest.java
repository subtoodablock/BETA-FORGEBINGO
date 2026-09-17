package com.forgebingo;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TileArtworkLoaderTest
{
    @Test
    public void prefersTheWebsiteTileIconOverTheAutomationItemSprite()
    {
        ForgeBingoModels.Tile tile = automatedTile();
        tile.iconUrl = "https://cdn.example.com/custom-tile-icon.png";

        assertEquals("https://cdn.example.com/custom-tile-icon.png", TileArtworkLoader.websiteIconUrl(tile));
        assertEquals(Integer.valueOf(4151), TileArtworkLoader.fallbackItemId(tile));
    }

    @Test
    public void rejectsInsecureWebsiteIconsAndRetainsTheItemFallback()
    {
        ForgeBingoModels.Tile tile = automatedTile();
        tile.iconUrl = "http://cdn.example.com/custom-tile-icon.png";

        assertNull(TileArtworkLoader.websiteIconUrl(tile));
        assertEquals(Integer.valueOf(4151), TileArtworkLoader.fallbackItemId(tile));
    }

    @Test
    public void supportsWebsiteIconsOnManualTiles()
    {
        ForgeBingoModels.Tile tile = new ForgeBingoModels.Tile();
        tile.iconUrl = "https://cdn.example.com/manual-tile.png";

        assertEquals("https://cdn.example.com/manual-tile.png", TileArtworkLoader.websiteIconUrl(tile));
        assertNull(TileArtworkLoader.fallbackItemId(tile));
    }

    private static ForgeBingoModels.Tile automatedTile()
    {
        ForgeBingoModels.Tile tile = new ForgeBingoModels.Tile();
        tile.automationRule = new ForgeBingoModels.LootRule();
        tile.automationRule.type = "npc_loot";
        tile.automationRule.itemIds = Arrays.asList(4151);
        tile.automationRule.requiredQuantity = 1;
        return tile;
    }
}
