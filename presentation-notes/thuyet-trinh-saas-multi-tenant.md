# Thuyết trình: SaaS, Multi-tenant & Nền tảng Backend

> **Mục đích:** Tài liệu Visual Anchor — mở lên khi báo cáo trực tiếp với leader.
> Mỗi phần = 1 "slide". Nhìn sơ đồ → đọc bullet → tự nói mở rộng.

---

## Slide 1 — Em đã học gì trong Phase 1?

```mermaid
graph LR
    P1["Phase 1"] --> S["SaaS<br/>Foundation"]
    P1 --> M["Multi-tenant<br/>Architecture"]
    P1 --> DB["Backend &<br/>Database"]

    S --> S1["Delivery model"]
    S --> S2["3 trục độc lập"]

    M --> M1["3 mô hình isolation"]
    M --> M2["Trade-off analysis"]

    DB --> DB1["PostgreSQL<br/>tenant-aware"]
    DB --> DB2["Index / Lock /<br/>Migration"]

    style P1 fill:#1565c0,color:white
    style S fill:#43a047,color:white
    style M fill:#ef6c00,color:white
    style DB fill:#6a1b9a,color:white
```

**Core facts:**
- Gần hoàn tất nền tảng SaaS + multi-tenant.
- Đang đi sâu vào database/backend.
- Repo = kho kiến thức, chưa có code demo.

> **Speaker script:** "Em đã dành thời gian tập trung vào 3 mảng: hiểu SaaS là gì ở mức delivery model, hiểu multi-tenant là kiến trúc phục vụ nhiều khách hàng, và bắt đầu tìm hiểu database backend liên quan trực tiếp. Em xin trình bày qua từng phần."

---

## Slide 2 — SaaS là gì?

```mermaid
graph TB
    NCC["Nhà cung cấp<br/>Host · Vận hành · Cập nhật"]
    KH["Khách hàng<br/>Truy cập qua internet"]
    NCC -->|"cung cấp dịch vụ"| KH

    style NCC fill:#1565c0,color:white
    style KH fill:#e8f5e9,stroke:#4caf50
```

**Core facts:**
- SaaS = mô hình **phân phối phần mềm** (delivery model).
- Nhà cung cấp chịu trách nhiệm vận hành, khách hàng dùng như dịch vụ.
- SaaS ≠ Cloud. SaaS ≠ Subscription. SaaS ≠ Multi-tenant.

> **Speaker script:** "SaaS không phải là 'chạy trên cloud'. Nó là câu chuyện AI host và vận hành phần mềm. Cloud là hạ tầng, subscription là cách tính tiền, multi-tenant là cách phục vụ nhiều khách — ba thứ khác nhau."

---

## Slide 3 — 3 trục độc lập

```mermaid
graph TB
    subgraph AXES["3 quyết định RIÊNG BIỆT"]
        D["🚀 Delivery<br/>SaaS / On-premise"]
        P["💰 Pricing<br/>Subscription / License"]
        A["🏗️ Architecture<br/>Multi-tenant / Single"]
    end

    style D fill:#e3f2fd,stroke:#1565c0
    style P fill:#fce4ec,stroke:#c62828
    style A fill:#e8f5e9,stroke:#2e7d32
```

**Core facts:**
- 3 trục này **thường đi cùng** nhưng **không bắt buộc**.
- JetBrains = on-premise + subscription.
- SaaS single-tenant tồn tại. Multi-tenant on-premise cũng tồn tại.

> **Speaker script:** "Đây là điểm em đã sửa lại trong quá trình học. Ban đầu em hay gộp SaaS với multi-tenant. Thực tế đây là ba quyết định riêng. Dự án mình chọn cả 3: SaaS delivery, subscription pricing, multi-tenant architecture."

---

## Slide 4 — Multi-tenant là gì?

```mermaid
graph TB
    PLAT["Nền tảng ERP / Kế toán"]
    PLAT --> TA["Tenant A<br/>Công ty A"]
    PLAT --> TB["Tenant B<br/>Công ty B"]
    PLAT --> TC["Tenant C<br/>Công ty C"]

    TA -.- DATA_A["data · users · config"]
    TB -.- DATA_B["data · users · config"]
    TC -.- DATA_C["data · users · config"]

    style PLAT fill:#1565c0,color:white
    style TA fill:#e8f5e9,stroke:#4caf50
    style TB fill:#fff3e0,stroke:#ff9800
    style TC fill:#f3e5f5,stroke:#9c27b0
```

**Core facts:**
- Nhiều doanh nghiệp dùng **chung nền tảng**, nhưng dữ liệu **phải cách ly**.
- Chia sẻ: code, hạ tầng, deploy pipeline.
- Cách ly: data, quyền, cache, log, config.
- Rủi ro lớn nhất = **data leakage**.

> **Speaker script:** "Mỗi doanh nghiệp trong bài toán kế toán là một tenant. Họ dùng chung hệ thống nhưng dữ liệu kế toán phải hoàn toàn tách biệt. Nếu để lộ dữ liệu — đó là lỗi bảo mật nghiêm trọng."

---

## Slide 5 — 3 mô hình Tenant Isolation

```mermaid
graph TB
    subgraph MH1["MH1: Shared Table + tenant_id"]
        DB1[(1 Database)] --> TBL1["1 bảng chung<br/>phân biệt bằng tenant_id"]
    end

    subgraph MH2["MH2: Schema per Tenant"]
        DB2[(1 Database)] --> SCH1["Schema tenant_A"]
        DB2 --> SCH2["Schema tenant_B"]
    end

    subgraph MH3["MH3: DB per Tenant"]
        DBA[(DB tenant_A)]
        DBB[(DB tenant_B)]
    end

    style MH1 fill:#e8f5e9,stroke:#4caf50
    style MH2 fill:#fff3e0,stroke:#ff9800
    style MH3 fill:#ffebee,stroke:#f44336
```

