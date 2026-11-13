import { useState } from 'react';
import { EMAIL_PATTERN } from '../../shared/email';
import { auth } from './authService';
import { errorMessage } from '../../shared/api';
import type { Identity } from '../../shared/types';

export function LoginForm({ onLoginSuccess }: { onLoginSuccess: (usuario: Identity) => void }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  return <section className="surface auth-panel">
    <p className="eyebrow">Acesso da equipe</p><h1>Entrar</h1>
    <p>Use o email e a senha fornecidos pelo administrador.</p>
    {erro && <p role="alert" className="error">{erro}</p>}
    <form onSubmit={async event => {
      event.preventDefault(); if (salvando) return;
      setSalvando(true); setErro('');
      try { onLoginSuccess(await auth.login({ email, senha })); }
      catch (error) { setErro(errorMessage(error)); }
      finally { setSenha(''); setSalvando(false); }
    }}>
      <fieldset disabled={salvando}>
        <label htmlFor="login-email">Email</label>
        <input id="login-email" type="email" pattern={EMAIL_PATTERN} title="Informe email com domínio completo, por exemplo nome@exemplo.org." autoComplete="username" maxLength={254} required value={email} onChange={e => setEmail(e.target.value)} />
        <label htmlFor="login-senha">Senha</label>
        <input id="login-senha" type="password" autoComplete="current-password" required value={senha} onChange={e => setSenha(e.target.value)} />
        <button type="submit">{salvando ? 'Entrando…' : 'Entrar'}</button>
      </fieldset>
    </form>
  </section>;
}
