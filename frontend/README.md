# Frontend — Doutor Embalagem

React 19.3.1, TypeScript 7.0.2, Vite 8.3.8, Tailwind 4.3.5 e React Router 7.18.5, sem novas dependências nesta entrega.

## Fluxos implementados

Login, recuperação da sessão por `/api/auth/me`, troca obrigatória da senha inicial e logout. Painel com empresas e usuários paginados, criação de empresas pelo SUPERADMIN, criação de usuários e gestão de permissões conforme nível/subconjunto do ator. ADMIN usa a própria empresa e cria somente CONSULTOR; CONSULTOR não possui administração. SUPERADMIN seleciona empresa com paginação, sem restringir à primeira página.

API central com cookies, CSRF em memória e renovação após login/troca/logout. Não armazena senha/token no navegador. Mutações não são repetidas automaticamente. Falha de rede não comprova que uma gravação foi desfeita: conferir a listagem antes de reenviar. Resposta 403 não distingue CSRF, permissão e primeiro acesso no backend; a interface informa essa limitação e permite atualizar a sessão. Sessão expirada mantém rascunhos não secretos montados para reautenticação na mesma conta; recarregar/sair/trocar conta descarta rascunhos. Senhas são limpas após tentativa de envio ao serviço.

## Comandos locais

Usar Node 24 LTS e npm, com as dependências fixadas em `package-lock.json`. Validação desta entrega usou Node 24.11.0 já disponível; não afirma atualização de segurança nem instalação do runtime indicado em materiais antigos. `package.json` atual não fixa `engines`/`packageManager`.

```sh
npm ci
npm test
npm run build
```

`npm test` usa o runner nativo Node com suporte a TypeScript, sem framework instalado. Testa cliente HTTP com transporte em memória, autorização visual, política de senha e paginação, sem serviços. Build executa `tsc --noEmit` antes de `vite build`.

Antes de testes funcionais, o responsável deve iniciar os serviços e confirmar URLs. Não iniciar/parar serviços automaticamente.

- Desenvolvimento configurado: `http://127.0.0.1:5175`; proxy `/api` para `http://127.0.0.1:8091`.
- `npm run dev` inicia Vite somente quando autorizado.
- `npm run preview`: `http://127.0.0.1:4173`, sem proxy de API, não é servidor de produção.

## Limites de validação

Tipos/build e testes locais não comprovam cookie/CSRF reais, persistência, isolamento no servidor, acessibilidade renderizada ou fluxos de celular/desktop. HTTP, banco, navegador e testes manuais não foram executados nesta entrega. Autenticação depende de backend/schema/usuário inicial preparados por procedimento autorizado. Não publicar com dados reais antes da validação de segurança e definição operacional.
