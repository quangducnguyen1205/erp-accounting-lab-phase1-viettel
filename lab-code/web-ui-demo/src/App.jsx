import { useEffect, useMemo, useState } from 'react';
import { createMasterData, loadMasterData } from './api';
import { config } from './config';
import { getSafeUserInfo, initKeycloak, keycloak, refreshToken } from './keycloak';

function defaultForm() {
  return {
    code: `UI-DEMO-${Date.now()}`,
    name: 'UI Demo Master Data',
    category: 'WEB_DEMO',
    isActive: true
  };
}

function StatusLine({ lastResult }) {
  if (!lastResult) {
    return (
      <p className="hint">
        Sau mỗi request, dùng requestId để đối chiếu log `tenant-demo` và metric trong observability lab.
      </p>
    );
  }

  return (
    <div className={lastResult.ok ? 'status status-ok' : 'status status-error'}>
      <strong>{lastResult.status}</strong>
      <span>{lastResult.endpoint}</span>
      <code>{lastResult.requestId}</code>
    </div>
  );
}

function AuthPanel({ authenticated, userInfo, onLogin, onLogout, onRefresh }) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Keycloak login</h2>
        <span className={authenticated ? 'badge badge-ok' : 'badge'}>{authenticated ? 'Logged in' : 'Guest'}</span>
      </div>

      <div className="actions">
        <button onClick={onLogin} disabled={authenticated}>Login</button>
        <button onClick={onRefresh} disabled={!authenticated}>Refresh token</button>
        <button onClick={onLogout} disabled={!authenticated}>Logout</button>
      </div>

      {authenticated ? (
        <dl className="facts">
          <dt>User</dt>
          <dd>{userInfo.username}</dd>
          <dt>tenant_id</dt>
          <dd>{userInfo.tenantId}</dd>
          <dt>Roles</dt>
          <dd>{[...userInfo.realmRoles, ...userInfo.clientRoles].join(', ') || '(none)'}</dd>
        </dl>
      ) : (
        <p className="hint">Login bằng user local trong realm `viettel-lab`, ví dụ `tenant1-user` hoặc `tenant2-user`.</p>
      )}
    </section>
  );
}

function ApiPanel() {
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>API through Gateway</h2>
        <span className="badge">Thin client</span>
      </div>
      <dl className="facts">
        <dt>API base</dt>
        <dd>{config.apiBaseUrl}</dd>
        <dt>Path</dt>
        <dd>/api/master-data</dd>
      </dl>
      <p className="hint">
        Browser gọi Gateway. Gateway forward `Authorization` và `X-Request-Id`; backend vẫn validate JWT và enforce tenant-aware query.
      </p>
    </section>
  );
}

function MasterDataList({ rows, onLoad, loading, disabled }) {
  return (
    <section className="panel wide">
      <div className="panel-heading">
        <h2>Master Data</h2>
        <button onClick={onLoad} disabled={disabled || loading}>{loading ? 'Loading...' : 'Load master data'}</button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Code</th>
              <th>Name</th>
              <th>Category</th>
              <th>Active</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.id}</td>
                <td>{row.code}</td>
                <td>{row.name}</td>
                <td>{row.category}</td>
                <td>{String(row.isActive)}</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan="5" className="empty">Chưa có dữ liệu hoặc chưa load.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function CreateMasterDataForm({ form, setForm, onSubmit, disabled, loading }) {
  const updateField = (field) => (event) => {
    const value = field === 'isActive' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  return (
    <section className="panel wide">
      <div className="panel-heading">
        <h2>Create Master Data</h2>
        <span className="badge">POST via Gateway</span>
      </div>

      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Code
          <input value={form.code} onChange={updateField('code')} required />
        </label>
        <label>
          Name
          <input value={form.name} onChange={updateField('name')} required />
        </label>
        <label>
          Category
          <input value={form.category} onChange={updateField('category')} required />
        </label>
        <label className="check-row">
          <input type="checkbox" checked={form.isActive} onChange={updateField('isActive')} />
          Active
        </label>

        <div className="form-actions">
          <button type="button" onClick={() => setForm(defaultForm())}>Generate new code</button>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Creating...' : 'Create'}</button>
        </div>
      </form>
      <p className="hint">`code` phải unique trong từng tenant vì DB có constraint theo `(tenant_id, code)`.</p>
    </section>
  );
}

export default function App() {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [lastResult, setLastResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    initKeycloak()
      .then((isAuthenticated) => setAuthenticated(isAuthenticated))
      .catch((err) => setError(err.message ?? String(err)))
      .finally(() => setReady(true));
  }, []);

  const userInfo = useMemo(() => (authenticated ? getSafeUserInfo() : null), [authenticated]);

  async function runRequest(action) {
    setLoading(true);
    setError('');
    try {
      const result = await action();
      setLastResult(result);
      if (!result.ok) {
        setError(typeof result.data === 'string' ? result.data : JSON.stringify(result.data));
      }
      return result;
    } catch (err) {
      setError(err.message ?? String(err));
      return null;
    } finally {
      setLoading(false);
    }
  }

  async function handleLoad() {
    const result = await runRequest(loadMasterData);
    if (result?.ok && Array.isArray(result.data)) {
      setRows(result.data);
    }
  }

  async function handleCreate(event) {
    event.preventDefault();
    const result = await runRequest(() => createMasterData(form));
    if (result?.ok) {
      setForm(defaultForm());
      await handleLoad();
    }
  }

  if (!ready) {
    return <main className="app-shell"><p>Loading Keycloak...</p></main>;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <h1>Viettel Phase 1 Web UI Demo</h1>
          <p>React Web to Keycloak to Gateway to tenant-demo to backend integrations</p>
        </div>
      </header>

      <div className="grid two">
        <AuthPanel
          authenticated={authenticated}
          userInfo={userInfo}
          onLogin={() => keycloak.login()}
          onLogout={() => keycloak.logout({ redirectUri: window.location.origin })}
          onRefresh={async () => {
            await refreshToken(0);
            setAuthenticated(Boolean(keycloak.authenticated));
          }}
        />
        <ApiPanel />
      </div>

      <StatusLine lastResult={lastResult} />
      {error && <pre className="error-box">{error}</pre>}

      <div className="grid">
        <MasterDataList rows={rows} onLoad={handleLoad} loading={loading} disabled={!authenticated} />
        <CreateMasterDataForm form={form} setForm={setForm} onSubmit={handleCreate} loading={loading} disabled={!authenticated} />
      </div>
    </main>
  );
}
