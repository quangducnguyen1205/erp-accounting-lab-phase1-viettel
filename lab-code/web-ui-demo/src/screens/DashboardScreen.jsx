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
  const action = event.changeType ?? event.eventType ?? 'Changed';
  const code = event.aggregateCode ?? event.code ?? event.aggregateId ?? '(unknown record)';
  return `${action} ${code}`;
}

export function DashboardScreen({ authState, rows, auditEvents, masterDataLoaded, activityLoaded, lastResult, onNavigate }) {
  const activeRecords = rows.filter(isActiveRecord).length;
  const role = primaryRole(authState.userInfo);
  const recentEvents = auditEvents.slice(0, 4);

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Dashboard</p>
        <h2>Business overview</h2>
        <p>Track tenant-scoped reference records and recent activity for the current account.</p>
      </section>

      <section className="stack-grid panel-span-3">
        <StatusCard label="Records" title={masterDataLoaded ? rows.length : 'Not loaded'} badge="Total" tone="blue">
          Total master data records loaded in this browser session.
        </StatusCard>
        <StatusCard label="Active records" title={masterDataLoaded ? activeRecords : 'Not loaded'} badge="Active" tone="success">
          Active records are computed from the loaded list.
        </StatusCard>
        <StatusCard label="Recent changes" title={activityLoaded ? auditEvents.length : 'Not loaded'} badge="Activity" tone="teal">
          Activity is loaded from the audit API when requested.
        </StatusCard>
        <StatusCard label="Tenant" title={authState.userInfo?.tenantId ?? '(none)'} badge="Tenant" tone="indigo">
          Tenant scope comes from the validated Keycloak token.
        </StatusCard>
        <StatusCard label="Role" title={role} badge="Access" tone={role === 'ACCOUNTANT' ? 'success' : 'indigo'}>
          Role controls whether create actions are allowed by the backend.
        </StatusCard>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Recent activity</h3>
            <p>Latest activity loaded for the current tenant.</p>
          </div>
          <Badge tone={activityLoaded ? 'success' : 'neutral'}>{activityLoaded ? 'Loaded' : 'Not loaded'}</Badge>
        </div>
        {recentEvents.length > 0 ? (
          <ul className="activity-list">
            {recentEvents.map((event) => (
              <li key={event.eventId ?? `${event.aggregateId}-${event.consumedAt}`}>
                <div>
                  <strong>{formatActivity(event)}</strong>
                  <span>{event.occurredAt ?? event.consumedAt ?? 'Time not returned'}</span>
                </div>
                <code>{event.eventId ?? '(no event id)'}</code>
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState title="Load activity to see recent changes">
            Open Activity Log after creating or loading tenant activity.
          </EmptyState>
        )}
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Getting started</h3>
            <p>Run the normal business workflow.</p>
          </div>
          <Badge tone="blue">Workflow</Badge>
        </div>
        <div className="action-list">
          <button type="button" className="button-secondary" onClick={() => onNavigate('master-data')}>Load master data</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('master-data')}>Create a record</button>
          <button type="button" className="button-secondary" onClick={() => onNavigate('activity-log')}>Review activity</button>
        </div>
        <p className="hint">For the live architecture explanation, use the demo script to inspect Kafka UI and Grafana Loki outside the product flow.</p>
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Last request</h3>
            <p>Use this when a user action needs backend troubleshooting.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : lastResult ? 'danger' : 'neutral'}>{lastResult ? `HTTP ${lastResult.status}` : 'No request yet'}</Badge>
        </div>
        <dl className="facts">
          <dt>Request ID</dt>
          <dd><code>{lastResult?.requestId ?? '(none yet)'}</code></dd>
          <dt>Endpoint</dt>
          <dd>{lastResult?.endpoint ?? '(none yet)'}</dd>
        </dl>
      </section>
    </div>
  );
}
