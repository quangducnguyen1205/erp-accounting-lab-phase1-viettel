import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';

const columns = [
  { key: 'eventId', label: 'Event ID', render: (event) => <code>{event.eventId}</code> },
  { key: 'tenantId', label: 'Tenant' },
  { key: 'eventType', label: 'Event Type' },
  { key: 'aggregateType', label: 'Aggregate Type' },
  { key: 'aggregateId', label: 'Aggregate ID' },
  { key: 'aggregateCode', label: 'Code', render: (event) => event.aggregateCode ?? event.code ?? '(missing)' },
  { key: 'changeType', label: 'Change', render: (event) => <Badge tone={event.changeType === 'CREATED' ? 'success' : 'blue'}>{event.changeType}</Badge> },
  { key: 'occurredAt', label: 'Occurred' },
  { key: 'consumedAt', label: 'Consumed' }
];

export function AuditEventsScreen({ events, onLoad, loading, disabled, auditEventsLoaded, tenantId }) {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Cross-service Kafka flow</p>
        <h2>Audit Events</h2>
        <p>Read audit events stored by audit-log-service after tenant-demo publishes MasterDataChangedEvent.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="flow-strip" aria-label="Audit event flow">
          {['Create master data', 'tenant-demo publishes Kafka event', 'audit-log-service consumes', 'audit table', 'Read API through Kong'].map((step, index) => (
            <div key={step} className="flow-step">
              <span>{index + 1}</span>
              <strong>{step}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Audit event table</h3>
            <p>GET /api/audit-events?limit=20 through Kong.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Loading...' : 'Load audit events'}</button>
        </div>

        <DataTable
          columns={columns}
          rows={events}
          emptyTitle={auditEventsLoaded ? 'No audit events for this tenant' : 'Audit events not loaded yet'}
          emptyMessage={auditEventsLoaded
            ? `HTTP 200 for tenant ${tenantId ?? '(unknown)'} means tenant isolation is working.`
            : 'Create master data as tenant1, then load audit events after audit-log-service consumes the Kafka event.'}
        />
      </section>
    </div>
  );
}
