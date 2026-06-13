export function RequestStatus({ lastResult, error }) {
  if (!lastResult && !error) {
    return (
      <section className="request-status request-status-idle">
        <strong>No request yet</strong>
        <span>Run an action, then use the requestId here if you need to inspect backend logs.</span>
      </section>
    );
  }

  return (
    <section className={`request-status ${lastResult?.ok ? 'request-status-ok' : 'request-status-error'}`}>
      <div>
        <strong>{lastResult ? `HTTP ${lastResult.status}` : 'Request failed'}</strong>
        {lastResult?.endpoint && <span>{lastResult.endpoint}</span>}
      </div>
      {lastResult?.requestId && <code>{lastResult.requestId}</code>}
      {error && <p>{error}</p>}
    </section>
  );
}
