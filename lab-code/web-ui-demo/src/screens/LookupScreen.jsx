import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';
import { formatDateTime } from '../utils/formatDateTime';

export function LookupScreen({
  lookupCode,
  setLookupCode,
  lookupResult,
  onLookup,
  searchKeyword,
  setSearchKeyword,
  searchResults,
  searchLoaded,
  onBackendSearch,
  loading,
  disabled
}) {
  const resultColumns = [
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
    { key: 'updatedAt', label: 'Cập nhật', render: (row) => formatDateTime(row.updatedAt ?? row.createdAt) }
  ];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tra cứu</p>
        <h2>Tra cứu danh mục</h2>
        <p>Tìm kiếm bản ghi danh mục theo mã chính xác hoặc từ khóa.</p>
      </section>

      <section className="panel panel-span-3">
        <div className="lookup-grid">
          <div className="lookup-card">
            <div className="panel-heading">
              <div>
                <h3>Tra cứu theo mã</h3>
                <p>Tìm một bản ghi bằng mã nghiệp vụ chính xác.</p>
              </div>
              <Badge tone="blue">Chính xác</Badge>
            </div>
            <form className="inline-form" onSubmit={onLookup}>
              <label>
                Mã danh mục
                <input value={lookupCode} onChange={(event) => setLookupCode(event.target.value)} placeholder="VD: LAPTOP-01" required />
              </label>
              <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tìm...' : 'Tra cứu'}</button>
            </form>
            {lookupResult ? (
              <dl className="facts result-facts">
                <dt>ID</dt>
                <dd>{lookupResult.id}</dd>
                <dt>Mã danh mục</dt>
                <dd><code>{lookupResult.code}</code></dd>
                <dt>Tên danh mục</dt>
                <dd>{lookupResult.name}</dd>
                <dt>Nhóm danh mục</dt>
                <dd>{lookupResult.category}</dd>
                <dt>Trạng thái</dt>
                <dd>
                  <Badge tone={(lookupResult.isActive ?? lookupResult.active) ? 'success' : 'warning'}>
                    {(lookupResult.isActive ?? lookupResult.active) ? 'Đang hoạt động' : 'Tạm ngưng'}
                  </Badge>
                </dd>
              </dl>
            ) : (
              <EmptyState title="Chưa tìm kiếm">Nhập mã danh mục để xem thông tin chi tiết.</EmptyState>
            )}
          </div>

          <div className="lookup-card">
            <div className="panel-heading">
              <div>
                <h3>Tìm kiếm danh mục</h3>
                <p>Tìm theo mã, tên hoặc nhóm danh mục. Kết quả có thể xuất hiện sau vài giây sau khi dữ liệu được cập nhật.</p>
              </div>
              <Badge tone="blue">Từ khóa</Badge>
            </div>
            <form className="inline-form" onSubmit={onBackendSearch}>
              <label>
                Từ khóa
                <input value={searchKeyword} onChange={(event) => setSearchKeyword(event.target.value)} placeholder="Nhập mã, tên hoặc nhóm" required />
              </label>
              <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tìm...' : 'Tìm kiếm'}</button>
            </form>
            {searchLoaded ? (
              <DataTable
                columns={resultColumns}
                rows={searchResults}
                emptyTitle="Không có kết quả"
                emptyMessage="Nếu vừa tạo hoặc cập nhật bản ghi, chờ vài giây rồi thử lại."
              />
            ) : (
              <EmptyState title="Chưa tìm kiếm">
                Nhập từ khóa để tìm trong dữ liệu danh mục.
              </EmptyState>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
