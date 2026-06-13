import { Badge } from '../components/Badge';

const demoTools = [
  ['Grafana Loki', 'http://localhost:13001', 'Read backend logs during the live demo.'],
  ['Kafka UI', 'http://localhost:18082', 'Inspect the event topic and consumer group.'],
  ['Kong proxy', 'http://localhost:18000', 'Gateway base URL for business APIs.'],
  ['Keycloak', 'http://localhost:18080', 'Local identity provider and demo users.']
];

const recipes = [
  '{service="tenant-demo"}',
  '{service="audit-log-service"}',
  '{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="',
  '{service=~"tenant-demo|kong-gateway"} |= "409"',
  '{service=~"tenant-demo|kong-gateway"} |= "403"'
];

export function AccountScreen({
  authState,
  apiBaseUrl,
  setApiBaseUrl,
  gatewayPresets,
  gatewayName,
  onLogout,
  onRefresh
}) {
  const roles = [...(authState.userInfo?.realmRoles ?? []), ...(authState.userInfo?.clientRoles ?? [])];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Account</p>
        <h2>User and local demo settings</h2>
        <p>Review your account context and switch the local gateway preset when comparing gateway labs.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Signed-in account</h3>
            <p>Token details stay hidden; backend services validate the token on every API request.</p>
          </div>
          <Badge tone="success">Authenticated</Badge>
        </div>
        <dl className="facts">
          <dt>Username</dt>
          <dd>{authState.userInfo?.username ?? '(unknown)'}</dd>
          <dt>tenant_id</dt>
          <dd>{authState.userInfo?.tenantId ?? '(missing)'}</dd>
          <dt>Roles</dt>
          <dd>{roles.join(', ') || '(none)'}</dd>
          <dt>Access token</dt>
          <dd>{authState.hasToken ? 'available (hidden)' : 'missing'}</dd>
          <dt>Expires at</dt>
          <dd>{authState.tokenExpiresAt}</dd>
        </dl>
        <div className="form-actions account-actions">
          <button type="button" className="button-secondary" onClick={onRefresh}>Refresh token</button>
          <button type="button" onClick={onLogout}>Logout</button>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Gateway preset</h3>
            <p>Business API calls use this base URL.</p>
          </div>
          <Badge tone={gatewayName === 'Kong Gateway' ? 'blue' : 'warning'}>{gatewayName}</Badge>
        </div>
        <label className="field-label">
          API base URL
          <input value={apiBaseUrl} onChange={(event) => setApiBaseUrl(event.target.value)} aria-label="API base URL" />
        </label>
        <div className="preset-row account-presets">
          {gatewayPresets.map((preset) => (
            <button type="button" className="button-secondary" key={preset.url} onClick={() => setApiBaseUrl(preset.url)}>
              {preset.label}
            </button>
          ))}
        </div>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Demo tools</h3>
            <p>Secondary links for explaining the backend flow. The product UI does not call these APIs.</p>
          </div>
          <Badge tone="neutral">Local only</Badge>
        </div>
        <div className="tool-link-grid">
          {demoTools.map(([title, url, body]) => (
            <a className="tool-link" href={url} target="_blank" rel="noreferrer" key={title}>
              <strong>{title}</strong>
              <span>{body}</span>
              <code>{url}</code>
            </a>
          ))}
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Log recipes</h3>
            <p>Use these outside the product flow during a technical walkthrough.</p>
          </div>
          <Badge tone="teal">Optional</Badge>
        </div>
        <div className="code-list">
          {recipes.map((recipe) => <code key={recipe}>{recipe}</code>)}
        </div>
      </section>
    </div>
  );
}
