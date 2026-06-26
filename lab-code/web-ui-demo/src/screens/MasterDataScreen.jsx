import { useMemo, useState } from 'react';
import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { Drawer } from '../components/Drawer';
import { formatDateTime } from '../utils/formatDateTime';

export function MasterDataScreen({
  rows,
  onLoad,
  loading,
  disabled,
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
  const [createOpen, setCreateOpen] = useState(false);

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

  async function submitCreate(event) {
    event.preventDefault();
    const result = await onCreate(event);
    if (result?.ok) {
      setCreateOpen(false);
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
    { key: 'code', label: 'Mã danh mục', render: (row) => <code>{row.code}</code> },
    { key: 'name', label: 'Tên danh mục' },
    { key: 'category', label: 'Nhóm danh mục', render: (row) => row.category ?? row.type ?? '(không trả về)' },
    {
      key: 'isActive',
      label: 'Trạng thái',
      render: (row) => {
        const active = row.isActive ?? row.active ?? false;
        return <Badge tone={active ? 'success' : 'warning'}>{active ? 'Đang hoạt động' : 'Tạm ngưng'}</Badge>;
      }
    },
    { key: 'updatedAt', label: 'Cập nhật', render: (row) => formatDateTime(row.updatedAt ?? row.createdAt) },
    {
      key: 'actions',
      label: 'Thao tác',
      render: (row) => (
        <div className="row-actions">
          <button type="button" className="button-secondary button-sm" onClick={() => startEdit(row)} disabled={disabled || loading}>
            Sửa
          </button>
          <button type="button" className="button-danger button-sm" onClick={() => deactivate(row)} disabled={disabled || loading}>
            Tạm ngưng
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <div className="screen-heading-row">
          <div>
            <p className="eyebrow">Danh mục tham chiếu</p>
            <h2>Danh mục tham chiếu</h2>
            <p>Quản lý bản ghi dùng chung trong tenant: tải danh sách, tạo mới, chỉnh sửa và tạm ngưng.</p>
          </div>
          <div className="screen-heading-actions">
            <button type="button" onClick={() => { onGenerateCode(); setCreateOpen(true); }} disabled={disabled || loading}>
              Tạo danh mục
            </button>
          </div>
        </div>
      </section>

      <section className="panel panel-span-3 master-list-panel">
        <div className="panel-heading list-heading">
          <div>
            <h3>Danh sách bản ghi</h3>
            <p>Dữ liệu chỉ thuộc tenant hiện tại.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải dữ liệu'}</button>
        </div>
        <div className="list-toolbar">
          <label className="field-label table-filter">
            Lọc nhanh
            <input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Tìm theo mã hoặc tên" />
          </label>
          <span className="list-count">{filteredRows.length} bản ghi</span>
        </div>
        <DataTable
          columns={columns}
          rows={filteredRows}
          emptyTitle="Chưa có dữ liệu"
          emptyMessage={searchTerm ? 'Không có bản ghi nào khớp với tìm kiếm.' : 'Hãy tải dữ liệu sau khi đăng nhập. Tenant mới có thể chưa có dữ liệu.'}
        />
        {postCreateHint && <Alert tone="success" title="Thao tác thành công">{postCreateHint}</Alert>}
      </section>

      <Drawer open={createOpen} onClose={() => setCreateOpen(false)} title="Tạo danh mục mới">
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem dữ liệu nhưng không được tạo bản ghi mới.
          </Alert>
        )}
        <form className="drawer-form" onSubmit={submitCreate}>
          <label>
            Mã danh mục
            <input value={form.code} onChange={updateField('code')} required />
          </label>
          <label>
            Tên danh mục
            <input value={form.name} onChange={updateField('name')} required />
          </label>
          <label>
            Nhóm danh mục
            <input value={form.category} onChange={updateField('category')} required />
          </label>
          <label className="check-row">
            <input type="checkbox" checked={form.isActive} onChange={updateField('isActive')} />
            Đang hoạt động
          </label>
          <div className="drawer-actions">
            <button type="button" className="button-secondary" onClick={onGenerateCode}>Tạo mã mới</button>
            <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tạo...' : 'Tạo bản ghi'}</button>
          </div>
        </form>
        <p className="hint">Mã chỉ cần duy nhất với bản ghi đang hoạt động trong từng tenant.</p>
      </Drawer>

      <Drawer open={Boolean(editingRow)} onClose={() => setEditingRow(null)} title="Sửa bản ghi">
        <form className="drawer-form" onSubmit={submitEdit}>
          <label>
            Mã danh mục
            <input value={editForm.code} onChange={updateEditField('code')} required />
          </label>
          <label>
            Tên danh mục
            <input value={editForm.name} onChange={updateEditField('name')} required />
          </label>
          <label>
            Nhóm danh mục
            <input value={editForm.category} onChange={updateEditField('category')} required />
          </label>
          <label className="check-row">
            <input type="checkbox" checked={editForm.isActive} onChange={updateEditField('isActive')} />
            Đang hoạt động
          </label>
          <div className="drawer-actions">
            <button type="button" className="button-secondary" onClick={() => setEditingRow(null)}>Hủy</button>
            <button type="submit" disabled={disabled || loading}>{loading ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
          </div>
        </form>
      </Drawer>
    </div>
  );
}
