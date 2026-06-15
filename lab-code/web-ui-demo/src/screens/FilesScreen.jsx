import { useMemo, useState } from 'react';
import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';

function formatSize(bytes) {
  if (bytes === null || bytes === undefined) {
    return '(không rõ)';
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function FilesScreen({
  files,
  onLoad,
  onUpload,
  onDownload,
  onDelete,
  loading,
  disabled,
  lastResult,
  userInfo
}) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadHint, setUploadHint] = useState('');
  const [uploadHintTone, setUploadHintTone] = useState('info');
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  const isViewer = roles.includes('VIEWER') && !roles.includes('ACCOUNTANT') && !roles.includes('ADMIN');

  const columns = useMemo(() => [
    { key: 'originalFilename', label: 'Tên file' },
    { key: 'contentType', label: 'Loại' },
    { key: 'sizeBytes', label: 'Dung lượng', render: (row) => formatSize(row.sizeBytes) },
    { key: 'createdAt', label: 'Tải lên lúc', render: (row) => row.createdAt ?? '(vừa tải lên)' },
    {
      key: 'actions',
      label: 'Thao tác',
      render: (row) => (
        <div className="row-actions">
          <button type="button" className="button-secondary" onClick={() => onDownload(row)} disabled={disabled || loading}>
            Tải xuống
          </button>
          <button type="button" className="button-danger" onClick={() => onDelete(row)} disabled={disabled || loading}>
            Xóa
          </button>
        </div>
      )
    }
  ], [disabled, loading, onDelete, onDownload]);

  async function submitUpload(event) {
    event.preventDefault();
    setUploadHint('');
    if (!selectedFile) {
      setUploadHint('Hãy chọn file trước khi tải lên.');
      setUploadHintTone('warning');
      return;
    }

    const result = await onUpload(selectedFile);
    if (result?.ok) {
      setSelectedFile(null);
      event.target.reset();
      setUploadHint('Đã tải file lên. Metadata đã lưu theo tenant hiện tại.');
      setUploadHintTone('success');
    }
  }

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Tệp tin</p>
        <h2>Tệp tin tenant</h2>
        <p>Tải lên, tải xuống và quản lý tệp tin trong phạm vi tenant hiện tại.</p>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Tải file lên</h3>
            <p>Chọn một file nhỏ để lưu vào kho tệp tin của tenant.</p>
          </div>
          <Badge tone="blue">Tenant-safe</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem và tải file xuống, nhưng không được upload hoặc xóa file.
          </Alert>
        )}
        <form className="upload-form" onSubmit={submitUpload}>
          <label className="field-label">
            Chọn file
            <input type="file" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} />
          </label>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tải lên...' : 'Tải lên'}</button>
        </form>
        <p className="hint">Mọi thao tác tệp tin đều được kiểm tra theo tenant và vai trò đăng nhập.</p>
        {uploadHint && <Alert tone={uploadHintTone} title="File">{uploadHint}</Alert>}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Danh sách file</h3>
            <p>Danh sách tệp tin thuộc tenant hiện tại.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải danh sách'}</button>
        </div>
        {files.length > 0 ? (
          <DataTable
            columns={columns}
            rows={files}
            emptyTitle="Chưa có file"
            emptyMessage="Tải file lên để thấy metadata tại đây."
          />
        ) : (
          <EmptyState title="Chưa có file trong tenant này">
            Chưa có file nào được tải lên cho tenant này.
          </EmptyState>
        )}
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Trạng thái thường gặp</h3>
            <p>Các thông báo phổ biến khi quản lý tệp tin.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : lastResult ? 'danger' : 'neutral'}>
            {lastResult ? (lastResult.ok ? 'Thành công' : 'Cần kiểm tra') : 'Chưa chạy'}
          </Badge>
        </div>
        <div className="state-grid">
          <Alert tone="info" title="Tải lên thành công">File mới sẽ xuất hiện trong danh sách sau khi tải lại.</Alert>
          <Alert tone="warning" title="Không đủ quyền">Viewer không được upload hoặc xóa file.</Alert>
          <Alert tone="danger" title="Không tìm thấy">File không tồn tại hoặc bạn không có quyền truy cập.</Alert>
          <Alert tone="info" title="Hệ thống chưa sẵn sàng">Chờ một chút rồi thử lại sau.</Alert>
        </div>
      </section>
    </div>
  );
}
