package com.forgebingo;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
final class ForgeBingoApiClient
{
    private static final String BASE_URL = "https://lrzkmuipnfjszxatjiox.supabase.co/functions/v1/plugin-api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType PNG = MediaType.parse("image/png");
    private final OkHttpClient httpClient;
    private final Gson gson;

    @Inject
    ForgeBingoApiClient(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    interface ApiCallback<T>
    {
        void onSuccess(T value);
        void onFailure(String message);
    }

    void getMe(String apiKey, ApiCallback<ForgeBingoModels.Me> callback)
    {
        enqueueJson("GET", endpoint("v1", "me"), apiKey, null, ForgeBingoModels.Me.class, callback);
    }

    void getBoards(String apiKey, ApiCallback<ForgeBingoModels.BoardsResponse> callback)
    {
        enqueueJson("GET", endpoint("v1", "boards"), apiKey, null, ForgeBingoModels.BoardsResponse.class, callback);
    }

    void getBoard(String apiKey, String boardId, ApiCallback<ForgeBingoModels.BoardResponse> callback)
    {
        enqueueJson("GET", endpoint("v1", "boards", boardId), apiKey, null, ForgeBingoModels.BoardResponse.class, callback);
    }

    void sendLootEvent(String apiKey, String boardId, ForgeBingoModels.LootEventRequest event,
        ApiCallback<ForgeBingoModels.LootEventResponse> callback)
    {
        enqueueJson("POST", endpoint("v1", "boards", boardId, "loot-events"), apiKey, gson.toJson(event),
            ForgeBingoModels.LootEventResponse.class, callback);
    }

    void uploadProof(String apiKey, String boardId, List<String> tileIds, BufferedImage image,
        ApiCallback<Boolean> callback)
    {
        final byte[] imageBytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            if (!ImageIO.write(image, "png", output))
            {
                callback.onFailure("Could not encode screenshot");
                return;
            }
            imageBytes = output.toByteArray();
        }
        catch (IOException ex)
        {
            callback.onFailure("Could not encode screenshot");
            return;
        }
        if (imageBytes.length > 10 * 1024 * 1024)
        {
            callback.onFailure("Screenshot exceeds the 10 MB proof limit");
            return;
        }

        RequestBody multipart = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("tileIds", String.join(",", tileIds))
            .addFormDataPart("file", "forgebingo-proof.png", RequestBody.create(PNG, imageBytes))
            .build();
        Request request = baseRequest(endpoint("v1", "boards", boardId, "proofs"), apiKey)
            .post(multipart)
            .build();
        enqueue(request, Boolean.class, callback);
    }

    void cancelAll()
    {
        for (Call call : httpClient.dispatcher().queuedCalls())
        {
            if (call.request().tag() == ForgeBingoApiClient.class) call.cancel();
        }
        for (Call call : httpClient.dispatcher().runningCalls())
        {
            if (call.request().tag() == ForgeBingoApiClient.class) call.cancel();
        }
    }

    private HttpUrl endpoint(String... parts)
    {
        HttpUrl base = HttpUrl.parse(BASE_URL);
        if (base == null) throw new IllegalStateException("Invalid ForgeBingo API URL");
        HttpUrl.Builder builder = base.newBuilder();
        for (String part : parts) builder.addPathSegment(part);
        return builder.build();
    }

    private Request.Builder baseRequest(HttpUrl url, String apiKey)
    {
        return new Request.Builder()
            .url(url)
            .header("X-API-Key", apiKey)
            .header("Accept", "application/json")
            .tag(ForgeBingoApiClient.class);
    }

    private <T> void enqueueJson(String method, HttpUrl url, String apiKey, String json, Class<T> responseType,
        ApiCallback<T> callback)
    {
        Request.Builder request = baseRequest(url, apiKey);
        if ("POST".equals(method)) request.post(RequestBody.create(JSON, json == null ? "{}" : json));
        else request.get();
        enqueue(request.build(), responseType, callback);
    }

    private <T> void enqueue(Request request, Class<T> responseType, ApiCallback<T> callback)
    {
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                if (!call.isCanceled()) callback.onFailure("ForgeBingo is unavailable");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response closeable = response)
                {
                    ResponseBody body = closeable.body();
                    String text = body == null ? "" : body.string();
                    if (!closeable.isSuccessful())
                    {
                        ForgeBingoModels.ApiError apiError = null;
                        try { apiError = gson.fromJson(text, ForgeBingoModels.ApiError.class); } catch (RuntimeException ignored) { }
                        String message = apiError != null && apiError.error != null
                            ? apiError.error
                            : "Request failed (" + closeable.code() + ")";
                        if (closeable.code() == 404)
                        {
                            message = "ForgeBingo backend update required";
                        }
                        callback.onFailure(message);
                        return;
                    }
                    if (responseType == Boolean.class)
                    {
                        callback.onSuccess(responseType.cast(Boolean.TRUE));
                    }
                    else
                    {
                        callback.onSuccess(gson.fromJson(text, responseType));
                    }
                }
                catch (RuntimeException ex)
                {
                    callback.onFailure("ForgeBingo returned an invalid response");
                }
            }
        });
    }

}
