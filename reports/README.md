# Báo cáo

Thư mục này lưu nguồn báo cáo tiến độ học tập.

## Quy ước

- Báo cáo chính được viết bằng LaTeX trong `reports/latex/`.
- File `.tex` được commit.
- File PDF và artifact build của LaTeX không được commit mặc định.

## Compile báo cáo

```bash
cd reports/latex
xelatex bao-cao-saas-multi-tenant.tex
```

Nếu cần publish PDF trong tương lai, có thể chủ động force-add file PDF sau khi kiểm tra nội dung.
