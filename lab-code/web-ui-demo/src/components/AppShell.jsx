import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export function AppShell({ activeScreen, onNavigate, authState, onLogout, onRefresh, children }) {
  return (
    <div className="console-shell">
      <Sidebar activeScreen={activeScreen} onNavigate={onNavigate} />
      <div className="console-main">
        <Topbar
          authState={authState}
          onLogout={onLogout}
          onRefresh={onRefresh}
        />
        <main className="screen-content">{children}</main>
      </div>
    </div>
  );
}
