const navItems = [
  { id: 'dashboard', label: 'Dashboard', eyebrow: 'Stack status' },
  { id: 'master-data', label: 'Master Data', eyebrow: 'Business API' },
  { id: 'audit-events', label: 'Audit Events', eyebrow: 'Kafka flow' },
  { id: 'observability', label: 'Observability', eyebrow: 'Logs/tools' }
];

export function Sidebar({ activeScreen, onNavigate }) {
  return (
    <aside className="sidebar">
      <div className="brand-block">
        <span className="brand-mark">AI</span>
        <div>
          <strong>AI Knowledge</strong>
          <span>Workspace Ops Console</span>
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
        <strong>Local Phase 1.5</strong>
        <span>Kong to services to Kafka to Loki</span>
      </div>
    </aside>
  );
}
