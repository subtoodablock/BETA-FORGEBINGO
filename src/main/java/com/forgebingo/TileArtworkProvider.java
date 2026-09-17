package com.forgebingo;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

interface TileArtworkProvider
{
    void load(ForgeBingoModels.Tile tile, Consumer<BufferedImage> callback);
}
