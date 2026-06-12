# ==============================================================
# Makefile — Lab Code automation cho Phase 1 PoC
# ==============================================================
#
# Gõ `make` hoặc `make help` để xem danh sách lệnh.
# Tất cả lệnh chạy từ thư mục lab-code/.
#
# ==============================================================

.DEFAULT_GOAL := help

# --- Cấu hình (phải khớp với docker/docker-compose.yml) --------
COMPOSE_FILE   := docker/docker-compose.yml
CONTAINER_NAME := erp-postgres
DB_USER        := erpuser
DB_NAME        := erpdb
SQL_DIR        := sql-playground
APP_DIR        := tenant-demo
ELASTIC_DIR    := elasticsearch-lab
KEYCLOAK_DIR   := keycloak-lab
MINIO_DIR      := minio-lab
REDIS_DIR      := redis-lab
KAFKA_DIR      := kafka-lab
KAFKA_UI_DIR   := kafka-ui-lab
AUDIT_LOG_DIR  := audit-log-service
OBSERVABILITY_DIR := observability-lab
LOKI_DIR       := loki-lab
GATEWAY_DIR    := gateway-demo
KONG_DIR       := kong-gateway-lab
WEB_UI_DIR     := web-ui-demo

# --- Docker / Database -----------------------------------------

.PHONY: db-up
db-up: ## Khởi chạy PostgreSQL container (background)
	docker compose -f $(COMPOSE_FILE) up -d

.PHONY: db-down
db-down: ## Dừng và xóa PostgreSQL container
	docker compose -f $(COMPOSE_FILE) down

.PHONY: db-logs
db-logs: ## Xem log của PostgreSQL container
	docker logs -f $(CONTAINER_NAME)

.PHONY: db-status
db-status: ## Kiểm tra container đang chạy không
	@docker ps --filter "name=$(CONTAINER_NAME)" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: db-shell
db-shell: ## Mở psql shell trực tiếp vào database
	docker exec -it $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME)

# --- KeyCloak mini-lab ------------------------------------
.PHONY: keycloak-up
keycloak-up: ## Khởi chạy KeyCloak local persistent DB cho auth mini-lab
	cd $(KEYCLOAK_DIR) && docker compose up -d

.PHONY: keycloak-down
keycloak-down: ## Dừng KeyCloak local, giữ lại volume PostgreSQL của Keycloak
	cd $(KEYCLOAK_DIR) && docker compose down

.PHONY: keycloak-status
keycloak-status: ## Kiểm tra KeyCloak local
	@docker ps --filter "name=viettel-keycloak" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: keycloak-logs
keycloak-logs: ## Xem log KeyCloak local persistent lab
	cd $(KEYCLOAK_DIR) && docker compose logs -f

.PHONY: keycloak-setup
keycloak-setup: ## Bootstrap realm/client/user/role local cho KeyCloak demo
	$(KEYCLOAK_DIR)/setup-keycloak-demo.sh

.PHONY: keycloak-reset
keycloak-reset: ## DESTRUCTIVE: dừng KeyCloak và xóa volume DB realm/client/user local
	@echo "WARNING: target này xóa volume PostgreSQL của Keycloak local."
	@echo "Sau đó cần chạy lại: make keycloak-up && make keycloak-setup"
	cd $(KEYCLOAK_DIR) && docker compose down -v

# --- Elasticsearch mini-lab ------------------------------------

.PHONY: elastic-up
elastic-up: ## Khởi chạy Elasticsearch local cho search mini-lab
	cd $(ELASTIC_DIR) && docker compose up -d

.PHONY: elastic-down
elastic-down: ## Dừng Elasticsearch local cho search mini-lab
	cd $(ELASTIC_DIR) && docker compose down

.PHONY: elastic-status
elastic-status: ## Kiểm tra Elasticsearch local
	@docker ps --filter "name=viettel-elasticsearch" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# --- MinIO mini-lab --------------------------------------------