| | MH1 Shared table | MH2 Schema/tenant | MH3 DB/tenant |
|---|:---:|:---:|:---:|
| **Chi phí** | 💰 | 💰💰 | 💰💰💰 |
| **Isolation** | Thấp | Trung bình | Cao |
| **Migration** | 1 lần | N lần | N lần (độc lập) |
| **Phase 1** | ✅ Chọn | Chưa cần | Quá phức tạp |

> **Speaker script:** "Phase 1 chọn MH1 — shared table với tenant_id. Đơn giản nhất, học được đúng vấn đề. Nếu sau này có khách enterprise yêu cầu isolation mạnh, có thể chuyển sang hybrid: SME dùng MH1, enterprise dùng MH3."

---

## Slide 6 — Tenant-aware: mọi tầng phải biết tenant

```mermaid
sequenceDiagram
    participant Client
    participant Auth as Auth Middleware
    participant Svc as Service Layer
    participant Cache as Redis Cache
    participant DB as PostgreSQL

    Client->>Auth: Request + JWT
    Auth->>Auth: Extract tenant_id
    Auth->>Svc: tenant_id = 42
    Svc->>Cache: GET tenant:42:categories
    alt Cache miss
        Svc->>DB: WHERE tenant_id = 42
        DB-->>Svc: Result
        Svc->>Cache: SET tenant:42:categories
    end
    Svc-->>Client: Response
```

**Core facts:**
- Auth → Service → Cache → DB: tất cả phải biết tenant.
- Cache key thiếu tenant prefix = data leakage qua cache.
- Query thiếu `WHERE tenant_id` = data leakage qua DB.

> **Speaker script:** "Không chỉ database. Cache, log, auth, metrics — tất cả phải tenant-aware. Em đã phân tích cụ thể các tình huống lỗi như cache key thiếu prefix, query thiếu filter, và cách phòng tránh."

---

## Slide 7 — Các tình huống thực tế đã phân tích

```mermaid
graph LR
    subgraph RISKS["Rủi ro SaaS multi-tenant"]
        R1["Data leakage<br/>Quên tenant filter"]
        R2["Blast radius<br/>1 deploy lỗi = tất cả"]
        R3["Noisy neighbor<br/>Tenant A làm B chậm"]
        R4["Migration lock<br/>ALTER TABLE block all"]
    end

    subgraph SOLUTIONS["Giải pháp"]
        S1["Base Repository<br/>+ RLS + Test"]
        S2["Canary / Rolling<br/>+ Feature flags"]
        S3["Composite index<br/>+ Partitioning"]
        S4["Backward-compatible<br/>migration"]
    end

    R1 --> S1
    R2 --> S2
    R3 --> S3
    R4 --> S4

    style RISKS fill:#ffebee,stroke:#c62828
    style SOLUTIONS fill:#e8f5e9,stroke:#2e7d32
```

> **Speaker script:** "Em đã phân tích 4 tình huống rủi ro chính. Mỗi cái có giải pháp cụ thể. Ví dụ data leakage — phòng bằng Base Repository auto-filter, PostgreSQL RLS, và integration test. Blast radius — giảm bằng canary deployment và feature flags."

---

## Slide 8 — Kết luận và hướng tiếp

```mermaid
graph TB
    DONE["✅ Đã hoàn tất"]
    NEXT["🔄 Tiếp theo"]

    DONE --> D1["SaaS concept + 3 trục"]
    DONE --> D2["Multi-tenant 3 mô hình"]
    DONE --> D3["Trade-off analysis"]
    DONE --> D4["Tình huống production"]

    NEXT --> N1["PostgreSQL thực hành"]
    NEXT --> N2["Backend demo nhỏ"]
    NEXT --> N3["Auth / RBAC"]

    style DONE fill:#4caf50,color:white
    style NEXT fill:#1565c0,color:white
```

**Core facts:**
- Nền tảng SaaS + multi-tenant: **gần hoàn tất**.
- Đã phát triển tư duy **trade-off**, không chỉ liệt kê đặc điểm.
- Tiếp theo: PostgreSQL thực hành, code demo, Auth/RBAC.

> **Speaker script:** "Tóm lại, em đã nắm được nền tảng SaaS và multi-tenant. Bước tiến lớn nhất là chuyển từ 'biết là gì' sang 'hiểu trade-off'. Tiếp theo em sẽ bắt đầu thực hành code demo để kiểm chứng kiến thức, bắt đầu từ shared table + tenant_id trên PostgreSQL."

---

> **Ghi chú khi leader hỏi ngoài lề:**
>
> - *"Tại sao không dùng MongoDB?"* → Kế toán cần ACID mạnh, foreign key constraint, dữ liệu quan hệ chặt. MongoDB eventual consistency không phù hợp cho nghiệp vụ tài chính chính thống.
> - *"Schema per tenant có phức tạp lắm không?"* → 500 tenant = 500 schema. Migration phải loop qua tất cả. 1 schema fail = partial migration = inconsistency.
> - *"Noisy neighbor giải quyết thế nào?"* → Composite index `(tenant_id, ...)`, table partitioning, connection pool isolation, read replica cho report nặng.
> - *"Feature flag hoạt động thế nào?"* → 2 bảng: `feature_flags` + `tenant_feature_flags`. Check ở service layer, cache per tenant.
