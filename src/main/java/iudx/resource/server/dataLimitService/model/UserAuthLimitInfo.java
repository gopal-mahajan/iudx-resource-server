package iudx.resource.server.dataLimitService.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.model.AuthInfo;

public class UserAuthLimitInfo {
    public AuthInfo authInfo;
    private String userId;
    private String resourceId;
    private IudxRole role;
    private JsonObject access;
    private String accessPolicy;

    public UserAuthLimitInfo(AuthInfo  authInfo){
        this.authInfo=authInfo;
        setUserAuthInfo(authInfo);
    }

    public void setUserAuthInfo(AuthInfo authInfo){
        userId=



    }


}
