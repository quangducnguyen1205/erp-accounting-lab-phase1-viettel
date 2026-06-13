import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export function AppShell({ activeScreen, onNavigate, authState, apiBaseUrl, gatewayName, onLogout, onRefresh, children }) {
  return (
    <div className="console-shell">
      <Sidebar activeScreen={activeScreen} onNavigate={onNavigate} />
      <div className="console-main">
        <Topbar
          authState={authState}
          apiBaseUrl={apiBaseUrl}
          gatewayName={gatewayName}
          onLogout={onLogout}
          onRefresh={onRefresh}
        />
        <main className="screen-content">{children}</main>
      </div>
    </div>
  );
}
