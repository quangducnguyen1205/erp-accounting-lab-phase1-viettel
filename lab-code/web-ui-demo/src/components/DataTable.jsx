import { EmptyState } from './EmptyState';

export function DataTable({ columns, rows, emptyTitle, emptyMessage }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => <th key={column.key}>{column.label}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={row.key ?? row.id ?? row.eventId ?? rowIndex}>
              {columns.map((column) => (
                <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {rows.length === 0 && (
        <EmptyState title={emptyTitle} tone="soft">
          {emptyMessage}
        </EmptyState>
      )}
    </div>
  );
}
