const navItems = [
  { id: 'dashboard', label: 'Tổng quan', eyebrow: 'Bức tranh chung' },
  { id: 'master-data', label: 'Dữ liệu danh mục', eyebrow: 'Bản ghi' },
  { id: 'files', label: 'Tài liệu đính kèm', eyebrow: 'Tệp tin của tenant' },
  { id: 'activity-log', label: 'Nhật ký hoạt động', eyebrow: 'Lịch sử' },
  { id: 'account', label: 'Tài khoản', eyebrow: 'Thiết lập' }
];

export function Sidebar({ activeScreen, onNavigate }) {
  return (
    <aside className="sidebar">
      <div className="brand-block">
        <span className="brand-mark">MD</span>
        <div>
          <strong>Master Data</strong>
          <span>Portal</span>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label="Các khu vực trong ứng dụng">
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            className={activeScreen === item.id ? 'nav-item nav-item-active' : 'nav-item'}
            onClick={() => onNavigate(item.id)}
          >
            <span>{item.label}</span>
            <small>{item.eyebrow}</small>
          </button>
        ))}
      </nav>

      <div className="sidebar-note">
        <strong>Phiên làm việc</strong>
        <span>Dữ liệu, tài liệu và nhật ký theo tenant</span>
      </div>
    </aside>
  );
}
