package iudx.resource.server.database.redis;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.RedisCountRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class RedisServiceImpl implements RedisService {
    private final RedisAPI redisAPI;
    private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);

    public RedisServiceImpl(Vertx vertx, RedisOptions options) {
        Redis redisClient = Redis.createClient(vertx, options);
        this.redisAPI = RedisAPI.api(redisClient);
    }

    private void handleGetResult(Response result, Promise<JsonObject> promise, String key) {
        if (result == null || result.toBuffer().length() == 0) {
            LOGGER.warn("Key does not exist in Redis: {}", key);
            promise.complete(new JsonObject()); // Return an empty JSON object
        } else {
            LOGGER.info("Result from Redis for key {}: {}", key, result.getClass());
            promise.complete(new JsonObject().put("array", result.toBuffer().toJsonArray()));
        }
    }

    @Override
    public Future<ConsumedDataInfo> getConsumedInfo(RedisCountRequest redisCountRequest) {
        Promise<ConsumedDataInfo> promise = Promise.promise();
        List<String> keys = List.of(redisCountRequest.getApiCountKey(), redisCountRequest.getTotalSizeKey());

        redisAPI.mget(keys).onSuccess(result -> {
            try {
                // Ensure that result contains entries for all keys or defaults to null values
                long apiCount = (result != null && result.size() > 0 && result.get(0) != null)
                        ? Long.parseLong(result.get(0).toString()) : 0L;
                long consumedData = (result != null && result.size() > 1 && result.get(1) != null)
                        ? Long.parseLong(result.get(1).toString()) : 0L;

                ConsumedDataInfo consumedDataInfo = new ConsumedDataInfo();
                consumedDataInfo.setApiCount(apiCount);
                consumedDataInfo.setConsumedData(consumedData);

                promise.complete(consumedDataInfo);
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse values from Redis result: {}", e.getMessage());
                promise.fail("Error parsing data from Redis: " + e.getMessage());
            }
        }).onFailure(err -> {
            LOGGER.error("Failed to get values from Redis for keys {}: {}", keys, err.getMessage());
            promise.fail("Failed to get keys from Redis: " + err);
        });

        return promise.future();
    }


    @Override
    public Future<ConsumedDataInfo> insertJson(String key, JsonObject jsonValue) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> args = buildJsonSetArgs(key, jsonValue);

        redisAPI.jsonSet(args).onSuccess(res -> {
            LOGGER.info("Successfully inserted JSON value in Redis for key: {}", key);
            promise.complete(new JsonObject().put("status", "success"));
        }).onFailure(err -> {
            LOGGER.error("Failed to insert JSON value in Redis for key {}: {}", key, err.getMessage());
            promise.fail("Failed to set key in Redis: " + err);
        });

        return null;
    }

    private List<String> buildJsonSetArgs(String key, JsonObject jsonValue) {
        return Arrays.asList(key, ".", jsonValue.encode());
    }
}
