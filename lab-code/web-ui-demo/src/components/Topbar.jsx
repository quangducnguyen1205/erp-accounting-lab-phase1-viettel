import { Badge } from './Badge';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

export function Topbar({ authState, apiBaseUrl, gatewayName, onLogout, onRefresh }) {
  const user = authState.userInfo;
  const role = primaryRole(user);

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Phase 1.5 demo console</p>
        <h1>AI Knowledge Workspace</h1>
      </div>

      <div className="topbar-status">
        <Badge tone={gatewayName === 'Kong Gateway' ? 'blue' : 'neutral'}>{gatewayName}</Badge>
        <span className="api-pill">{apiBaseUrl}</span>
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
