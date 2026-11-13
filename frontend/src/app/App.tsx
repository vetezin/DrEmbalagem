import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { BrowserRouter, Routes, Route, NavLink, Navigate, Link } from 'react-router-dom';
import './styles.css';
import { LoginForm } from '../features/auth/LoginForm';
import { NovaSenhaForm } from '../features/auth/NovaSenhaForm';
import { auth } from '../features/auth/authService';
import { EmpresasPage } from '../features/empresa/EmpresasPage';
import { UsuariosPage } from '../features/usuario/UsuariosPage';
import { api, ApiError, errorMessage } from '../shared/api';
import { can } from '../shared/access';
import type { Identity } from '../shared/types';

function SessionApp() {
  const [actor, setActor] = useState<Identity | null>(null);
  const [loading, setLoading] = useState(true);
  const [expired, setExpired] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let current = true;
    api.onExpired = () => { if (current) setExpired(true); };
    setLoading(true); setError('');
    auth.me().then(identity => { if (current) { setActor(identity); setExpired(false); } })
      .catch(error => { if (current && !(error instanceof ApiError && error.status === 401)) setError(errorMessage(error)); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; api.onExpired = undefined; };
  }, [revision]);
  const onLogin = (identity: Identity) => { setActor(identity); setExpired(false); setError(''); };
  const refreshSession = async () => {
    setBusy(true); setError('');
    try { onLogin(await auth.me()); }
    catch (error) { setError(errorMessage(error)); }
    finally { setBusy(false); }
  };
  const logout = async () => {
    if (busy) return;
    setBusy(true); setError('');
    try { await auth.logout(); setActor(null); setExpired(false); }
    catch (error) {
      if (error instanceof ApiError && error.status === 401) { setActor(null); setExpired(false); }
      else setError(errorMessage(error));
    } finally { setBusy(false); }
  };
  const usersAllowed = actor && ['USUARIOS_LISTAR', 'USUARIOS_CRIAR', 'USUARIOS_GERIR_PERMISSOES'].some(permission => can(actor, permission));
  const companiesAllowed = actor && !actor.trocaSenhaObrigatoria && actor.nivel !== 'CONSULTOR';
  const protectedPage = (allowed: boolean, children: ReactNode) => !actor ? <Navigate to="/login" replace />
    : actor.trocaSenhaObrigatoria ? <Navigate to="/nova-senha" replace />
    : allowed ? children : <section className="surface"><h1>Acesso não disponível</h1><p>Seu perfil não permite acessar esta área.</p><Link to="/">Voltar ao painel</Link></section>;
  return <>
    <a href="#conteudo" className="skip-link">Ir para o conteúdo</a>
    <header className="site-header"><div className="header-inner"><span className="brand">Doutor Embalagem</span><span>Área da equipe</span></div></header>
    {actor && !expired && !loading && <nav className="main-nav" aria-label="Navegação principal">
      {!actor.trocaSenhaObrigatoria && <><NavLink to="/" end>Painel</NavLink>{companiesAllowed && <NavLink to="/empresas">Empresas</NavLink>}{usersAllowed && <NavLink to="/usuarios">Usuários</NavLink>}</>}
      <span className="identity">{actor.nome} · {actor.nivel}</span><button className="secondary" disabled={busy} onClick={() => void refreshSession()}>Atualizar sessão</button><button className="secondary" disabled={busy} onClick={logout}>{busy ? 'Aguarde…' : 'Sair'}</button>
    </nav>}
    <main id="conteudo" tabIndex={-1}>
      {loading ? <p role="status">Recuperando sessão…</p> : <>
        {error && <div className="surface"><p className="error" role="alert">{error}</p><button disabled={busy} onClick={() => actor ? void refreshSession() : setRevision(v => v + 1)}>Atualizar sessão</button></div>}
        {expired && actor && <section><p role="alert" className="error">Sessão expirada. Entre novamente. Os campos não secretos permanecem nesta página enquanto ela estiver aberta.</p><LoginForm onLoginSuccess={onLogin} /></section>}
        {/* Keep non-secret drafts mounted during reauthentication; different accounts get a fresh tree. */}
        <div key={actor?.id ?? 'anonymous'} hidden={expired && !!actor}>
          <Routes>
            <Route path="/login" element={actor ? <Navigate to={actor.trocaSenhaObrigatoria ? '/nova-senha' : '/'} replace /> : <LoginForm onLoginSuccess={onLogin} />} />
            <Route path="/nova-senha" element={!actor ? <Navigate to="/login" replace /> : actor.trocaSenhaObrigatoria ? <NovaSenhaForm onSuccess={onLogin} /> : <Navigate to="/" replace />} />
            <Route path="/" element={protectedPage(true, <section className="surface"><p className="eyebrow">Área da equipe</p><h1>Painel</h1><p>Selecione uma área para continuar.</p><div className="panel-links">{companiesAllowed && <Link to="/empresas">Empresas</Link>}{usersAllowed && <Link to="/usuarios">Usuários</Link>}</div>{actor?.nivel === 'CONSULTOR' && <p>Seu perfil não possui administração de empresas ou usuários. Os fluxos operacionais serão disponibilizados em outra etapa.</p>}</section>)} />
            <Route path="/empresas" element={protectedPage(!!companiesAllowed, actor && <EmpresasPage actor={actor} />)} />
            <Route path="/usuarios" element={protectedPage(!!usersAllowed, actor && <UsuariosPage actor={actor} refreshSession={refreshSession} />)} />
            <Route path="/cadastro-empresa" element={<Navigate to="/empresas" replace />} />
            <Route path="/criar-usuario" element={<Navigate to="/usuarios" replace />} />
            <Route path="*" element={<Navigate to={actor ? '/' : '/login'} replace />} />
          </Routes>
        </div>
      </>}
    </main>
  </>;
}
export default function App() { return <BrowserRouter><SessionApp /></BrowserRouter>; }
