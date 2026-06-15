import { Badge } from './Badge';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

export function Topbar({ activeScreen, authState, apiBaseUrl, onLogout, onRefresh }) {
  const user = authState.userInfo;
  const role = primaryRole(user);
  const pageTitles = {
    dashboard: 'Tổng quan',
    'master-data': 'Danh mục',
    files: 'Tệp tin',
    'activity-log': 'Lịch sử hoạt động',
    account: 'Tài khoản'
  };

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Cổng quản lý danh mục</p>
        <h1>{pageTitles[activeScreen] ?? 'Tổng quan'}</h1>
      </div>

      <div className="topbar-status">
        <Badge tone="blue">API sẵn sàng</Badge>
        <span className="api-pill" title={apiBaseUrl}>Kết nối local</span>
        <Badge tone={role === 'ACCOUNTANT' ? 'success' : role === 'VIEWER' ? 'indigo' : 'neutral'}>{role}</Badge>
        <div className="user-summary">
          <strong>{user?.username ?? 'Khách'}</strong>
          <span>tenant_id {user?.tenantId ?? '(none)'}</span>
          <small>token {authState.hasToken ? 'có (ẩn)' : 'thiếu'}</small>
        </div>
        <button type="button" className="button-secondary" onClick={onRefresh}>Làm mới</button>
        <button type="button" className="button-secondary" onClick={onLogout}>Đăng xuất</button>
      </div>
    </header>
  );
}
