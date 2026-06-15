export function RequestStatus({ lastResult, error }) {
  if (!lastResult && !error) {
    return null;
  }

  return (
    <section className={`request-status ${lastResult?.ok ? 'request-status-ok' : 'request-status-error'}`}>
      <div>
        <strong>{lastResult?.ok ? 'Thao tác thành công' : 'Thao tác chưa thành công'}</strong>
        {lastResult && <span>{lastResult.ok ? 'Dữ liệu đã được cập nhật.' : 'Vui lòng xem thông báo và thử lại.'}</span>}
      </div>
      {error && <p>{error}</p>}
    </section>
  );
}
