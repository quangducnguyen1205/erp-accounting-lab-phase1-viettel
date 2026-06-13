export function EmptyState({ title, children, tone = 'neutral' }) {
  return (
    <div className={`empty-state empty-state-${tone}`}>
      <strong>{title}</strong>
      <p>{children}</p>
    </div>
  );
}
