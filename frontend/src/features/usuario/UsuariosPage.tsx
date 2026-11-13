import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../shared/api';
import { can, canManage, grantable } from '../../shared/access';
import { usePage } from '../../shared/usePage';
import { Pagination } from '../../shared/Pagination';
import type { Identity, Usuario } from '../../shared/types';
import { UsuarioForm } from './UsuarioForm';
import { Permissoes } from './Permissoes';

function PermissionEditor({ actor, target, catalog, onSaved }: { actor: Identity; target: Usuario; catalog: string[]; onSaved: () => void }) {
  const [selected, setSelected] = useState(target.permissoes.filter(p => catalog.includes(p)));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const excluded = target.permissoes.filter(p => !catalog.includes(p));
  return <form onSubmit={async e => {
    e.preventDefault(); if (saving) return;
    setSaving(true); setError(''); setSuccess('');
    try {
      await api.request<Usuario>(`/usuarios/${target.id}/permissoes`, 'PUT', { permissoes: selected.filter(p => catalog.includes(p)) });
      setSuccess('Permissões salvas.'); onSaved();
    } catch (error) { setError(errorMessage(error)); }
    finally { setSaving(false); }
  }}>
    <Permissoes id={`editar-${target.id}`} disabled={saving} catalog={catalog} selected={selected} onChange={setSelected} />
    {excluded.length > 0 && <p className="hint">Este usuário tem permissões que você não pode conceder. Ao salvar, elas serão removidas: {excluded.join(', ')}.</p>}
    {actor.id === target.id && <p className="hint">Alterar suas próprias permissões pode remover seu acesso a esta área.</p>}
    {error && <p className="error" role="alert">{error}</p>}{success && <p className="success" role="status">{success}</p>}
    <button disabled={saving}>{saving ? 'Salvando…' : 'Salvar permissões'}</button>
  </form>;
}
function UserList({ actor, catalog, refreshSession }: { actor: Identity; catalog: string[] | null; refreshSession: () => Promise<void> }) {
  const page = usePage<Usuario>('/usuarios');
  const [notice, setNotice] = useState('');
  return <section className="surface" aria-busy={page.loading}><h2>Usuários cadastrados</h2>
    {notice && <p className="success" role="status">{notice}</p>}
    {page.loading ? <p role="status">Carregando usuários…</p> : page.erro ? <><p role="alert" className="error">{page.erro}</p><button onClick={page.reload}>Tentar novamente</button></> : !page.itens.length ? <p>Nenhum usuário cadastrado.</p> : <ul className="records">{page.itens.map(usuario => <li key={usuario.id}>
      <h3>{usuario.nome}</h3><p>{usuario.email}</p><dl><dt>Nível</dt><dd>{usuario.nivel}</dd><dt>Empresa</dt><dd>{usuario.empresaId ? `#${usuario.empresaId}` : 'Global'}</dd><dt>Situação</dt><dd>{usuario.ativo ? 'Ativo' : 'Inativo'}{usuario.trocaSenhaObrigatoria ? ' · Troca inicial pendente' : ''}</dd><dt>Permissões atuais</dt><dd>{usuario.permissoes.join(', ') || 'Nenhuma permissão explícita'}</dd></dl>
      {catalog && canManage(actor, usuario) && <details><summary>Gerir permissões de {usuario.nome}</summary><PermissionEditor key={`${usuario.id}-${usuario.permissoes.join(',')}`} actor={actor} target={usuario} catalog={catalog} onSaved={() => { setNotice(`Permissões de ${usuario.nome} salvas.`); page.reload(); void refreshSession(); }} /></details>}
    </li>)}</ul>}
    <Pagination {...page} onChange={page.setPage} />
  </section>;
}
export function UsuariosPage({ actor, refreshSession }: { actor: Identity; refreshSession: () => Promise<void> }) {
  const [catalog, setCatalog] = useState<string[] | null>(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [listRevision, setListRevision] = useState(0);
  useEffect(() => {
    let current = true;
    setError('');
    api.request<string[]>('/permissoes').then(values => { if (current) setCatalog(values); })
      .catch(error => { if (current) setError(errorMessage(error)); });
    return () => { current = false; };
  }, [actor, revision]);
  const allowedCatalog = catalog === null ? null : grantable(actor, catalog);
  return <><h1>Usuários</h1>
    {error ? <section className="surface"><p role="alert" className="error">{error}</p><button onClick={() => setRevision(v => v + 1)}>Carregar permissões novamente</button></section> : catalog === null && <p role="status">Carregando permissões…</p>}
    {can(actor, 'USUARIOS_LISTAR') && <UserList key={listRevision} actor={actor} catalog={allowedCatalog} refreshSession={refreshSession} />}
    {!can(actor, 'USUARIOS_LISTAR') && <p>Seu acesso não permite listar usuários.</p>}
    {can(actor, 'USUARIOS_CRIAR') && allowedCatalog && <UsuarioForm actor={actor} catalog={allowedCatalog} onUsuarioCriado={() => setListRevision(v => v + 1)} />}
    {can(actor, 'USUARIOS_GERIR_PERMISSOES') && !can(actor, 'USUARIOS_LISTAR') && <p>Para selecionar usuários e gerir permissões, solicite também acesso à listagem.</p>}
  </>;
}
