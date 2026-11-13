import { useState } from 'react';
import { auth } from './authService';
import { passwordError } from '../../shared/access';
import { errorMessage } from '../../shared/api';
import type { Identity } from '../../shared/types';
export const passwordHint = 'Mínimo de 8 caracteres, um número e um caractere especial não branco. Máximo de 72 bytes; acentos podem ocupar mais de um byte.';
export function NovaSenhaForm({ onSuccess }: { onSuccess: (usuario: Identity) => void }) {
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  return <section className="surface auth-panel"><h1>Definir nova senha</h1>
    <p>Troque a senha temporária para acessar o painel. A nova senha deve ser diferente da atual.</p>
    {erro && <p className="error" role="alert">{erro}</p>}
    <form onSubmit={async e => {
      e.preventDefault(); if (salvando) return;
      const validation = passwordError(novaSenha) || (novaSenha !== confirmacao ? 'Confirmação de senha não confere.' : '');
      if (validation) { setErro(validation); return; }
      setSalvando(true); setErro('');
      try { onSuccess(await auth.trocarSenha({ novaSenha, confirmacao })); }
      catch (error) { setErro(errorMessage(error)); }
      finally { setNovaSenha(''); setConfirmacao(''); setSalvando(false); }
    }}><fieldset disabled={salvando}>
      <label htmlFor="nova-senha">Nova senha</label><input id="nova-senha" type="password" autoComplete="new-password" aria-describedby="senha-ajuda" required value={novaSenha} onChange={e => setNovaSenha(e.target.value)} />
      <p id="senha-ajuda" className="hint">{passwordHint}</p>
      <label htmlFor="confirmacao">Confirmar nova senha</label><input id="confirmacao" type="password" autoComplete="new-password" required value={confirmacao} onChange={e => setConfirmacao(e.target.value)} />
      <button>{salvando ? 'Salvando…' : 'Salvar nova senha'}</button>
    </fieldset></form>
  </section>;
}
