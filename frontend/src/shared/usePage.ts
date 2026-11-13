import { useEffect, useState } from 'react';
import { api, errorMessage } from './api';
import type { Pagina } from './types';
export function usePage<T>(path: string) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Pagina<T>>({ page: 0, total: 0, itens: [] });
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let current = true;
    setLoading(true); setErro('');
    api.request<Pagina<T>>(`${path}?page=${page}&size=20`)
      .then(result => { if (current) setData(result); })
      .catch(error => { if (current) setErro(errorMessage(error)); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [path, page, revision]);
  return { ...data, page, setPage, loading, erro, reload: () => setRevision(value => value + 1) };
}