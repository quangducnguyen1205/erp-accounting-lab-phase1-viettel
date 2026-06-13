import { Badge } from '../components/Badge';
import { StatusCard } from '../components/StatusCard';

const tools = [
  ['Grafana Loki', 'http://localhost:13001', 'Read centralized logs by service, requestId and code.', 'LOGS'],
  ['Kafka UI', 'http://localhost:18082', 'Inspect topic, message, consumer group and lag.', 'EVENTS'],
  ['Kong proxy', 'http://localhost:18000', 'Main API Gateway entry point.', 'GATEWAY'],
  ['Keycloak', 'http://localhost:18080', 'Realm, users, clients and roles.', 'AUTH'],
  ['Metrics Grafana', 'http://localhost:13000', 'Optional Prometheus/Grafana metrics lab.', 'METRICS']
];

const recipes = [
  '{service=\"tenant-demo\"}',
  '{service=\"audit-log-service\"}',
  '{service=\"kong-gateway\"}',
  '{service=~\"tenant-demo|audit-log-service|kong-gateway\"} |= \"requestId=\"',
  '{service=~\"tenant-demo|audit-log-service|kong-gateway\"} |= \"UI-DEMO\"',
  '{service=~\"tenant-demo|kong-gateway\"} |= \"409\"',
  '{service=~\"tenant-demo|kong-gateway\"} |= \"403\"'
];

export function ObservabilityScreen() {
  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Logs and tools</p>
        <h2>Observability</h2>
        <p>The UI links to local tools and provides recipes. It does not call Loki, Grafana or Kafka UI APIs directly.</p>
      </section>

      <section className="stack-grid panel-span-3">
        {tools.map(([title, url, body, badge]) => (
          <a className="tool-card" href={url} target="_blank" rel="noreferrer" key={title}>
            <StatusCard label="Local tool" title={title} badge={badge} tone="teal">
              {body}
            </StatusCard>
            <code>{url}</code>
          </a>
        ))}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>How to read logs</h3>
            <p>Start with labels, then narrow with text filters.</p>
          </div>
          <Badge tone="teal">LogQL</Badge>
        </div>
        <div className="code-list">
          {recipes.map((recipe) => <code key={recipe}>{recipe}</code>)}
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <h3>Reading model</h3>
          <Badge tone="neutral">Local</Badge>
        </div>
        <ul className="notes-list">
          <li><strong>service</strong> and <strong>source</strong> are Loki labels.</li>
          <li>requestId, code and status are searched as text with <code>|=</code>.</li>
          <li>Browser console errors are not Loki logs.</li>
          <li>tenant-demo and audit-log-service write file logs for Alloy to tail.</li>
        </ul>
      </section>
    </div>
  );
}
