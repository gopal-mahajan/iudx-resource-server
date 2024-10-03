package iudx.resource.server.dataLimitService.util;

import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataAccessLimitValidator {
  private static final Logger LOGGER = LogManager.getLogger(DataAccessLimitValidator.class);

  public static boolean isUsageWithinLimits(AuthInfo authInfo, ConsumedDataInfo quotaConsumed) {
    if (!isLimitEnabled(authInfo)) {
      return true;
    }

    String accessType = authInfo.getAccessPolicy();
    long allowedLimit = authInfo.getAccess().getJsonObject(accessType).getLong("limit");
    LOGGER.debug("Access type: {}, Allowed limit: {}", accessType, allowedLimit);

    boolean isWithinLimits;

    switch (accessType.toLowerCase()) {
      case "api":
        isWithinLimits = quotaConsumed.getApiCount() <= allowedLimit;
        break;
      case "async":
        isWithinLimits = quotaConsumed.getConsumedData() <= allowedLimit;
        break;
      case "sub":
        isWithinLimits = true;
        break;
      default:
        isWithinLimits = false; // Handle unexpected accessType cases if needed
        break;
    }

    LOGGER.info("Usage {} defined limits", isWithinLimits ? "within" : "exceeds");
    return isWithinLimits;
  }

  private static boolean isLimitEnabled(AuthInfo authInfo) {
    return "CONSUMER".equalsIgnoreCase(authInfo.getRole().getRole())
        && !"OPEN".equalsIgnoreCase(authInfo.getAccessPolicy());
  }
}
