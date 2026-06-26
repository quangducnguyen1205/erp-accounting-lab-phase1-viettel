import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { formatDateTime } from '../utils/formatDateTime';

const columns = [
  { key: 'occurredAt', label: 'Thời gian', render: (event) => formatDateTime(event.occurredAt ?? event.consumedAt, 'Chưa có thời gian') },
  { key: 'changeType', label: 'Hành động', render: (event) => <Badge tone={event.changeType === 'CREATED' ? 'success' : event.changeType === 'DEACTIVATED' ? 'warning' : 'blue'}>{event.changeType ?? event.eventType ?? 'Đã thay đổi'}</Badge> },
  { key: 'aggregateCode', label: 'Mã bản ghi', render: (event) => <code>{event.aggregateCode ?? event.code ?? event.aggregateId ?? '(missing)'}</code> },
  { key: 'aggregateType', label: 'Loại', render: (event) => event.aggregateType ?? 'Master data' },
  { key: 'eventId', label: 'Mã sự kiện', render: (event) => <small className="text-muted"><code>{event.eventId}</code></small> }
];

export function ActivityLogScreen({ events, onLoad, loading, disabled, activityLoaded, tenantId }) {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <div className="screen-heading-row">
          <div>
            <p className="eyebrow">Nhật ký hoạt động</p>
            <h2>Nhật ký hoạt động</h2>
            <p>Xem các thay đổi đã ghi nhận cho tenant hiện tại. Nhật ký có thể xuất hiện sau vài giây vì thay đổi được xử lý bất đồng bộ.</p>
          </div>
          <div className="screen-heading-actions">
            <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải nhật ký'}</button>
          </div>
        </div>
      </section>

      <section className="panel panel-span-3 activity-panel">
        <DataTable
          columns={columns}
          rows={events}
          emptyTitle={activityLoaded ? 'Chưa có nhật ký trong tenant này' : 'Chưa tải nhật ký'}
          emptyMessage={activityLoaded
            ? `Không có nhật ký trong tenant ${tenantId ?? '(unknown)'}. Điều này cũng xác nhận tài khoản hiện tại không nhìn thấy nhật ký của tenant khác.`
            : 'Tạo một bản ghi với vai trò ACCOUNTANT, sau đó tải nhật ký để xem thay đổi.'}
        />
      </section>
    </div>
  );
}
