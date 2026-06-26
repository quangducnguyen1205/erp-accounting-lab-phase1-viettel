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

export function DashboardScreen({ authState, rows, auditEvents, files, masterDataLoaded, activityLoaded, filesLoaded, onNavigate }) {
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
          Số hoạt động đã tải trong phiên làm việc hiện tại.
        </StatusCard>
        <StatusCard label="Tài liệu" title={filesLoaded ? files.length : 'Chưa tải'} badge="Tệp" tone="indigo">
          Số tệp tin đã tải trong phạm vi tenant hiện tại.
        </StatusCard>
        <StatusCard label="Tenant" title={authState.userInfo?.tenantId ?? '(none)'} badge="Tenant" tone="indigo">
          Phạm vi dữ liệu đang được áp dụng cho người dùng hiện tại.
        </StatusCard>
        <StatusCard label="Vai trò" title={role} badge="Quyền" tone={role === 'ACCOUNTANT' ? 'indigo' : 'blue'}>
          Vai trò quyết định thao tác nào được phép thực hiện.
        </StatusCard>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Nhật ký gần đây</h3>
            <p>Những hoạt động mới nhất đã tải cho tenant hiện tại.</p>
          </div>
          <Badge tone={activityLoaded ? 'blue' : 'neutral'}>{activityLoaded ? 'Đã tải' : 'Chưa tải'}</Badge>
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
          <EmptyState title="Tải nhật ký để xem thay đổi gần đây">
            Mở Nhật ký hoạt động sau khi tạo bản ghi hoặc tải nhật ký của tenant.
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
          <button type="button" className="button-secondary" onClick={() => onNavigate('files')}>Quản lý tài liệu</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('activity-log')}>Xem nhật ký</button>
        </div>
        <p className="hint">Bắt đầu bằng cách tải danh mục, tạo bản ghi mới, sau đó kiểm tra nhật ký hoạt động.</p>
      </section>
    </div>
  );
}
