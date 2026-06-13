import { useEffect, useMemo, useState } from 'react';
import { AppShell } from './components/AppShell';
import { RequestStatus } from './components/RequestStatus';
import { createMasterData, loadAuditEvents, loadMasterData, loadMasterDataByCode } from './api';
import { config } from './config';
import { getAuthSnapshot, initKeycloak, keycloak, refreshToken } from './keycloak';
import { AccountScreen } from './screens/AccountScreen';
import { ActivityLogScreen } from './screens/ActivityLogScreen';
import { DashboardScreen } from './screens/DashboardScreen';
import { MasterDataScreen } from './screens/MasterDataScreen';
import { WelcomeScreen } from './screens/WelcomeScreen';

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
    code: `MDP-${Date.now()}`,
    name: 'Portal Master Data',
    category: 'BUSINESS_REFERENCE',
    isActive: true
  };
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
    return '401 Unauthorized: missing, expired or invalid token. Sign in again.';
  }

  if (result.status === 403) {
    return '403 Forbidden: authenticated, but the current role is not allowed to perform this action.';
  }

  if (result.status === 409) {
    return '409 Conflict: this master_data code already exists in the current tenant.';
  }

  if (result.status >= 500) {
    return 'Unexpected server error. Use the requestId to inspect backend logs in Grafana Loki.';
  }

  return `Request failed with HTTP ${result.status}.`;
}

function formatRoles(userInfo) {
  if (!userInfo) {
    return '(none)';
  }

  return [...userInfo.realmRoles, ...userInfo.clientRoles].join(', ') || '(none)';
}

function gatewayName(apiBaseUrl) {
  return gatewayPresets.find((preset) => preset.url === apiBaseUrl.trim())?.label ?? 'Custom Gateway';
}

export default function App() {
  const [authState, setAuthState] = useState(initialAuthState);
  const [activeScreen, setActiveScreen] = useState('dashboard');
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
  const [demoProgress, setDemoProgress] = useState({
    masterDataLoaded: false,
    createdMasterData: false,
    auditEventsLoaded: false,
    tenantIsolationChecked: false,
    viewerCreateForbidden: false
  });

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
        if (snapshot.authenticated) {
          console.info('[web-ui-demo] Keycloak ready', {
            username: snapshot.userInfo?.username,
            tenantId: snapshot.userInfo?.tenantId,
            roles: snapshot.userInfo ? formatRoles(snapshot.userInfo) : '(none)'
          });
        }
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
  const currentGatewayName = gatewayName(apiBaseUrl);
  const currentTenantId = authState.userInfo?.tenantId;

  const actionDisabledReason = useMemo(() => {
    if (authState.initializing) {
      return 'Keycloak is initializing.';
    }

    if (!authState.authenticated) {
      return 'Sign in with Keycloak first.';
    }

    if (!authState.hasToken) {
      return 'Login succeeded, but the access token is not ready.';
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
      setDemoProgress((current) => ({ ...current, masterDataLoaded: true }));
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
      setDemoProgress((current) => ({ ...current, createdMasterData: true }));
      if (result.data) {
        setRows((current) => {
          if (result.data.id === undefined) {
            return [result.data, ...current];
          }
          return [result.data, ...current.filter((row) => row.id !== result.data.id)];
        });
      }
      setPostCreateHint('Record created. Wait a moment, then open Activity Log to confirm the change appears.');
      return;
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
  }

  async function handleLoadAuditEvents() {
    const result = await runRequest(() => loadAuditEvents(apiBaseUrl.trim()));
    if (result?.ok && Array.isArray(result.data)) {
      setAuditEvents(result.data);
      setDemoProgress((current) => ({
        ...current,
        auditEventsLoaded: true,
        tenantIsolationChecked: current.tenantIsolationChecked || currentTenantId === 2 || currentTenantId === '2'
      }));
    }
  }

  function handleGenerateCode() {
    setForm(defaultForm());
  }

  async function handleRefreshToken() {
    try {
      await refreshToken(-1);
      syncAuthState();
    } catch (err) {
      const message = err.message ?? String(err);
      console.error('[web-ui-demo] Token refresh failed', message);
      syncAuthState({ error: message });
    }
  }

  function renderScreen() {
    if (activeScreen === 'master-data') {
      return (
        <MasterDataScreen
          rows={rows}
          onLoad={handleLoad}
          loading={loading}
          disabled={!authReady}
          lookupCode={lookupCode}
          setLookupCode={setLookupCode}
          lookupResult={lookupResult}
          onLookup={handleLookupByCode}
          form={form}
          setForm={setForm}
          onCreate={handleCreate}
          onGenerateCode={handleGenerateCode}
          postCreateHint={postCreateHint}
          lastResult={lastResult}
          userInfo={authState.userInfo}
        />
      );
    }

    if (activeScreen === 'activity-log') {
      return (
        <ActivityLogScreen
          events={auditEvents}
          onLoad={handleLoadAuditEvents}
          loading={loading}
          disabled={!authReady}
          activityLoaded={demoProgress.auditEventsLoaded}
          tenantId={currentTenantId}
        />
      );
    }

    if (activeScreen === 'account') {
      return (
        <AccountScreen
          authState={authState}
          apiBaseUrl={apiBaseUrl}
          setApiBaseUrl={setApiBaseUrl}
          gatewayPresets={gatewayPresets}
          gatewayName={currentGatewayName}
          onLogout={() => keycloak.logout({ redirectUri: window.location.origin })}
          onRefresh={handleRefreshToken}
        />
      );
    }

    return (
      <DashboardScreen
        authState={authState}
        rows={rows}
        auditEvents={auditEvents}
        masterDataLoaded={demoProgress.masterDataLoaded}
        activityLoaded={demoProgress.auditEventsLoaded}
        lastResult={lastResult}
        onNavigate={setActiveScreen}
      />
    );
  }

  if (!authReady) {
    return (
      <WelcomeScreen
        authState={{ ...authState, error: authState.error || error }}
        onLogin={() => {
          setError('');
          keycloak.login({ redirectUri: window.location.origin });
        }}
      />
    );
  }

  return (
    <AppShell
      activeScreen={activeScreen}
      onNavigate={setActiveScreen}
      authState={authState}
      apiBaseUrl={apiBaseUrl}
      gatewayName={currentGatewayName}
      onLogout={() => keycloak.logout({ redirectUri: window.location.origin })}
      onRefresh={handleRefreshToken}
    >
      <RequestStatus lastResult={lastResult} error={error || actionDisabledReason} />
      {authState.warning && <div className="screen-message">{authState.warning}</div>}
      {renderScreen()}
    </AppShell>
  );
}
