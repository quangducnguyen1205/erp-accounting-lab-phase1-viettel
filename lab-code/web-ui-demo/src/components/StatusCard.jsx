import { Badge } from './Badge';

export function StatusCard({ label, title, children, badge, tone = 'blue' }) {
  return (
    <section className="status-card">
      <div className="status-card-header">
        <span>{label}</span>
        {badge && <Badge tone={tone}>{badge}</Badge>}
      </div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
    </section>
  );
}
