import { Badge } from '../components/Badge';

const demoTools = [
  ['Grafana Loki', 'http://localhost:13001', 'Xem log backend trong lúc demo.'],
  ['Kafka UI', 'http://localhost:18082', 'Xem topic event và consumer group.'],
  ['Kong proxy', 'http://localhost:18000', 'Gateway base URL cho business API.'],
  ['Keycloak', 'http://localhost:18080', 'Identity provider và user demo local.']
];

const recipes = [
  '{service="tenant-demo"}',
  '{service="audit-log-service"}',
  '{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="',
  '{service=~"tenant-demo|kong-gateway"} |= "409"',
  '{service=~"tenant-demo|kong-gateway"} |= "403"'
];

export function AccountScreen({
  authState,
  apiBaseUrl,
  setApiBaseUrl,
  gatewayName,
  onLogout,
  onRefresh
}) {
  const roles = [...(authState.userInfo?.realmRoles ?? []), ...(authState.userInfo?.clientRoles ?? [])];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tài khoản</p>
        <h2>Thông tin tài khoản</h2>
        <p>Xem ngữ cảnh đăng nhập, vai trò và API base URL đang dùng cho demo.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Tài khoản đã đăng nhập</h3>
            <p>Chi tiết token được ẩn; backend service tự validate token trong mỗi API request.</p>
          </div>
          <Badge tone="success">Đã đăng nhập</Badge>
        </div>
        <dl className="facts">
          <dt>Username</dt>
          <dd>{authState.userInfo?.username ?? '(unknown)'}</dd>
          <dt>tenant_id</dt>
          <dd>{authState.userInfo?.tenantId ?? '(missing)'}</dd>
          <dt>Roles</dt>
          <dd>{roles.join(', ') || '(none)'}</dd>
          <dt>Access token</dt>
          <dd>{authState.hasToken ? 'có (ẩn)' : 'thiếu'}</dd>
          <dt>Hết hạn lúc</dt>
          <dd>{authState.tokenExpiresAt}</dd>
        </dl>
        <div className="form-actions account-actions">
          <button type="button" className="button-secondary" onClick={onRefresh}>Làm mới token</button>
          <button type="button" onClick={onLogout}>Đăng xuất</button>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>API qua Kong Gateway</h3>
            <p>Business API call của UI đi qua Kong Gateway.</p>
          </div>
          <Badge tone={gatewayName === 'Kong Gateway' ? 'blue' : 'warning'}>{gatewayName}</Badge>
        </div>
        <label className="field-label">
          API base URL
          <input value={apiBaseUrl} onChange={(event) => setApiBaseUrl(event.target.value)} aria-label="API base URL" />
        </label>
        <div className="preset-row account-presets">
          <button type="button" className="button-secondary" onClick={() => setApiBaseUrl('http://localhost:18000')}>
            Dùng Kong Gateway
          </button>
        </div>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Công cụ demo</h3>
            <p>Link phụ để giải thích backend flow. Product UI không gọi API của các công cụ này.</p>
          </div>
          <Badge tone="neutral">Chỉ local</Badge>
        </div>
        <div className="tool-link-grid">
          {demoTools.map(([title, url, body]) => (
            <a className="tool-link" href={url} target="_blank" rel="noreferrer" key={title}>
              <strong>{title}</strong>
              <span>{body}</span>
              <code>{url}</code>
            </a>
          ))}
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Câu lệnh LogQL</h3>
            <p>Dùng bên ngoài luồng sản phẩm khi giải thích kỹ thuật.</p>
          </div>
          <Badge tone="teal">Tùy chọn</Badge>
        </div>
        <div className="code-list">
          {recipes.map((recipe) => <code key={recipe}>{recipe}</code>)}
        </div>
      </section>
    </div>
  );
}
