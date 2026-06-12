import { useEffect, useMemo, useState } from 'react';
import { createMasterData, loadAuditEvents, loadMasterData, loadMasterDataByCode } from './api';
import { config } from './config';
import { getAuthSnapshot, initKeycloak, keycloak, refreshToken } from './keycloak';

const initialAuthState = {
  initializing: true,
  authenticated: false,
  hasToken: false,
  userInfo: null,
  tokenExpiresAt: '(missing)',
  warning: '',
  error: ''
};

const gatewayPresets = [
  { label: 'Kong Gateway', url: 'http://localhost:18000' },
  { label: 'Spring Gateway legacy', url: 'http://localhost:8081' }
];

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

function describeApiFailure(result) {
  if (!result) {
    return '';
  }

  if (typeof result.data === 'string' && result.data.trim()) {
    return result.data;
  }

  if (result.data && typeof result.data === 'object' && Object.keys(result.data).length > 0) {
    return JSON.stringify(result.data);
  }

  if (result.status === 401) {
    return '401 Unauthorized: thiếu token, token hết hạn hoặc token không hợp lệ.';
  }

  if (result.status === 403) {
    return '403 Forbidden: token hợp lệ nhưng user không đủ role/authority cho action này.';
  }

  if (result.status >= 500) {
    return 'Server error: kiểm log tenant-demo bằng requestId ở trên.';
  }

  return `Request failed with HTTP ${result.status}.`;
}

function formatRoles(userInfo) {
  if (!userInfo) {
    return '(none)';
  }

  return [...userInfo.realmRoles, ...userInfo.clientRoles].join(', ') || '(none)';
}

function formatResourceRoles(userInfo) {
  if (!userInfo?.resourceRoles) {
    return '(none)';
  }

  const entries = Object.entries(userInfo.resourceRoles)
    .filter(([, roles]) => roles.length > 0)
    .map(([clientId, roles]) => `${clientId}: ${roles.join(', ')}`);

  return entries.join(' | ') || '(none)';
}

function AuthPanel({ authState, actionDisabledReason, onLogin, onLogout, onRefresh }) {
  const { initializing, authenticated, hasToken, userInfo, tokenExpiresAt, warning, error } = authState;
  const authReady = authenticated && hasToken;

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Keycloak login</h2>
        <span className={authReady ? 'badge badge-ok' : 'badge'}>{authReady ? 'Logged in' : 'Guest'}</span>
      </div>

      <div className="actions">
        <button onClick={onLogin} disabled={initializing || authReady}>Login</button>
        <button onClick={onRefresh} disabled={!authReady}>Refresh token</button>
        <button onClick={onLogout} disabled={!authReady}>Logout</button>
      </div>

      {authReady ? (
        <dl className="facts">
          <dt>User</dt>
          <dd>{userInfo.username}</dd>
          <dt>tenant_id</dt>
          <dd>{userInfo.tenantId}</dd>
          <dt>Realm roles</dt>
          <dd>{userInfo.realmRoles.join(', ') || '(none)'}</dd>
          <dt>Client roles</dt>
          <dd>{formatResourceRoles(userInfo)}</dd>
          <dt>All roles</dt>
          <dd>{formatRoles(userInfo)}</dd>
          <dt>Token expires</dt>
          <dd>{tokenExpiresAt}</dd>
        </dl>
      ) : (
        <p className="hint">Login bằng user local trong realm `viettel-lab`, ví dụ `tenant1-user` hoặc `tenant2-user`.</p>
      )}

      <dl className="facts debug-facts">
        <dt>authenticated</dt>
        <dd>{String(authenticated)}</dd>
        <dt>access token</dt>
        <dd>{hasToken ? 'available (hidden)' : 'missing'}</dd>
        <dt>actions</dt>
        <dd>{actionDisabledReason || 'enabled'}</dd>
      </dl>

      {warning && <p className="warning-box">{warning}</p>}
      {error && <p className="warning-box">{error}</p>}
    </section>
  );
}

