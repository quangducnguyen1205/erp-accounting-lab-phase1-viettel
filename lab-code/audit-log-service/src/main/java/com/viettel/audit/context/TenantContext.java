package com.viettel.audit.context;

/*
 * Request-scoped tenant holder for audit-log-service.
 *
 * Service nay khong dung chung class voi tenant-demo de giu boundary ro:
 * moi service tu validate token va tu tao tenant context cua minh.
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
