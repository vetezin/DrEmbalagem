export function Pagination({ page, total, loading, onChange }: { page: number; total: number; loading: boolean; onChange: (page: number) => void }) {
  return <nav className="pagination" aria-label="Paginação">
    <button type="button" className="secondary" disabled={loading || page === 0} onClick={() => onChange(page - 1)}>Anterior</button>
    <span aria-live="polite">Página {page + 1} · {total} registro(s)</span>
    <button type="button" className="secondary" disabled={loading || !hasNextPage(page, total)} onClick={() => onChange(page + 1)}>Próxima</button>
  </nav>;
}

function hasNextPage(page: number, total: number, size = 20) {
  return (page + 1) * size < total;
}