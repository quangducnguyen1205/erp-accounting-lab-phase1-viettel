import re

with open('README.md', 'r') as f:
    readme = f.read()

# 1. Update title and description
readme = re.sub(
    r'# Phase 1 - Kho kiến thức SaaS, Multi-tenant và Backend Database\n\nRepository này là nơi lưu trữ kiến thức đã được tổng hợp trong Phase 1 của quá trình học backend cho bài toán ERP/kế toán SaaS đa tenant\.',
    '# Phase 1 / Phase 1.5 - Technical Reference Project\n\nRepository này là kho lưu trữ kiến thức và mã nguồn tham chiếu tĩnh (closed technical reference) cho kiến trúc ERP/kế toán SaaS đa tenant Phase 1 / Phase 1.5.',
    readme
)

# 2. Update 'Trạng thái học hiện tại'
readme = readme.replace('## Trạng thái học hiện tại', '## Kiến trúc tham chiếu hiện tại')

# 3. Tree structure
readme = readme.replace('│   ├── 00-gioi-thieu/\n', '')
readme = readme.replace('│   ├── 07-architecture/\n│   └── 99-tong-ket/', '│   └── 07-architecture/')
readme = readme.replace('├── reports/\n│   └── latex/\n├── presentation-notes/\n', '')

# 4. Remove content bullets
readme = re.sub(r'- Báo cáo LaTeX dạng nguồn `\.tex`\.\n', '', readme)
readme = re.sub(r'- Tài liệu thuyết trình dạng markdown có sơ đồ, bảng và ghi chú nói ngắn\.\n', '', readme)

# 5. Remove 'Báo cáo' and 'Tài liệu thuyết trình' sections
readme = re.sub(r'## Báo cáo\n\n.*?(?=## Quyết định về code demo)', '', readme, flags=re.DOTALL)
# Wait, I also need to remove ## Tài liệu thuyết trình which is before ## Quyết định về code demo
# The regex `## Báo cáo\n\n.*?(?=## Quyết định về code demo)` will remove both if they are contiguous. Let's verify they are contiguous.
# Looking at README.md, they are.

# 6. Update 'Điểm điều hướng chính:'
nav_replacement = """Điểm điều hướng chính:

- `docs/README.md`: Bản đồ chỉ dẫn tài liệu (reading map) chính của dự án.
"""
readme = re.sub(r'Điểm điều hướng chính:\n\n.*?(?=## Chạy final demo local)', nav_replacement + '\n', readme, flags=re.DOTALL)

# 7. Remove 'Workflow học code dự kiến'
readme = re.sub(r'Workflow học code dự kiến:\n\n1\. Tự implement task code được giao trước\.\n2\. Sau đó mới nhờ Agent review, chỉ ra lỗi, đề xuất sửa và giải thích\.\n3\. Mục tiêu là học bằng cách tự viết code, không phụ thuộc vào tự động hóa ngay từ đầu\.\n', '', readme)

with open('README.md', 'w') as f:
    f.write(readme)

with open('docs/README.md', 'r') as f:
    docs_readme = f.read()

# docs/README.md changes
# 1. Update intro
docs_intro = """# Mục lục tài liệu

## Mục tiêu

Thư mục `docs/` là source of truth cho kiến thức Phase 1 và Phase 1.5 đã được chuẩn hóa. Bản đồ tài liệu này (reading map) được dùng để tra cứu các chủ đề kỹ thuật và kiến trúc của repository.

## Thứ tự đọc đề xuất

1. [01-saas/](01-saas/) - SaaS, cloud, subscription, on-premise.
2. [02-multi-tenant/](02-multi-tenant/) - multi-tenant, tenant isolation và trade-off.
3. [03-backend-database-mo-rong/](03-backend-database-mo-rong/) - PostgreSQL, index, migration, transaction.
4. [04-spring-boot/](04-spring-boot/) - Spring Boot, JPA, filter, repository/service/controller, test.
5. [05-security/](05-security/) - JWT, Spring Security Resource Server, Keycloak/OIDC/RBAC.
6. [07-architecture/](07-architecture/) - gateway, service boundary, Kafka, MinIO, Elasticsearch, Redis, observability.
7. [06-frontend/](06-frontend/) - React Web UI demo `Master Data Portal`."""

docs_readme = re.sub(r'# Mục lục tài liệu\n\n## Mục tiêu\n\n.*?(?=## Nội dung cốt lõi và nội dung mở rộng)', docs_intro + '\n\n', docs_readme, flags=re.DOTALL)

# 2. Update table to remove '99-tong-ket'
# Replace `| Mở rộng sau | Chưa làm |` with just the previous rows? Wait, 99-tong-ket is not in the table, it was `| 9. [99-tong-ket/](99-tong-ket/) - tổng kết, demo script cuối và giới hạn production-like.` which is already handled by the replacement above.
# Wait, "Loki/Grafana/Alloy, Kafka UI, Kong" - "Tooling production-like để quan sát hệ thống local" is fine.

# 3. Remove 99-tong-ket templates links
docs_readme = re.sub(r'5\. Summary sau khi xong: `docs/99-tong-ket/nhung-gi-da-nam-duoc\.md`\.\n\nTemplate chung: \[99-tong-ket/technology-mini-lab-template\.md\]\(99-tong-ket/technology-mini-lab-template\.md\)\.\n\nChuẩn viết theory doc chi tiết:\n\[99-tong-ket/theory-doc-writing-standard\.md\]\(99-tong-ket/theory-doc-writing-standard\.md\)\.\n\nDemo script cuối Phase 1\.5:\n\[99-tong-ket/final-production-like-demo-script\.md\]\(99-tong-ket/final-production-like-demo-script\.md\)\.\n\nTài liệu rehearsal và Q&A:\n\n- \[99-tong-ket/final-demo-evidence-checklist\.md\]\(99-tong-ket/final-demo-evidence-checklist\.md\)\n- \[99-tong-ket/final-demo-operational-qna\.md\]\(99-tong-ket/final-demo-operational-qna\.md\)\n- \[99-tong-ket/final-demo-rehearsal-transcript\.md\]\(99-tong-ket/final-demo-rehearsal-transcript\.md\)\n\nPlan Phase 1\.5:\n\[99-tong-ket/phase1-5-production-like-demo-plan\.md\]\(99-tong-ket/phase1-5-production-like-demo-plan\.md\)\.\n\n', '', docs_readme)

with open('docs/README.md', 'w') as f:
    f.write(docs_readme)

