export function Alert({ title, children, tone = 'info' }) {
  return (
    <div className={`alert alert-${tone}`}>
      {title && <strong>{title}</strong>}
      <span>{children}</span>
    </div>
  );
}
