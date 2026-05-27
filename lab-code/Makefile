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
keycloak-up: ## Khởi chạy KeyCloak local cho auth mini-lab
	cd $(KEYCLOAK_DIR) && docker compose up -d

.PHONY: keycloak-down
keycloak-down: ## Dừng KeyCloak local cho auth mini-lab
	cd $(KEYCLOAK_DIR) && docker compose down

.PHONY: keycloak-status
keycloak-status: ## Kiểm tra KeyCloak local
	@docker ps --filter "name=viettel-keycloak" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

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

# --- Common dev infra ------------------------------------------
#
# Dùng target riêng khi chỉ học từng lab nhỏ:
# - db-up cho SQL/Spring Boot baseline
# - keycloak-up cho auth mini-lab
# - elastic-up cho search mini-lab
#
# Dùng infra-up khi cần demo đầy đủ hơn: PostgreSQL + Keycloak +
# Elasticsearch. App Spring Boot vẫn chạy riêng bằng `make app-run`.

.PHONY: infra-up
infra-up: db-up keycloak-up elastic-up ## Khởi chạy PostgreSQL + KeyCloak + Elasticsearch

.PHONY: infra-status
infra-status: db-status keycloak-status elastic-status ## Kiểm tra PostgreSQL + KeyCloak + Elasticsearch

.PHONY: infra-down
infra-down: elastic-down keycloak-down db-down ## Dừng PostgreSQL + KeyCloak + Elasticsearch

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
