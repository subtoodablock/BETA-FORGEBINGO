package com.forgebingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ForgeBingoConfig.GROUP)
public interface ForgeBingoConfig extends Config
{
    String GROUP = "forgebingo-companion";

    @ConfigItem(
        keyName = "apiKey",
        name = "API key",
        description = "Create a revocable key in ForgeBingo Account Settings, then paste it here.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0,
        secret = true
    )
    default String apiKey()
    {
        return "";
    }

    @ConfigItem(
        keyName = "uploadProofScreenshots",
        name = "Upload proof screenshots",
        description = "Capture the next RuneLite game frame after every matching NPC drop and upload it to each server-confirmed matching tile. In-game chat and UI may be visible.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 1
    )
    default boolean uploadProofScreenshots()
    {
        return false;
    }

    @ConfigItem(
        keyName = "activeBoardId",
        name = "Selected board",
        description = "Internal selected ForgeBingo team board.",
        hidden = true,
        position = 2
    )
    default String activeBoardId()
    {
        return "";
    }
}
