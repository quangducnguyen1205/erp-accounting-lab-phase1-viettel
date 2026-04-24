# Phase 1 - Kho kiến thức SaaS, Multi-tenant và Backend Database

Repository này là nơi lưu trữ kiến thức đã được tổng hợp trong Phase 1 của quá trình học backend cho bài toán ERP/kế toán SaaS đa tenant.

Mục tiêu của repo không phải là lưu mọi nháp học tập, mà là làm nguồn sự thật công khai cho các phần kiến thức đã được học, rà soát và chuẩn hóa. Các câu hỏi tự học, câu trả lời nháp, review, ghi chú mentor, prompt và scratch notes được giữ ở local để không làm nhiễu lịch sử GitHub.

## Trạng thái học hiện tại

- Đã gần hoàn tất nền tảng về SaaS và kiến trúc multi-tenant.
- Đang tổng hợp lại kiến thức và đi sâu thêm vào backend/database.
- Trọng tâm mở rộng hiện tại: PostgreSQL, index, query planning, locking, migration strategy, partitioning, read replica và hành vi backend trong môi trường production.

## Cấu trúc thư mục

```text
.
├── docs/
│   ├── 00-gioi-thieu/
│   ├── 01-saas/
│   ├── 02-multi-tenant/
│   ├── 03-backend-database-mo-rong/
│   └── 99-tong-ket/
├── reports/
│   └── latex/
├── presentation-notes/
├── local/
├── .gitignore
├── LICENSE
└── README.md
```

## Nội dung được commit

- Ghi chú lý thuyết markdown đã được tổng hợp.
- Báo cáo LaTeX dạng nguồn `.tex`.
- Tài liệu thuyết trình dạng markdown có sơ đồ, bảng và ghi chú nói ngắn.
- Các README giải thích cách sử dụng repo.

## Nội dung local-only

Các nội dung sau không được commit:

- Câu hỏi tự học.
- Câu trả lời nháp.
- Review câu trả lời.
- Ghi chú mentor/reviewer.
- Prompt thô.
- Scratch notes, bản nháp tạm, file backup.
- File build từ LaTeX, bao gồm PDF mặc định.

Thư mục `local/` chỉ giữ một file README làm placeholder. Mọi nội dung thực tế trong `local/` được ignore.

## Báo cáo

Báo cáo tiến độ nằm tại:

```text
reports/latex/bao-cao-saas-multi-tenant.tex
```

Báo cáo được viết bằng LaTeX để có thể compile sang PDF khi cần:

```bash
cd reports/latex
xelatex bao-cao-saas-multi-tenant.tex
```

PDF không được commit mặc định vì là file build. Nếu sau này cần publish PDF, có thể chủ động force-add file PDF.

## Tài liệu thuyết trình

Tài liệu thuyết trình nằm tại:

```text
presentation-notes/thuyet-trinh-saas-multi-tenant.md
```

Đây không phải slide thiết kế. File này là talk-through document: có sơ đồ ASCII, bảng so sánh, ý chính và ghi chú khi trình bày để có thể mở ra và trao đổi nhanh với leader.

## Quyết định về code demo

Hiện tại repository này được ưu tiên làm kho kiến thức Phase 1. Chưa bắt đầu full coding project trong repo này.

Khi đến thời điểm phù hợp:

- Nếu demo backend còn nhỏ và bám sát lý thuyết, có thể đặt tạm trong `lab-code/` hoặc `demo/`.
- Nếu demo phát triển thành một project thật sự lớn, nên tách sang repository riêng để repo kiến thức này vẫn gọn, rõ và dễ đọc.

Workflow học code dự kiến:

1. Tự implement task code được giao trước.
2. Sau đó mới nhờ Agent review, chỉ ra lỗi, đề xuất sửa và giải thích.
3. Mục tiêu là học bằng cách tự viết code, không phụ thuộc vào tự động hóa ngay từ đầu.

## Nguyên tắc quản lý repo

1. Học đến đâu, chuẩn hóa kiến thức đến đó.
2. GitHub chỉ lưu kiến thức đã được tổng hợp, không lưu toàn bộ nháp học tập.
3. Câu hỏi, câu trả lời nháp, review, prompt và scratch notes là local-only.
4. Báo cáo được viết bằng LaTeX để có thể compile sang PDF.
5. Tài liệu thuyết trình là markdown dạng sơ đồ/tóm tắt, không phải slide thiết kế.
6. Code demo sẽ được bắt đầu khi phần lý thuyết đủ nền tảng.

## Chủ đề chính đã tổng hợp

- SaaS là mô hình phân phối phần mềm.
- Phân biệt SaaS, cloud, subscription, on-premise và multi-tenant.
- Multi-tenant là pattern kiến trúc, thường đi cùng SaaS nhưng không đồng nghĩa với SaaS.
- Các mô hình tenant isolation: shared table với `tenant_id`, schema per tenant, database per tenant.
- Trade-off giữa chi phí, isolation, migration complexity, blast radius, operational complexity và customization.
- Feature flags, zero-downtime deployment, rolling deployment, blue-green deployment và backward-compatible migration.
- Noisy neighbor, index tenant-aware, locking, rollback, partitioning, vacuum, read replica và vai trò của PostgreSQL.
