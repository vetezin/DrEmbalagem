const labels: Record<string, string> = {
  USUARIOS_LISTAR: 'Listar usuários', USUARIOS_CRIAR: 'Criar usuários', USUARIOS_GERIR_PERMISSOES: 'Gerir permissões de usuários',
};
export function Permissoes({ catalog, selected, onChange, disabled = false, id }: { catalog: string[]; selected: string[]; onChange: (values: string[]) => void; disabled?: boolean; id: string }) {
  return <fieldset disabled={disabled} className="permissions"><legend>Permissões</legend>
    <p className="hint">Selecione as permissões concedíveis. Nenhuma seleção remove todas.</p>
    {catalog.length === 0 && <p>Nenhuma permissão disponível para conceder.</p>}
    {catalog.map(permission => <label className="check" key={permission} htmlFor={`${id}-${permission}`}>
      <input id={`${id}-${permission}`} type="checkbox" checked={selected.includes(permission)} onChange={e => onChange(e.target.checked ? [...selected, permission] : selected.filter(value => value !== permission))} />
      <span>{labels[permission] || permission}</span>
    </label>)}
  </fieldset>;
}
