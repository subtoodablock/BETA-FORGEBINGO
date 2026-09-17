package com.forgebingo;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ForgeBingoThemeTest
{
    @Test
    public void parsesWebsiteHexTileColors()
    {
        assertEquals(new Color(18, 52, 86), ForgeBingoTheme.parseTileColor("#123456"));
    }

    @Test
    public void parsesWebsiteHslTileColors()
    {
        Color gold = ForgeBingoTheme.parseTileColor("hsl(45, 80%, 25%)");
        assertEquals(new Color(115, 89, 13), gold);
    }

    @Test
    public void rejectsMalformedTileColors()
    {
        assertNull(ForgeBingoTheme.parseTileColor("url(https://example.invalid/image.png)"));
    }
}
