const navItems = [
  { id: 'dashboard', label: 'Tổng quan', eyebrow: 'Overview' },
  { id: 'master-data', label: 'Danh mục', eyebrow: 'Bản ghi' },
  { id: 'activity-log', label: 'Lịch sử hoạt động', eyebrow: 'Lịch sử' },
  { id: 'account', label: 'Tài khoản', eyebrow: 'Thiết lập' }
];

export function Sidebar({ activeScreen, onNavigate }) {
  return (
    <aside className="sidebar">
      <div className="brand-block">
        <span className="brand-mark">MD</span>
        <div>
          <strong>Quản lý</strong>
          <span>Danh mục</span>
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
        <strong>Demo local</strong>
        <span>Dữ liệu danh mục và lịch sử theo tenant</span>
      </div>
    </aside>
  );
}
