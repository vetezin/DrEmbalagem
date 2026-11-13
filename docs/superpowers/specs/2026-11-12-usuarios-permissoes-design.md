# Design — Usuários e permissões (primeira montagem)

Data: 12/11/2026 · Status: aprovado pelo responsável nesta conversa ("primeiro vamos montar, depois definimos regras").

## Intenção

Estrutura inicial do módulo de acessos: usuários em uma única tabela com nível, e permissões granulares em tabela própria. Regras detalhadas virão depois; banco e catálogo abertos a mudança.

## Decisões (aprovadas)

- Superadmin na mesma tabela `usuario`, diferenciado por `nivel`.
- `nivel` enum-like: `SUPERADMIN`, `ADMIN`, `CONSULTOR` (lista provisória; "etc" aberto).
- Permissões granulares em `usuario_permissao` (chave por usuário).
- Catálogo de permissões vive no backend (enum); banco guarda a chave string.
- SUPERADMIN tem todas as permissões por regra fixa no Service; sem linhas na tabela.
- `senha_hash` desde já (bcrypt via spring-security-crypto); login/auth completa fica para depois.
- Primeiro superadmin: procedimento de bootstrap protegido por variáveis de ambiente, sem seed com credenciais no Git.

## Proposta de funcionamento (provisória, sujeita a redefinição)

- Nível define hierarquia de criação: SUPERADMIN cria ADMIN e CONSULTOR; ADMIN cria CONSULTOR; CONSULTOR não cria ninguém.
- Permissão define funcionalidade; nível não concede permissão automática.
- Autorização verificada no backend; frontend não é fonte de confiança.

## Escopo desta entrega

1. Migration SQL (`database/migrations/`) com as duas tabelas.
2. Domínio backend `usuario`: entidades, DTOs, Repository, Service, Controller com CRUD e gestão de permissões.
3. Bootstrap do superadmin via variáveis de ambiente.
4. Testes de persistência e HTTP com PostgreSQL real em cluster isolado (padrão de `database/test-migrations.ps1`).

Fora desta entrega: autenticação/login, recuperação de senha, telas de administração no frontend, permissões ligadas a módulos futuros (empresas, consultorias). Nada publicado.

## Pendências registradas

- Lista definitiva de níveis e de permissões do catálogo.
- Regras de edição, desativação, exclusão e unicidade de email.
- Banco de desenvolvimento definitivo (execução desta entrega usa cluster isolado).
- Commit da base técnica e deste módulo: depende de autorização; push exige autorização explícita.
