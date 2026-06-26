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
              <strong>Master Data Portal</strong>
              <span>Cổng quản lý danh mục</span>
            </div>
          </div>

          <p>
            Quản lý dữ liệu danh mục dùng chung cho từng tenant.
            Đăng nhập, phân quyền và cô lập tenant được xử lý ở phía backend.
          </p>

          <div className="flow-grid">
            {['Dữ liệu theo tenant', 'Tra cứu danh mục', 'Phân quyền theo vai trò', 'Tài liệu đính kèm'].map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
        </div>

        <div className="welcome-card">
          <p className="eyebrow">Master Data Portal</p>
          <h1>Cổng quản lý danh mục</h1>
          <p>
            Quản lý dữ liệu dùng chung theo tenant. Đăng nhập để tải dữ liệu,
            tạo bản ghi mới và tra cứu danh mục.
          </p>

          <button type="button" className="button-primary button-large" onClick={onLogin} disabled={authState.initializing}>
            {authState.initializing ? 'Đang chuẩn bị đăng nhập...' : 'Đăng nhập'}
          </button>

          <div className="account-hints">
            <h2>Tài khoản demo</h2>
            <div>
              <code>tenant1-user</code>
              <Badge tone="indigo">ACCOUNTANT</Badge>
            </div>
            <div>
              <code>tenant2-user</code>
              <Badge tone="blue">VIEWER</Badge>
            </div>
            <small>Thông tin mật khẩu demo được giữ trong tài liệu hướng dẫn nội bộ.</small>
          </div>

          <p className="token-note">Access token không bao giờ được hiển thị trong giao diện này.</p>

          {authState.warning && <Alert tone="warning" title="Cảnh báo auth">{authState.warning}</Alert>}
          {authState.error && <Alert tone="danger" title="Lỗi đăng nhập">{authState.error}</Alert>}
        </div>
      </section>
    </main>
  );
}
