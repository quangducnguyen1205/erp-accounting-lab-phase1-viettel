import { Badge } from './Badge';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

export function Topbar({ activeScreen, authState, apiBaseUrl, gatewayName, onLogout, onRefresh }) {
  const user = authState.userInfo;
  const role = primaryRole(user);
  const pageTitles = {
    dashboard: 'Dashboard',
    'master-data': 'Master Data',
    'activity-log': 'Activity Log',
    account: 'Account'
  };

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Master Data Portal</p>
        <h1>{pageTitles[activeScreen] ?? 'Dashboard'}</h1>
      </div>

      <div className="topbar-status">
        <Badge tone={gatewayName === 'Kong Gateway' ? 'blue' : 'neutral'}>{gatewayName}</Badge>
        <span className="api-pill" title={apiBaseUrl}>API ready</span>
        <Badge tone={role === 'ACCOUNTANT' ? 'success' : role === 'VIEWER' ? 'indigo' : 'neutral'}>{role}</Badge>
        <div className="user-summary">
          <strong>{user?.username ?? 'Guest'}</strong>
          <span>tenant_id {user?.tenantId ?? '(none)'}</span>
          <small>token {authState.hasToken ? 'available (hidden)' : 'missing'}</small>
        </div>
        <button type="button" className="button-secondary" onClick={onRefresh}>Refresh</button>
        <button type="button" className="button-secondary" onClick={onLogout}>Logout</button>
      </div>
    </header>
  );
}
