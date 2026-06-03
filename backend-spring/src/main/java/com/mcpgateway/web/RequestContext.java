package com.mcpgateway.web;

public class RequestContext {
    private static final ThreadLocal<Long> user = new ThreadLocal<>();
    private static final ThreadLocal<Long> org = new ThreadLocal<>();

    public static void setUserId(Long userId) { user.set(userId); }
    public static Long getUserId() { return user.get(); }
    public static void setOrgId(Long orgId) { org.set(orgId); }
    public static Long getOrgId() { return org.get(); }
    public static void clear() { user.remove(); org.remove(); }
}
