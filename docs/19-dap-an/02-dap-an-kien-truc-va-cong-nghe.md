# ✅ Đáp án mẫu — Kiến trúc, Auth & Công nghệ

> ⚠️ **Đọc file này CHỈ SAU KHI đã tự trả lời.**

---

## Câu 1 (Service Decomposition): Modular monolith vs Microservices cho team 2-3 người

**Trả lời ngắn:** Modular monolith phù hợp hơn.

**Lập luận:**
- 2-3 developer không đủ để quản lý nhiều service riêng biệt (deploy, monitor, debug distributed)
- Modular monolith: 1 repo, 1 deployment, nhưng tách module rõ ràng → dễ code, dễ debug, dễ deploy
- Microservices: cần CI/CD per service, service discovery, distributed tracing, API gateway → overhead quá lớn cho team nhỏ
- Conway's Law: team nhỏ → kiến trúc nên compact

**Alternatives:**
- Micro-monolith: monolith nhưng tổ chức theo bounded context, có thể tách sau
- Serverless: nếu cloud-native, nhưng không phù hợp ở đây

**Kết luận Phase 1:** Modular monolith. Tách module bằng package structure, không tách deployment.

---

## Câu 4 (Conway's Law): 1 team + 10 microservices

**Trả lời ngắn:** Không hợp lý. Vi phạm Conway's Law.

**Lập luận:**
- Conway's Law: "Organizations design systems that mirror their communication structures"
- 1 team quản lý 10 services = mỗi người phải context-switch liên tục
- Không ai "own" service nào → thiếu accountability
- Merge conflict giảm nhưng integration conflict tăng
- Deployment pipeline × 10 = ops burden × 10

**Khi nào 10 services hợp lý:** Khi có 5-10 teams, mỗi team own 1-2 services.

**Kết luận Phase 1:** Không tách quá nhiều. 1 modular monolith hoặc 2-3 services max.

---

## Câu 8 (Auth): JWT stateless vs Session-based cho SaaS

**Trả lời ngắn:** JWT phù hợp hơn cho SaaS multi-service, nhưng cần bổ sung cơ chế revocation.

**Lập luận JWT phù hợp:**
- Stateless → dễ scale horizontal (nhiều backend instances)
- Self-contained → service tự verify, không cần call auth service mỗi request
- Phù hợp microservices → mỗi service tự validate token

**Vấn đề revoke (sa thải nhân viên):**
- JWT thuần không thể revoke trước khi hết hạn
- Giải pháp 1: JWT TTL ngắn (15 phút) + Refresh token
- Giải pháp 2: Token blacklist trong Redis (mất stateless nhưng đơn giản)
- Giải pháp 3: Check user.isActive trong DB mỗi request (performance trade-off)

**Kết luận Phase 1:** JWT + TTL ngắn. Nếu cần revoke: thêm Redis blacklist.

---

## Câu 9 (Auth): RBAC và cross-tenant access

**Trả lời ngắn:** Check role KHÔNG đủ. Phải check cả tenant ownership.

**Lập luận:**
- Admin của Tenant A có quyền DELETE, nhưng chỉ trong scope Tenant A
- Nếu chỉ check `hasPermission('DELETE')` → Admin A xóa được record Tenant B
- Phải thêm: `record.tenantId == currentUser.tenantId`

**Implementation:**
```java
public void delete(Long id) {
    Long tenantId = TenantContext.getCurrentTenant();
    MasterData record = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Not found"));
    
    // CHECK: record thuộc tenant hiện tại không?
    if (!record.getTenantId().equals(tenantId)) {
        throw new ForbiddenException("Access denied");
    }
    
    repository.delete(record);
}
```

**Hoặc tốt hơn:** Query luôn bao gồm tenant_id:
```java
repository.findByIdAndTenantId(id, tenantId)
    .orElseThrow(() -> new NotFoundException("Not found"));
// Nếu không tìm thấy → record không tồn tại TRONG tenant này
```

---

## Câu 4 (PostgreSQL): UNIQUE(code) vs UNIQUE(tenant_id, code)

**Trả lời ngắn:** `UNIQUE(code)` sai vì ngăn hai tenant có cùng code.

**Ví dụ:**
- Tenant A tạo master-data code = "VT001" → OK
- Tenant B tạo master-data code = "VT001" → FAIL (duplicate) ← SAI!
- VT001 ở Tenant A và VT001 ở Tenant B là 2 record hoàn toàn khác nhau

**Đúng:** `UNIQUE(tenant_id, code)` — code chỉ unique TRONG tenant.

---

## Câu 5 (PostgreSQL): Soft delete vs Hard delete cho kế toán

**Trả lời ngắn:** Soft delete gần như bắt buộc trong hệ thống kế toán.

**Lập luận:**
- Dữ liệu kế toán có ý nghĩa pháp lý → không được xóa vĩnh viễn
- Audit trail: cần biết ai xóa gì, lúc nào
- Recovery: nếu xóa nhầm, có thể khôi phục
- Reporting: dữ liệu cũ vẫn cần cho báo cáo

**Implementation:**
```sql
-- Soft delete
is_active BOOLEAN DEFAULT TRUE
-- hoặc
deleted_at TIMESTAMP NULL  -- NULL = active, có giá trị = đã xóa
deleted_by BIGINT NULL
```

---

## Câu 12 (Kafka): Sync vs Async — 3 ví dụ mỗi loại

**Sync (REST) phù hợp:**
1. Login → cần response ngay (token)
2. GET master-data → cần data ngay
3. Validate tenant exists → cần kết quả ngay để tiếp tục

**Async (Kafka) phù hợp:**
1. Tạo bút toán sau khi mua hàng → accounting xử lý nền
2. Gửi email notification → không cần chờ
3. Sync data sang search index → indexing nặng, không block business

---

## Câu 20 (Clean Code): Controller chứa business logic

**Ví dụ vi phạm:**
```java
// ❌ SAI — Controller chứa business logic
@PostMapping
public ResponseEntity create(@RequestBody MasterDataDTO dto) {
    // Business logic TRONG controller
    if (repository.existsByCode(dto.getCode())) {
        return ResponseEntity.status(409).body("Duplicate");
    }
    MasterData entity = new MasterData();
    entity.setCode(dto.getCode());
    entity.setTenantId(getTenantId());
    repository.save(entity);
    return ResponseEntity.ok(entity); // Trả entity trực tiếp!
}
```

**Cách sửa:**
```java
// ✅ ĐÚNG — Controller delegate cho Service
@PostMapping
public ResponseEntity<ApiResponse<MasterDataDTO>> create(
        @Valid @RequestBody CreateMasterDataRequest request) {
    MasterDataDTO result = masterDataService.create(request);
    return ResponseEntity.status(201).body(ApiResponse.success(result));
}
```

**Lý do:** Controller chỉ nên nhận request + trả response. Business logic ở Service để: dễ test, dễ reuse, dễ maintain.
