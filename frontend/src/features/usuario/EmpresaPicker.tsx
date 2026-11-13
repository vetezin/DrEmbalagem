import { usePage } from '../../shared/usePage';
import { Pagination } from '../../shared/Pagination';
import type { Empresa } from '../../shared/types';
export function EmpresaPicker({ value, label, onChange, disabled }: { value: number | null; label: string; onChange: (empresa: Empresa) => void; disabled: boolean }) {
  const page = usePage<Empresa>('/empresas');
  return <fieldset disabled={disabled}><legend>Empresa do usuário</legend>
    <p role="status">{value ? `Selecionada: ${label} (#${value})` : 'Selecione uma empresa abaixo.'}</p>
    {page.loading ? <p role="status">Carregando empresas…</p> : page.erro ? <><p className="error" role="alert">{page.erro}</p><button type="button" onClick={page.reload}>Tentar novamente</button></> : <>
      {!page.itens.length && <p>Nenhuma empresa. Cadastre uma empresa antes de criar o usuário.</p>}
      {page.itens.map(empresa => <label className="check" key={empresa.id}>
        <input type="radio" name="empresa" checked={value === empresa.id} onChange={() => onChange(empresa)} />
        <span>{empresa.razaoSocial} · {empresa.cnpj}</span>
      </label>)}
    </>}
    <Pagination {...page} onChange={page.setPage} />
  </fieldset>;
}
