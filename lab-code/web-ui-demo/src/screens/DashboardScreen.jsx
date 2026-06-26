import { Badge } from '../components/Badge';
import { EmptyState } from '../components/EmptyState';
import { StatusCard } from '../components/StatusCard';
import { formatDateTime } from '../utils/formatDateTime';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

function isActiveRecord(row) {
  return row.isActive ?? row.active ?? row.status === 'ACTIVE';
}

function humanTenant(tenantId) {
  if (!tenantId) {
    return '(không xác định)';
  }
  return `Tenant ${tenantId}`;
}

function formatActivity(event) {
  const action = event.changeType ?? event.eventType ?? 'Đã thay đổi';
  const code = event.aggregateCode ?? event.code ?? event.aggregateId ?? '(không rõ bản ghi)';
  return `${action} ${code}`;
}

const WORKFLOW_STEPS = [
  { step: 1, label: 'Tải dữ liệu danh mục', target: 'master-data' },
  { step: 2, label: 'Tạo hoặc cập nhật bản ghi', target: 'master-data' },
  { step: 3, label: 'Tra cứu dữ liệu', target: 'lookup' },
  { step: 4, label: 'Tải tài liệu đính kèm', target: 'files' },
  { step: 5, label: 'Xem nhật ký hoạt động', target: 'activity-log' }
];

export function DashboardScreen({ authState, rows, auditEvents, files, masterDataLoaded, activityLoaded, filesLoaded, onNavigate }) {
  const activeRecords = rows.filter(isActiveRecord).length;
  const role = primaryRole(authState.userInfo);
  const recentEvents = auditEvents.slice(0, 4);

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tổng quan</p>
        <h2>Tổng quan</h2>
        <p>Theo dõi dữ liệu danh mục và hoạt động gần đây trong {humanTenant(authState.userInfo?.tenantId)}.</p>
      </section>

      <section className="stack-grid panel-span-3">
        <StatusCard label="Bản ghi" title={masterDataLoaded ? rows.length : 'Chưa tải'} badge="Tổng" tone="blue">
          Số bản ghi danh mục đã tải trong phiên này.
        </StatusCard>
        <StatusCard label="Đang hoạt động" title={masterDataLoaded ? activeRecords : 'Chưa tải'} badge="Hoạt động" tone="success">
          Số bản ghi đang hoạt động từ danh sách đã tải.
        </StatusCard>
        <StatusCard label="Nhật ký" title={activityLoaded ? auditEvents.length : 'Chưa tải'} badge="Sự kiện" tone="blue">
          Số hoạt động đã tải cho tenant hiện tại.
        </StatusCard>
        <StatusCard label="Tài liệu" title={filesLoaded ? files.length : 'Chưa tải'} badge="Tệp" tone="indigo">
          Số tệp tin trong phạm vi tenant hiện tại.
        </StatusCard>
        <StatusCard label="Tenant" title={humanTenant(authState.userInfo?.tenantId)} badge="Phạm vi" tone="indigo">
          Phạm vi dữ liệu đang áp dụng.
        </StatusCard>
        <StatusCard label="Vai trò" title={role} badge="Quyền" tone={role === 'ACCOUNTANT' ? 'indigo' : 'blue'}>
          Vai trò quyết định thao tác được phép.
        </StatusCard>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Nhật ký gần đây</h3>
            <p>Hoạt động mới nhất đã tải cho tenant hiện tại.</p>
          </div>
          <Badge tone={activityLoaded ? 'success' : 'neutral'}>{activityLoaded ? 'Đã tải' : 'Chưa tải'}</Badge>
        </div>
        {recentEvents.length > 0 ? (
          <ul className="activity-list">
            {recentEvents.map((event) => (
              <li key={event.eventId ?? `${event.aggregateId}-${event.consumedAt}`}>
                <div>
                  <strong>{formatActivity(event)}</strong>
                  <span>{formatDateTime(event.occurredAt ?? event.consumedAt, 'Chưa có thời gian')}</span>
                </div>
                <Badge tone={event.changeType === 'CREATED' ? 'success' : event.changeType === 'DEACTIVATED' ? 'warning' : 'blue'}>
                  {event.changeType ?? 'Thay đổi'}
                </Badge>
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
            <p>Luồng nghiệp vụ chính.</p>
          </div>
          <Badge tone="blue">Workflow</Badge>
        </div>
        <div className="workflow-steps">
          {WORKFLOW_STEPS.map((item) => (
            <button
              key={item.step}
              type="button"
              className="workflow-step"
              onClick={() => onNavigate(item.target)}
            >
              <span className="workflow-number">{item.step}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
