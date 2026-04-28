package com.viettel.demo.service;

/*
 * ==============================================================
 * TODO TASK: MasterData Service — business logic layer
 * ==============================================================
 *
 * [Mục tiêu]
 * Service layer xử lý business logic cho MasterData.
 * Tầng này gọi Repository (đã tenant-aware) và cung cấp
 * API cho Controller.
 *
 * [Nhiệm vụ của tôi]
 * 1. Inject MasterDataRepository.
 * 2. Viết method: getAll() → trả về tất cả master_data của tenant hiện tại.
 * 3. Viết method: getById(Long id) → trả về 1 record, verify thuộc tenant.
 * 4. Viết method: create(MasterData data) → tạo mới.
 *    Suy nghĩ: tenant_id được set ở đâu? Ở service hay ở entity @PrePersist?
 * 5. Viết method: update(Long id, MasterData data) → cập nhật.
 *    Suy nghĩ: cần verify ownership (id thuộc tenant hiện tại) không?
 * 6. Viết method: delete(Long id) → soft delete (set isActive = false).
 *    Suy nghĩ: tại sao không dùng hard delete trong hệ thống kế toán?
 *
 * [Kiến thức cần tự research]
 * - @Service annotation
 * - Constructor injection vs @Autowired
 * - Optional<T> và cách xử lý khi entity không tìm thấy
 * - ResponseStatusException hoặc custom exception
 * - Soft delete pattern
 *
 * ==============================================================
 */

public class MasterDataService {

    // TODO: Inject repository

    // TODO: getAll()

    // TODO: getById(Long id)

    // TODO: create(...)

    // TODO: update(Long id, ...)

    // TODO: delete(Long id) — soft delete

}
