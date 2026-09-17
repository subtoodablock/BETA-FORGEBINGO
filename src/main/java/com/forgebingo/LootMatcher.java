package com.forgebingo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.game.ItemStack;

final class LootMatcher
{
    private LootMatcher()
    {
    }

    static List<ForgeBingoModels.LootItem> matchedItems(
        ForgeBingoModels.BoardResponse board,
        int npcId,
        Iterable<ItemStack> loot)
    {
        Set<Integer> acceptedIds = new HashSet<>();
        if (board != null)
        {
            for (ForgeBingoModels.Tile tile : board.safeTiles())
            {
                if (!tile.completed && tile.automationRule != null
                    && tile.automationRule.isNpcLoot() && tile.automationRule.allowsNpc(npcId))
                {
                    acceptedIds.addAll(tile.automationRule.itemIds);
                }
            }
        }

        List<ForgeBingoModels.LootItem> result = new ArrayList<>();
        if (loot == null || acceptedIds.isEmpty())
        {
            return result;
        }
        for (ItemStack item : loot)
        {
            if (item != null && item.getQuantity() > 0 && acceptedIds.contains(item.getId()))
            {
                result.add(new ForgeBingoModels.LootItem(item.getId(), item.getQuantity()));
            }
        }
        return result;
    }
}
