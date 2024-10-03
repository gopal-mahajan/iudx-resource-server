package iudx.resource.server.dataLimitService.model;

import iudx.resource.server.dataLimitService.util.UniqueKeyUtil;

public class RedisCountRequest {

    private String userid;
    private String resourceId;
    private String apiCountKey;
    private String totalSizeKey;


    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setApiCountKey() {
        this.apiCountKey = UniqueKeyUtil.generateUniqueKey(userid, resourceId, "apiCount");
    }

    public String getApiCountKey() {
        return apiCountKey;
    }

    public String getTotalSizeKey() {
        return totalSizeKey;
    }

    public void setTotalSizeKey() {
        this.totalSizeKey = UniqueKeyUtil.generateUniqueKey(userid, resourceId, "totalSize");
    }
}
