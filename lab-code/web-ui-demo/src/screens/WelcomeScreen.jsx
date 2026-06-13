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
              <strong>Cổng quản lý danh mục</strong>
              <span>Quản lý dữ liệu dùng chung theo tenant</span>
            </div>
          </div>

          <p>
            Tạo, xem và theo dõi dữ liệu danh mục dùng chung cho từng tenant.
            Đăng nhập, phân quyền và cô lập tenant được xử lý ở phía sau.
          </p>

          <div className="flow-grid">
            {['Dữ liệu theo tenant', 'Lịch sử hoạt động', 'Phân quyền theo vai trò', 'Trạng thái request'].map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
        </div>

        <div className="welcome-card">
          <p className="eyebrow">Business portal local</p>
          <h1>Cổng quản lý danh mục</h1>
          <p>
            Quản lý dữ liệu dùng chung theo tenant. Đăng nhập để tải dữ liệu,
            tạo bản ghi mới và xem lịch sử hoạt động.
          </p>

          <button type="button" className="button-primary button-large" onClick={onLogin} disabled={authState.initializing}>
            {authState.initializing ? 'Đang chuẩn bị đăng nhập...' : 'Đăng nhập'}
          </button>

          <div className="account-hints">
            <h2>Tài khoản demo</h2>
            <div>
              <code>tenant1-user / password</code>
              <Badge tone="success">ACCOUNTANT</Badge>
            </div>
            <div>
              <code>tenant2-user / password</code>
              <Badge tone="indigo">VIEWER</Badge>
            </div>
            <small>Tài khoản local-only cho lab học tập.</small>
          </div>

          <p className="token-note">Access token không bao giờ được hiển thị trong UI này.</p>

          {authState.warning && <Alert tone="warning" title="Cảnh báo auth">{authState.warning}</Alert>}
          {authState.error && <Alert tone="danger" title="Lỗi đăng nhập">{authState.error}</Alert>}
        </div>
      </section>
    </main>
  );
}
