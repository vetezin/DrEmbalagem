import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

// Extract the actual shared HTML pattern: no browser or HTTP is needed.
const form = readFileSync(new URL('../src/features/usuario/UsuarioForm.tsx', import.meta.url), 'utf8');
const login = readFileSync(new URL('../src/features/auth/LoginForm.tsx', import.meta.url), 'utf8');

test('cadastro e login usam mesmo pattern compartilhado para domínio completo', () => {
  for (const source of [form, login]) assert.match(source, /pattern=\{EMAIL_PATTERN\}/);
});

test('pattern rejeita domínio incompleto ou inválido e aceita TLDs variados', async () => {
  const { EMAIL_PATTERN } = await import('../src/shared/email.ts');
  const pattern = new RegExp(`^(?:${EMAIL_PATTERN})$`, 'v');
  for (const email of ['pessoa@exemplo.com', 'pessoa@exemplo.com.br', 'pessoa@exemplo.org', 'nome+visita@sub.exemplo.technology', 'pessoa@exemplo.xn--p1ai']) assert.equal(pattern.test(email), true, email);
  for (const email of ['pessoa', 'pessoa.exemplo.com', 'pessoa@exemplo', 'pessoa@@exemplo.com', 'pessoa@.com', 'pessoa@exemplo.', 'pessoa@exemplo..com', 'pessoa@-exemplo.com', 'pessoa@exemplo-.com', 'pessoa@exem_plo.com', 'pessoa@exemplo.123', 'pessoa@exemplo.c', 'pessoa@ex emplo.com', `pessoa@${'a'.repeat(64)}.com`]) assert.equal(pattern.test(email), false, email);
});
