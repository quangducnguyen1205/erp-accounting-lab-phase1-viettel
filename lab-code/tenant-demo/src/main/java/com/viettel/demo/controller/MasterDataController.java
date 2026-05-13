package com.viettel.demo.controller;

/*
 * ==============================================================
 * MasterData Controller — REST API endpoints
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo REST API cho CRUD MasterData.
 * Khi request đến đây, TenantFilter đã chạy xong →
 * TenantContext đã có tenant_id → Service/Repository
 * tự filter theo tenant.
 *
 * [Cách hoạt động hiện tại]
 * 1. Tạo REST controller với base path /api/master-data.
 * 2. GET    /api/master-data       → getAll (list theo tenant)
 * 3. GET    /api/master-data/{id}  → getById (verify tenant ownership)
 * 4. GET    /api/master-data/code/{code} → find by code trong tenant hiện tại
 * 5. GET    /api/master-data/category/{category} → list theo category trong tenant hiện tại
 * 6. POST   /api/master-data       → create (auto tenant_id)
 * 7. PUT    /api/master-data/{id}  → update (verify tenant ownership)
 * 8. DELETE /api/master-data/{id}  → soft delete
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

import com.viettel.demo.entity.MasterData;
import com.viettel.demo.service.MasterDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataService service;

    public MasterDataController(MasterDataService service) {
        this.service = service;
    }

    @GetMapping
    public List<MasterData> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MasterData> getById(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<MasterData> getByCode(
            @PathVariable("code") String code
    ) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping("/category/{category}")
    public List<MasterData> getByCategory(
            @PathVariable("category") String category
    ) {
        return service.getByCategory(category);
    }

    @PostMapping
    public ResponseEntity<MasterData> create(
            @RequestBody MasterData data
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MasterData> update(
            @PathVariable("id") Long id,
            @RequestBody MasterData data
    ) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