.PHONY: minio-up
minio-up: ## Khởi chạy MinIO local cho file storage mini-lab
	cd $(MINIO_DIR) && docker compose up -d

.PHONY: minio-down
minio-down: ## Dừng MinIO local cho file storage mini-lab
	cd $(MINIO_DIR) && docker compose down

.PHONY: minio-status
minio-status: ## Kiểm tra MinIO local
	@docker ps --filter "name=viettel-minio" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# --- Redis mini-lab --------------------------------------------

.PHONY: redis-up
redis-up: ## Khởi chạy Redis local cho cache mini-lab
	cd $(REDIS_DIR) && docker compose up -d

.PHONY: redis-down
redis-down: ## Dừng Redis local cho cache mini-lab
	cd $(REDIS_DIR) && docker compose down

.PHONY: redis-status
redis-status: ## Kiểm tra Redis local
	@docker ps --filter "name=viettel-redis" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# --- Kafka mini-lab --------------------------------------------

.PHONY: kafka-up
kafka-up: ## Khởi chạy Kafka local cho async messaging mini-lab
	cd $(KAFKA_DIR) && docker compose up -d

.PHONY: kafka-down
kafka-down: ## Dừng Kafka local cho async messaging mini-lab
	cd $(KAFKA_DIR) && docker compose down

.PHONY: kafka-status
kafka-status: ## Kiểm tra Kafka local
	@docker ps --filter "name=viettel-kafka" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# --- Kafka UI mini-lab -----------------------------------------

.PHONY: kafka-ui-up
kafka-ui-up: kafka-up ## Khởi chạy Kafka UI local ở port 18082
	cd $(KAFKA_UI_DIR) && docker compose up -d

.PHONY: kafka-ui-down
kafka-ui-down: ## Dừng Kafka UI local, không dừng Kafka broker
	cd $(KAFKA_UI_DIR) && docker compose down

.PHONY: kafka-ui-status
kafka-ui-status: ## Kiểm tra Kafka UI local
	@docker ps --filter "name=viettel-kafka-ui" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: kafka-ui-logs
kafka-ui-logs: ## Xem log Kafka UI local
	cd $(KAFKA_UI_DIR) && docker compose logs -f

.PHONY: kafka-ui-info
kafka-ui-info: ## In hướng dẫn nhanh cho Kafka UI lab
	@echo ""
	@echo "Kafka UI lab nằm ở $(KAFKA_UI_DIR)/"
	@echo "Workflow chính là Docker-first."
	@echo ""
	@echo "Chạy:"
	@echo "  make kafka-ui-up"
	@echo "  make kafka-ui-status"
	@echo ""
	@echo "URL:"
	@echo "  Kafka UI: http://localhost:18082"
	@echo ""
	@echo "Ghi chú: Kafka UI container kết nối Kafka broker qua Docker network viettel-kafka-net và bootstrap server kafka:9092."

# --- Audit Log Service mini-lab --------------------------------

.PHONY: audit-log-validate
audit-log-validate: ## Validate audit-log-service Maven project
	mvn -f $(AUDIT_LOG_DIR)/pom.xml validate

.PHONY: audit-log-run
audit-log-run: ## Chạy audit-log-service bằng Maven ở port 8082
	mvn -f $(AUDIT_LOG_DIR)/pom.xml spring-boot:run

.PHONY: audit-log-build
audit-log-build: ## Build Docker image cho audit-log-service
	cd $(AUDIT_LOG_DIR) && docker build -t viettel-audit-log-service:latest .

.PHONY: audit-log-up
audit-log-up: db-up kafka-up keycloak-up ## Khởi chạy audit-log-service container ở port 8082
	cd $(AUDIT_LOG_DIR) && docker compose up -d --build

.PHONY: audit-log-down
audit-log-down: ## Dừng audit-log-service container
	cd $(AUDIT_LOG_DIR) && docker compose down

