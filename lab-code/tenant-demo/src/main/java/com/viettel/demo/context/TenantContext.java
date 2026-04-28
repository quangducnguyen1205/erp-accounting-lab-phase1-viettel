package com.viettel.demo.context;

/*
 * ==============================================================
 * TODO TASK: Tenant Context — lưu tenant_id cho request hiện tại
 * ==============================================================
 *
 * [Mục tiêu]
 * Trong multi-tenant, mỗi HTTP request phải mang theo tenant_id.
 * Class này lưu tenant_id vào một nơi mà TẤT CẢ các tầng
 * (service, repository) có thể truy cập, mà không cần truyền
 * tenant_id qua parameter của mỗi method.
 *
 * [Nhiệm vụ của tôi]
 * 1. Tạo một biến static có thể lưu giá trị PER-THREAD
 *    (mỗi request có thread riêng → mỗi request có tenant_id riêng).
 * 2. Viết method: setCurrentTenant(Long tenantId)
 * 3. Viết method: getCurrentTenant() → Long
 * 4. Viết method: clear() — dọn dẹp sau khi request xong.
 *    Suy nghĩ: nếu không clear, chuyện gì xảy ra khi thread
 *    được reuse cho request khác?
 *
 * [Kiến thức cần tự research]
 * - java.lang.ThreadLocal<T>
 * - ThreadLocal.set(), ThreadLocal.get(), ThreadLocal.remove()
 * - Tại sao ThreadLocal phù hợp cho per-request context
 * - Thread pool reuse và memory leak risk khi không clear
 * - Đọc lại: docs/02-multi-tenant/tong-quan-multi-tenant.md
 *   (phần "Tenant-aware everything")
 *
 * ==============================================================
 */

public class TenantContext {

    // TODO: Khai báo ThreadLocal

    // TODO: setCurrentTenant(Long tenantId)

    // TODO: getCurrentTenant() → Long

    // TODO: clear()

}
