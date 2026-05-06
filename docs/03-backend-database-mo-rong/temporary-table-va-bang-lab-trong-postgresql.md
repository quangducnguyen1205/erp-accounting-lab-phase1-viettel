# Temporary table và bảng lab trong PostgreSQL

## Temporary table là gì?

Temporary table, hay bảng tạm, là bảng chỉ tồn tại trong phạm vi một session PostgreSQL. Nó dùng để lưu dữ liệu tạm thời trong lúc làm việc, chạy script, xử lý batch hoặc thử nghiệm.

Ví dụ:

```sql
CREATE TEMP TABLE master_data_index_lab AS
SELECT *
FROM master_data;
```

Bảng `master_data_index_lab` ở ví dụ trên chỉ tồn tại trong session hiện tại. Khi session kết thúc, PostgreSQL tự xóa bảng tạm đó.

## Temporary table tồn tại bao lâu?

Thông thường:

- Bảng tạm tồn tại trong session hiện tại.
- Session khác không nhìn thấy bảng tạm của session này.
- Khi session đóng, bảng tạm tự biến mất.

Trong bài lab, mình vẫn nên cleanup rõ ràng:

```sql
DROP TABLE IF EXISTS master_data_index_lab;
```

Việc cleanup explicit giúp script dễ đọc hơn và chạy lại dễ hơn trong cùng một session.

## Vì sao `CREATE TEMP TABLE ... AS SELECT ...` hữu ích?

`CREATE TEMP TABLE ... AS SELECT ...` cho phép tạo một bảng tạm từ kết quả của một query.

Điểm quan trọng:

- Nó copy dữ liệu/kết quả SELECT.
- Nó không copy primary key, unique constraint, foreign key hoặc index của bảng gốc.
- Vì vậy mình có thể tự tạo/drop index thử nghiệm trên bảng tạm mà không phá schema thật.

Trong lab index, điều này rất hữu ích. Bảng thật `master_data` có `UNIQUE (tenant_id, code)` để bảo vệ business rule. PostgreSQL tự tạo index cho constraint này. Nếu drop constraint đó để benchmark, mình đang phá tính đúng đắn của schema thật.

Bảng tạm giúp tránh chuyện đó:

```text
master_data thật
├── giữ PRIMARY KEY
├── giữ UNIQUE (tenant_id, code)
└── giữ business rule

master_data_index_lab tạm
├── dùng để thử Seq Scan / Index Scan
├── tạo/drop index tự do
└── xóa sau khi học xong
```

## Vì sao phù hợp với learning lab?

Temporary table phù hợp cho bài lab vì:

- An toàn: không làm hỏng bảng thật.
- Dễ thử nghiệm: có thể tạo index, drop index, sinh thêm dữ liệu.
- Dễ reset: chạy lại script là có bảng lab mới.
- Tách rõ hai mục tiêu: correctness của schema thật và performance experiment.

Trong bài lab PostgreSQL Phase 1, bảng tạm giúp mình học được:

- Vì sao bảng nhỏ thường dùng `Seq Scan`.
- Khi nào index trên `tenant_id` bắt đầu có ích.
- Khi nào composite index `(tenant_id, code)` phù hợp.
- Vì sao query thiếu `tenant_id` vẫn nguy hiểm dù có thể chạy nhanh.

## Temporary table trong learning khác gì production?

Trong learning lab, temporary table là công cụ để thử nghiệm an toàn. Mình dùng nó để quan sát hành vi PostgreSQL mà không sợ phá dữ liệu thật.

Trong production, temporary table không phải giải pháp mặc định cho mọi vấn đề performance. Dùng bảng tạm trong production cần hiểu rõ:

- Session và transaction đang sống bao lâu.
- Dữ liệu tạm lớn đến mức nào.
- Bảng tạm tiêu thụ memory/disk ra sao.
- Query có chạy thường xuyên không.
- Có cách đơn giản hơn bằng index, query rewrite hoặc batch job không.

Nói ngắn gọn: trong lab, temp table giúp học. Trong production, temp table là công cụ cần đo đạc và kiểm soát.

## Một số tình huống thực tế có thể gặp

Temporary table hoặc staging table có thể xuất hiện trong các tình huống nâng cao hơn:

| Tình huống | Cách dùng có thể gặp |
|---|---|
| Batch processing | Tạm lưu danh sách bản ghi cần xử lý theo đợt |
| ETL/data import | Load dữ liệu thô vào staging trước khi validate và merge |
| Report generation | Tính toán trung gian cho báo cáo phức tạp |
| Intermediate result | Chia query phức tạp thành nhiều bước dễ kiểm soát hơn |
| Data cleanup | Lưu danh sách dòng nghi vấn trước khi sửa/xóa |
| Migration preparation | Chuẩn bị dữ liệu trước khi chạy migration thật |
| Query strategy comparison | So sánh nhiều cách viết query/index mà không đụng bảng thật |

Không phải tình huống nào cũng cần temp table. Nhiều bài toán chỉ cần index đúng, query đúng hoặc xử lý trong application/job queue.

## Các lưu ý quan trọng

- Temp table là session-scoped: session khác không thấy được.
- Temp table vẫn tiêu thụ tài nguyên.
- Dữ liệu tạm lớn vẫn có thể chậm và dùng disk.
- Không nên coi temp table là cách chữa mặc định cho query chậm.
- Backend service không nên tùy tiện tạo bảng tạm rất lớn cho mỗi request.
- Production usage cần đo bằng `EXPLAIN`, metrics và hiểu rõ transaction/session.

## Liên hệ với backend service

Trong backend API thông thường, service nên ưu tiên:

- Query tenant-aware rõ ràng.
- Index phù hợp.
- Pagination.
- Job queue cho tác vụ nặng.
- Report pipeline riêng nếu report phức tạp.

Không nên để mỗi request người dùng tạo một temp table lớn rồi xử lý tùy tiện. Cách này có thể làm database tốn tài nguyên và khó kiểm soát khi nhiều user gọi cùng lúc.

Temp/staging table phù hợp hơn trong:

- Batch worker.
- Admin tool.
- Migration script.
- Data import pipeline.
- Reporting job được kiểm soát.
- Script học tập hoặc benchmark local.

## Kết luận

Temporary table là một công cụ tốt để học và thử nghiệm an toàn trong PostgreSQL. Trong lab index, nó giúp mình so sánh `Seq Scan` và `Index Scan` mà không phá `UNIQUE (tenant_id, code)` của bảng thật.

Tuy nhiên, đây là ghi chú nâng cao. Để làm tốt Phase 1, trước hết cần nắm chắc: query phải tenant-aware, index phải phù hợp với query, và business constraint không nên bị phá chỉ để thử performance.
