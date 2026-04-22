# 🏗️ Kiến trúc Demo Phase 1

## Nguyên tắc thiết kế Demo

1. **Đơn giản nhưng có cấu trúc** — không overengineer, nhưng không bừa bãi
2. **Thể hiện multi-tenant** — đây là mục tiêu chính
3. **Có auth hoàn chỉnh** — login + JWT + RBAC
4. **1 module CRUD** — đủ để minh họa full flow
5. **Có thể tiến hóa** — thiết kế để mở rộng, không phải rewrite

---

## Kiến trúc tổng thể Demo Phase 1

```mermaid
graph TB
    subgraph "Frontend"
        REACT[React App<br/>Vite + React Router]
    end
    
    subgraph "Backend - Spring Boot Modular Monolith"
        FILTER[JWT Filter + Tenant Filter]
        
        subgraph "Modules"
            AUTH[Auth Module<br/>Login, JWT generation]
            TENANT[Tenant Module<br/>Tenant management]
            USER[User Module<br/>User + Role management]
            MASTER[Master Data Module<br/>CRUD + Search]
        end
    end
    
    subgraph "Database"
        PG[(PostgreSQL<br/>1 database<br/>tenant_id column)]
    end
    
    REACT -->|REST API + JWT| FILTER
    FILTER --> AUTH & TENANT & USER & MASTER
    AUTH & TENANT & USER & MASTER --> PG
```

---

## Request Flow chi tiết

### Login Flow

```mermaid
sequenceDiagram
    actor User
    participant FE as React App
    participant BE as Spring Boot
    participant DB as PostgreSQL

    User->>FE: Nhập username, password, tenant code
    FE->>BE: POST /api/auth/login
    BE->>DB: SELECT tenant WHERE code = ?
    DB-->>BE: Tenant{id=1, name="Công ty ABC"}
    BE->>DB: SELECT user WHERE username=? AND tenant_id=1
    DB-->>BE: User{id=5, passwordHash="$2a$..."}
    BE->>BE: BCrypt.verify(password, hash)
    BE->>DB: SELECT roles, permissions WHERE user_id=5
    DB-->>BE: [ADMIN] → [CREATE, READ, UPDATE, DELETE]
    BE->>BE: Generate JWT{userId=5, tenantId=1, roles=[ADMIN]}
    BE-->>FE: {token: "eyJ...", user: {name, roles}}
    FE->>FE: Lưu token, redirect /dashboard
```

### CRUD Flow

```mermaid
sequenceDiagram
    actor User
    participant FE as React App
    participant BE as Spring Boot
    participant DB as PostgreSQL

    User->>FE: Bấm "Thêm mới", nhập form
    FE->>BE: POST /api/master-data<br/>Authorization: Bearer {JWT}<br/>{code: "VT001", name: "Vật tư 001"}
    
    BE->>BE: JWT Filter: verify token, extract tenantId=1
    BE->>BE: Security: check role ADMIN has CREATE permission
    BE->>BE: Validate: code format, required fields
    BE->>DB: SELECT count WHERE tenant_id=1 AND code='VT001'
    DB-->>BE: 0 (not duplicate)
    BE->>DB: INSERT INTO master_data(tenant_id, code, name, ...) VALUES(1, 'VT001', 'Vật tư 001', ...)
    DB-->>BE: Saved{id=42}
    BE-->>FE: 201 Created {id: 42, code: "VT001", name: "Vật tư 001"}
    FE->>FE: Refresh danh sách
```

### Tenant Data Isolation Flow

```mermaid
sequenceDiagram
    participant U1 as User@TenantA (tenantId=1)
    participant U2 as User@TenantB (tenantId=2)
    participant BE as Backend
    participant DB as PostgreSQL

    U1->>BE: GET /api/master-data (JWT: tenantId=1)
    BE->>DB: SELECT * FROM master_data WHERE tenant_id=1
    DB-->>BE: [VT001, VT002, VT003] — data tenant A
    BE-->>U1: 3 records

    U2->>BE: GET /api/master-data (JWT: tenantId=2)
    BE->>DB: SELECT * FROM master_data WHERE tenant_id=2
    DB-->>BE: [SP001, SP002] — data tenant B
    BE-->>U2: 2 records

    Note over U1,U2: Tenant A KHÔNG BAO GIỜ thấy data Tenant B
```

---

## Database Schema cho Demo

```mermaid
erDiagram
    TENANTS ||--o{ USERS : has
    TENANTS ||--o{ ROLES : has
    TENANTS ||--o{ MASTER_DATA : has
    USERS }o--o{ USER_ROLES : assigned
    ROLES }o--o{ USER_ROLES : assigned
    ROLES }o--o{ ROLE_PERMISSIONS : granted
    PERMISSIONS }o--o{ ROLE_PERMISSIONS : granted

    TENANTS {
        bigint id PK
        varchar code UK
        varchar name
        boolean is_active
        timestamp created_at
    }
    USERS {
        bigint id PK
        bigint tenant_id FK
        varchar username
        varchar email
        varchar password_hash
        boolean is_active
        timestamp created_at
    }
    ROLES {
        bigint id PK
        bigint tenant_id FK
        varchar code
        varchar name
    }
    PERMISSIONS {
        bigint id PK
        varchar code UK
        varchar module
        varchar action
    }
    MASTER_DATA {
        bigint id PK
        bigint tenant_id FK
        varchar code
        varchar name
        varchar category
        varchar description
        boolean is_active
        bigint created_by FK
        timestamp created_at
        bigint updated_by FK
        timestamp updated_at
    }
```

---

## Tech Stack Demo

| Layer | Technology | Lý do chọn |
|-------|-----------|-----------|
| Frontend | React + Vite | Nhẹ, nhanh, dự án target dùng React |
| Backend | Spring Boot 3 | Dự án target dùng Java/Spring |
| Database | PostgreSQL 15 | Dự án target dùng PostgreSQL |
| Auth | Spring Security + JWT | Tự implement để học, migrate Keycloak sau |
| Build | Maven/Gradle | Standard Java |
| Container | Docker Compose | Dev environment nhất quán |

---

## Docker Compose cho Demo

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: phase1_demo
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/phase1_demo
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  postgres_data:
```

---

## Tiến hóa sau Phase 1

```mermaid
graph LR
    subgraph "Phase 1"
        P1[Monolith<br/>1 Spring Boot<br/>1 PostgreSQL<br/>JWT tự implement]
    end
    
    subgraph "Phase 2"
        P2[Thêm Keycloak<br/>Thêm Redis cache<br/>Tách Auth module<br/>Schema per tenant]
    end
    
    subgraph "Phase 3"
        P3[Thêm Kong Gateway<br/>Tách microservices<br/>Thêm Kafka<br/>Thêm MinIO<br/>Thêm Prometheus/Grafana]
    end
    
    P1 --> P2 --> P3
```

---

*Tiếp theo: [16-lo-trinh-thuc-hien/](../16-lo-trinh-thuc-hien/)*
