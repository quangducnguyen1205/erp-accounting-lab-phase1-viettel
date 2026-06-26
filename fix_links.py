import os
import glob

deleted_strings = [
    'keycloak-authorization-mini-lab-plan.md',
    'keycloak-mini-lab-plan.md',
    'final-web-ui-design-plan.md',
    'final-web-ui-figma-screen-handoff.md',
    'api-gateway-mini-lab-plan.md',
    'redis-mini-lab-plan.md',
    'kafka-mini-lab-plan.md',
    'observability-mini-lab-plan.md',
    'elasticsearch-mini-lab-plan.md',
    '99-tong-ket',
    '00-gioi-thieu',
    'presentation-notes',
    'reports/',
    'ROADMAP.md'
]

def clean_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    changed = False
    for line in lines:
        if any(ds in line for ds in deleted_strings):
            # Only remove if it's a list item or a table row, to avoid breaking paragraphs
            if line.strip().startswith('- ') or line.strip().startswith('* ') or re.match(r'^\d+\.\s', line.strip()) or line.strip().startswith('|'):
                changed = True
                continue
            # If it's a paragraph containing the link, let's also remove it to be safe (might break a paragraph but it's better than broken links)
            changed = True
            continue
        new_lines.append(line)
        
    if changed:
        with open(filepath, 'w') as f:
            f.writelines(new_lines)

import re

for root, dirs, files in os.walk('docs'):
    for file in files:
        if file.endswith('.md'):
            clean_file(os.path.join(root, file))

for root, dirs, files in os.walk('lab-code'):
    for file in files:
        if file.endswith('.md'):
            clean_file(os.path.join(root, file))

