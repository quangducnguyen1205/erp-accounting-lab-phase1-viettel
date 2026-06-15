import { Badge } from '../components/Badge';

export function AccountScreen({
  authState,
  onLogout,
  onRefresh
}) {
  const roles = [...(authState.userInfo?.realmRoles ?? []), ...(authState.userInfo?.clientRoles ?? [])];

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tài khoản</p>
        <h2>Thông tin tài khoản</h2>
        <p>Xem ngữ cảnh đăng nhập và vai trò của phiên hiện tại.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Tài khoản đã đăng nhập</h3>
            <p>Chi tiết token được ẩn. Hệ thống tự kiểm tra quyền trong mỗi thao tác.</p>
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

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Ghi chú bảo mật</h3>
            <p>Thông tin kỹ thuật nhạy cảm không hiển thị trực tiếp trong giao diện.</p>
          </div>
          <Badge tone="neutral">An toàn</Badge>
        </div>
        <ul className="plain-list">
          <li>Access token chỉ hiển thị trạng thái, không hiển thị nội dung.</li>
          <li>tenant_id được lấy từ token đã xác thực, không lấy từ form.</li>
          <li>Vai trò quyết định thao tác nào được phép thực hiện.</li>
        </ul>
      </section>
    </div>
  );
}
