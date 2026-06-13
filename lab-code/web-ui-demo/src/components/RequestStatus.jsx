export function RequestStatus({ lastResult, error }) {
  if (!lastResult && !error) {
    return (
      <section className="request-status request-status-idle">
        <strong>Chưa có request</strong>
        <span>Thực hiện một thao tác, sau đó dùng requestId ở đây nếu cần tra log backend.</span>
      </section>
    );
  }

  return (
    <section className={`request-status ${lastResult?.ok ? 'request-status-ok' : 'request-status-error'}`}>
      <div>
        <strong>{lastResult ? `HTTP ${lastResult.status}` : 'Request thất bại'}</strong>
        {lastResult?.endpoint && <span>{lastResult.endpoint}</span>}
      </div>
      {lastResult?.requestId && <code>{lastResult.requestId}</code>}
      {error && <p>{error}</p>}
    </section>
  );
}
