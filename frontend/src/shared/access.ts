import type { Identity } from './types.ts';
export function can(actor: Identity, permission: string) {
  return !actor.trocaSenhaObrigatoria && (actor.nivel === 'SUPERADMIN' || (actor.nivel === 'ADMIN' && actor.permissoes.includes(permission)));
}
export function grantable(actor: Identity, catalog: string[]) {
  return catalog.filter(permission => can(actor, permission));
}
export function canManage(actor: Identity, target: Identity) {
  return can(actor, 'USUARIOS_GERIR_PERMISSOES') && target.nivel !== 'SUPERADMIN'
    && (actor.nivel === 'SUPERADMIN' || (target.nivel === 'CONSULTOR' && target.empresaId === actor.empresaId));
}
export function passwordError(password: string) {
  // Java isWhitespace: separators except the three nonbreaking spaces, plus these controls.
  // JS \s differs (e.g. FEFF and U+001C); isDigit is Nd, not the broader N category.
  if (/^[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]*$/u.test(password)) return 'Senha obrigatória.';
  if (new TextEncoder().encode(password).length > 72) return 'Senha deve ter no máximo 72 bytes (acentos podem ocupar mais de um byte).';
  if ([...password].length < 8) return 'Senha deve ter no mínimo 8 caracteres.';
  if (!/\p{Nd}/u.test(password)) return 'Senha deve conter pelo menos um número.';
  // isWhitespace OR isSpaceChar excludes all separators and Java whitespace controls.
  if (!/[^\p{L}\p{Nd}\p{Z}\u0009-\u000D\u001C-\u001F]/u.test(password)) return 'Senha deve conter pelo menos um caractere especial não branco.';
  return '';
}
export function hasNextPage(page: number, total: number, size = 20) {
  return (page + 1) * size < total;
}
