import { useMemo, useState } from 'react';
import { Alert } from '../components/Alert';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';

const columns = [
  { key: 'code', label: 'Code', render: (row) => <code>{row.code}</code> },
  { key: 'name', label: 'Name' },
  { key: 'category', label: 'Type', render: (row) => row.category ?? row.type ?? '(not returned)' },
  { key: 'isActive', label: 'Status', render: (row) => <Badge tone={row.isActive ? 'success' : 'warning'}>{row.isActive ? 'Active' : 'Inactive'}</Badge> },
  { key: 'updatedAt', label: 'Updated', render: (row) => row.updatedAt ?? row.createdAt ?? '(not returned)' }
];

export function MasterDataScreen({
  rows,
  onLoad,
  loading,
  disabled,
  lookupCode,
  setLookupCode,
  lookupResult,
  onLookup,
  form,
  setForm,
  onCreate,
  onGenerateCode,
  postCreateHint,
  lastResult,
  userInfo
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const roles = [...(userInfo?.realmRoles ?? []), ...(userInfo?.clientRoles ?? [])];
  const isViewer = roles.includes('VIEWER') && !roles.includes('ACCOUNTANT') && !roles.includes('ADMIN');
  const filteredRows = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) {
      return rows;
    }

    return rows.filter((row) => {
      const code = String(row.code ?? '').toLowerCase();
      const name = String(row.name ?? '').toLowerCase();
      return code.includes(term) || name.includes(term);
    });
  }, [rows, searchTerm]);

  const updateField = (field) => (event) => {
    const value = field === 'isActive' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  return (
    <div className="screen-grid">
      <section className="screen-heading">
        <p className="eyebrow">Master Data</p>
        <h2>Master Data</h2>
        <p>Create, find and review tenant-scoped reference records.</p>
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Records</h3>
            <p>Load records for the current tenant, then filter by code or name.</p>
          </div>
          <button type="button" onClick={onLoad} disabled={disabled || loading}>{loading ? 'Loading...' : 'Load master data'}</button>
        </div>
        <label className="field-label table-filter">
          Search loaded records
          <input value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Search by code or name" />
        </label>
        <DataTable
          columns={columns}
          rows={filteredRows}
          emptyTitle="No records loaded"
          emptyMessage={searchTerm ? 'No loaded records match this search.' : 'Load master data after signing in. Empty data is valid for a fresh tenant.'}
        />
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Load by code</h3>
            <p>Find one record by its business code.</p>
          </div>
          <Badge tone="teal">Lookup</Badge>
        </div>
        <form className="inline-form" onSubmit={onLookup}>
          <label>
            Code
            <input value={lookupCode} onChange={(event) => setLookupCode(event.target.value)} required />
          </label>
          <button type="submit" disabled={disabled || loading}>{loading ? 'Loading...' : 'Load by code'}</button>
        </form>
        {lookupResult ? (
          <dl className="facts result-facts">
            <dt>ID</dt>
            <dd>{lookupResult.id}</dd>
            <dt>Code</dt>
            <dd><code>{lookupResult.code}</code></dd>
            <dt>Name</dt>
            <dd>{lookupResult.name}</dd>
            <dt>Type</dt>
            <dd>{lookupResult.category}</dd>
          </dl>
        ) : (
          <EmptyState title="No lookup yet">Cache behavior is verified through backend logs/metrics, not a UI badge.</EmptyState>
        )}
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Create master data</h3>
            <p>Add a tenant-scoped reference record.</p>
          </div>
          <Badge tone="blue">ACCOUNTANT</Badge>
        </div>
        {isViewer && (
          <Alert tone="warning" title="Viewer role">
            Viewer can read records but cannot create new records. Submitting will show the backend 403 behavior.
          </Alert>
        )}
        <form className="form-grid" onSubmit={onCreate}>
          <label>
            Code
            <input value={form.code} onChange={updateField('code')} required />
          </label>
          <label>
            Name
            <input value={form.name} onChange={updateField('name')} required />
          </label>
          <label>
            Category
            <input value={form.category} onChange={updateField('category')} required />
          </label>
          <label className="check-row">
            <input type="checkbox" checked={form.isActive} onChange={updateField('isActive')} />
            Active
          </label>
          <div className="form-actions">
            <button type="button" className="button-secondary" onClick={onGenerateCode}>Generate code</button>
            <button type="submit" disabled={disabled || loading}>{loading ? 'Creating...' : 'Create'}</button>
          </div>
        </form>
        <p className="hint">Code must be unique per tenant. Duplicate codes return `409 Conflict`.</p>
        {postCreateHint && <Alert tone="success" title="Create succeeded">{postCreateHint}</Alert>}
      </section>

      <section className="panel panel-span-2">
        <div className="panel-heading">
          <div>
            <h3>Error states</h3>
            <p>These states should be clear during the mentor demo.</p>
          </div>
          <Badge tone={lastResult?.ok ? 'success' : 'neutral'}>{lastResult ? `HTTP ${lastResult.status}` : 'Idle'}</Badge>
        </div>
        <div className="state-grid">
          <Alert tone="danger" title="401 Unauthorized">Missing, expired or invalid token. Sign in again.</Alert>
          <Alert tone="warning" title="403 Forbidden">Authenticated but the role is not allowed to create.</Alert>
          <Alert tone="danger" title="409 Conflict">Duplicate code in the same tenant. Change the code and retry.</Alert>
          <Alert tone="info" title="Service unavailable">Kong cannot reach an upstream service. Check process and port.</Alert>
        </div>
      </section>
    </div>
  );
}
