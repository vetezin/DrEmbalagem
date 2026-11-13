export class ApiError extends Error {
  status: number;
  constructor(message: string, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}
interface Csrf { headerName: string; token: string }

// Tokens live only in this closure. Mutations are never replayed automatically.
export function createApi(fetcher: typeof fetch = fetch) {
  let csrf: Csrf | null = null;
  let csrfPending: Promise<Csrf> | null = null;
  const client = {
    onExpired: undefined as (() => void) | undefined,
    async refreshCsrf(): Promise<Csrf> {
      csrf = null;
      const next = await client.request<Csrf>('/auth/csrf');
      if (!next?.headerName || !next.token) throw new ApiError('Não foi possível proteger a solicitação. Tente novamente.');
      csrf = next;
      return next;
    },
    async request<T = void>(path: string, method = 'GET', body?: unknown): Promise<T> {
      const headers = new Headers({ Accept: 'application/json' });
      if (body !== undefined) headers.set('Content-Type', 'application/json');
      if (method !== 'GET') {
        if (!csrf) {
          csrfPending ??= client.refreshCsrf().finally(() => { csrfPending = null; });
          await csrfPending;
        }
        headers.set(csrf!.headerName, csrf!.token);
      }
      let response: Response;
      try {
        response = await fetcher(`/api${path}`, { method, headers, credentials: 'include', cache: 'no-store', body: body === undefined ? undefined : JSON.stringify(body) });
      } catch {
        throw new ApiError('Falha de conexão. Não foi possível confirmar a operação. Confira os dados antes de tentar novamente.');
      }
      let text: string;
      try { text = response.status === 204 ? '' : await response.text(); }
      catch { throw new ApiError('Falha de conexão ao receber a resposta. Não foi possível confirmar a operação. Confira os dados antes de tentar novamente.'); }
      let data: unknown;
      try { data = text ? JSON.parse(text) : undefined; } catch { data = undefined; }
      if (!response.ok) {
        if (response.status === 403 || response.status === 401) csrf = null;
        if (response.status === 401 && path !== '/auth/login') client.onExpired?.();
        const message = data && typeof data === 'object' && 'mensagem' in data && typeof data.mensagem === 'string'
          ? data.mensagem : `Não foi possível concluir a solicitação (${response.status}).`;
        throw new ApiError(response.status === 403 ? `${message} Sua permissão ou a proteção da sessão pode ter mudado. Atualize a sessão antes de tentar novamente.` : message, response.status);
      }
      if (method === 'POST' && ['/auth/login', '/auth/trocar-senha', '/auth/logout'].includes(path)) {
        csrf = null;
        // The mutation has succeeded; refresh failure must not misreport it as failed.
        await client.refreshCsrf().catch(() => { csrf = null; });
      }
      if (text && data === undefined) throw new ApiError('Resposta inválida do serviço. Não foi possível confirmar os dados.', response.status);
      return data as T;
    },
  };
  return client;
}
export const api = createApi();
export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Não foi possível concluir a operação.';
}