.PHONY: audit-log-status
audit-log-status: ## Kiểm tra audit-log-service container
	@docker ps --filter "name=viettel-audit-log-service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: audit-log-logs
audit-log-logs: ## Xem log audit-log-service container
	cd $(AUDIT_LOG_DIR) && docker compose logs -f

.PHONY: audit-log-config
audit-log-config: ## Validate/hiển thị Docker Compose config cho audit-log-service
	cd $(AUDIT_LOG_DIR) && docker compose config

.PHONY: audit-log-info
audit-log-info: ## In hướng dẫn nhanh cho audit-log-service
	@echo ""
	@echo "Audit Log Service nằm ở $(AUDIT_LOG_DIR)/"
	@echo "Workflow chính là Docker-first."
	@echo ""
	@echo "Chạy:"
	@echo "  make audit-log-up"
	@echo "  make audit-log-status"
	@echo ""
	@echo "URLs:"
	@echo "  Direct service: http://localhost:8082"
	@echo "  Through Kong:   http://localhost:18000/api/audit-events"
	@echo ""
	@echo "Ghi chú: service consume Kafka topic master-data-events bằng group audit-log-service."

# --- Observability mini-lab ------------------------------------

.PHONY: observability-up
observability-up: ## Khởi chạy Prometheus + Grafana local cho observability mini-lab
	cd $(OBSERVABILITY_DIR) && docker compose up -d

.PHONY: observability-down
observability-down: ## Dừng Prometheus + Grafana local cho observability mini-lab
	cd $(OBSERVABILITY_DIR) && docker compose down

.PHONY: observability-status
observability-status: ## Kiểm tra Prometheus + Grafana local
	@docker ps --filter "name=viettel-prometheus" --filter "name=viettel-grafana" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# --- Loki log aggregation mini-lab -----------------------------

.PHONY: loki-up
loki-up: ## Khởi chạy Loki + Alloy + Grafana local cho log aggregation
	cd $(LOKI_DIR) && docker compose up -d

.PHONY: loki-down
loki-down: ## Dừng Loki + Alloy + Grafana local
	cd $(LOKI_DIR) && docker compose down

.PHONY: loki-status
loki-status: ## Kiểm tra Loki + Alloy + Grafana local
	@docker ps --filter "name=viettel-loki" --filter "name=viettel-alloy" --filter "name=viettel-loki-grafana" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: loki-logs
loki-logs: ## Xem log của Loki + Alloy + Grafana local
	cd $(LOKI_DIR) && docker compose logs -f

.PHONY: loki-info
loki-info: ## In hướng dẫn nhanh cho Loki log aggregation lab
	@echo ""
	@echo "Loki log aggregation lab nằm ở $(LOKI_DIR)/"
	@echo "Workflow chính là Docker-first."
	@echo ""
	@echo "Chạy:"
	@echo "  make loki-up"
	@echo "  make loki-status"
	@echo ""
	@echo "URLs:"
	@echo "  Loki readiness: http://localhost:3100/ready"
	@echo "  Grafana Explore: http://localhost:13001"
	@echo "  Alloy debug UI: http://localhost:12345"
	@echo ""
	@echo "Ghi chú: Alloy Docker collector chỉ đọc log container. App chạy bằng Maven trên host vẫn hiện log ở terminal host."

.PHONY: logs-up
logs-up: loki-up ## Alias: khởi chạy Loki log aggregation lab

.PHONY: logs-down
logs-down: loki-down ## Alias: dừng Loki log aggregation lab

# --- API Gateway mini-lab --------------------------------------

.PHONY: gateway-run
gateway-run: ## Chạy Spring Cloud Gateway demo ở port 8081
	mvn -f $(GATEWAY_DIR)/pom.xml spring-boot:run

.PHONY: gateway-validate
gateway-validate: ## Validate gateway-demo Maven project
	mvn -f $(GATEWAY_DIR)/pom.xml validate

