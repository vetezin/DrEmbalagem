export type Nivel = 'SUPERADMIN' | 'ADMIN' | 'CONSULTOR';
export interface Identity {
  id: number;
  nome: string;
  email: string;
  nivel: Nivel;
  empresaId: number | null;
  permissoes: string[];
  trocaSenhaObrigatoria: boolean;
}
export interface Usuario extends Identity { ativo: boolean }
export interface Empresa {
  id: number;
  razaoSocial: string;
  nomeFantasia: string | null;
  cnpj: string;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
}
export interface Pagina<T> { page: number; total: number; itens: T[] }
export type EmpresaRequest = Omit<Empresa, 'id'>;
export interface UsuarioRequest {
  nome: string;
  email: string;
  senhaTemporaria: string;
  nivel: 'ADMIN' | 'CONSULTOR';
  empresaId: number;
  permissoes: string[];
}
