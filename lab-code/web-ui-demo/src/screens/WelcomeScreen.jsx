import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';

export function WelcomeScreen({ authState, onLogin }) {
  return (
    <main className="welcome-screen">
      <section className="welcome-hero">
        <div className="welcome-architecture">
          <div className="brand-block brand-block-large">
            <span className="brand-mark">MD</span>
            <div>
              <strong>Master Data Portal</strong>
              <span>Manage shared business data across tenants</span>
            </div>
          </div>

          <p>
            Create, review and track shared reference records for each tenant.
            Authentication, roles and tenant isolation are handled behind the scenes.
          </p>

          <div className="flow-grid">
            {['Tenant records', 'Activity history', 'Role-aware access', 'Request status'].map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
        </div>

        <div className="welcome-card">
          <p className="eyebrow">Local business portal</p>
          <h1>Master Data Portal</h1>
          <p>
            Manage shared business data across tenants. Sign in to load records,
            create a new entry and review activity history.
          </p>

          <button type="button" className="button-primary button-large" onClick={onLogin} disabled={authState.initializing}>
            {authState.initializing ? 'Preparing sign in...' : 'Sign in'}
          </button>

          <div className="account-hints">
            <h2>Demo accounts</h2>
            <div>
              <code>tenant1-user / password</code>
              <Badge tone="success">Accountant</Badge>
            </div>
            <div>
              <code>tenant2-user / password</code>
              <Badge tone="indigo">Viewer</Badge>
            </div>
            <small>Local-only credentials for the learning lab.</small>
          </div>

          <p className="token-note">Access tokens are never displayed in this UI.</p>

          {authState.warning && <Alert tone="warning" title="Auth warning">{authState.warning}</Alert>}
          {authState.error && <Alert tone="danger" title="Login error">{authState.error}</Alert>}
        </div>
      </section>
    </main>
  );
}
