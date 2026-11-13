import test from 'node:test';
import assert from 'node:assert/strict';
import { can, grantable, canManage, passwordError, hasNextPage } from '../src/shared/access.ts';
import type { Identity } from '../src/shared/types.ts';
const admin: Identity = { id: 1, nome: 'Admin', email: 'a@a.com', empresaId: 10, nivel: 'ADMIN', permissoes: ['USUARIOS_CRIAR', 'USUARIOS_GERIR_PERMISSOES'], trocaSenhaObrigatoria: false };
test('ADMIN limited to granted permissions and CONSULTOR never administers', () => {
  assert.equal(can(admin, 'USUARIOS_CRIAR'), true);
  assert.equal(can(admin, 'USUARIOS_LISTAR'), false);
  assert.equal(can({ ...admin, nivel: 'CONSULTOR' }, 'USUARIOS_CRIAR'), false);
  assert.equal(can({ ...admin, trocaSenhaObrigatoria: true }, 'USUARIOS_CRIAR'), false);
  assert.equal(can({ ...admin, nivel: 'SUPERADMIN', permissoes: [] }, 'USUARIOS_LISTAR'), true);
  assert.deepEqual(grantable(admin, ['USUARIOS_CRIAR', 'USUARIOS_LISTAR']), ['USUARIOS_CRIAR']);
});
test('ADMIN may manage only own company CONSULTOR, never ADMIN or SUPERADMIN', () => {
  assert.equal(canManage(admin, { ...admin, id: 2, nivel: 'CONSULTOR' }), true);
  assert.equal(canManage(admin, { ...admin, empresaId: 11, nivel: 'CONSULTOR' }), false);
  assert.equal(canManage(admin, { ...admin, id: 2 }), false);
  assert.equal(canManage({ ...admin, nivel: 'SUPERADMIN' }, { ...admin, nivel: 'SUPERADMIN' }), false);
});
test('password policy counts code points and UTF-8 bytes; whitespace not special', () => {
  assert.equal(passwordError('abcdef1!'), '');
  assert.equal(passwordError('abc def1!'), '');
  assert.notEqual(passwordError('abcdef1 '), '');
  assert.notEqual(passwordError('é'.repeat(36) + '1!'), '');
  assert.notEqual(passwordError('abc1!'), '');
});
test('password accepts Unicode decimal digits, including supplementary code points', () => {
  for (const digit of ['١', '１', '𝟙']) assert.equal(passwordError(`abcdef${digit}!`), '', digit);
});
test('non-decimal Unicode numbers cannot satisfy the digit requirement', () => {
  for (const number of ['²', 'Ⅳ', '½']) assert.match(passwordError(`abcdef${number}!`), /número/, number);
});
test('non-decimal numbers and combining marks count as special, like Java', () => {
  for (const special of ['²', 'Ⅳ', '½', '\u0301', '\uFEFF', '\u0085']) {
    assert.equal(passwordError(`abcdef1${special}`), '', JSON.stringify(special));
  }
});
test('Java whitespace and space characters do not count as special', () => {
  for (const space of [' ', '\t', '\n', '\u001C', '\u001D', '\u001E', '\u001F', '\u00A0', '\u2007', '\u202F', '\u2028', '\u2029', '\u3000']) {
    assert.match(passwordError(`abcdef1${space}`), /especial/, JSON.stringify(space));
  }
  assert.match(passwordError('abcdef1١'), /especial/);
  assert.match(passwordError('abcdef1é'), /especial/);
});
test('blank password follows Java isBlank before length and number checks', () => {
  for (const blank of ['', ' '.repeat(80), '\u001C'.repeat(8)]) assert.match(passwordError(blank), /obrigatória/);
  // NBSP isSpaceChar=true, but isWhitespace=false: not blank under String.isBlank.
  assert.match(passwordError('\u00A0'.repeat(8)), /número/);
});
test('pagination uses total records, including companies after first page', () => {
  assert.equal(hasNextPage(0, 41, 20), true);
  assert.equal(hasNextPage(1, 41, 20), true);
  assert.equal(hasNextPage(2, 41, 20), false);
  assert.equal(hasNextPage(0, 0, 20), false);
});
