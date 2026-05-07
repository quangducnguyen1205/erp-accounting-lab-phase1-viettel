-- ==============================================================
-- TASK 5: Kiểm tra data leakage trong shared-table multi-tenant
-- ==============================================================
--
-- [Mục tiêu]
--
-- Hiểu cách data leakage xảy ra khi query trong hệ thống multi-tenant
-- quên điều kiện `tenant_id`.
--
-- Trọng tâm của bài này là:
-- - correctness;
-- - tenant isolation;
-- - bảo vệ dữ liệu giữa các tenant.
--
-- Đây KHÔNG phải bài benchmark index. Query có thể nhanh hoặc chậm,
-- nhưng nếu trả dữ liệu sai tenant thì vẫn là bug nghiêm trọng.
--
-- Bối cảnh dữ liệu mẫu:
-- - Tenant 1: VIETTEL.
-- - Tenant 2: FPT.
-- - Cả hai tenant đều có code `LAPTOP-01` để chứng minh rằng code chỉ
--   unique trong phạm vi từng tenant, không unique toàn hệ thống.
--
-- ==============================================================


-- ==============================================================
-- 1. Pre-check: inspect dữ liệu mẫu
-- ==============================================================
--
-- Mục tiêu:
-- Xác nhận có ít nhất 2 tenant và xem dữ liệu `master_data` đang thuộc
-- tenant nào trước khi kiểm tra leakage.

SELECT id, code, name, is_active
FROM tenants
ORDER BY id;

SELECT
    md.tenant_id,
    t.code AS tenant_code,
    md.id,
    md.code,
    md.name,
    md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
ORDER BY md.tenant_id, md.code, md.id;

-- Tìm những `code` xuất hiện ở nhiều tenant.
-- Đây là dữ liệu tốt để chứng minh rủi ro query thiếu `tenant_id`.

SELECT
    code,
    COUNT(DISTINCT tenant_id) AS tenant_count
FROM master_data
GROUP BY code
HAVING COUNT(DISTINCT tenant_id) > 1
ORDER BY code;


-- ==============================================================
-- 2. Case 1: Query an toàn có tenant_id
-- ==============================================================
--
-- Pattern đúng trong backend multi-tenant:
-- luôn giới hạn dữ liệu theo tenant hiện tại.
--
-- Trong ví dụ này, giả sử tenant hiện tại là VIETTEL (`tenant_id = 1`).
-- Trong backend thật, giá trị này phải đến từ context đáng tin cậy
-- như JWT/header đã validate hoặc TenantContext, không phải request body.
--
-- Điều cần quan sát:
-- Query chỉ trả bản ghi `LAPTOP-01` của tenant VIETTEL.

SELECT
    md.id,
    md.tenant_id,
    t.code AS tenant_code,
    md.code,
    md.name,
    md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
WHERE md.tenant_id = 1
  AND md.code = 'LAPTOP-01'
ORDER BY md.id;


-- ==============================================================
-- 3. Case 2: Query nguy hiểm thiếu tenant_id
-- ==============================================================
--
-- Query dưới đây lọc theo `code` nhưng quên `tenant_id`.
--
-- Vì `LAPTOP-01` tồn tại ở nhiều tenant, query này có thể trả dữ liệu
-- của cả VIETTEL và FPT. Đây là data leakage.
--
-- Lưu ý:
-- Vấn đề chính ở đây không phải PostgreSQL chọn plan nào.
-- Vấn đề chính là query không còn giới hạn theo tenant hiện tại.

SELECT
    md.id,
    md.tenant_id,
    t.code AS tenant_code,
    md.code,
    md.name,
    md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
WHERE md.code = 'LAPTOP-01'
ORDER BY md.tenant_id, md.id;


-- ==============================================================
-- 4. Case 3: Mô phỏng bug ở backend/service
-- ==============================================================
--
-- Tình huống giả lập:
-- - Request hiện tại thuộc tenant VIETTEL (`tenant_id = 1`).
-- - API nhận `code = 'LAPTOP-01'`, ví dụ:
--   GET /master-data/by-code/LAPTOP-01
-- - Service/repository lại chỉ query theo `code`, quên tenant context.
--
-- Query bug:

