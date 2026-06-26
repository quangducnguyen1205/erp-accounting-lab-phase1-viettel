import { Badge } from './Badge';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

function humanTenant(tenantId) {
  if (!tenantId) {
    return '(không xác định)';
  }
  return `Tenant ${tenantId}`;
}

export function Topbar({ activeScreen, authState, onLogout, onRefresh }) {
  const user = authState.userInfo;
  const role = primaryRole(user);
  const pageTitles = {
    dashboard: 'Tổng quan',
    'master-data': 'Dữ liệu danh mục',
    files: 'Tài liệu đính kèm',
    'activity-log': 'Nhật ký hoạt động',
    account: 'Tài khoản'
  };

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Master Data Portal</p>
        <h1>{pageTitles[activeScreen] ?? 'Tổng quan'}</h1>
      </div>

      <div className="topbar-status">
        <Badge tone={role === 'ACCOUNTANT' ? 'indigo' : role === 'VIEWER' ? 'blue' : 'neutral'}>{role}</Badge>
        <div className="user-summary">
          <strong>{user?.username ?? 'Khách'}</strong>
          <span>{humanTenant(user?.tenantId)}</span>
        </div>
        <button type="button" className="button-secondary" onClick={onRefresh}>Làm mới</button>
        <button type="button" className="button-secondary" onClick={onLogout}>Đăng xuất</button>
      </div>
    </header>
  );
}