function ApiPanel({ apiBaseUrl, setApiBaseUrl }) {
  const normalizedBaseUrl = apiBaseUrl.trim();

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>API through Gateway</h2>
        <span className={normalizedBaseUrl === 'http://localhost:18000' ? 'badge badge-ok' : 'badge'}>
          {normalizedBaseUrl === 'http://localhost:18000' ? 'Kong' : 'Gateway'}
        </span>
      </div>
      <dl className="facts">
        <dt>API base</dt>
        <dd>
          <input
            value={apiBaseUrl}
            onChange={(event) => setApiBaseUrl(event.target.value)}
            aria-label="API base URL"
          />
        </dd>
        <dt>Master API</dt>
        <dd>/api/master-data</dd>
        <dt>Audit API</dt>
        <dd>/api/audit-events</dd>
      </dl>
      <div className="preset-row">
        {gatewayPresets.map((preset) => (
          <button type="button" key={preset.url} onClick={() => setApiBaseUrl(preset.url)}>
            {preset.label}
          </button>
        ))}
      </div>
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

function MasterDataByCodeLookup({ code, setCode, result, onSubmit, disabled, loading }) {
  return (
    <section className="panel wide">
      <div className="panel-heading">
        <h2>Load by Code</h2>
        <span className="badge">Redis demo path</span>
      </div>

      <form className="inline-form" onSubmit={onSubmit}>
        <label>
          Code
          <input value={code} onChange={(event) => setCode(event.target.value)} required />
        </label>
        <button type="submit" disabled={disabled || loading}>{loading ? 'Loading...' : 'Load by code'}</button>
      </form>

      {result ? (
        <dl className="facts result-facts">
          <dt>ID</dt>
          <dd>{result.id}</dd>
          <dt>Code</dt>
          <dd>{result.code}</dd>
          <dt>Name</dt>
          <dd>{result.name}</dd>
          <dt>Category</dt>
          <dd>{result.category}</dd>
          <dt>Active</dt>
          <dd>{String(result.isActive)}</dd>
        </dl>
      ) : (
        <p className="hint">Chưa lookup code nào trong phiên UI này.</p>
      )}

      <p className="hint">
        Nếu bật `APP_CACHE_ENABLED=true`, gọi cùng code hai lần rồi kiểm log/metric backend để thấy miss rồi hit. UI không tự đoán cache status.
      </p>
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

function AuditEventsPanel({ events, onLoad, loading, disabled }) {
  return (
    <section className="panel wide">
      <div className="panel-heading">
        <div>
          <h2>Audit Events</h2>
          <p className="hint inline-hint">Read-only API từ audit-log-service qua Kong.</p>
        </div>
        <button onClick={onLoad} disabled={disabled || loading}>
          {loading ? 'Loading...' : 'Load audit events'}
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Event ID</th>
              <th>Tenant</th>
              <th>Type</th>
              <th>Aggregate</th>
              <th>Code</th>
              <th>Change</th>
              <th>Occurred</th>
              <th>Consumed</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.eventId}>
                <td><code>{event.eventId}</code></td>
                <td>{event.tenantId}</td>
                <td>{event.eventType}</td>
                <td>{event.aggregateType}:{event.aggregateId}</td>
                <td>{event.aggregateCode ?? event.code ?? '(missing)'}</td>
                <td>{event.changeType}</td>
                <td>{event.occurredAt}</td>
                <td>{event.consumedAt}</td>
              </tr>
            ))}
            {events.length === 0 && (
              <tr>
                <td colSpan="8" className="empty">
                  Chưa có audit event trong phiên UI này hoặc tenant hiện tại chưa có event.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="hint">
        Sau khi create `master_data`, đợi một chút rồi bấm nút này. UI chỉ kết luận audit đã có khi API trả event thật.
      </p>
    </section>
  );
}

function DemoChecklist({ authReady, apiBaseUrl, rows, auditEvents, lastResult }) {
  const isKong = apiBaseUrl.trim() === 'http://localhost:18000';
  const auditEventsLoaded = lastResult?.ok && lastResult.endpoint?.includes('/api/audit-events');

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Demo checklist</h2>
        <span className="badge">Phase 1.5</span>
      </div>
      <ul className="checklist">
        <li className={authReady ? 'done' : ''}>Login Keycloak</li>
        <li className={isKong ? 'done' : ''}>API base là Kong `http://localhost:18000`</li>
        <li className={rows.length > 0 ? 'done' : ''}>Load master data</li>
        <li className={auditEventsLoaded ? 'done' : ''}>Load audit events</li>
        <li className={lastResult?.requestId ? 'done' : ''}>Có requestId để kiểm log</li>
      </ul>
      <dl className="facts tool-links">
        <dt>Kong</dt>
        <dd>http://localhost:18000</dd>
        <dt>Kafka UI</dt>
        <dd>http://localhost:18082</dd>
        <dt>Grafana logs</dt>
        <dd>http://localhost:13001</dd>
        <dt>Grafana metrics</dt>
        <dd>http://localhost:13000</dd>
      </dl>
    </section>
  );
}