SELECT
    md.id,
    md.tenant_id,
    t.code AS tenant_code,
    md.code,
    md.name,
    md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
WHERE md.code = 'LAPTOP-01'
ORDER BY md.tenant_id, md.id;

-- Vì sao nguy hiểm?
-- API của tenant VIETTEL có thể trả cả dữ liệu thuộc tenant FPT.
-- Trong hệ thống ERP/kế toán, lỗi tương tự có thể làm lộ danh mục vật tư,
-- khách hàng, chứng từ, hóa đơn hoặc số liệu nội bộ của doanh nghiệp khác.
--
-- Đây là lỗi tenant isolation ở tầng backend, không phải chỉ là lỗi SQL nhỏ.


-- ==============================================================
-- 5. Case 4: Sửa query bằng tenant_id
-- ==============================================================
--
-- Cách sửa:
-- Repository/service phải thêm điều kiện `tenant_id` lấy từ trusted context.
--
-- Điều cần quan sát:
-- - Nếu request thuộc tenant VIETTEL, query chỉ trả dữ liệu của VIETTEL.
-- - Dữ liệu FPT có cùng `code` không được trả ra.
--
-- Pattern tương tự cũng áp dụng khi query theo `id`:
-- WHERE tenant_id = <tenant hiện tại> AND id = <id từ path/request>

SELECT
    md.id,
    md.tenant_id,
    t.code AS tenant_code,
    md.code,
    md.name,
    md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
WHERE md.tenant_id = 1
  AND md.code = 'LAPTOP-01'
ORDER BY md.id;


-- ==============================================================
-- 6. Reflection: kết luận sau khi chạy lab
-- ==============================================================
--
-- 1. Unsafe query của tôi là gì?
-- Trả lời:
-- Query unsafe là query chỉ lọc theo `code = 'LAPTOP-01'` nhưng thiếu
-- điều kiện `tenant_id = <tenant hiện tại>`.
--
-- 2. Dữ liệu nào có thể bị leak?
-- Trả lời:
-- Dữ liệu `master_data` của tenant khác có cùng code hoặc cùng điều kiện
-- nghiệp vụ có thể bị trả nhầm. Trong ví dụ này, request của VIETTEL có thể
-- nhìn thấy dòng `LAPTOP-01` thuộc FPT.
--
-- 3. Vì sao đây là correctness issue trước khi là performance issue?
-- Trả lời:
-- Dù query chạy nhanh, kết quả vẫn sai nếu trả dữ liệu của tenant khác.
-- Multi-tenant backend phải đúng về isolation trước, rồi mới bàn tối ưu.
--
-- 4. Backend rule tôi cần nhớ là gì?
-- Trả lời:
-- Mọi query đọc/ghi dữ liệu thuộc tenant phải enforce `tenant_id` từ trusted
-- backend context, ví dụ TenantContext/JWT/header đã validate.
--
-- 5. Vì sao tenant_id phải được enforce nhất quán ở repository/service query?
-- Trả lời:
-- Chỉ cần một endpoint quên `tenant_id` là có thể tạo data leakage.
-- Không nên dựa vào UI hoặc request body để tự bảo vệ tenant isolation.


-- ==============================================================
-- 7. Optional: dùng EXPLAIN ANALYZE để quan sát plan
-- ==============================================================
--
-- Phần này là tùy chọn.
--
-- Có thể dùng `EXPLAIN ANALYZE` hoặc `EXPLAIN (ANALYZE, BUFFERS)`
-- để xem PostgreSQL chạy query safe/unsafe như thế nào.
--
-- Tuy nhiên, mục tiêu chính của file này không phải index benchmarking.
-- Mục tiêu chính là chứng minh:
-- - query thiếu `tenant_id` có thể lộ dữ liệu tenant khác;
-- - query đúng phải giới hạn dữ liệu theo tenant hiện tại.
--
-- Nếu muốn ghi số liệu local, hãy copy từ output EXPLAIN trên máy của bạn.

-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT *
-- FROM master_data
-- WHERE code = 'LAPTOP-01';

-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT *
-- FROM master_data
-- WHERE tenant_id = 1
--   AND code = 'LAPTOP-01';
