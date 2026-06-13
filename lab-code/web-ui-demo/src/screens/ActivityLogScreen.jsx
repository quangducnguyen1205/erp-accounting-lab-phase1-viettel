import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';

const columns = [
  { key: 'occurredAt', label: 'Thời gian', render: (event) => event.occurredAt ?? event.consumedAt ?? '(không trả về)' },
  { key: 'changeType', label: 'Hành động', render: (event) => <Badge tone={event.changeType === 'CREATED' ? 'success' : 'blue'}>{event.changeType ?? event.eventType ?? 'Đã thay đổi'}</Badge> },
  { key: 'aggregateCode', label: 'Mã bản ghi', render: (event) => <code>{event.aggregateCode ?? event.code ?? event.aggregateId ?? '(missing)'}</code> },
  { key: 'aggregateType', label: 'Loại', render: (event) => event.aggregateType ?? 'Master data' },
  { key: 'eventId', label: 'Event ID', render: (event) => <small><code>{event.eventId}</code></small> }
];

export function ActivityLogScreen({ events, onLoad, loading, disabled, activityLoaded, tenantId }) {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Lịch sử hoạt động</p>
        <h2>Lịch sử hoạt động</h2>
        <p>Lịch sử được sinh ra khi dữ liệu danh mục thay đổi. Mỗi tài khoản chỉ thấy dữ liệu trong tenant của mình.</p>
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Hoạt động trong tenant</h3>
            <p>Tải hoạt động mới nhất của tài khoản hiện tại.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải lịch sử'}</button>
        </div>

        <DataTable
          columns={columns}
          rows={events}
          emptyTitle={activityLoaded ? 'Chưa có hoạt động nào trong tenant này' : 'Chưa tải lịch sử'}
          emptyMessage={activityLoaded
            ? `Không có hoạt động trong tenant ${tenantId ?? '(unknown)'}. HTTP 200 với danh sách rỗng cũng xác nhận tenant isolation cho tài khoản hiện tại.`
            : 'Tạo một bản ghi với vai trò ACCOUNTANT, sau đó tải lịch sử để xem thay đổi.'}
        />
      </section>
    </div>
  );
}