export default function App() {
  const [authState, setAuthState] = useState(initialAuthState);
  const [apiBaseUrl, setApiBaseUrl] = useState(config.apiBaseUrl);
  const [rows, setRows] = useState([]);
  const [auditEvents, setAuditEvents] = useState([]);
  const [lookupCode, setLookupCode] = useState('LAPTOP-01');
  const [lookupResult, setLookupResult] = useState(null);
  const [form, setForm] = useState(defaultForm);
  const [lastResult, setLastResult] = useState(null);
  const [postCreateHint, setPostCreateHint] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function syncAuthState(extra = {}) {
    const snapshot = getAuthSnapshot();
    setAuthState({
      initializing: false,
      ...snapshot,
      error: '',
      ...extra
    });
    return snapshot;
  }

  useEffect(() => {
    let active = true;

    initKeycloak()
      .then(() => {
        if (!active) {
          return;
        }

        const snapshot = getAuthSnapshot();
        console.info('[web-ui-demo] Keycloak init complete', {
          authenticated: snapshot.authenticated,
          hasToken: snapshot.hasToken,
          username: snapshot.userInfo?.username,
          tenantId: snapshot.userInfo?.tenantId,
          roles: snapshot.userInfo ? formatRoles(snapshot.userInfo) : '(none)'
        });
        syncAuthState();
      })
      .catch((err) => {
        if (!active) {
          return;
        }

        const message = err.message ?? String(err);
        console.error('[web-ui-demo] Keycloak init failed', message);
        setAuthState({
          ...initialAuthState,
          initializing: false,
          error: message
        });
      });

    return () => {
      active = false;
    };
  }, []);

  const authReady = authState.authenticated && authState.hasToken;
  const actionDisabledReason = useMemo(() => {
    if (authState.initializing) {
      return 'Đang khởi tạo Keycloak.';
    }

    if (!authState.authenticated) {
      return 'Cần login Keycloak trước.';
    }

    if (!authState.hasToken) {
      return 'Login đã xong nhưng access token chưa sẵn sàng.';
    }

    return '';
  }, [authState]);

  async function runRequest(action) {
    setLoading(true);
    setError('');
    try {
      const result = await action();
      setLastResult(result);
      if (!result.ok) {
        setError(describeApiFailure(result));
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
    const result = await runRequest(() => loadMasterData(apiBaseUrl.trim()));
    if (result?.ok && Array.isArray(result.data)) {
      setRows(result.data);
    }
  }

  async function handleLookupByCode(event) {
    event.preventDefault();
    const code = lookupCode.trim();
    if (!code) {
      return;
    }

    const result = await runRequest(() => loadMasterDataByCode(code, apiBaseUrl.trim()));
    if (result?.ok) {
      setLookupResult(result.data);
    }
  }

  async function handleCreate(event) {
    event.preventDefault();
    setPostCreateHint('');
    const result = await runRequest(() => createMasterData(form, apiBaseUrl.trim()));
    if (result?.ok) {
      setForm(defaultForm());
      if (result.data) {
        setRows((current) => {
          if (result.data.id === undefined) {
            return [result.data, ...current];
          }
          return [result.data, ...current.filter((row) => row.id !== result.data.id)];
        });
      }
      setPostCreateHint('Nếu Kafka và audit-log-service đang chạy, đợi một chút rồi bấm Load audit events để xác nhận audit event đã được lưu.');
    }
  }

  async function handleLoadAuditEvents() {
    const result = await runRequest(() => loadAuditEvents(apiBaseUrl.trim()));
    if (result?.ok && Array.isArray(result.data)) {
      setAuditEvents(result.data);
    }
  }

  if (authState.initializing) {
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
          authState={authState}
          actionDisabledReason={actionDisabledReason}
          onLogin={() => {
            setError('');
            console.info('[web-ui-demo] Redirecting to Keycloak login');
            keycloak.login({ redirectUri: window.location.origin });
          }}
          onLogout={() => keycloak.logout({ redirectUri: window.location.origin })}
          onRefresh={async () => {
            try {
              await refreshToken(-1);
              syncAuthState();
            } catch (err) {
              const message = err.message ?? String(err);
              console.error('[web-ui-demo] Token refresh failed', message);
              syncAuthState({ error: message });
            }
          }}
        />
        <ApiPanel apiBaseUrl={apiBaseUrl} setApiBaseUrl={setApiBaseUrl} />
      </div>

      <StatusLine lastResult={lastResult} />
      {error && <pre className="error-box">{error}</pre>}

      <div className="grid">
        <DemoChecklist
          authReady={authReady}
          apiBaseUrl={apiBaseUrl}
          rows={rows}
          auditEvents={auditEvents}
          lastResult={lastResult}
        />
        <MasterDataList rows={rows} onLoad={handleLoad} loading={loading} disabled={!authReady} />
        <MasterDataByCodeLookup
          code={lookupCode}
          setCode={setLookupCode}
          result={lookupResult}
          onSubmit={handleLookupByCode}
          loading={loading}
          disabled={!authReady}
        />
        <CreateMasterDataForm form={form} setForm={setForm} onSubmit={handleCreate} loading={loading} disabled={!authReady} />
        {postCreateHint && <p className="status status-ok">{postCreateHint}</p>}
        <AuditEventsPanel events={auditEvents} onLoad={handleLoadAuditEvents} loading={loading} disabled={!authReady} />
      </div>
    </main>
  );
}
