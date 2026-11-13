import { EmpresaForm } from './EmpresaForm';
import { usePage } from '../../shared/usePage';
import { Pagination } from '../../shared/Pagination';
import type { Empresa, Identity } from '../../shared/types';
export function EmpresasPage({ actor }: { actor: Identity }) {
  const page = usePage<Empresa>('/empresas');
  return <><h1>Empresas</h1><section className="surface" aria-busy={page.loading}><h2>Empresas cadastradas</h2>
    {page.loading ? <p role="status">Carregando empresas…</p> : page.erro ? <div><p role="alert" className="error">{page.erro}</p><button onClick={page.reload}>Tentar novamente</button></div> : <>
      {!page.itens.length ? <p>Nenhuma empresa cadastrada.</p> : <ul className="records">{page.itens.map(empresa => <li key={empresa.id}>
        <h3>{empresa.razaoSocial}</h3><p>{empresa.nomeFantasia || 'Sem nome fantasia'}</p>
        <dl><dt>CNPJ</dt><dd>{empresa.cnpj}</dd><dt>Empresa</dt><dd>#{empresa.id}</dd>
          <dt>Endereço</dt><dd>{[empresa.logradouro, empresa.numero, empresa.complemento, empresa.bairro, empresa.cidade, empresa.uf, empresa.cep].filter(Boolean).join(', ') || 'Não informado'}</dd></dl>
      </li>)}</ul>}
    </>}
    <Pagination {...page} onChange={page.setPage} />
  </section>{actor.nivel === 'SUPERADMIN' && <EmpresaForm onEmpresaCriada={page.reload} />}</>;
}