.PHONY: gateway-status
gateway-status: ## Kiểm tra gateway-demo health endpoint
	curl -s http://localhost:8081/actuator/health

# --- Kong Gateway mini-lab -------------------------------------

.PHONY: kong-up
kong-up: ## Khởi chạy Kong Gateway DB-less local ở port 18000
	cd $(KONG_DIR) && docker compose up -d

.PHONY: kong-down
kong-down: ## Dừng Kong Gateway local
	cd $(KONG_DIR) && docker compose down

.PHONY: kong-status
kong-status: ## Kiểm tra Kong Gateway local
	@docker ps --filter "name=viettel-kong" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: kong-logs
kong-logs: ## Xem log Kong Gateway local
	cd $(KONG_DIR) && docker compose logs -f

.PHONY: kong-config
kong-config: ## Validate/hiển thị Docker Compose config cho Kong lab
	cd $(KONG_DIR) && docker compose config

.PHONY: kong-info
kong-info: ## In hướng dẫn nhanh cho Kong Gateway lab
	@echo ""
	@echo "Kong Gateway lab nằm ở $(KONG_DIR)/"
	@echo "Workflow chính là Docker-first, DB-less declarative config."
	@echo ""
	@echo "Chạy:"
	@echo "  make kong-up"
	@echo "  make kong-status"
	@echo ""
	@echo "URLs:"
	@echo "  Kong proxy:     http://localhost:18000"
	@echo "  Kong Admin API: http://localhost:18001 (local-only)"
	@echo ""
	@echo "Ghi chú: Kong container route tới tenant-demo host app qua host.docker.internal:8080."

# --- React Web UI demo -----------------------------------------

.PHONY: web-ui-info
web-ui-info: ## In hướng dẫn chạy React Web UI demo bằng Docker
	@echo ""
	@echo "React Web UI demo nằm ở $(WEB_UI_DIR)/"
	@echo "Workflow chính là Docker-first, không yêu cầu local npm."
	@echo ""
	@echo "Chạy background:"
	@echo "  make web-ui-up"
	@echo ""
	@echo "Chạy foreground:"
	@echo "  make web-ui-run"
	@echo ""
	@echo "Build kiểm tra:"
	@echo "  make web-ui-build"
	@echo ""
	@echo "Mặc định UI mở ở http://localhost:5173 và gọi Kong Gateway http://localhost:18000"

.PHONY: web-ui-build
web-ui-build: ## Build React Web UI demo bằng Docker
	cd $(WEB_UI_DIR) && docker build --target build -t viettel-web-ui-demo:build .

.PHONY: web-ui-up
web-ui-up: ## Khởi chạy React Web UI demo container ở port 5173
	cd $(WEB_UI_DIR) && docker compose up -d --build

.PHONY: web-ui-run
web-ui-run: ## Chạy React Web UI demo foreground bằng Docker Compose
	cd $(WEB_UI_DIR) && docker compose up --build

.PHONY: web-ui-down
web-ui-down: ## Dừng React Web UI demo container
	cd $(WEB_UI_DIR) && docker compose down

.PHONY: web-ui-status
web-ui-status: ## Kiểm tra React Web UI demo container
	@docker ps --filter "name=viettel-web-ui-demo" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: web-ui-logs
web-ui-logs: ## Xem log React Web UI demo container
	docker logs -f viettel-web-ui-demo

