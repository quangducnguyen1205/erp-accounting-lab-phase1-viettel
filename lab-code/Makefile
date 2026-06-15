# ==============================================================
# Makefile — Final demo workflow cho Master Data Portal
# ==============================================================
#
# Main workflow cố tình ngắn để người mới không bị lạc giữa nhiều
# mini-lab lịch sử. Các target cũ vẫn được giữ trong Makefile.legacy:
#
#   make -f Makefile.legacy help
#   make -f Makefile.legacy <target>
#
# ==============================================================

.DEFAULT_GOAL := help

.PHONY: up
up: ## Start full local demo stack
	$(MAKE) -f Makefile.legacy demo-up

.PHONY: down
down: ## Stop full local demo stack, keep volumes and logs
	$(MAKE) -f Makefile.legacy demo-down

.PHONY: status
status: ## Show Docker services, Java process PIDs and log files
	$(MAKE) -f Makefile.legacy demo-status

.PHONY: clean-logs
clean-logs: ## Delete generated lab-code/logs/*.log, keep .gitkeep
	$(MAKE) -f Makefile.legacy logs-clean

.PHONY: info
info: ## Print final demo URLs and runtime model
	$(MAKE) -f Makefile.legacy demo-info

.PHONY: help
help: ## Show concise final demo commands
	@echo ""
	@echo "  Master Data Portal — final local demo"
	@echo "  ====================================="
	@echo ""
	@echo "  make up          Start Docker infra/tooling/web UI and Maven Java services"
	@echo "  make status      Show service status, PIDs and generated logs"
	@echo "  make down        Stop the demo without deleting volumes or logs"
	@echo "  make clean-logs  Delete generated logs/*.log when you are done inspecting"
	@echo "  make info        Print URLs and runtime model"
	@echo ""
	@echo "  Legacy mini-lab targets are still available:"
	@echo "    make -f Makefile.legacy help"
	@echo ""

# Backward-compatible aliases used by older docs/scripts.
.PHONY: demo-up
demo-up: up

.PHONY: demo-down
demo-down: down

.PHONY: demo-status
demo-status: status

.PHONY: demo-info
demo-info: info

.PHONY: logs-clean
logs-clean: clean-logs

.PHONY: logs-list
logs-list:
	$(MAKE) -f Makefile.legacy logs-list

.PHONY: web-ui-build
web-ui-build:
	$(MAKE) -f Makefile.legacy web-ui-build

.PHONY: keycloak-up
keycloak-up:
	$(MAKE) -f Makefile.legacy keycloak-up

.PHONY: keycloak-setup
keycloak-setup:
	$(MAKE) -f Makefile.legacy keycloak-setup

.PHONY: keycloak-down
keycloak-down:
	$(MAKE) -f Makefile.legacy keycloak-down

.PHONY: common-security-install
common-security-install:
	$(MAKE) -f Makefile.legacy common-security-install
