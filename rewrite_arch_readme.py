import re

with open('docs/07-architecture/README.md', 'r') as f:
    content = f.read()

# Remove Demo script link
content = re.sub(r'Demo script cuối Phase 1\.5:\n\[\.\./99-tong-ket/final-production-like-demo-script\.md\]\(\.\./99-tong-ket/final-production-like-demo-script\.md\)\.\n\n', '', content)

# Remove writing standard link
content = re.sub(r'Chuẩn chi tiết:\n\[\.\./99-tong-ket/theory-doc-writing-standard\.md\]\(\.\./99-tong-ket/theory-doc-writing-standard\.md\)\.\n\n', '', content)

with open('docs/07-architecture/README.md', 'w') as f:
    f.write(content)
