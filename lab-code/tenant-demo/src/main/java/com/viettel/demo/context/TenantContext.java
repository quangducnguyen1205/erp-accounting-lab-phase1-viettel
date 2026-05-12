package com.viettel.demo.context;

/*
 * ==============================================================
 * Tenant Context — lưu tenant_id cho request hiện tại
 * ==============================================================
 *
 * [Mục tiêu]
 * Trong multi-tenant, mỗi HTTP request phải mang theo tenant_id.
 * Class này lưu tenant_id vào ThreadLocal để các tầng
 * (service, repository) có thể truy cập, mà không cần truyền
 * tenant_id qua parameter của mỗi method.
 *
 * [Cách hoạt động hiện tại]
 * 1. Mỗi thread xử lý request có một giá trị tenant riêng.
 * 2. TenantFilter set tenant trước khi request đi vào controller.
 * 3. Service/repository sau này có thể đọc tenant hiện tại.
 * 4. TenantFilter phải gọi clear() sau request để tránh rò tenant
 *    khi thread được tái sử dụng.
 *
 * [Kiến thức đã áp dụng]
 * - java.lang.ThreadLocal<T>
 * - ThreadLocal.set(), ThreadLocal.get(), ThreadLocal.remove()
 * - Tại sao ThreadLocal phù hợp cho per-request context
 * - Thread pool reuse và memory leak risk khi không clear
 * - Đọc lại: docs/02-multi-tenant/tong-quan-multi-tenant.md
 *   (phần "Tenant-aware everything")
 *
 * ==============================================================
 */

public final class TenantContext {

    // ThreadLocal lưu tenant theo thread đang xử lý request.
    // Vì server có thể tái sử dụng thread, TenantFilter bắt buộc phải gọi clear().
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    private TenantContext() {
        // Utility class: không tạo object bằng new.
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
