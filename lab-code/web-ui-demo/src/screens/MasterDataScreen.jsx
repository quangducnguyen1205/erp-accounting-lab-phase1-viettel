import { useMemo, useState } from 'react';
import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';

export function MasterDataScreen({
  rows,
  onLoad,
  loading,
  disabled,
  lookupCode,
  setLookupCode,
  lookupResult,
  onLookup,
  searchKeyword,
  setSearchKeyword,
  searchResults,
  searchLoaded,
  onBackendSearch,
  form,
  setForm,
  onCreate,
  onUpdate,
  onDeactivate,
  onGenerateCode,
  postCreateHint,
  userInfo
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [editingRow, setEditingRow] = useState(null);
  const [editForm, setEditForm] = useState({ code: '', name: '', category: '', isActive: true });
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

  const updateEditField = (field) => (event) => {
    const value = field === 'isActive' ? event.target.checked : event.target.value;
    setEditForm((current) => ({ ...current, [field]: value }));
  };

  function startEdit(row) {
    setEditingRow(row);
    setEditForm({
      code: row.code ?? '',
      name: row.name ?? '',
      category: row.category ?? row.type ?? '',
      isActive: row.isActive ?? row.active ?? true
    });
  }

  async function submitEdit(event) {
    event.preventDefault();
    if (!editingRow) {
      return;
    }
    const result = await onUpdate(editingRow, editForm);
    if (result?.ok) {
      setEditingRow(null);
    }
  }

  async function deactivate(row) {
    const confirmed = window.confirm(`Tạm ngưng bản ghi ${row.code}?`);
    if (!confirmed) {
      return;
    }
    const result = await onDeactivate(row);
    if (result?.ok && editingRow?.id === row.id) {
      setEditingRow(null);
    }
  }

  const columns = [
    { key: 'code', label: 'Mã', render: (row) => <code>{row.code}</code> },
    { key: 'name', label: 'Tên' },
    { key: 'category', label: 'Loại', render: (row) => row.category ?? row.type ?? '(không trả về)' },
    {
      key: 'isActive',
      label: 'Trạng thái',
      render: (row) => {
        const active = row.isActive ?? row.active ?? false;
        return <Badge tone={active ? 'success' : 'warning'}>{active ? 'Đang hoạt động' : 'Tạm ngưng'}</Badge>;
      }
    },
    { key: 'updatedAt', label: 'Cập nhật', render: (row) => row.updatedAt ?? row.createdAt ?? '(không trả về)' },
    {
      key: 'actions',
      label: 'Thao tác',
      render: (row) => (
        <div className="row-actions">
          <button type="button" className="button-secondary" onClick={() => startEdit(row)} disabled={disabled || loading}>
            Sửa
          </button>
          <button type="button" className="button-danger" onClick={() => deactivate(row)} disabled={disabled || loading}>
            Tạm ngưng
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="screen-grid master-data-layout">
      <section className="screen-heading">
        <p className="eyebrow">Danh mục</p>
        <h2>Dữ liệu danh mục</h2>
        <p>Quản lý bản ghi dùng chung trong tenant: tải danh sách, tạo mới, chỉnh sửa và tạm ngưng khi không còn sử dụng.</p>
      </section>

      <section className="panel panel-span-3 master-list-panel">
        <div className="panel-heading list-heading">
          <div>
            <h3>Danh sách bản ghi</h3>
            <p>Đây là vùng làm việc chính. Dữ liệu chỉ thuộc tenant hiện tại.</p>
          </div>
          <div className="toolbar-actions">
            <button type="button" className="button-secondary" onClick={onGenerateCode}>Tạo mã mới</button>
            <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải dữ liệu'}</button>
          </div>
        </div>
        <div className="list-toolbar">
          <label className="field-label table-filter">
            Lọc nhanh
            <input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Tìm theo mã hoặc tên" />
          </label>
          <span className="list-count">{filteredRows.length} bản ghi đang hiển thị</span>
        </div>
        <DataTable
          columns={columns}
          rows={filteredRows}
          emptyTitle="Chưa có dữ liệu"
          emptyMessage={searchTerm ? 'Không có bản ghi đã tải nào khớp với tìm kiếm.' : 'Hãy tải dữ liệu sau khi đăng nhập. Tenant mới có thể chưa có dữ liệu.'}
        />
      </section>

      {editingRow && (
        <section className="panel panel-span-3 edit-panel">
          <div className="panel-heading">
            <div>
              <h3>Sửa bản ghi</h3>
              <p>Cập nhật mã, tên, loại hoặc trạng thái của bản ghi đang chọn.</p>
            </div>
            <Badge tone="blue">Đang sửa</Badge>
          </div>
          <form className="form-grid" onSubmit={submitEdit}>
            <label>
              Mã
              <input value={editForm.code} onChange={updateEditField('code')} required />
            </label>
            <label>
              Tên
              <input value={editForm.name} onChange={updateEditField('name')} required />
            </label>
            <label>
              Loại
              <input value={editForm.category} onChange={updateEditField('category')} required />
            </label>
            <label className="check-row">
              <input type="checkbox" checked={editForm.isActive} onChange={updateEditField('isActive')} />
              Đang hoạt động
            </label>
            <div className="form-actions">
              <button type="button" className="button-secondary" onClick={() => setEditingRow(null)}>Hủy</button>
              <button type="submit" disabled={disabled || loading}>{loading ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
            </div>
          </form>
        </section>
      )}

      <section className="panel create-panel">
        <div className="panel-heading">
          <div>
            <h3>Tạo bản ghi</h3>
            <p>Thêm dữ liệu danh mục mới trong tenant.</p>
          </div>
          <Badge tone="blue">ACCOUNTANT</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem dữ liệu nhưng không được tạo bản ghi mới. Nếu vẫn gửi form, hệ thống sẽ từ chối thao tác.
          </Alert>
        )}
        <form className="form-grid compact-form" onSubmit={onCreate}>
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
        <p className="hint">Mã chỉ cần duy nhất với bản ghi đang hoạt động trong từng tenant.</p>
        {postCreateHint && <Alert tone="success" title="Thao tác thành công">{postCreateHint}</Alert>}
      </section>

      <details className="panel panel-span-2 lookup-tools" open>
        <summary>
          <span>
            <strong>Công cụ tra cứu</strong>
            <small>Tra cứu theo mã hoặc tìm kiếm nâng cao khi cần đối chiếu dữ liệu.</small>
          </span>
        </summary>

        <div className="secondary-tool-grid">
          <section className="tool-section">
            <div className="panel-heading">
              <div>
                <h3>Tìm theo mã</h3>
                <p>Tìm một bản ghi bằng mã nghiệp vụ.</p>
              </div>
              <Badge tone="teal">Tra cứu</Badge>
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
              <EmptyState title="Chưa tìm kiếm">Nhập một mã nghiệp vụ để tải chi tiết bản ghi.</EmptyState>
            )}
          </section>

          <section className="tool-section">
            <div className="panel-heading">
              <div>
                <h3>Tìm kiếm nâng cao</h3>
                <p>Tìm theo mã, tên hoặc loại. Kết quả mới cập nhật có thể trễ vài giây.</p>
              </div>
              <Badge tone="teal">Tìm kiếm</Badge>
            </div>
            <form className="inline-form" onSubmit={onBackendSearch}>
              <label>
                Từ khóa
                <input value={searchKeyword} onChange={(event) => setSearchKeyword(event.target.value)} placeholder="Nhập mã, tên hoặc loại" required />
              </label>
              <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tìm...' : 'Tìm kiếm'}</button>
            </form>
            {searchLoaded ? (
              <DataTable
                columns={columns.filter((column) => column.key !== 'actions')}
                rows={searchResults}
                emptyTitle="Không có kết quả"
                emptyMessage="Nếu vừa tạo/sửa bản ghi, chờ vài giây rồi thử lại."
              />
            ) : (
              <EmptyState title="Chưa tìm kiếm nâng cao">
                Nhập từ khóa để tìm trong dữ liệu danh mục.
              </EmptyState>
            )}
          </section>
        </div>
      </details>
    </div>
  );
}
