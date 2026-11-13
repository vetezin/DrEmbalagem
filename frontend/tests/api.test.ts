import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi, ApiError } from '../src/shared/api.ts';

function fixture(responses: Response[]) {
  const calls: { path: string; options?: RequestInit }[] = [];
  const api = createApi(async (input, options) => {
    calls.push({ path: String(input), options });
    const response = responses.shift();
    if (!response) throw new TypeError('offline');
    return response;
  });
  return { api, calls };
}
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const token = { headerName: 'X-CSRF-TOKEN', token: 'primeiro' };

test('mutation fetches CSRF, sends cookies and accepts 204 without JSON', async () => {
  const { api, calls } = fixture([json(token), new Response(null, { status: 204 })]);
  assert.equal(await api.request('/empresas', 'POST', { razaoSocial: 'Empresa' }), undefined);
  assert.equal(calls.length, 2);
  assert.equal(calls[0].path, '/api/auth/csrf');
  assert.equal(calls[1].options?.credentials, 'include');
  assert.equal(new Headers(calls[1].options?.headers).get('X-CSRF-TOKEN'), 'primeiro');
});
test('403 clears stale CSRF but never repeats mutation; next explicit attempt obtains token', async () => {
  const { api, calls } = fixture([json(token), json({ mensagem: 'Acesso negado.' }, 403), json({ ...token, token: 'novo' }), json({ id: 1 })]);
  await assert.rejects(api.request('/usuarios', 'POST', {}), (error: unknown) => error instanceof ApiError && error.status === 403);
  assert.equal(calls.length, 2);
  await api.request('/usuarios', 'POST', {});
  assert.equal(calls.length, 4);
  assert.equal(new Headers(calls[3].options?.headers).get('X-CSRF-TOKEN'), 'novo');
});
test('non JSON errors retain HTTP status and 401 invokes expiration handler', async () => {
  const { api } = fixture([new Response('internal', { status: 500 }), json({ mensagem: 'Autenticacao necessaria.' }, 401)]);
  let expired = 0;
  api.onExpired = () => { expired++; };
  await assert.rejects(api.request('/empresas'), (error: unknown) => error instanceof ApiError && error.status === 500 && !error.message.includes('internal'));
  await assert.rejects(api.request('/usuarios'), /Autenticacao necessaria/);
  assert.equal(expired, 1);
});
test('invalid login does not announce session expiration', async () => {
  const { api } = fixture([json(token), json({ mensagem: 'Credenciais invalidas.' }, 401)]);
  let expired = false;
  api.onExpired = () => { expired = true; };
  await assert.rejects(api.request('/auth/login', 'POST', {}), /Credenciais invalidas/);
  assert.equal(expired, false);
});
test('network failure does not retry or report successful persistence', async () => {
  const { api, calls } = fixture([json(token)]);
  await assert.rejects(api.request('/usuarios', 'POST', {}), /conexão/);
  assert.equal(calls.length, 2);
});
test('login, password change and logout discard old CSRF and fetch a new token', async () => {
  for (const path of ['/auth/login', '/auth/trocar-senha', '/auth/logout']) {
    const { api, calls } = fixture([json(token), path.endsWith('logout') ? new Response(null, { status: 204 }) : json({ id: 1 }), json({ ...token, token: 'renovado' }), json({ id: 2 })]);
    await api.request(path, 'POST', {});
    await api.request('/usuarios', 'POST', {});
    assert.equal(calls[2].path, '/api/auth/csrf');
    assert.equal(new Headers(calls[3].options?.headers).get('X-CSRF-TOKEN'), 'renovado');
  }
});
test('conflicts preserve backend message and status', async () => {
  const { api } = fixture([json(token), json({ mensagem: 'Conflito com dados existentes.' }, 409)]);
  await assert.rejects(api.request('/empresas', 'POST', {}), (error: unknown) => error instanceof ApiError && error.status === 409 && error.message === 'Conflito com dados existentes.');
});
test('interrupted response body yields actionable connection error without retry', async () => {
  const response = new Response('body');
  response.text = async () => { throw new TypeError('terminated'); };
  const { api, calls } = fixture([response]);
  await assert.rejects(api.request('/usuarios'), /conexão/);
  assert.equal(calls.length, 1);
});
test('successful auth transition remains successful if subsequent token refresh fails', async () => {
  const { api } = fixture([json(token), json({ id: 1 })]);
  assert.deepEqual(await api.request('/auth/login', 'POST', {}), { id: 1 });
  await assert.rejects(api.request('/usuarios', 'POST', {}), /conexão/);
});
