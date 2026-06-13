import { Badge } from '../components/Badge';
import { StatusCard } from '../components/StatusCard';

const stackCards = [
  ['Web UI', 'Thin React client', 'UI'],
  ['Keycloak', 'Login, users, roles, token issuer', 'AUTH'],
  ['Kong Gateway', 'Main Phase 1.5 gateway route', 'ACTIVE'],
  ['tenant-demo', 'Master Data Service on port 8080', 'API'],
  ['Kafka', 'master-data-events transport', 'EVENTS'],
  ['audit-log-service', 'Consumes events and exposes audit API', 'SERVICE'],
  ['PostgreSQL', 'Service-owned relational data', 'DATA'],
  ['Loki/Grafana', 'Centralized local logs', 'LOGS'],
  ['Kafka UI', 'Topic, message and consumer group inspection', 'TOOL']
];

function ChecklistItem({ label, state }) {
  const className = state === 'done' ? 'done' : state === 'manual' ? 'manual' : '';
  return (
    <li className={className}>
      <span>{label}</span>
      <small>{state === 'done' ? 'done' : state === 'manual' ? 'manual check' : 'pending'}</small>
    </li>
  );
}

export function DashboardScreen({ authState, apiBaseUrl, setApiBaseUrl, gatewayPresets, gatewayName, lastResult, demoProgress }) {
  const isKong = apiBaseUrl.trim() === 'http://localhost:18000';
  const roles = [...(authState.userInfo?.realmRoles ?? []), ...(authState.userInfo?.clientRoles ?? [])];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Dashboard</p>
        <h2>Architecture Overview</h2>
        <p>A live map for the final demo path: browser to Keycloak to Kong to backend services to Kafka and Loki.</p>
      </section>

      <section className="stack-grid">
        {stackCards.map(([title, body, badge]) => (
          <StatusCard key={title} label="Stack component" title={title} badge={badge} tone={badge === 'ACTIVE' ? 'blue' : 'neutral'}>
            {body}
          </StatusCard>
        ))}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Current status</h3>
            <p>Runtime context from the authenticated browser session.</p>
          </div>
          <Badge tone={isKong ? 'blue' : 'warning'}>{gatewayName}</Badge>
        </div>
        <div className="status-grid">
          <label>
            API base URL
            <input value={apiBaseUrl} onChange={(event) => setApiBaseUrl(event.target.value)} aria-label="API base URL" />
          </label>
          <div className="preset-row">
            {gatewayPresets.map((preset) => (
              <button type="button" className="button-secondary" key={preset.url} onClick={() => setApiBaseUrl(preset.url)}>
                {preset.label}
              </button>
            ))}
          </div>
          <dl className="facts">
            <dt>User</dt>
            <dd>{authState.userInfo?.username ?? 'Guest'}</dd>
            <dt>tenant_id</dt>
            <dd>{authState.userInfo?.tenantId ?? '(none)'}</dd>
            <dt>Roles</dt>
            <dd>{roles.join(', ') || '(none)'}</dd>
            <dt>Last requestId</dt>
            <dd><code>{lastResult?.requestId ?? '(none yet)'}</code></dd>
          </dl>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Demo checklist</h3>
            <p>Only mark what the UI can observe. Tool checks stay manual.</p>
          </div>
          <Badge tone="indigo">Phase 1.5</Badge>
        </div>
        <ul className="checklist">
          <ChecklistItem label="Infrastructure running" state="manual" />
          <ChecklistItem label="Logged in" state={authState.authenticated ? 'done' : 'pending'} />
          <ChecklistItem label="API base URL is Kong" state={isKong ? 'done' : 'pending'} />
          <ChecklistItem label="Create master data" state={demoProgress.createdMasterData ? 'done' : 'pending'} />
          <ChecklistItem label="Audit event appears or audit API loaded" state={demoProgress.auditEventsLoaded ? 'done' : 'pending'} />
          <ChecklistItem label="Kafka UI shows message" state="manual" />
          <ChecklistItem label="Loki shows logs" state="manual" />
          <ChecklistItem label="Tenant2 isolation verified" state={demoProgress.tenantIsolationChecked ? 'done' : 'pending'} />
          <ChecklistItem label="Viewer create returns 403" state={demoProgress.viewerCreateForbidden ? 'done' : 'pending'} />
        </ul>
      </section>
    </div>
  );
}
