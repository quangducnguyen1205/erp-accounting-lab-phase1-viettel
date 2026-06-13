import { useMemo, useState } from 'react';
import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';

const columns = [
  { key: 'code', label: 'Mã', render: (row) => <code>{row.code}</code> },
  { key: 'name', label: 'Tên' },
  { key: 'category', label: 'Loại', render: (row) => row.category ?? row.type ?? '(không trả về)' },
  { key: 'isActive', label: 'Trạng thái', render: (row) => <Badge tone={row.isActive ? 'success' : 'warning'}>{row.isActive ? 'Đang hoạt động' : 'Tạm ngưng'}</Badge> },
  { key: 'updatedAt', label: 'Cập nhật', render: (row) => row.updatedAt ?? row.createdAt ?? '(không trả về)' }
];

export function MasterDataScreen({
  rows,
  onLoad,
  loading,
  disabled,
  lookupCode,
  setLookupCode,
  lookupResult,
  onLookup,
  form,
  setForm,
  onCreate,
  onGenerateCode,
  postCreateHint,
  lastResult,
  userInfo
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  const isViewer = roles.includes('VIEWER') && !roles.includes('ACCOUNTANT') && !roles.includes('ADMIN');
  const filteredRows = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) {
      return rows;
    }

    return rows.filter((row) => {
      const code = String(row.code ?? '').toLowerCase();
      const name = String(row.name ?? '').toLowerCase();
      return code.includes(term) || name.includes(term);
    });
  }, [rows, searchTerm]);

  const updateField = (field) => (event) => {
    const value = field === 'isActive' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Danh mục</p>
        <h2>Dữ liệu danh mục</h2>
        <p>Tạo, tìm kiếm và xem dữ liệu dùng chung trong phạm vi tenant.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Danh sách bản ghi</h3>
            <p>Tải dữ liệu của tenant hiện tại, sau đó lọc theo mã hoặc tên.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải dữ liệu'}</button>
        </div>
        <label className="field-label table-filter">
          Tìm kiếm trong dữ liệu đã tải
          <input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Tìm theo mã hoặc tên" />
        </label>
        <DataTable
          columns={columns}
          rows={filteredRows}
          emptyTitle="Chưa có dữ liệu"
          emptyMessage={searchTerm ? 'Không có bản ghi đã tải nào khớp với tìm kiếm.' : 'Hãy tải dữ liệu sau khi đăng nhập. Tenant mới có thể chưa có dữ liệu.'}
        />
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Tìm theo mã</h3>
            <p>Tìm một bản ghi bằng mã nghiệp vụ.</p>
          </div>
          <Badge tone="teal">Lookup</Badge>
        </div>
        <form className="inline-form" onSubmit={onLookup}>
          <label>
            Mã
            <input value={lookupCode} onChange={(event) => setLookupCode(event.target.value)} required />
          </label>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tìm theo mã'}</button>
        </form>
        {lookupResult ? (
          <dl className="facts result-facts">
            <dt>ID</dt>
            <dd>{lookupResult.id}</dd>
            <dt>Mã</dt>
            <dd><code>{lookupResult.code}</code></dd>
            <dt>Tên</dt>
            <dd>{lookupResult.name}</dd>
            <dt>Loại</dt>
            <dd>{lookupResult.category}</dd>
          </dl>
        ) : (
          <EmptyState title="Chưa tìm kiếm">Cache behavior được kiểm chứng qua log/metric backend, không hiển thị bằng badge giả trong UI.</EmptyState>
        )}
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Tạo bản ghi</h3>
            <p>Thêm một bản ghi danh mục trong phạm vi tenant.</p>
          </div>
          <Badge tone="blue">ACCOUNTANT</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem dữ liệu nhưng không được tạo bản ghi mới. Nếu gửi form, backend sẽ trả HTTP 403.
          </Alert>
        )}
        <form className="form-grid" onSubmit={onCreate}>
          <label>
            Mã
            <input value={form.code} onChange={updateField('code')} required />
          </label>
          <label>
            Tên
            <input value={form.name} onChange={updateField('name')} required />
          </label>
          <label>
            Loại
            <input value={form.category} onChange={updateField('category')} required />
          </label>
          <label className="check-row">
            <input type="checkbox" checked={form.isActive} onChange={updateField('isActive')} />
            Đang hoạt động
          </label>
          <div className="form-actions">
            <button type="button" className="button-secondary" onClick={onGenerateCode}>Tạo mã mới</button>
            <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tạo...' : 'Tạo bản ghi'}</button>
          </div>
        </form>
        <p className="hint">Mã phải duy nhất trong từng tenant. Mã trùng sẽ trả `409 Conflict`.</p>
        {postCreateHint && <Alert tone="success" title="Tạo thành công">{postCreateHint}</Alert>}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Trạng thái lỗi</h3>
            <p>Các trạng thái này cần rõ ràng khi demo với mentor.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : 'neutral'}>{lastResult ? `HTTP ${lastResult.status}` : 'Chưa chạy'}</Badge>
        </div>
        <div className="state-grid">
          <Alert tone="danger" title="401 Unauthorized">Token bị thiếu, hết hạn hoặc không hợp lệ. Hãy đăng nhập lại.</Alert>
          <Alert tone="warning" title="403 Forbidden">Đã đăng nhập nhưng vai trò hiện tại không được phép tạo.</Alert>
          <Alert tone="danger" title="409 Conflict">Mã bị trùng trong cùng tenant. Đổi mã rồi thử lại.</Alert>
          <Alert tone="info" title="Service unavailable">Kong không gọi được upstream service. Kiểm tra process và port.</Alert>
        </div>
      </section>
    </div>
  );
}
