INSERT INTO tenants (code, name)
VALUES ('VIETTEL', 'Tập đoàn Công nghiệp - Viễn thông Quân đội');

INSERT INTO tenants (code, name)
VALUES ('FPT', 'Công ty Cổ phần FPT');

-- Sau 2 lệnh trên:
--   Tenant 1 (id=1): VIETTEL
--   Tenant 2 (id=2): FPT
-- Các bước sau sẽ dùng id = 1 và id = 2.

-- >>> Tenant 1 (VIETTEL): 3 vật tư <<<
INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'LAPTOP-01', 'Laptop Dell Latitude 5540', 'Laptop');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'MOUSE-01', 'Chuột Logitech M330', 'Chuột');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'MOUSE-02', 'Chuột Logitech MX Master 3S', 'Chuột');

-- >>> Tenant 2 (FPT): 2 vật tư <<<
INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'MON-01', 'Màn hình Dell U2723QE 27 inch', 'Màn hình');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'MON-02', 'Màn hình LG 32UN880-B 32 inch', 'Màn hình');


INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'LAPTOP-01', 'Laptop HP EliteBook 840 G9', 'Laptop');