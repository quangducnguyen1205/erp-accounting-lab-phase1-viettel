package com.viettel.common.security;

/*
 * Request-scoped tenant holder shared by backend resource servers.
 *
 * This is infrastructure plumbing only: services still validate JWTs,
 * enforce route rules, and query their own tenant-scoped data.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(Long tenantId) {
        if (tenantId == null) {
            clear();
            return;
        }
        currentTenant.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
