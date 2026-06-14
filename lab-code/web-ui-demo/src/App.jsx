import { useEffect, useMemo, useState } from 'react';
import { AppShell } from './components/AppShell';
import { RequestStatus } from './components/RequestStatus';
import {
  createMasterData,
  deleteFile,
  deleteMasterData,
  downloadFile,
  listFiles,
  loadAuditEvents,
  loadMasterData,
  loadMasterDataByCode,
  searchMasterData,
  uploadFile,
  updateMasterData
} from './api';
import { config } from './config';
import { getAuthSnapshot, initKeycloak, keycloak, refreshToken } from './keycloak';
import { AccountScreen } from './screens/AccountScreen';
import { ActivityLogScreen } from './screens/ActivityLogScreen';
import { DashboardScreen } from './screens/DashboardScreen';
import { FilesScreen } from './screens/FilesScreen';
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
  { label: 'Kong Gateway', url: 'http://localhost:18000' }
];

function defaultForm() {
  return {
    code: `MDP-${Date.now()}`,
    name: 'Danh mục mẫu',
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
    return '401 Unauthorized: token bị thiếu, hết hạn hoặc không hợp lệ. Hãy đăng nhập lại.';
  }

  if (result.status === 403) {
    return '403 Forbidden: đã đăng nhập nhưng vai trò hiện tại không được phép thực hiện thao tác này.';
  }

  if (result.status === 409) {
    return '409 Conflict: mã master_data này đã tồn tại trong tenant hiện tại.';
  }

  if (result.status >= 500) {
    return 'Lỗi server ngoài dự kiến. Dùng requestId để tra log backend trong Grafana Loki.';
  }

  return `Request thất bại với HTTP ${result.status}.`;
}

function formatRoles(userInfo) {
  if (!userInfo) {
    return '(none)';
  }

  return [...userInfo.realmRoles, ...userInfo.clientRoles].join(', ') || '(none)';
}

function gatewayName(apiBaseUrl) {
  return gatewayPresets.find((preset) => preset.url === apiBaseUrl.trim())?.label ?? 'Gateway tùy chỉnh';
}

export default function App() {
  const [authState, setAuthState] = useState(initialAuthState);
  const [activeScreen, setActiveScreen] = useState('dashboard');
  const [apiBaseUrl, setApiBaseUrl] = useState(config.apiBaseUrl);
  const [rows, setRows] = useState([]);
  const [auditEvents, setAuditEvents] = useState([]);
  const [files, setFiles] = useState([]);
  const [lookupCode, setLookupCode] = useState('LAPTOP-01');
  const [lookupResult, setLookupResult] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoaded, setSearchLoaded] = useState(false);
  const [form, setForm] = useState(defaultForm);
  const [lastResult, setLastResult] = useState(null);
  const [postCreateHint, setPostCreateHint] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [demoProgress, setDemoProgress] = useState({
    masterDataLoaded: false,
    createdMasterData: false,
    filesLoaded: false,
    uploadedFile: false,
    searchLoaded: false,
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
      return 'Keycloak đang khởi tạo.';
    }

    if (!authState.authenticated) {
      return 'Hãy đăng nhập bằng Keycloak trước.';
    }

    if (!authState.hasToken) {
      return 'Đăng nhập thành công nhưng access token chưa sẵn sàng.';
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

  async function handleSearchMasterData(event) {
    event.preventDefault();
    const keyword = searchKeyword.trim();
    if (!keyword) {
      return;
    }

    const result = await runRequest(() => searchMasterData(keyword, apiBaseUrl.trim()));
    if (result?.ok && Array.isArray(result.data)) {
      setSearchResults(result.data);
      setSearchLoaded(true);
      setDemoProgress((current) => ({ ...current, searchLoaded: true }));
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
      setPostCreateHint('Đã tạo bản ghi. Chờ một chút rồi mở Lịch sử hoạt động để xác nhận thay đổi.');
      return;
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
  }

  async function handleUpdate(row, payload) {
    setPostCreateHint('');
    const result = await runRequest(() => updateMasterData(row.id, payload, apiBaseUrl.trim()));

    if (result?.ok && result.data) {
      setRows((current) => current.map((item) => (item.id === result.data.id ? result.data : item)));
      if (lookupResult?.id === result.data.id) {
        setLookupResult(result.data);
      }
      setPostCreateHint('Đã cập nhật bản ghi.');
      return result;
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
    return result;
  }

  async function handleDeactivate(row) {
    setPostCreateHint('');
    const result = await runRequest(() => deleteMasterData(row.id, apiBaseUrl.trim()));

    if (result?.ok) {
      setRows((current) => current.filter((item) => item.id !== row.id));
      if (lookupResult?.id === row.id) {
        setLookupResult(null);
      }
      setPostCreateHint('Đã chuyển bản ghi sang trạng thái tạm ngưng.');
      return result;
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
    return result;
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

  async function handleLoadFiles() {
    const result = await runRequest(() => listFiles(apiBaseUrl.trim()));
    if (result?.ok && Array.isArray(result.data)) {
      setFiles(result.data);
      setDemoProgress((current) => ({ ...current, filesLoaded: true }));
    }
    return result;
  }

  async function handleUploadFile(file) {
    const result = await runRequest(() => uploadFile(file, apiBaseUrl.trim()));
    if (result?.ok && result.data) {
      setFiles((current) => [
        {
          ...result.data,
          tenantId: currentTenantId,
          createdAt: new Date().toISOString()
        },
        ...current.filter((item) => item.fileId !== result.data.fileId)
      ]);
      setDemoProgress((current) => ({ ...current, uploadedFile: true, filesLoaded: true }));
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
    return result;
  }

  async function handleDownloadFile(row) {
    const result = await runRequest(() => downloadFile(row.fileId, apiBaseUrl.trim()));
    if (result?.ok && result.data?.blob) {
      const url = URL.createObjectURL(result.data.blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = result.data.filename || row.originalFilename || row.fileId;
      link.click();
      URL.revokeObjectURL(url);
    }
    return result;
  }

  async function handleDeleteFile(row) {
    const confirmed = window.confirm(`Xóa file ${row.originalFilename}?`);
    if (!confirmed) {
      return null;
    }

    const result = await runRequest(() => deleteFile(row.fileId, apiBaseUrl.trim()));
    if (result?.ok) {
      setFiles((current) => current.filter((item) => item.fileId !== row.fileId));
    }

    if (result?.status === 403) {
      setDemoProgress((current) => ({ ...current, viewerCreateForbidden: true }));
    }
    return result;
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
          searchKeyword={searchKeyword}
          setSearchKeyword={setSearchKeyword}
          searchResults={searchResults}
          searchLoaded={searchLoaded}
          onBackendSearch={handleSearchMasterData}
          form={form}
          setForm={setForm}
          onCreate={handleCreate}
          onUpdate={handleUpdate}
          onDeactivate={handleDeactivate}
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

    if (activeScreen === 'files') {
      return (
        <FilesScreen
          files={files}
          onLoad={handleLoadFiles}
          onUpload={handleUploadFile}
          onDownload={handleDownloadFile}
          onDelete={handleDeleteFile}
          loading={loading}
          disabled={!authReady}
          lastResult={lastResult}
          userInfo={authState.userInfo}
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
        files={files}
        masterDataLoaded={demoProgress.masterDataLoaded}
        activityLoaded={demoProgress.auditEventsLoaded}
        filesLoaded={demoProgress.filesLoaded}
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
