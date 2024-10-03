package iudx.resource.server.dataLimitService.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.RedisCountRequest;
import iudx.resource.server.dataLimitService.util.DataAccessLimitValidator;
import iudx.resource.server.dataLimitService.util.ValidateDataAccessResult;
import iudx.resource.server.database.redis.RedisService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.common.Constants.REDIS_SERVICE_ADDRESS;

public class DataAccessLimitService {
    private static final Logger LOGGER = LogManager.getLogger(DataAccessLimitService.class);
    private final boolean isEnableLimit;
    RedisService redisService;

    public DataAccessLimitService(Vertx vertx, boolean isEnableLimit)
    {
        this.redisService = RedisService.createProxy(vertx,REDIS_SERVICE_ADDRESS);
        this.isEnableLimit=isEnableLimit;
    }
    public Future<ValidateDataAccessResult> validateDataAccess(AuthInfo authInfo) {

        Promise<ValidateDataAccessResult> promise = Promise.promise();
        RedisCountRequest redisCountRequest = getRedisCountRequest(authInfo);

        if (isEnableLimit && authInfo.getRole().getRole().equalsIgnoreCase("consumer")) {

            Future<ConsumedDataInfo> consumedDataInfoFuture = redisService.getConsumedInfo(redisCountRequest);

            consumedDataInfoFuture
                    .onSuccess(
                            consumedData -> {
                                LOGGER.info(
                                        "consumedData: {}",
                                        consumedData.getConsumedData() + " , " + consumedData.getApiCount());
                                ValidateDataAccessResult validateDataAccessResult = new ValidateDataAccessResult();
                                if (DataAccessLimitValidator.isUsageWithinLimits(authInfo, consumedData)) {
                                    LOGGER.info("User access is allowed.");
                                    validateDataAccessResult.setConsumedDataInfo(consumedData);
                                    validateDataAccessResult.setWithInLimit(true);
                                    promise.complete(validateDataAccessResult);
                                } else {
                                    LOGGER.error("Limit Exceeded");
                                    validateDataAccessResult.setConsumedDataInfo(consumedData);
                                    validateDataAccessResult.setWithInLimit(false);
                                    promise.complete(validateDataAccessResult);
                                }
                            })
                    .onFailure(
                            failure -> {
                                LOGGER.error("failed to get redis response: ", failure);
//                                Response response = getInternalServerError();
//                                promise.fail(response.toString());
                                promise.fail(failure.getMessage());
                            });
        }
        return promise.future();
    }

    private RedisCountRequest getRedisCountRequest(AuthInfo authInfo) {

        RedisCountRequest redisCountRequest = new RedisCountRequest();
        redisCountRequest.setUserid(authInfo.getUserid());

        if (authInfo.getEndPoint().equalsIgnoreCase("/ngsi-ld/v1/async/status")) {
            redisCountRequest.setResourceId(authInfo.getResourceId());
        } else {
//      redisCountRequest.setResourceId("resourceId", authInfo.getValue(ID));
            // TODO: Need to check and verify
        }
        redisCountRequest.setApiCountKey();
        redisCountRequest.setTotalSizeKey();
        LOGGER.trace("Redis count" + redisCountRequest);
        return redisCountRequest;
    }
}
