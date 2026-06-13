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
        <p>Upload, tải xuống và quản lý metadata file trong phạm vi tenant hiện tại.</p>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Tải file lên</h3>
            <p>File binary được lưu ở MinIO; metadata và tenant ownership lưu trong PostgreSQL.</p>
          </div>
          <Badge tone="blue">MinIO</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem và tải file xuống, nhưng không được upload hoặc xóa file. Backend sẽ trả HTTP 403.
          </Alert>
        )}
        <form className="upload-form" onSubmit={submitUpload}>
          <label className="field-label">
            Chọn file
            <input type="file" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} />
          </label>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tải lên...' : 'Tải lên'}</button>
        </form>
        <p className="hint">UI không gọi MinIO trực tiếp. Mọi request đi qua Kong Gateway rồi tới file-service.</p>
        {uploadHint && <Alert tone={uploadHintTone} title="File">{uploadHint}</Alert>}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Danh sách file</h3>
            <p>Danh sách metadata của tenant hiện tại. Cross-tenant download trả 404 để không lộ fileId.</p>
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
            HTTP 200 với danh sách rỗng vẫn là trạng thái hợp lệ.
          </EmptyState>
        )}
      </section>

      <section className="panel panel-span-3">
        <div className="panel-heading">
          <div>
            <h3>Trạng thái cần demo</h3>
            <p>Dùng requestId/status để đối chiếu log file-service trong Grafana Loki.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : lastResult ? 'danger' : 'neutral'}>
            {lastResult ? `HTTP ${lastResult.status}` : 'Chưa chạy'}
          </Badge>
        </div>
        <div className="state-grid">
          <Alert tone="info" title="Upload thành công">Kỳ vọng HTTP 201 và metadata có fileId mới.</Alert>
          <Alert tone="warning" title="403 Forbidden">Viewer không được upload/xóa file.</Alert>
          <Alert tone="danger" title="404 Not Found">File không tồn tại trong tenant hiện tại hoặc thuộc tenant khác.</Alert>
          <Alert tone="info" title="Loki">Tìm log bằng <code>{'{service="file-service"}'}</code> hoặc requestId.</Alert>
        </div>
      </section>
    </div>
  );
}
