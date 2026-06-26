import { Badge } from '../components/Badge';

function primaryRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? roles[0] ?? 'NO_ROLE';
}

function humanTenant(tenantId) {
  if (!tenantId) {
    return '(không xác định)';
  }
  return `Tenant ${tenantId}`;
}

const OBSERVABILITY_TOOLS = [
  { label: 'Grafana / Loki', description: 'Log aggregation và truy vấn', url: 'http://localhost:13001' },
  { label: 'Kafka UI', description: 'Quản lý topic và consumer', url: 'http://localhost:18082' },
  { label: 'Keycloak Admin', description: 'Quản lý realm, user, role', url: 'http://localhost:18080' },
  { label: 'Kong Gateway', description: 'API proxy và routing', url: 'http://localhost:18000' }
];

const DEMO_FLOW_STEPS = [
  { step: 1, label: 'React UI' },
  { step: 2, label: 'Keycloak' },
  { step: 3, label: 'Kong Gateway' },
  { step: 4, label: 'Backend Services' },
  { step: 5, label: 'Storage & Events' }
];

export function DemoScreen({ authState, apiBaseUrl, lastResult }) {
  const user = authState.userInfo;
  const role = primaryRole(user);

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Demo & kỹ thuật</p>
        <h2>Demo & kỹ thuật</h2>
        <p>Thông tin phiên, bằng chứng API, và công cụ quan sát cho mục đích trình bày và gỡ lỗi.</p>
      </section>

      <section className="panel panel-span-2 demo-panel">
        <div className="panel-heading">
          <div>
            <h3>Phiên hiện tại</h3>
            <p>Thông tin phiên đăng nhập đang hoạt động.</p>
          </div>
          <Badge tone="success">Đang hoạt động</Badge>
        </div>
        <dl className="facts">
          <dt>Username</dt>
          <dd>{user?.username ?? '(unknown)'}</dd>
          <dt>Tenant</dt>
          <dd>{humanTenant(user?.tenantId)}</dd>
          <dt>Vai trò ứng dụng</dt>
          <dd><Badge tone={role === 'ACCOUNTANT' ? 'indigo' : role === 'VIEWER' ? 'blue' : 'neutral'}>{role}</Badge></dd>
          <dt>Token</dt>
          <dd><Badge tone={authState.hasToken ? 'success' : 'warning'}>{authState.hasToken ? 'Sẵn sàng' : 'Chưa sẵn sàng'}</Badge></dd>
        </dl>
      </section>

      <section className="panel demo-panel">
        <div className="panel-heading">
          <div>
            <h3>Yêu cầu gần nhất</h3>
            <p>Dữ liệu từ lần gọi API cuối cùng trong phiên.</p>
          </div>
          <Badge tone={lastResult ? (lastResult.ok ? 'success' : 'danger') : 'neutral'}>
            {lastResult ? (lastResult.ok ? 'OK' : 'Lỗi') : 'Chưa có'}
          </Badge>
        </div>
        {lastResult ? (
          <dl className="facts technical-facts">
            <dt>Endpoint</dt>
            <dd><code>{lastResult.endpoint}</code></dd>
            <dt>Request ID</dt>
            <dd><code>{lastResult.requestId}</code></dd>
            <dt>HTTP Status</dt>
            <dd><Badge tone={lastResult.ok ? 'success' : 'danger'}>{lastResult.status}</Badge></dd>
            <dt>API Base URL</dt>
            <dd><code>{apiBaseUrl}</code></dd>
          </dl>
        ) : (
          <div className="empty-state empty-state-soft">
            <strong>Chưa có thao tác API</strong>
            <p>Thực hiện một thao tác nghiệp vụ (tải dữ liệu, tạo bản ghi, v.v.) để xem thông tin tại đây.</p>
          </div>
        )}
      </section>

      <section className="panel panel-span-3 demo-panel">
        <div className="panel-heading">
          <div>
            <h3>Luồng demo</h3>
            <p>Đường đi của một yêu cầu từ giao diện đến hệ thống lưu trữ.</p>
          </div>
          <Badge tone="blue">Kiến trúc</Badge>
        </div>
        <div className="flow-strip">
          {DEMO_FLOW_STEPS.map((item) => (
            <div key={item.step} className="flow-step">
              <span>{item.step}</span>
              <strong>{item.label}</strong>
            </div>
          ))}
        </div>
        <p className="hint">
          Mỗi yêu cầu đi qua Keycloak (xác thực) → Kong (proxy, rate-limit) → backend service (nghiệp vụ, phân quyền tenant) → lưu trữ (PostgreSQL, MinIO, Kafka, Elasticsearch).
        </p>
      </section>

      <section className="panel panel-span-3 demo-panel">
        <div className="panel-heading">
          <div>
            <h3>Công cụ quan sát</h3>
            <p>Các công cụ local đã được cấu hình trong dự án. Mở trong tab mới.</p>
          </div>
          <Badge tone="neutral">Local</Badge>
        </div>
        <div className="tool-link-grid">
          {OBSERVABILITY_TOOLS.map((tool) => (
            <a key={tool.url} href={tool.url} target="_blank" rel="noopener noreferrer" className="tool-link">
              <strong>{tool.label}</strong>
              <span>{tool.description}</span>
              <code>{tool.url}</code>
            </a>
          ))}
        </div>
      </section>
    </div>
  );
}
