import { Badge } from '../components/Badge';
import { EmptyState } from '../components/EmptyState';
import { StatusCard } from '../components/StatusCard';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

function isActiveRecord(row) {
  return row.isActive ?? row.active ?? row.status === 'ACTIVE';
}

function formatActivity(event) {
  const action = event.changeType ?? event.eventType ?? 'Đã thay đổi';
  const code = event.aggregateCode ?? event.code ?? event.aggregateId ?? '(không rõ bản ghi)';
  return `${action} ${code}`;
}

export function DashboardScreen({ authState, rows, auditEvents, files, masterDataLoaded, activityLoaded, filesLoaded, lastResult, onNavigate }) {
  const activeRecords = rows.filter(isActiveRecord).length;
  const role = primaryRole(authState.userInfo);
  const recentEvents = auditEvents.slice(0, 4);

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tổng quan</p>
        <h2>Tổng quan nghiệp vụ</h2>
        <p>Theo dõi dữ liệu danh mục và hoạt động gần đây trong tenant hiện tại.</p>
      </section>

      <section className="stack-grid panel-span-3">
        <StatusCard label="Bản ghi" title={masterDataLoaded ? rows.length : 'Chưa tải'} badge="Tổng số" tone="blue">
          Tổng số bản ghi danh mục đã tải trong phiên trình duyệt này.
        </StatusCard>
        <StatusCard label="Đang hoạt động" title={masterDataLoaded ? activeRecords : 'Chưa tải'} badge="Hoạt động" tone="success">
          Số bản ghi đang hoạt động được tính từ danh sách đã tải.
        </StatusCard>
        <StatusCard label="Thay đổi gần đây" title={activityLoaded ? auditEvents.length : 'Chưa tải'} badge="Lịch sử" tone="teal">
          Lịch sử hoạt động được tải từ audit API khi người dùng yêu cầu.
        </StatusCard>
        <StatusCard label="Tệp tin" title={filesLoaded ? files.length : 'Chưa tải'} badge="Files" tone="indigo">
          Số tệp tin đã tải trong phạm vi tenant hiện tại.
        </StatusCard>
        <StatusCard label="Tenant" title={authState.userInfo?.tenantId ?? '(none)'} badge="Tenant" tone="indigo">
          Phạm vi tenant lấy từ Keycloak token đã được backend kiểm tra.
        </StatusCard>
        <StatusCard label="Vai trò" title={role} badge="Quyền" tone={role === 'ACCOUNTANT' ? 'success' : 'indigo'}>
          Vai trò quyết định backend có cho phép tạo bản ghi hay không.
        </StatusCard>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Hoạt động gần đây</h3>
            <p>Những hoạt động mới nhất đã tải cho tenant hiện tại.</p>
          </div>
          <Badge tone={activityLoaded ? 'success' : 'neutral'}>{activityLoaded ? 'Đã tải' : 'Chưa tải'}</Badge>
        </div>
        {recentEvents.length > 0 ? (
          <ul className="activity-list">
            {recentEvents.map((event) => (
              <li key={event.eventId ?? `${event.aggregateId}-${event.consumedAt}`}>
                <div>
                  <strong>{formatActivity(event)}</strong>
                  <span>{event.occurredAt ?? event.consumedAt ?? 'Chưa có thời gian'}</span>
                </div>
                <code>{event.eventId ?? '(không có eventId)'}</code>
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState title="Tải lịch sử để xem thay đổi gần đây">
            Mở Lịch sử hoạt động sau khi tạo bản ghi hoặc tải hoạt động của tenant.
          </EmptyState>
        )}
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Bắt đầu</h3>
            <p>Thực hiện luồng nghiệp vụ chính.</p>
          </div>
          <Badge tone="blue">Workflow</Badge>
        </div>
        <div className="action-list">
          <button type="button" className="button-secondary" onClick={() => onNavigate('master-data')}>Tải dữ liệu danh mục</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('master-data')}>Tạo bản ghi</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('files')}>Quản lý tệp tin</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('activity-log')}>Xem lịch sử</button>
        </div>
        <p className="hint">Phần giải thích kỹ thuật nằm trong demo script, không phải thao tác chính của người dùng.</p>
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Thao tác gần nhất</h3>
            <p>Hiển thị kết quả thao tác mới nhất trong phiên làm việc.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : lastResult ? 'danger' : 'neutral'}>
            {lastResult ? (lastResult.ok ? 'Thành công' : 'Cần kiểm tra') : 'Chưa có'}
          </Badge>
        </div>
        {lastResult ? (
          <details className="technical-details">
            <summary>Chi tiết kỹ thuật</summary>
            <dl className="facts">
              <dt>HTTP status</dt>
              <dd>{lastResult.status}</dd>
              <dt>requestId</dt>
              <dd><code>{lastResult.requestId}</code></dd>
            </dl>
          </details>
        ) : (
          <EmptyState title="Chưa có thao tác">Tải dữ liệu hoặc tạo bản ghi để xem trạng thái gần nhất.</EmptyState>
        )}
      </section>
    </div>
  );
}
