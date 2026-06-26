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

export function Topbar({ authState, onLogout, onRefresh }) {
  const user = authState.userInfo;
  const role = primaryRole(user);

  return (
    <header className="topbar">
      <div className="topbar-status">
        <div className="user-summary">
          <strong>{user?.username ?? 'Khách'}</strong>
          <span>{humanTenant(user?.tenantId)}</span>
        </div>
        <Badge tone={role === 'ACCOUNTANT' ? 'indigo' : role === 'VIEWER' ? 'blue' : 'neutral'}>{role}</Badge>
        <button type="button" className="button-secondary button-sm" onClick={onRefresh}>Làm mới</button>
        <button type="button" className="button-secondary button-sm" onClick={onLogout}>Đăng xuất</button>
      </div>
    </header>
  );
}