# --- Common dev infra ------------------------------------------
#
# Dùng target riêng khi chỉ học từng lab nhỏ:
# - db-up cho SQL/Spring Boot baseline
# - keycloak-up cho auth mini-lab
# - elastic-up cho search mini-lab
# - minio-up cho file storage mini-lab
# - redis-up cho cache mini-lab
# - kafka-up cho async messaging mini-lab
# - kafka-ui-up cho Kafka topic/message/consumer inspection
# - audit-log-up cho service split đầu tiên và Kafka cross-service flow
# - observability-up cho Prometheus/Grafana mini-lab
# - loki-up cho Loki/Grafana log aggregation mini-lab
# - gateway-run cho API Gateway static route mini-lab
# - kong-up cho Kong Gateway DB-less platform lab
# - web-ui-up cho React Web UI demo Docker-first
#
# Dùng infra-up khi cần demo đầy đủ hơn: PostgreSQL + Keycloak +
# Elasticsearch + MinIO + Redis + Kafka. App Spring Boot vẫn chạy riêng
# bằng `make app-run`, Gateway chạy riêng bằng `make gateway-run`, UI chạy
# riêng bằng `make web-ui-up`.
# Prometheus/Grafana metrics và Loki/Grafana logs là observability optional,
# chạy riêng bằng `make observability-up` hoặc `make loki-up`.
# Kafka UI là inspect tool optional, chạy riêng bằng `make kafka-ui-up` để
# full infra mặc định không quá nặng.
# Kong Gateway là Phase 1.5 platform lab, chạy riêng bằng `make kong-up`.
# audit-log-service chạy riêng bằng `make audit-log-up` khi demo service split.

.PHONY: infra-up
infra-up: db-up keycloak-up elastic-up minio-up redis-up kafka-up ## Khởi chạy PostgreSQL + KeyCloak + Elasticsearch + MinIO + Redis + Kafka

.PHONY: infra-status
infra-status: db-status keycloak-status elastic-status minio-status redis-status kafka-status ## Kiểm tra PostgreSQL + KeyCloak + Elasticsearch + MinIO + Redis + Kafka

.PHONY: infra-down
infra-down: kafka-down redis-down minio-down elastic-down keycloak-down db-down ## Dừng PostgreSQL + KeyCloak + Elasticsearch + MinIO + Redis + Kafka

# --- SQL Playground ---------------------------------------------

.PHONY: sql-1
sql-1: ## Nạp 01-setup-tables.sql (tạo bảng)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/01-setup-tables.sql

.PHONY: sql-2
sql-2: ## Nạp 02-insert-sample-data.sql (data mẫu)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/02-insert-sample-data.sql

.PHONY: sql-3
sql-3: ## Nạp 03-query-with-explain.sql (EXPLAIN ANALYZE)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/03-query-with-explain.sql

.PHONY: sql-4
sql-4: ## Nạp 04-index-comparison.sql (so sánh index)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/04-index-comparison.sql

.PHONY: sql-5
sql-5: ## Nạp 05-data-leakage-test.sql (test leakage)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/05-data-leakage-test.sql

.PHONY: sql-6
sql-6: ## Nạp 06-migration-lock-observation.sql (migration/lock lab)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/06-migration-lock-observation.sql

.PHONY: sql-7
sql-7: ## Nạp 07-index-query-patterns.sql (index/query pattern lab)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/07-index-query-patterns.sql

.PHONY: sql-9
sql-9: ## Setup 09-acid-isolation-observation.sql (phần quan sát cần 2 session thủ công)
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) < $(SQL_DIR)/09-acid-isolation-observation.sql

.PHONY: sql-all
sql-all: sql-1 sql-2 ## Nạp bảng + data mẫu (setup nhanh)

.PHONY: sql-reset
sql-reset: ## Xóa toàn bộ bảng rồi tạo lại từ đầu
	docker exec -i $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME) -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	@echo "Schema đã reset. Chạy 'make sql-all' để tạo lại."

# --- Spring Boot App (placeholder) ------------------------------

.PHONY: app-run
app-run: ## Chạy Spring Boot application
	cd $(APP_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; ./mvnw spring-boot:run

.PHONY: app-test
app-test: ## Chạy integration tests
	cd $(APP_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; ./mvnw test

# --- Help -------------------------------------------------------

.PHONY: help
help: ## Hiển thị danh sách lệnh
	@echo ""
	@echo "  Lab Code — Các lệnh thực hành Phase 1"
	@echo "  ======================================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
