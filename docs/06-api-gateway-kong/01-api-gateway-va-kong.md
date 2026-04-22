# 🚪 API Gateway & Kong

## API Gateway là gì?

API Gateway là **điểm vào duy nhất** (single entry point) cho tất cả request từ client vào hệ thống backend.

```mermaid
graph LR
    subgraph "Không có Gateway"
        C1[Client] --> S1[Auth Service]
        C1 --> S2[Tenant Service]
        C1 --> S3[Business Service]
        NOTE1["❌ Client phải biết địa chỉ từng service<br/>❌ Không có điểm kiểm soát chung"]
    end
```

```mermaid
graph LR
    subgraph "Có Gateway"
        C2[Client] --> GW[API Gateway]
        GW --> S4[Auth Service]
        GW --> S5[Tenant Service]
        GW --> S6[Business Service]
        NOTE2["✅ Client chỉ biết 1 địa chỉ<br/>✅ Gateway kiểm soát tập trung"]
    end
```

---

## Các chức năng chính của API Gateway

| Chức năng | Mô tả | Tại sao cần |
|-----------|-------|-------------|
| **Routing** | Chuyển request đến đúng service | Client không cần biết internal topology |
| **Authentication** | Verify JWT token | Kiểm tra auth tập trung, service không cần tự verify |
| **Rate Limiting** | Giới hạn số request/giây | Bảo vệ backend khỏi quá tải |
| **Load Balancing** | Phân tải giữa các instance | Scalability |
| **SSL Termination** | Xử lý HTTPS | Service nội bộ dùng HTTP cho đơn giản |
| **Request Transform** | Thêm/sửa header | Inject tenant_id, correlation_id |
| **Logging/Metrics** | Ghi log tập trung | Observability |
| **CORS** | Xử lý cross-origin | Frontend ở domain khác |

---

## Request Flow qua API Gateway

```mermaid
sequenceDiagram
    participant FE as React App
    participant GW as Kong Gateway
    participant KC as Keycloak
    participant BE as Backend Service
    
    FE->>GW: GET /api/master-data<br/>Authorization: Bearer {JWT}
    
    GW->>GW: Rate limit check
    GW->>KC: Validate JWT token
    KC-->>GW: Token valid, user info
    GW->>GW: Add headers: X-Tenant-Id, X-User-Id
    GW->>BE: Forward request + enriched headers
    BE->>BE: Process business logic
    BE-->>GW: Response data
    GW->>GW: Add response headers, logging
    GW-->>FE: Response data
```

---

## Kong API Gateway

### Tại sao Kong?

Kong là API Gateway phổ biến nhất cho microservices:

| Tiêu chí | Kong | Nginx (raw) | AWS API Gateway | Traefik |
|----------|------|-------------|-----------------|---------|
| **Open-source** | ✅ (Community) | ✅ | ❌ (AWS-only) | ✅ |
| **Plugin ecosystem** | ✅ Rất phong phú | ⚠️ Ít | ✅ AWS ecosystem | ⚠️ Trung bình |
| **Admin API** | ✅ REST API | ❌ Config file | ✅ Console | ✅ |
| **Service Discovery** | ✅ Built-in | ❌ | ✅ | ✅ |
| **Rate Limiting** | ✅ Plugin | ⚠️ Tự config | ✅ | ✅ |
| **Auth plugins** | ✅ JWT, OAuth2 | ⚠️ Basic | ✅ | ⚠️ |
| **Learning curve** | ⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐ |
| **Performance** | ✅ Rất cao (Lua/Nginx) | ✅ Rất cao | ✅ | ✅ |

### Kong architecture

```mermaid
graph TB
    subgraph "Kong Gateway"
        PROXY[Proxy Layer<br/>Nhận và forward request]
        PLUGINS[Plugin Chain<br/>Auth, Rate Limit, Logging...]
        ADMIN[Admin API<br/>Quản lý routes, services]
        DB_KONG[(Kong Database<br/>PostgreSQL/Cassandra)]
        
        PROXY --> PLUGINS
        ADMIN --> DB_KONG
    end
    
    CLIENT[Client] --> PROXY
    PLUGINS --> UPSTREAM[Upstream Services]
```

---

## Phase 1: Dùng Gateway hay chưa?

### Phân tích

| Tiêu chí | Dùng Kong ngay | Không dùng vội |
|----------|----------------|----------------|
| **Học được** | Hiểu gateway concept thực tế | Tập trung học backend |
| **Complexity** | Thêm 1 container (Docker) | Đơn giản hơn |
| **Cần thiết?** | Chỉ có 1 backend service | Chưa cần route nhiều service |
| **Auth** | Kong verify JWT | Backend tự verify JWT |

### Khuyến nghị

```mermaid
graph TD
    START[Phase 1 Demo] --> Q{Có nhiều service?}
    Q -->|Không - chỉ 1 backend| OPT1[Không cần Kong<br/>Backend tự handle]
    Q -->|Có - 2+ services| OPT2[Dùng Kong<br/>Routing + Auth tập trung]
    
    OPT1 -->|Sau khi hiểu concept| LATER[Thêm Kong khi tách service]
    
    style OPT1 fill:#4CAF50,color:white
    style LATER fill:#FF9800,color:white
```

**Kết luận Phase 1:**
- **Ban đầu:** Không cần Kong. Backend Spring Boot tự handle auth + routing.
- **Khi hiểu concept:** Thêm Kong vào Docker Compose, thử route request.
- **Khi tách service:** Kong trở nên cần thiết.

---

## Nếu thêm Kong vào Demo

```yaml
# docker-compose.yml (tham khảo)
services:
  kong:
    image: kong:latest
    environment:
      KONG_DATABASE: postgres
      KONG_PG_HOST: kong-db
    ports:
      - "8000:8000"   # Proxy
      - "8001:8001"   # Admin API
  
  kong-db:
    image: postgres:15
    environment:
      POSTGRES_DB: kong
```

Cấu hình route:
```bash
# Tạo service
curl -i -X POST http://localhost:8001/services \
  --data name=backend-service \
  --data url=http://backend:8080

# Tạo route
curl -i -X POST http://localhost:8001/services/backend-service/routes \
  --data paths=/api
```

---

*Tiếp theo: [07-postgresql-data-modeling/](../07-postgresql-data-modeling/)*
