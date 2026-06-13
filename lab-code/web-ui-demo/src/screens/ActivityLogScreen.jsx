import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';

const columns = [
  { key: 'occurredAt', label: 'Time', render: (event) => event.occurredAt ?? event.consumedAt ?? '(not returned)' },
  { key: 'changeType', label: 'Action', render: (event) => <Badge tone={event.changeType === 'CREATED' ? 'success' : 'blue'}>{event.changeType ?? event.eventType ?? 'Changed'}</Badge> },
  { key: 'aggregateCode', label: 'Record code', render: (event) => <code>{event.aggregateCode ?? event.code ?? event.aggregateId ?? '(missing)'}</code> },
  { key: 'aggregateType', label: 'Type', render: (event) => event.aggregateType ?? 'Master data' },
  { key: 'eventId', label: 'Event ID', render: (event) => <small><code>{event.eventId}</code></small> }
];

export function ActivityLogScreen({ events, onLoad, loading, disabled, activityLoaded, tenantId }) {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Activity Log</p>
        <h2>Activity history</h2>
        <p>Activity is generated when master data changes. Each account only sees activity for its own tenant.</p>
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Tenant activity</h3>
            <p>Load the latest activity for the current account.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Loading...' : 'Load activity'}</button>
        </div>

        <DataTable
          columns={columns}
          rows={events}
          emptyTitle={activityLoaded ? 'No activity for this tenant' : 'Activity not loaded yet'}
          emptyMessage={activityLoaded
            ? `No activity for tenant ${tenantId ?? '(unknown)'}. This also confirms tenant isolation for the current account.`
            : 'Create a master data record as an Accountant, then load activity to see the change history.'}
        />
      </section>
    </div>
  );
}
