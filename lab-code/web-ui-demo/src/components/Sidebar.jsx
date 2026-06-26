const workspaceItems = [
  { id: 'dashboard', label: 'Tổng quan' },
  { id: 'master-data', label: 'Danh mục tham chiếu' },
  { id: 'lookup', label: 'Tra cứu' },
  { id: 'files', label: 'Tài liệu đính kèm' },
  { id: 'activity-log', label: 'Nhật ký hoạt động' }
];

const secondaryItems = [
  { id: 'demo', label: 'Demo & kỹ thuật' },
  { id: 'account', label: 'Tài khoản' }
];

export function Sidebar({ activeScreen, onNavigate }) {
  return (
    <aside className="sidebar">
      <div className="brand-block">
        <span className="brand-mark">MD</span>
        <div>
          <strong>Master Data Portal</strong>
          <span>Cổng quản lý danh mục</span>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label="Điều hướng chính">
        <small className="nav-group-label">Workspace</small>
        {workspaceItems.map((item) => (
          <button
            key={item.id}
            type="button"
            className={activeScreen === item.id ? 'nav-item nav-item-active' : 'nav-item'}
            onClick={() => onNavigate(item.id)}
          >
            {item.label}
          </button>
        ))}

        <div className="nav-divider" />

        {secondaryItems.map((item) => (
          <button
            key={item.id}
            type="button"
            className={activeScreen === item.id ? 'nav-item nav-item-active' : 'nav-item'}
            onClick={() => onNavigate(item.id)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <div className="sidebar-note">
        <span>Dữ liệu theo phạm vi tenant đăng nhập</span>
      </div>
    </aside>
  );
}
