package com.forgebingo;

import com.google.inject.Provides;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "ForgeBingo",
    description = "Read-only ForgeBingo boards with server-validated NPC-loot progress",
    tags = {"bingo", "clan", "team", "loot", "external", "integration"}
)
public class ForgeBingoPlugin extends Plugin
{
    private static final long MAX_RETRY_SECONDS = 300L;

    @Inject private ForgeBingoConfig config;
    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private DrawManager drawManager;
    @Inject private TileArtworkLoader tileArtworkLoader;
    @Inject private ScheduledExecutorService executor;
    @Inject private ForgeBingoApiClient api;

    private final EventDeduplicator eventDeduplicator = new EventDeduplicator(Clock.systemUTC());
    private volatile ForgeBingoModels.BoardResponse activeBoard;
    private volatile boolean stopped;
    private volatile int consecutiveFailures;
    private volatile long lastFullBoardRefreshMillis;
    private ForgeBingoPanel panel;
    private NavigationButton navigationButton;
    private ScheduledFuture<?> pollFuture;
    private PendingProof failedProof;

    @Provides
    ForgeBingoConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(ForgeBingoConfig.class);
    }

    @Override
    protected void startUp()
    {
        stopped = false;
        SwingUtilities.invokeLater(() -> {
            if (stopped) return;
            panel = new ForgeBingoPanel(this, tileArtworkLoader);
            BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/forgebingo-icon.png");
            navigationButton = NavigationButton.builder()
                .tooltip("ForgeBingo")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();
            clientToolbar.addNavigation(navigationButton);
            clientToolbar.openPanel(navigationButton);
            refreshConnection();
        });
    }

    @Override
    protected void shutDown()
    {
        stopped = true;
        cancelPoll();
        api.cancelAll();
        tileArtworkLoader.cancelAll();
        eventDeduplicator.clear();
        activeBoard = null;
        lastFullBoardRefreshMillis = 0L;
        failedProof = null;
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
        navigationButton = null;
        panel = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!ForgeBingoConfig.GROUP.equals(event.getGroup())) return;
        if ("apiKey".equals(event.getKey()))
        {
            activeBoard = null;
            lastFullBoardRefreshMillis = 0L;
            api.cancelAll();
            eventDeduplicator.clear();
            refreshConnection();
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        ForgeBingoModels.BoardResponse board = activeBoard;
        String apiKey = apiKey();
        if (board == null || apiKey.isEmpty() || event.getNpc() == null) return;

        List<ForgeBingoModels.LootItem> matches = LootMatcher.matchedItems(
            board, event.getNpc().getId(), event.getItems());
        if (matches.isEmpty()) return;

        String signature = EventDeduplicator.lootSignature(board.teamBoardId, event.getNpc().getId(),
            event.getNpc().getIndex(), client.getTickCount(), matches);
        if (!eventDeduplicator.firstSeen(signature)) return;

        CompletableFuture<BufferedImage> captured = ProofUploadPolicy.shouldCapture(config.uploadProofScreenshots(), matches)
            ? captureProofFrameAfterDelay()
            : CompletableFuture.completedFuture(null);
        ForgeBingoModels.LootEventRequest request = new ForgeBingoModels.LootEventRequest(
            UUID.randomUUID().toString(),
            event.getNpc().getId(),
            event.getNpc().getName(),
            Instant.now().toString(),
            matches
        );
        sendLootWithRetry(apiKey, board.teamBoardId, request, captured, 0);
    }

    void manualRefresh()
    {
        consecutiveFailures = 0;
        cancelPoll();
        refreshConnection();
    }

    void selectBoard(String boardId)
    {
        if (boardId == null || boardId.isEmpty()) return;
        configManager.setConfiguration(ForgeBingoConfig.GROUP, "activeBoardId", boardId);
        activeBoard = null;
        lastFullBoardRefreshMillis = 0L;
        consecutiveFailures = 0;
        cancelPoll();
        loadBoard(boardId);
    }

    void retryFailedProof()
    {
        PendingProof proof = failedProof;
        if (proof == null) return;
        executor.execute(() -> uploadProof(proof));
    }

    private void refreshConnection()
    {
        if (stopped || panel == null) return;
        String key = apiKey();
        if (key.isEmpty())
        {
            panel.showError("Add an API key in settings");
            cancelPoll();
            return;
        }
        panel.showConnecting();
        api.getMe(key, new ForgeBingoApiClient.ApiCallback<ForgeBingoModels.Me>()
        {
            @Override
            public void onSuccess(ForgeBingoModels.Me me)
            {
                if (stopped) return;
                panel.showConnected(me.displayName == null ? "Unknown" : me.displayName);
                loadBoards();
            }

            @Override
            public void onFailure(String message)
            {
                pollFailed(message);
            }
        });
    }

    private void loadBoards()
    {
        String key = apiKey();
        if (key.isEmpty()) return;
        api.getBoards(key, new ForgeBingoApiClient.ApiCallback<ForgeBingoModels.BoardsResponse>()
        {
            @Override
            public void onSuccess(ForgeBingoModels.BoardsResponse response)
            {
                if (stopped) return;
                List<ForgeBingoModels.BoardSummary> boards = response == null || response.boards == null
                    ? new ArrayList<>() : response.boards;
                String configuredBoardId = config.activeBoardId();
                String selected = BoardSelection.select(boards, configuredBoardId);
                if (!selected.equals(configuredBoardId))
                {
                    configManager.setConfiguration(ForgeBingoConfig.GROUP, "activeBoardId", selected);
                }
                panel.setBoards(boards, selected);
                if (!selected.isEmpty()) loadBoard(selected);
                else schedulePoll(BoardRefreshPolicy.CHANGE_POLL_SECONDS);
            }

            @Override
            public void onFailure(String message)
            {
                pollFailed(message);
            }
        });
    }

    private void loadBoard(String boardId)
    {
        String key = apiKey();
        if (stopped || key.isEmpty() || boardId == null || boardId.isEmpty()) return;
        api.getBoard(key, boardId, new ForgeBingoApiClient.ApiCallback<ForgeBingoModels.BoardResponse>()
        {
            @Override
            public void onSuccess(ForgeBingoModels.BoardResponse board)
            {
                if (stopped || !boardId.equals(config.activeBoardId())) return;
                activeBoard = board;
                lastFullBoardRefreshMillis = System.currentTimeMillis();
                consecutiveFailures = 0;
                panel.setBoard(board);
                schedulePoll(BoardRefreshPolicy.CHANGE_POLL_SECONDS);
            }

            @Override
            public void onFailure(String message)
            {
                pollFailed(message);
            }
        });
    }

    private void pollFailed(String message)
    {
        if (stopped || panel == null) return;
        consecutiveFailures++;
        panel.showError(message);
        schedulePoll(RetryPolicy.pollDelaySeconds(consecutiveFailures,
            BoardRefreshPolicy.CHANGE_POLL_SECONDS, MAX_RETRY_SECONDS));
    }

    private void pollForBoardChanges()
    {
        String key = apiKey();
        if (stopped || key.isEmpty())
        {
            refreshConnection();
            return;
        }
        api.getBoards(key, new ForgeBingoApiClient.ApiCallback<ForgeBingoModels.BoardsResponse>()
        {
            @Override
            public void onSuccess(ForgeBingoModels.BoardsResponse response)
            {
                if (stopped) return;
                List<ForgeBingoModels.BoardSummary> boards = response == null || response.boards == null
                    ? new ArrayList<>() : response.boards;
                String configuredBoardId = config.activeBoardId();
                String selected = BoardSelection.select(boards, configuredBoardId);
                if (!selected.equals(configuredBoardId))
                {
                    configManager.setConfiguration(ForgeBingoConfig.GROUP, "activeBoardId", selected);
                }
                panel.setBoards(boards, selected);
                ForgeBingoModels.BoardSummary summary = findBoard(boards, selected);
                if (!selected.isEmpty() && BoardRefreshPolicy.needsFullRefresh(summary, activeBoard,
                    lastFullBoardRefreshMillis, System.currentTimeMillis()))
                {
                    loadBoard(selected);
                }
                else
                {
                    consecutiveFailures = 0;
                    schedulePoll(BoardRefreshPolicy.CHANGE_POLL_SECONDS);
                }
            }

            @Override
            public void onFailure(String message)
            {
                pollFailed(message);
            }
        });
    }

    private static ForgeBingoModels.BoardSummary findBoard(List<ForgeBingoModels.BoardSummary> boards, String boardId)
    {
        for (ForgeBingoModels.BoardSummary board : boards)
        {
            if (board != null && boardId.equals(board.teamBoardId))
            {
                return board;
            }
        }
        return null;
    }

    private synchronized void schedulePoll(long delaySeconds)
    {
        cancelPoll();
        if (stopped) return;
        pollFuture = executor.schedule(() -> {
            String boardId = config.activeBoardId();
            if (boardId == null || boardId.isEmpty()) refreshConnection();
            else pollForBoardChanges();
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private synchronized void cancelPoll()
    {
        if (pollFuture != null) pollFuture.cancel(false);
        pollFuture = null;
    }

    private void sendLootWithRetry(String key, String boardId, ForgeBingoModels.LootEventRequest request,
        CompletableFuture<BufferedImage> captured, int attempt)
    {
        if (stopped || !boardId.equals(config.activeBoardId()) || !key.equals(apiKey())) return;
        api.sendLootEvent(key, boardId, request, new ForgeBingoApiClient.ApiCallback<ForgeBingoModels.LootEventResponse>()
        {
            @Override
            public void onSuccess(ForgeBingoModels.LootEventResponse response)
            {
                if (stopped) return;
                if (response != null && response.automationDisabled)
                {
                    if (panel != null) panel.showAutomationPaused();
                    loadBoard(boardId);
                    return;
                }
                List<String> matchedTileIds = ProofUploadPolicy.matchedTileIds(response);
                if (!matchedTileIds.isEmpty() && config.uploadProofScreenshots())
                {
                    captured.thenAccept(image -> {
                        if (image != null) uploadProof(new PendingProof(boardId, matchedTileIds, image));
                    }).exceptionally(error -> {
                        if (panel != null) panel.showError("Screenshot capture failed");
                        return null;
                    });
                }
                loadBoard(boardId);
            }

            @Override
            public void onFailure(String message)
            {
                long delay = RetryPolicy.lootDelaySeconds(attempt, MAX_RETRY_SECONDS);
                executor.schedule(() -> sendLootWithRetry(key, boardId, request, captured, attempt + 1), delay, TimeUnit.SECONDS);
                if (panel != null) panel.showError("Loot sync retrying");
            }
        });
    }

    private CompletableFuture<BufferedImage> captureProofFrameAfterDelay()
    {
        CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        executor.schedule(() -> clientThread.invoke(() -> {
            if (stopped)
            {
                future.cancel(false);
                return;
            }
            drawManager.requestNextFrameListener((Image image) -> executor.execute(() -> {
                try
                {
                    future.complete(ImageUtil.bufferedImageFromImage(image));
                }
                catch (RuntimeException ex)
                {
                    future.completeExceptionally(ex);
                }
            }));
        }), ProofUploadPolicy.CAPTURE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        return future;
    }

    private void uploadProof(PendingProof proof)
    {
        if (stopped) return;
        String key = apiKey();
        if (key.isEmpty())
        {
            failedProof = proof;
            if (panel != null) panel.showProofFailure("API key required");
            return;
        }
        api.uploadProof(key, proof.boardId, proof.tileIds, proof.image, new ForgeBingoApiClient.ApiCallback<Boolean>()
        {
            @Override
            public void onSuccess(Boolean ignored)
            {
                failedProof = null;
                if (panel != null) panel.clearProofFailure();
                loadBoard(proof.boardId);
            }

            @Override
            public void onFailure(String message)
            {
                failedProof = proof;
                if (panel != null) panel.showProofFailure(message);
            }
        });
    }

    private String apiKey()
    {
        String key = config.apiKey();
        return key == null ? "" : key.trim();
    }

    private static final class PendingProof
    {
        private final String boardId;
        private final List<String> tileIds;
        private final BufferedImage image;

        private PendingProof(String boardId, List<String> tileIds, BufferedImage image)
        {
            this.boardId = boardId;
            this.tileIds = new ArrayList<>(tileIds);
            this.image = image;
        }
    }
}
