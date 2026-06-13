import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';

export function WelcomeScreen({ authState, onLogin }) {
  return (
    <main className="welcome-screen">
      <section className="welcome-hero">
        <div className="welcome-architecture">
          <div className="brand-block brand-block-large">
            <span className="brand-mark">AI</span>
            <div>
              <strong>AI Knowledge Workspace</strong>
              <span>Phase 1.5 Backend Architecture Demo</span>
            </div>
          </div>

          <p>
            A focused local ops console for demonstrating Keycloak login, Kong routing,
            tenant-aware APIs, Kafka events, audit logs, and centralized observability.
          </p>

          <div className="flow-grid">
            {['React Web', 'Keycloak', 'Kong', 'tenant-demo', 'Kafka', 'audit-log-service', 'Loki'].map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
        </div>

        <div className="welcome-card">
          <p className="eyebrow">Local learning console</p>
          <h1>Sign in to run the Phase 1.5 demo</h1>
          <p>
            The UI is a thin client. Backend services still validate JWTs, roles and tenant scope.
            Full access tokens are never displayed.
          </p>

          <button type="button" className="button-primary button-large" onClick={onLogin} disabled={authState.initializing}>
            {authState.initializing ? 'Initializing Keycloak...' : 'Sign in with Keycloak'}
          </button>

          <div className="account-hints">
            <h2>Demo accounts</h2>
            <div>
              <code>tenant1-user / password</code>
              <Badge tone="success">ACCOUNTANT</Badge>
            </div>
            <div>
              <code>tenant2-user / password</code>
              <Badge tone="indigo">VIEWER</Badge>
            </div>
            <small>Local-only credentials for the learning lab.</small>
          </div>

          {authState.warning && <Alert tone="warning" title="Auth warning">{authState.warning}</Alert>}
          {authState.error && <Alert tone="danger" title="Login error">{authState.error}</Alert>}
        </div>
      </section>
    </main>
  );
}
