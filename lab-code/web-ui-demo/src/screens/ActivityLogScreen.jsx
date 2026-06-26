import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';

const columns = [
  { key: 'occurredAt', label: 'Thời gian', render: (event) => event.occurredAt ?? event.consumedAt ?? '(không trả về)' },
  { key: 'changeType', label: 'Hành động', render: (event) => <Badge tone={event.changeType === 'CREATED' ? 'success' : 'blue'}>{event.changeType ?? event.eventType ?? 'Đã thay đổi'}</Badge> },
  { key: 'aggregateCode', label: 'Mã bản ghi', render: (event) => <code>{event.aggregateCode ?? event.code ?? event.aggregateId ?? '(missing)'}</code> },
  { key: 'aggregateType', label: 'Loại', render: (event) => event.aggregateType ?? 'Master data' },
  { key: 'eventId', label: 'Mã sự kiện', render: (event) => <small><code>{event.eventId}</code></small> }
];

export function ActivityLogScreen({ events, onLoad, loading, disabled, activityLoaded, tenantId }) {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Nhật ký hoạt động</p>
        <h2>Nhật ký hoạt động</h2>
        <p>Xem các thay đổi đã ghi nhận cho tenant hiện tại. Kết quả hoặc nhật ký có thể xuất hiện sau vài giây vì hệ thống xử lý bất đồng bộ.</p>
      </section>

      <section className="panel panel-span-3 activity-panel">
        <div className="panel-heading">
          <div>
            <h3>Dòng nhật ký</h3>
            <p>Tải những thay đổi mới nhất trong phạm vi tài khoản hiện tại.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải nhật ký'}</button>
        </div>

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
