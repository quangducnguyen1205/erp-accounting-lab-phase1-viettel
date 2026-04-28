package com.viettel.demo.controller;

/*
 * ==============================================================
 * TODO TASK: MasterData Controller — REST API endpoints
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo REST API cho CRUD MasterData.
 * Khi request đến đây, TenantFilter đã chạy xong →
 * TenantContext đã có tenant_id → Service/Repository
 * tự filter theo tenant.
 *
 * [Nhiệm vụ của tôi]
 * 1. Tạo REST controller với base path /api/master-data.
 * 2. GET    /api/master-data       → getAll (list theo tenant)
 * 3. GET    /api/master-data/{id}  → getById (verify tenant ownership)
 * 4. POST   /api/master-data       → create (auto tenant_id)
 * 5. PUT    /api/master-data/{id}  → update (verify tenant ownership)
 * 6. DELETE /api/master-data/{id}  → soft delete
 *
 * Suy nghĩ:
 * - Client có cần gửi tenant_id trong body không?
 *   Hay tenant_id lấy từ TenantContext (đã inject từ header/JWT)?
 * - Nếu client cố tình gửi tenant_id khác trong body → xử lý sao?
 *
 * [Kiến thức cần tự research]
 * - @RestController, @RequestMapping
 * - @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
 * - @PathVariable, @RequestBody
 * - ResponseEntity<T>
 * - HTTP status codes: 200, 201, 404, 403
 *
 * ==============================================================
 */

public class MasterDataController {

    // TODO: Inject service

    // TODO: GET /api/master-data

    // TODO: GET /api/master-data/{id}

    // TODO: POST /api/master-data

    // TODO: PUT /api/master-data/{id}

    // TODO: DELETE /api/master-data/{id}

}
