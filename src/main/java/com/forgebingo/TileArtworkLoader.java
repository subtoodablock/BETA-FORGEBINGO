package com.forgebingo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
final class TileArtworkLoader implements TileArtworkProvider
{
    private static final int MAX_ICON_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ICON_DIMENSION = 4096;
    private static final int MAX_CACHE_ENTRIES = 128;

    private final OkHttpClient httpClient;
    private final ItemManager itemManager;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<String, BufferedImage>(16, .75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
        {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private final Map<String, List<Consumer<BufferedImage>>> pending = new LinkedHashMap<>();

    @Inject
    TileArtworkLoader(OkHttpClient httpClient, ItemManager itemManager)
    {
        this.httpClient = httpClient;
        this.itemManager = itemManager;
    }

    @Override
    public void load(ForgeBingoModels.Tile tile, Consumer<BufferedImage> callback)
    {
        String websiteIcon = websiteIconUrl(tile);
        if (websiteIcon == null)
        {
            loadItemFallback(tile, callback);
            return;
        }

        loadWebsiteIcon(websiteIcon, image -> {
            if (image != null)
            {
                callback.accept(image);
            }
            else
            {
                loadItemFallback(tile, callback);
            }
        });
    }

    static String websiteIconUrl(ForgeBingoModels.Tile tile)
    {
        if (tile == null || tile.iconUrl == null)
        {
            return null;
        }
        String value = tile.iconUrl.trim();
        if (value.isEmpty() || value.length() > 2048)
        {
            return null;
        }
        HttpUrl url = HttpUrl.parse(value);
        return url != null && "https".equalsIgnoreCase(url.scheme()) ? url.toString() : null;
    }

    static Integer fallbackItemId(ForgeBingoModels.Tile tile)
    {
        if (tile == null || tile.automationRule == null || !tile.automationRule.isNpcLoot()
            || tile.automationRule.itemIds == null || tile.automationRule.itemIds.isEmpty())
        {
            return null;
        }
        Integer itemId = tile.automationRule.itemIds.get(0);
        return itemId != null && itemId > 0 ? itemId : null;
    }

    void cancelAll()
    {
        synchronized (this)
        {
            pending.clear();
        }
        for (Call call : httpClient.dispatcher().queuedCalls())
        {
            if (call.request().tag() == TileArtworkLoader.class) call.cancel();
        }
        for (Call call : httpClient.dispatcher().runningCalls())
        {
            if (call.request().tag() == TileArtworkLoader.class) call.cancel();
        }
    }

    private void loadWebsiteIcon(String url, Consumer<BufferedImage> callback)
    {
        BufferedImage cached;
        synchronized (this)
        {
            cached = cache.get(url);
            if (cached == null)
            {
                List<Consumer<BufferedImage>> callbacks = pending.get(url);
                if (callbacks != null)
                {
                    callbacks.add(callback);
                    return;
                }
                callbacks = new ArrayList<>();
                callbacks.add(callback);
                pending.put(url, callbacks);
            }
        }
        if (cached != null)
        {
            callback.accept(cached);
            return;
        }

        Request request = new Request.Builder()
            .url(url)
            .header("Accept", "image/png,image/jpeg,image/gif,*/*;q=0.5")
            .tag(TileArtworkLoader.class)
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException error)
            {
                completeWebsiteIcon(url, null);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                BufferedImage image = null;
                try (Response ignored = response)
                {
                    ResponseBody body = response.body();
                    if (response.isSuccessful() && body != null
                        && "https".equalsIgnoreCase(response.request().url().scheme())
                        && body.contentLength() <= MAX_ICON_BYTES)
                    {
                        byte[] bytes = readBounded(body.byteStream());
                        if (bytes != null)
                        {
                            image = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (image != null && (image.getWidth() > MAX_ICON_DIMENSION
                                || image.getHeight() > MAX_ICON_DIMENSION))
                            {
                                image = null;
                            }
                        }
                    }
                }
                catch (IOException | RuntimeException ignored)
                {
                    image = null;
                }
                completeWebsiteIcon(url, image);
            }
        });
    }

    private void completeWebsiteIcon(String url, BufferedImage image)
    {
        List<Consumer<BufferedImage>> callbacks;
        synchronized (this)
        {
            callbacks = pending.remove(url);
            if (image != null)
            {
                cache.put(url, image);
            }
        }
        if (callbacks == null)
        {
            return;
        }
        for (Consumer<BufferedImage> callback : callbacks)
        {
            callback.accept(image);
        }
    }

    private void loadItemFallback(ForgeBingoModels.Tile tile, Consumer<BufferedImage> callback)
    {
        Integer itemId = fallbackItemId(tile);
        if (itemId == null || itemManager == null)
        {
            return;
        }
        AsyncBufferedImage image = itemManager.getImage(itemId);
        AtomicBoolean delivered = new AtomicBoolean();
        Runnable deliver = () -> {
            if (image.getWidth() > 0 && image.getHeight() > 0 && delivered.compareAndSet(false, true))
            {
                callback.accept(image);
            }
        };
        deliver.run();
        image.onLoaded(deliver);
    }

    private static byte[] readBounded(InputStream input) throws IOException
    {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = source.read(buffer)) != -1)
            {
                total += read;
                if (total > MAX_ICON_BYTES)
                {
                    return null;
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
