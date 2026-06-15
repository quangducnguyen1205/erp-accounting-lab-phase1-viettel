export function RequestStatus({ lastResult, error }) {
  if (!lastResult && !error) {
    return (
      <section className="request-status request-status-idle">
        <strong>Chưa có thao tác</strong>
        <span>Thực hiện một thao tác để xem kết quả tại đây.</span>
      </section>
    );
  }

  return (
    <section className={`request-status ${lastResult?.ok ? 'request-status-ok' : 'request-status-error'}`}>
      <div>
        <strong>{lastResult?.ok ? 'Thao tác thành công' : 'Thao tác chưa thành công'}</strong>
        {lastResult && <span>{lastResult.ok ? 'Dữ liệu đã được cập nhật.' : 'Vui lòng xem thông báo và thử lại.'}</span>}
      </div>
      {error && <p>{error}</p>}
      {lastResult && (
        <details className="technical-details">
          <summary>Chi tiết kỹ thuật</summary>
          <dl>
            <dt>HTTP status</dt>
            <dd>{lastResult.status}</dd>
            <dt>requestId</dt>
            <dd><code>{lastResult.requestId}</code></dd>
            <dt>endpoint</dt>
            <dd>{lastResult.endpoint}</dd>
          </dl>
        </details>
      )}
    </section>
  );
}
