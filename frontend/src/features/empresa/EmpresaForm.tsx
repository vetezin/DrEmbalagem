import { useState } from 'react';
import { api, errorMessage } from '../../shared/api';
import type { Empresa, EmpresaRequest } from '../../shared/types';
const fields = [
  ['razaoSocial', 'Razão social', 200], ['nomeFantasia', 'Nome fantasia (opcional)', 200],
  ['cnpj', 'CNPJ numérico ou alfanumérico', 18], ['cep', 'CEP (opcional)', 9],
  ['logradouro', 'Logradouro (opcional)', 200], ['numero', 'Número (opcional)', 20],
  ['complemento', 'Complemento (opcional)', 120], ['bairro', 'Bairro (opcional)', 120],
  ['cidade', 'Cidade (opcional)', 120], ['uf', 'UF (opcional)', 2],
] as const;
const empty = Object.fromEntries(fields.map(([key]) => [key, ''])) as Record<keyof EmpresaRequest, string>;
export function EmpresaForm({ onEmpresaCriada }: { onEmpresaCriada: (empresa: Empresa) => void }) {
  const [dados, setDados] = useState(empty);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  return <section className="surface"><h2>Cadastrar empresa</h2>
    <p>Somente razão social e CNPJ são obrigatórios. Endereço pode ser preenchido parcialmente.</p>
    {erro && <p className="error" role="alert">{erro}</p>}{sucesso && <p role="status" className="success">{sucesso}</p>}
    <form onSubmit={async e => {
      e.preventDefault(); if (salvando) return;
      setSalvando(true); setErro(''); setSucesso('');
      try {
        const empresa = await api.request<Empresa>('/empresas', 'POST', dados);
        setDados(empty); setSucesso(`Empresa ${empresa.razaoSocial} cadastrada.`); onEmpresaCriada(empresa);
      } catch (error) { setErro(errorMessage(error)); }
      finally { setSalvando(false); }
    }}><fieldset disabled={salvando} className="form-grid">
      {fields.map(([key, label, maxLength]) => <div key={key}>
        <label htmlFor={`empresa-${key}`}>{label}</label>
        <input id={`empresa-${key}`} required={key === 'razaoSocial' || key === 'cnpj'} maxLength={maxLength}
          aria-describedby={key === 'cnpj' ? 'cnpj-ajuda' : undefined} value={dados[key]}
          onChange={e => setDados({ ...dados, [key]: e.target.value })} />
        {key === 'cnpj' && <p id="cnpj-ajuda" className="hint">14 posições, com ou sem máscara. Letras são aceitas; os dois últimos dígitos são numéricos.</p>}
      </div>)}
      <div className="full"><button>{salvando ? 'Cadastrando…' : 'Cadastrar empresa'}</button></div>
    </fieldset></form>
  </section>;
}
