import { useState } from 'react';
import { EMAIL_PATTERN } from '../../shared/email';
import { api, errorMessage } from '../../shared/api';
import { passwordError } from '../../shared/access';
import type { Identity, Usuario } from '../../shared/types';
import { passwordHint } from '../auth/NovaSenhaForm';
import { Permissoes } from './Permissoes';
import { EmpresaPicker } from './EmpresaPicker';
export function UsuarioForm({ actor, catalog, onUsuarioCriado }: { actor: Identity; catalog: string[]; onUsuarioCriado: () => void }) {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [nivel, setNivel] = useState<'ADMIN' | 'CONSULTOR'>('CONSULTOR');
  const [empresaId, setEmpresaId] = useState<number | null>(actor.empresaId);
  const [empresaLabel, setEmpresaLabel] = useState('');
  const [permissoes, setPermissoes] = useState<string[]>([]);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [salvando, setSalvando] = useState(false);
  return <section className="surface"><h2>Criar usuário</h2><p>A senha temporária deverá ser trocada no primeiro acesso.</p>
    {erro && <p role="alert" className="error">{erro}</p>}{sucesso && <p role="status" className="success">{sucesso}</p>}
    <form onSubmit={async e => {
      e.preventDefault(); if (salvando) return;
      setErro(''); setSucesso('');
      const validation = passwordError(senha) || (!empresaId ? 'Selecione uma empresa.' : '');
      if (validation) { setErro(validation); return; }
      setSalvando(true);
      try {
        const usuario = await api.request<Usuario>('/usuarios', 'POST', { nome, email, senhaTemporaria: senha, nivel: actor.nivel === 'SUPERADMIN' ? nivel : 'CONSULTOR', empresaId: actor.nivel === 'SUPERADMIN' ? empresaId : actor.empresaId, permissoes: permissoes.filter(p => catalog.includes(p)) });
        setNome(''); setEmail(''); setPermissoes([]); setSucesso(`Usuário ${usuario.nome} criado.`); onUsuarioCriado();
      } catch (error) { setErro(errorMessage(error)); }
      finally { setSenha(''); setSalvando(false); }
    }}><fieldset disabled={salvando}>
      <div className="form-grid"><div><label htmlFor="usuario-nome">Nome</label><input id="usuario-nome" maxLength={120} autoComplete="name" required value={nome} onChange={e => setNome(e.target.value)} /></div>
        <div><label htmlFor="usuario-email">Email</label><input id="usuario-email" type="email" pattern={EMAIL_PATTERN} title="Informe email com domínio completo, por exemplo nome@exemplo.org." maxLength={254} autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} /></div></div>
      <label htmlFor="senha-temporaria">Senha temporária</label><input id="senha-temporaria" type="password" autoComplete="new-password" aria-describedby="temporaria-ajuda" required value={senha} onChange={e => setSenha(e.target.value)} />
      <p id="temporaria-ajuda" className="hint">{passwordHint}</p>
      {actor.nivel === 'SUPERADMIN' ? <>
        <label htmlFor="usuario-nivel">Nível</label><select id="usuario-nivel" value={nivel} onChange={e => setNivel(e.target.value as 'ADMIN' | 'CONSULTOR')}><option value="CONSULTOR">CONSULTOR</option><option value="ADMIN">ADMIN</option></select>
        <EmpresaPicker value={empresaId} label={empresaLabel} disabled={salvando} onChange={empresa => { setEmpresaId(empresa.id); setEmpresaLabel(empresa.razaoSocial); }} />
      </> : <p>Empresa fixa: #{actor.empresaId}. Nível: CONSULTOR.</p>}
      <Permissoes id="novo" catalog={catalog} selected={permissoes} onChange={setPermissoes} />
      <button>{salvando ? 'Criando…' : 'Criar usuário'}</button>
    </fieldset></form>
  </section>;
}
