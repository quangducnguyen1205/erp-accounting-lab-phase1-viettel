import { useMemo, useRef, useState } from 'react';
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
  userInfo
}) {
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);
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
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      setUploadHint('Đã tải tệp tin lên. Metadata đã lưu theo tenant hiện tại.');
      setUploadHintTone('success');
    }
  }

  return (
    <div className="screen-grid files-layout">
      <section className="screen-heading">
        <p className="eyebrow">Tài liệu đính kèm</p>
        <h2>Tài liệu đính kèm</h2>
        <p>Tải lên, tải xuống và quản lý tệp tin trong phạm vi tenant hiện tại.</p>
      </section>

      <section className="panel panel-span-2 files-list-panel">
        <div className="panel-heading">
          <div>
            <h3>Danh sách tài liệu</h3>
            <p>Metadata tệp tin thuộc tenant hiện tại.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Đang tải...' : 'Tải danh sách'}</button>
        </div>
        {files.length > 0 ? (
          <DataTable
            columns={columns}
            rows={files}
            emptyTitle="Chưa có tệp tin"
            emptyMessage="Tải tệp tin lên để thấy metadata tại đây."
          />
        ) : (
          <EmptyState title="Chưa có tài liệu trong tenant này">
            Khi có tệp tin, danh sách sẽ hiển thị tên, dung lượng và thao tác tải xuống.
          </EmptyState>
        )}
      </section>

      <section className="panel upload-panel">
        <div className="panel-heading">
          <div>
            <h3>Tải tệp tin lên</h3>
            <p>Chọn một tệp tin nhỏ để lưu trong tenant hiện tại.</p>
          </div>
          <Badge tone={isViewer ? 'warning' : 'blue'}>{isViewer ? 'Chỉ đọc' : 'Có quyền ghi'}</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Vai trò VIEWER">
            Viewer có thể xem và tải tệp tin xuống, nhưng không được tải lên hoặc xóa.
          </Alert>
        )}
        <form className="upload-form" onSubmit={submitUpload}>
          <div className="file-input-wrap">
            <label className={`file-input-label${selectedFile ? ' has-file' : ''}`}>
              {selectedFile ? selectedFile.name : 'Chọn tệp tin để tải lên'}
              <input
                ref={fileInputRef}
                type="file"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
              />
            </label>
          </div>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Đang tải lên...' : 'Tải lên'}</button>
        </form>
        <p className="hint">Mọi thao tác tệp tin đều được kiểm tra theo tenant và vai trò đăng nhập.</p>
        {uploadHint && <Alert tone={uploadHintTone} title="Tệp tin">{uploadHint}</Alert>}
      </section>
    </div>
  );
}
