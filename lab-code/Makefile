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
FILE_SERVICE_DIR := file-service
SEARCH_SERVICE_DIR := search-service
COMMON_SECURITY_DIR := common-security
OBSERVABILITY_DIR := observability-lab
LOKI_DIR       := loki-lab
GATEWAY_DIR    := gateway-demo
KONG_DIR       := kong-gateway-lab
WEB_UI_DIR     := web-ui-demo
LOG_DIR        := logs
PID_DIR        := .pids

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

.PHONY: common-security-validate
common-security-validate: ## Validate shared common-security Maven module
	mvn -f $(COMMON_SECURITY_DIR)/pom.xml validate

.PHONY: common-security-install
common-security-install: ## Install shared common-security module vào local Maven repo
	mvn -f $(COMMON_SECURITY_DIR)/pom.xml -DskipTests install

.PHONY: audit-log-validate
audit-log-validate: common-security-install ## Validate audit-log-service Maven project
	mvn -f $(AUDIT_LOG_DIR)/pom.xml validate

.PHONY: audit-log-run
audit-log-run: common-security-install ## Chạy audit-log-service bằng Maven ở port 8082
	cd $(AUDIT_LOG_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; mvn spring-boot:run

.PHONY: audit-log-run-logs
audit-log-run-logs: common-security-install logs-dir ## Chạy audit-log-service bằng Maven và ghi file log để Loki/Alloy tail
	@rm -f $(LOG_DIR)/audit-log-service.log
	cd $(AUDIT_LOG_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
	LOGGING_FILE_NAME=$${LOGGING_FILE_NAME:-../$(LOG_DIR)/audit-log-service.log} \
	mvn spring-boot:run

.PHONY: audit-log-status
audit-log-status: ## Kiểm tra audit-log-service host health endpoint
	curl -s http://localhost:8082/actuator/health

.PHONY: audit-log-info
audit-log-info: ## In hướng dẫn nhanh cho audit-log-service
	@echo ""
	@echo "Audit Log Service nằm ở $(AUDIT_LOG_DIR)/"
	@echo "Workflow chính là Maven/IntelliJ host-run."
	@echo ""
	@echo "Chạy:"
	@echo "  make audit-log-run"
	@echo "  make audit-log-run-logs   # ghi $(LOG_DIR)/audit-log-service.log cho Loki"
	@echo "  make audit-log-status"
	@echo ""
	@echo "URLs:"
	@echo "  Direct service: http://localhost:8082"
	@echo "  Through Kong:   http://localhost:18000/api/audit-events"
	@echo ""
	@echo "Ghi chú: cần PostgreSQL, Keycloak và Kafka chạy trước khi demo đầy đủ."

# --- File Service mini-lab -------------------------------------

.PHONY: file-validate
file-validate: common-security-install ## Validate file-service Maven project
	mvn -f $(FILE_SERVICE_DIR)/pom.xml validate

.PHONY: file-run
file-run: common-security-install ## Chạy file-service bằng Maven ở port 8083
	cd $(FILE_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; mvn spring-boot:run

.PHONY: file-run-logs
file-run-logs: common-security-install logs-dir ## Chạy file-service bằng Maven và ghi file log để Loki/Alloy tail
	@rm -f $(LOG_DIR)/file-service.log
	cd $(FILE_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
	LOGGING_FILE_NAME=$${LOGGING_FILE_NAME:-../$(LOG_DIR)/file-service.log} \
	mvn spring-boot:run

.PHONY: file-status
file-status: ## Kiểm tra file-service host health endpoint
	curl -s http://localhost:8083/actuator/health

.PHONY: file-info
file-info: ## In hướng dẫn nhanh cho file-service
	@echo ""
	@echo "File Service nằm ở $(FILE_SERVICE_DIR)/"
	@echo "Workflow chính là Maven/IntelliJ host-run."
	@echo ""
	@echo "Chạy:"
	@echo "  make file-run"
	@echo "  make file-run-logs   # ghi $(LOG_DIR)/file-service.log cho Loki"
	@echo "  make file-status"
	@echo ""
	@echo "URLs:"
	@echo "  Direct service: http://localhost:8083"
	@echo "  Through Kong:   http://localhost:18000/api/files"
	@echo ""
	@echo "Ghi chú: cần PostgreSQL, Keycloak, MinIO và Kong chạy trước khi demo đầy đủ."

# --- Search Service mini-lab -----------------------------------

.PHONY: search-validate
search-validate: common-security-install ## Validate search-service Maven project
	mvn -f $(SEARCH_SERVICE_DIR)/pom.xml validate

.PHONY: search-run
search-run: common-security-install ## Chạy search-service bằng Maven ở port 8084
	cd $(SEARCH_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; mvn spring-boot:run

.PHONY: search-run-logs
search-run-logs: common-security-install logs-dir ## Chạy search-service bằng Maven và ghi file log để Loki/Alloy tail
	@rm -f $(LOG_DIR)/search-service.log
	cd $(SEARCH_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
	LOGGING_FILE_NAME=$${LOGGING_FILE_NAME:-../$(LOG_DIR)/search-service.log} \
	mvn spring-boot:run

.PHONY: search-status
search-status: ## Kiểm tra search-service host health endpoint
	curl -s http://localhost:8084/actuator/health

.PHONY: search-info
search-info: ## In hướng dẫn nhanh cho search-service
	@echo ""
	@echo "Search Service nằm ở $(SEARCH_SERVICE_DIR)/"
	@echo "Workflow chính là Maven/IntelliJ host-run."
	@echo ""
	@echo "Chạy:"
	@echo "  make search-run"
	@echo "  make search-run-logs   # ghi $(LOG_DIR)/search-service.log cho Loki"
	@echo "  make search-status"
	@echo ""
	@echo "URLs:"
	@echo "  Direct service: http://localhost:8084"
	@echo "  Through Kong:   http://localhost:18000/api/search/master-data?keyword=..."
	@echo ""
	@echo "Ghi chú: cần Keycloak, Kafka, Elasticsearch và Kong chạy trước khi demo đầy đủ."

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
loki-up: logs-dir ## Khởi chạy Loki + Alloy + Grafana local cho log aggregation
	cd $(LOKI_DIR) && docker compose up -d

.PHONY: logs-dir
logs-dir: ## Tạo thư mục log local để Alloy tail host-run app logs
	@mkdir -p $(LOG_DIR)
	@touch $(LOG_DIR)/.gitkeep

.PHONY: pids-dir
pids-dir: ## Tạo thư mục PID local cho one-command demo runner
	@mkdir -p $(PID_DIR)

.PHONY: logs-clean
logs-clean: logs-dir ## Xóa generated *.log local, giữ lại logs/.gitkeep
	@find $(LOG_DIR) -maxdepth 1 -type f -name '*.log' -delete
	@touch $(LOG_DIR)/.gitkeep
	@echo "Removed generated log files in $(LOG_DIR)/"

.PHONY: logs-list
logs-list: logs-dir ## Liệt kê log files local và kích thước hiện tại
	@find $(LOG_DIR) -maxdepth 1 -type f -name '*.log' -exec ls -lh {} \;

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
	@echo "Ghi chú: Docker logs đi qua Docker collector; Java host Maven logs đi qua $(LOG_DIR)/*.log."
	@echo "       Chạy tenant-demo bằng make app-run-logs và audit-log-service bằng make audit-log-run-logs để Loki thấy log file."
	@echo "       Chạy file-service bằng make file-run-logs để Loki thấy file-service log."
	@echo "       Chạy search-service bằng make search-run-logs để Loki thấy search-service log."

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

# --- Full local demo runner ------------------------------------
#
# Java backend services still run on the host via Maven/IntelliJ by
# default. These targets are only convenience wrappers for a local demo:
# Docker starts infra/tooling/web UI, then Maven services run in the
# background with file logging enabled for Loki.

.PHONY: demo-info
demo-info: ## In hướng dẫn one-command local demo
	@echo ""
	@echo "Full local demo runner"
	@echo "======================"
	@echo ""
	@echo "Start:"
	@echo "  make demo-up"
	@echo ""
	@echo "Status:"
	@echo "  make demo-status"
	@echo ""
	@echo "Stop:"
	@echo "  make demo-down"
	@echo "  make logs-clean   # optional, only when you want to delete generated *.log files"
	@echo ""
	@echo "What runs through Docker:"
	@echo "  PostgreSQL, Keycloak, Redis, Kafka, Kafka UI, MinIO, Elasticsearch, Kong, Loki/Grafana/Alloy, React Web UI"
	@echo ""
	@echo "What runs through Maven on host:"
	@echo "  tenant-demo          -> $(LOG_DIR)/tenant-demo.log"
	@echo "  audit-log-service    -> $(LOG_DIR)/audit-log-service.log"
	@echo "  file-service         -> $(LOG_DIR)/file-service.log"
	@echo "  search-service       -> $(LOG_DIR)/search-service.log"
	@echo ""
	@echo "URLs:"
	@echo "  Web UI:        http://localhost:5173"
	@echo "  Keycloak:      http://localhost:18080"
	@echo "  Kong proxy:    http://localhost:18000"
	@echo "  Kafka UI:      http://localhost:18082"
	@echo "  Grafana Loki:  http://localhost:13001"
	@echo ""
	@echo "For IntelliJ development, you can still run Java services manually instead of using this runner."

.PHONY: demo-up
demo-up: logs-dir pids-dir common-security-install ## Khởi chạy full local demo stack: Docker infra/tooling + Java Maven services background
	@echo "Starting Docker infra/tooling..."
	$(MAKE) db-up
	$(MAKE) keycloak-up
	$(MAKE) keycloak-setup
	$(MAKE) redis-up
	$(MAKE) kafka-up
	$(MAKE) minio-up
	$(MAKE) elastic-up
	$(MAKE) kong-up
	$(MAKE) kafka-ui-up
	$(MAKE) loki-up
	$(MAKE) web-ui-up
	@echo "Starting Java services with file logging..."
	@rm -f $(LOG_DIR)/tenant-demo.log $(LOG_DIR)/audit-log-service.log $(LOG_DIR)/file-service.log $(LOG_DIR)/search-service.log
	@if [ -f $(PID_DIR)/tenant-demo.pid ] && kill -0 $$(cat $(PID_DIR)/tenant-demo.pid) 2>/dev/null; then \
		echo "tenant-demo already running with PID $$(cat $(PID_DIR)/tenant-demo.pid)"; \
	else \
		nohup sh -c 'cd $(APP_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
			APP_AUTH_MODE=keycloak \
			APP_CACHE_ENABLED=true \
			APP_MESSAGING_ENABLED=true \
			KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
			LOGGING_FILE_NAME=../$(LOG_DIR)/tenant-demo.log \
			exec ./mvnw spring-boot:run' > $(LOG_DIR)/tenant-demo-runner.log 2>&1 & \
		echo $$! > $(PID_DIR)/tenant-demo.pid; \
		echo "tenant-demo starting with PID $$(cat $(PID_DIR)/tenant-demo.pid). Logs: $(LOG_DIR)/tenant-demo.log"; \
	fi
	@if [ -f $(PID_DIR)/audit-log-service.pid ] && kill -0 $$(cat $(PID_DIR)/audit-log-service.pid) 2>/dev/null; then \
		echo "audit-log-service already running with PID $$(cat $(PID_DIR)/audit-log-service.pid)"; \
	else \
		nohup sh -c 'cd $(AUDIT_LOG_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
			LOGGING_FILE_NAME=../$(LOG_DIR)/audit-log-service.log \
			exec mvn spring-boot:run' > $(LOG_DIR)/audit-log-service-runner.log 2>&1 & \
		echo $$! > $(PID_DIR)/audit-log-service.pid; \
		echo "audit-log-service starting with PID $$(cat $(PID_DIR)/audit-log-service.pid). Logs: $(LOG_DIR)/audit-log-service.log"; \
	fi
	@if [ -f $(PID_DIR)/file-service.pid ] && kill -0 $$(cat $(PID_DIR)/file-service.pid) 2>/dev/null; then \
		echo "file-service already running with PID $$(cat $(PID_DIR)/file-service.pid)"; \
	else \
		nohup sh -c 'cd $(FILE_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
			LOGGING_FILE_NAME=../$(LOG_DIR)/file-service.log \
			exec mvn spring-boot:run' > $(LOG_DIR)/file-service-runner.log 2>&1 & \
		echo $$! > $(PID_DIR)/file-service.pid; \
		echo "file-service starting with PID $$(cat $(PID_DIR)/file-service.pid). Logs: $(LOG_DIR)/file-service.log"; \
	fi
	@if [ -f $(PID_DIR)/search-service.pid ] && kill -0 $$(cat $(PID_DIR)/search-service.pid) 2>/dev/null; then \
		echo "search-service already running with PID $$(cat $(PID_DIR)/search-service.pid)"; \
	else \
		nohup sh -c 'cd $(SEARCH_SERVICE_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
			LOGGING_FILE_NAME=../$(LOG_DIR)/search-service.log \
			exec mvn spring-boot:run' > $(LOG_DIR)/search-service-runner.log 2>&1 & \
		echo $$! > $(PID_DIR)/search-service.pid; \
		echo "search-service starting with PID $$(cat $(PID_DIR)/search-service.pid). Logs: $(LOG_DIR)/search-service.log"; \
	fi
	@echo ""
	@echo "Demo stack is starting. Give Java services a moment, then open http://localhost:5173"
	@echo "If a Java service fails, inspect $(LOG_DIR)/*-runner.log and $(LOG_DIR)/*.log"

.PHONY: demo-status
demo-status: ## Kiểm tra trạng thái full local demo stack
	@echo ""
	@echo "Java service processes:"
	@if [ -f $(PID_DIR)/tenant-demo.pid ] && kill -0 $$(cat $(PID_DIR)/tenant-demo.pid) 2>/dev/null; then \
		echo "  tenant-demo: running PID $$(cat $(PID_DIR)/tenant-demo.pid)"; \
	else \
		echo "  tenant-demo: not running"; \
	fi
	@if [ -f $(PID_DIR)/audit-log-service.pid ] && kill -0 $$(cat $(PID_DIR)/audit-log-service.pid) 2>/dev/null; then \
		echo "  audit-log-service: running PID $$(cat $(PID_DIR)/audit-log-service.pid)"; \
	else \
		echo "  audit-log-service: not running"; \
	fi
	@if [ -f $(PID_DIR)/file-service.pid ] && kill -0 $$(cat $(PID_DIR)/file-service.pid) 2>/dev/null; then \
		echo "  file-service: running PID $$(cat $(PID_DIR)/file-service.pid)"; \
	else \
		echo "  file-service: not running"; \
	fi
	@if [ -f $(PID_DIR)/search-service.pid ] && kill -0 $$(cat $(PID_DIR)/search-service.pid) 2>/dev/null; then \
		echo "  search-service: running PID $$(cat $(PID_DIR)/search-service.pid)"; \
	else \
		echo "  search-service: not running"; \
	fi
	@echo ""
	@echo "Docker services:"
	@docker ps --filter "name=erp-postgres" --filter "name=viettel-keycloak" --filter "name=viettel-redis" --filter "name=viettel-kafka" --filter "name=viettel-kafka-ui" --filter "name=viettel-minio" --filter "name=viettel-elasticsearch" --filter "name=viettel-kong" --filter "name=viettel-loki" --filter "name=viettel-alloy" --filter "name=viettel-loki-grafana" --filter "name=viettel-web-ui-demo" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
	@echo ""
	@$(MAKE) logs-list

.PHONY: demo-down
demo-down: ## Dừng full local demo stack, giữ volumes và logs
	@echo "Stopping Java service processes..."
	@if [ -f $(PID_DIR)/tenant-demo.pid ] && kill -0 $$(cat $(PID_DIR)/tenant-demo.pid) 2>/dev/null; then \
		kill $$(cat $(PID_DIR)/tenant-demo.pid); \
		echo "Stopped tenant-demo PID $$(cat $(PID_DIR)/tenant-demo.pid)"; \
	else \
		echo "tenant-demo was not running"; \
	fi
	@rm -f $(PID_DIR)/tenant-demo.pid
	@if [ -f $(PID_DIR)/audit-log-service.pid ] && kill -0 $$(cat $(PID_DIR)/audit-log-service.pid) 2>/dev/null; then \
		kill $$(cat $(PID_DIR)/audit-log-service.pid); \
		echo "Stopped audit-log-service PID $$(cat $(PID_DIR)/audit-log-service.pid)"; \
	else \
		echo "audit-log-service was not running"; \
	fi
	@rm -f $(PID_DIR)/audit-log-service.pid
	@if [ -f $(PID_DIR)/file-service.pid ] && kill -0 $$(cat $(PID_DIR)/file-service.pid) 2>/dev/null; then \
		kill $$(cat $(PID_DIR)/file-service.pid); \
		echo "Stopped file-service PID $$(cat $(PID_DIR)/file-service.pid)"; \
	else \
		echo "file-service was not running"; \
	fi
	@rm -f $(PID_DIR)/file-service.pid
	@if [ -f $(PID_DIR)/search-service.pid ] && kill -0 $$(cat $(PID_DIR)/search-service.pid) 2>/dev/null; then \
		kill $$(cat $(PID_DIR)/search-service.pid); \
		echo "Stopped search-service PID $$(cat $(PID_DIR)/search-service.pid)"; \
	else \
		echo "search-service was not running"; \
	fi
	@rm -f $(PID_DIR)/search-service.pid
	@echo "Stopping Docker infra/tooling..."
	-$(MAKE) web-ui-down
	-$(MAKE) kong-down
	-$(MAKE) kafka-ui-down
	-$(MAKE) loki-down
	-$(MAKE) kafka-down
	-$(MAKE) elastic-down
	-$(MAKE) minio-down
	-$(MAKE) redis-down
	-$(MAKE) keycloak-down
	-$(MAKE) db-down
	@echo ""
	@echo "Demo stack stopped. Logs are kept under $(LOG_DIR)/. Run 'make logs-clean' when you want to delete generated logs."

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
# - audit-log-run / audit-log-run-logs cho service split đầu tiên và Kafka cross-service flow
# - file-run / file-run-logs cho MinIO upload/download service
# - search-run / search-run-logs cho Elasticsearch projection service
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
# Java services chạy Maven/IntelliJ-first; dùng các target `*-run-logs`
# khi muốn Loki tail file log của tenant-demo, audit, file và search.

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
app-run: common-security-install ## Chạy Spring Boot application
	cd $(APP_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; ./mvnw spring-boot:run

.PHONY: app-run-logs
app-run-logs: common-security-install logs-dir ## Chạy tenant-demo Keycloak+Kafka mode và ghi file log để Loki/Alloy tail
	@rm -f $(LOG_DIR)/tenant-demo.log
	cd $(APP_DIR) && if [ -f .env ]; then set -a; . ./.env; set +a; fi; \
	APP_AUTH_MODE=$${APP_AUTH_MODE:-keycloak} \
	APP_MESSAGING_ENABLED=$${APP_MESSAGING_ENABLED:-true} \
	KAFKA_BOOTSTRAP_SERVERS=$${KAFKA_BOOTSTRAP_SERVERS:-localhost:19092} \
	LOGGING_FILE_NAME=$${LOGGING_FILE_NAME:-../$(LOG_DIR)/tenant-demo.log} \
	./mvnw spring-boot:run

.PHONY: app-test
app-test: common-security-install ## Chạy integration tests
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
