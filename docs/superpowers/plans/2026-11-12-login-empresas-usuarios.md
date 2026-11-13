# Login, empresas e usuários — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans ou subagent-driven-development. Acompanhar caixas abaixo.

**Goal:** Login seguro e painel administrativo com empresas e usuários isolados por empresa.
**Architecture:** Spring Security com sessão e CSRF; JPA por domínio e pool HikariCP; React consome API por proxy local. SQL explícito pelo executor existente.
**Tech Stack:** Stack e versões atuais do projeto, sem atualização incidental. Adicionar somente starters Security/Validation e suporte de testes Security geridos pelo BOM.
**Spec:** docs/superpowers/specs/2026-11-12-login-empresas-usuarios-design.md

## Global Constraints

Sem commit/push. Preservar protótipos e trabalho preexistente. Não ler/exibir senha .env. Não executar limpeza em embalagem_db. ddl-auto=validate e open-in-view=false. Controllers com DTOs; Services transacionais e sem HTTP. Consultar documentação da versão atual antes de usar APIs Security. Não instalar ferramentas adicionais sem autorização.

## Review Focus

1. ADMIN enviando empresa de terceiro ou nível SUPERADMIN: rejeitar no servidor.
2. Sessão com senha temporária chamando API diretamente: bloquear até troca.
3. CSRF ausente, logout e sessão antiga: negar mutações e invalidar corretamente.
4. Email/CNPJ concorrentes ou case diferente: constraints e erro 409 sem SQL.
5. Senha/hash em resposta/log e caracteres multibyte: nunca expor nem truncar senha.

## Task 1 — Persistência

Arquivos: database/migrations/V001__criar_empresas_usuarios.sql; backend/.../empresa/{model,repository}; backend/.../usuario/{model,repository}.
- [ ] Conferir estado real das migrations e banco sem modificar dados; escolher próxima versão livre, nunca sobrescrever migration aplicada.
- [ ] Testes PostgreSQL isolado para email case-insensitive, CNPJ único, FK, CHECK de empresa/nível e ausência de cascade empresa-usuário; provar falha antes da migration/modelo.
- [ ] Implementar schema exato do design e entidades Lombok/JPA, enums NivelUsuario e Permissao. Permissões iniciais USUARIOS_LISTAR, USUARIOS_CRIAR, USUARIOS_GERIR_PERMISSOES; não aceitar chaves fora do catálogo.
- [ ] Verificar migrations, repetição e validate em banco isolado. Não criar schema automaticamente.

## Task 2 — Autenticação e autorização

Arquivos: backend/pom.xml; backend/.../autenticacao/{config,controller,service,dto}; backend/.../usuario/service; backend/.../erro.
Contratos: GET /api/auth/csrf; POST /api/auth/login (email,senha); GET /api/auth/me; POST /api/auth/trocar-senha (novaSenha,confirmacao); POST /api/auth/logout. Login retorna identidade sem hash e trocaSenhaObrigatoria. APIs não autenticadas retornam 401 JSON; proibidas 403; validação 400; conflito 409.
- [ ] Escrever testes HTTP com cookie real para login inválido/genérico, fixação de sessão, CSRF, logout, conta inativa e restrição de primeiro acesso.
- [ ] Usar Spring Security padrão com persistência explícita correta do contexto e rotação de sessão no login; BCrypt sem truncamento (limite de 72 bytes validado); sessão SameSite=Lax, HttpOnly, Secure em HTTPS de produção. Não desabilitar CSRF para facilitar frontend.
- [ ] Implementar troca obrigatória e política 8 caracteres/número/especial não branco, confirmação igual e senha distinta da temporária; salvar hash e liberar sessão somente após transação concluída.
- [ ] Testar isolamento de empresa e concessão de permissões como subconjunto das do ator; SUPERADMIN bypass global explícito. Consultor nunca administra usuários mesmo com payload forjado.
- [ ] Bootstrap explícito e idempotente por perfil/comando local; sem senha no Git ou logs; validar entrada e não redefinir conta existente.

## Task 3 — Empresas e usuários

Arquivos: backend/.../empresa/{controller,service,dto}; backend/.../usuario/{controller,service,dto}; testes correspondentes.
Contratos: GET/POST /api/empresas; GET/POST /api/usuarios; PUT /api/usuarios/{id}/permissoes; GET /api/permissoes. Listas paginadas com tamanho limitado; DTOs retornam pagina,total e itens. Campos conforme design; criacao de usuário inclui senhaTemporaria, nivel, empresaId e permissoes.
- [ ] Conferir CNPJ numérico/alfanumérico em fonte oficial; escrever exemplos válidos/inválidos antes de implementar normalização/verificação. Não consultar serviço externo automaticamente.
- [ ] Testar criação, endereço ausente/parcial, duplicidade, validação e rollback. Razão/nome/email trim; email canônico; CNPJ canônico maiúsculo sem máscara.
- [ ] Criar empresas somente como SUPERADMIN. ADMIN lista apenas própria empresa. ADMIN só cria CONSULTOR na própria empresa e concede subconjunto das suas permissões; SUPERADMIN cria ADMIN/CONSULTOR; SUPERADMIN não criado pela API pública de cadastro.
- [ ] Testar ID de terceiro, manipulação de empresa/nivel/permissoes, erro 403/404 sem vazamento e respostas sem segredos. Identidade sempre sessão, nunca ator enviado.

## Task 4 — Frontend

Arquivos: frontend/src/app; frontend/src/features/{autenticacao,empresas,usuarios}; frontend/src/shared somente reutilização real. Manter dependências atuais quando suficientes.
- [ ] Carregar skills frontend-design e TDD; preservar identidade existente e definir navegação simples Login / Nova senha / Painel (Empresas, Usuários, Sair).
- [ ] Cliente fetch central mínimo: cookie, obtenção/renovação CSRF após login/logout, erros e sessão expirada; sem armazenamento de tokens/senha.
- [ ] Telas acessíveis com labels persistentes, foco e estados carregando/salvando/erro/sucesso; senha e confirmação nunca reaproveitadas em listagens.
- [ ] Painel com listagens paginadas e formulários de criação, seleção de empresa restrita ao ator e permissões apenas concedíveis; endereço opcional. Primeiro acesso sem navegação administrativa.
- [ ] Testes unitários relevantes e npm test/build com TypeScript.

## Task 5 — Validação e documentação

- [ ] Executar backend verify, PostgreSQL real isolado e testes de segurança. Aplicar migration em embalagem_db apenas depois de validar isolamento e histórico; não registrar usuários de teste nesse banco.
- [ ] Playwright: login inválido/válido, troca obrigatória, logout, empresa e usuário; repetir com ADMIN de outra empresa e tentativa de acesso indevido; celular/desktop e console.
- [ ] Revisão independente focada em segurança e requisitos; corrigir achados e repetir testes.
- [ ] Atualizar README(s), instruções de bootstrap/.env sem credenciais, Notion 2.1/2.2/3/4/5/6 com decisões, implementado e validado separados. Confirmar gravação.
- [ ] Relatar arquivos e resultados reais, limitações; sem commit/push conforme pedido.
