const navItems = [
  { id: 'dashboard', label: 'Dashboard', eyebrow: 'Overview' },
  { id: 'master-data', label: 'Master Data', eyebrow: 'Records' },
  { id: 'activity-log', label: 'Activity Log', eyebrow: 'History' },
  { id: 'account', label: 'Account', eyebrow: 'Settings' }
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

      <nav className="sidebar-nav" aria-label="Application sections">
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
        <strong>Local demo</strong>
        <span>Tenant records and activity history</span>
      </div>
    </aside>
  );
}
