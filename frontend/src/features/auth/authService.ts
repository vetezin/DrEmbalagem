import { api } from '../../shared/api';
import type { Identity } from '../../shared/types';
export interface LoginRequest { email: string; senha: string }
export const auth = {
  login: (dados: LoginRequest) => api.request<Identity>('/auth/login', 'POST', dados),
  me: () => api.request<Identity>('/auth/me'),
  trocarSenha: (dados: { novaSenha: string; confirmacao: string }) => api.request<Identity>('/auth/trocar-senha', 'POST', dados),
  logout: () => api.request('/auth/logout', 'POST'),
};
