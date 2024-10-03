package iudx.resource.server.dataLimitService.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.dataLimitService.service.DataAccessLimitService;
import iudx.resource.server.dataLimitService.util.ContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.common.ResponseUrn.LIMIT_EXCEED_URN;

public class DataAccessHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(DataAccessHandler.class);
    boolean isEnableLimit;
    DataAccessLimitService dataAccessLimitService;

    public DataAccessHandler(Vertx vertx, boolean isEnabledLimit) {
        this.dataAccessLimitService = new DataAccessLimitService(vertx, isEnabledLimit);
    }

    private static Response getInternalServerError() {
        return new Response.Builder().withStatus(500).withUrn(ResponseUrn.DB_ERROR_URN.getUrn()).withTitle(INTERNAL_SERVER_ERROR.getDescription()).withDetail("Internal server error").build();
    }

    private static Response limitExceed() {
        Response response = new Response.Builder().withUrn(LIMIT_EXCEED_URN.getUrn()).withStatus(429).withTitle("Too Many Requests").withDetail(LIMIT_EXCEED_URN.getMessage()).build();
        return response;
    }

    @Override
    public void handle(RoutingContext context) {
        AuthInfo authInfo = ContextHelper.getAuthInfo(context);
        LOGGER.info("isLimit Enable : {}", isEnableLimit);
        if (isValidationNotRequired(authInfo)) {
            context.next();
            return;
        }
        dataAccessLimitService.validateDataAccess(authInfo).onSuccess(validateDataAccessResult -> {
            if (validateDataAccessResult.isWithInLimit()) {
                ContextHelper.putConsumedData(context, validateDataAccessResult.getConsumedDataInfo());
                context.next();
            } else {
                Response response = limitExceed();
            }
        }).onFailure(failureHandler -> {
            LOGGER.error("Failed to route {} ", failureHandler.getMessage());
            Response response = getInternalServerError();
//              ContextHelper.putResponse(context, new JsonObject(failureHandler.getMessage()));
            ContextHelper.putResponse(context, response.toJson());
            buildResponse(context);
        });
    }

    private boolean isValidationNotRequired(AuthInfo authInfo) {
        return !isEnableLimit && authInfo.getAccessPolicy().equalsIgnoreCase("OPEN");
    }

    public void buildResponse(RoutingContext routingContext) {
        routingContext.response().setStatusCode(routingContext.get("statusCode")).end((String) routingContext.get("response"));
    }
}
