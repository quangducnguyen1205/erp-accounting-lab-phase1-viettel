import { Badge } from '../components/Badge';

const KEYCLOAK_INTERNAL_ROLES = [
  'offline_access',
  'uma_authorization',
  'manage-account',
  'manage-account-links',
  'view-profile'
];

function isKeycloakInternal(role) {
  return KEYCLOAK_INTERNAL_ROLES.includes(role) || role.startsWith('default-roles-');
}

function primaryAppRole(userInfo) {
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  return roles.find((role) => ['ADMIN', 'ACCOUNTANT', 'VIEWER'].includes(role)) ?? '(không xác định)';
}

function humanTenant(tenantId) {
  if (!tenantId) {
    return '(không xác định)';
  }
  return `Tenant ${tenantId}`;
}

export function AccountScreen({
  authState,
  apiBaseUrl,
  lastResult,
  onLogout,
  onRefresh
}) {
  const appRole = primaryAppRole(authState.userInfo);

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tài khoản</p>
        <h2>Thông tin tài khoản</h2>
        <p>Xem ngữ cảnh đăng nhập và vai trò của phiên hiện tại.</p>
      </section>

      <section className="panel panel-span-2 account-profile-panel">
        <div className="panel-heading">
          <div>
            <h3>Tài khoản đã đăng nhập</h3>
            <p>Hệ thống tự kiểm tra quyền trong mỗi thao tác.</p>
          </div>
          <Badge tone="success">Đã đăng nhập</Badge>
        </div>
        <dl className="facts">
          <dt>Username</dt>
          <dd>{authState.userInfo?.username ?? '(unknown)'}</dd>
          <dt>Tenant</dt>
          <dd>{humanTenant(authState.userInfo?.tenantId)}</dd>
          <dt>Vai trò ứng dụng</dt>
          <dd><Badge tone={appRole === 'ACCOUNTANT' ? 'indigo' : appRole === 'VIEWER' ? 'blue' : 'neutral'}>{appRole}</Badge></dd>
        </dl>
        <div className="form-actions account-actions">
          <button type="button" className="button-secondary" onClick={onRefresh}>Làm mới phiên</button>
          <button type="button" onClick={onLogout}>Đăng xuất</button>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Ghi chú bảo mật</h3>
            <p>Thông tin kỹ thuật nhạy cảm không hiển thị trực tiếp trong giao diện.</p>
          </div>
          <Badge tone="neutral">An toàn</Badge>
        </div>
        <ul className="plain-list">
          <li>Nội dung access token không được hiển thị trong giao diện.</li>
          <li>Tenant được xác định từ token đã xác thực, không lấy từ form.</li>
          <li>Vai trò quyết định thao tác nào được phép thực hiện.</li>
        </ul>
      </section>

      <section className="panel panel-span-3 technical-panel">
        <div className="panel-heading">
          <div>
            <h3>Thông tin kỹ thuật</h3>
            <p>Dành cho lúc demo hoặc debug. Không cần dùng trong thao tác nghiệp vụ hằng ngày.</p>
          </div>
          <Badge tone="neutral">Dành cho kỹ thuật</Badge>
        </div>

        <dl className="facts technical-facts">
          <dt>API base URL</dt>
          <dd><code>{apiBaseUrl}</code></dd>
          <dt>requestId gần nhất</dt>
          <dd>{lastResult?.requestId ? <code>{lastResult.requestId}</code> : 'Chưa có thao tác API trong phiên này'}</dd>
          <dt>Trạng thái gần nhất</dt>
          <dd>{lastResult ? (lastResult.ok ? 'Thành công' : 'Chưa thành công') : 'Chưa có'}</dd>
          <dt>Endpoint gần nhất</dt>
          <dd>{lastResult?.endpoint ? <code>{lastResult.endpoint}</code> : 'Chưa có'}</dd>
        </dl>
      </section>
    </div>
  );
}
