# Login, empresas e usuários — design aprovado

## Situação

Modelo aprovado na conversa de 12/11/2026. Substitui o desenho anterior que adiava autenticação. Implementação ainda não realizada. Sem commit, push ou publicação.

## Objetivo

Entregar login, troca obrigatória de senha inicial, cadastro de empresas e painel de criação de usuários com isolamento por empresa. Tela exclusiva do superadmin fica para depois.

## Decisões aprovadas

- Empresa é um cadastro único reutilizado futuramente nas consultorias; pertencer à empresa não concede acesso automático a consultorias atendidas.
- Usuário comum pertence a exatamente uma empresa. SUPERADMIN global fica sem empresa e administra todas.
- ADMIN administra usuários somente da própria empresa, dentro das permissões recebidas; não cria/promove SUPERADMIN. CONSULTOR depende de permissões operacionais.
- Níveis iniciais: SUPERADMIN, ADMIN, CONSULTOR, enum Java persistido como texto.
- Permissões explícitas em usuario_permissao; SUPERADMIN possui acesso global sem linhas de permissão.
- Login somente por email globalmente único, sem distinguir maiúsculas/minúsculas.
- Sessão no backend; cookie HttpOnly; proteção CSRF; logout invalida sessão. Nenhum token em localStorage. Sessões iniciais em memória, perdidas ao reiniciar backend.
- Administrador informa senha temporária. Primeiro acesso bloqueia painel e APIs administrativas até trocar senha.
- Nova senha com confirmação: mínimo 8 caracteres, pelo menos um número e um caractere especial. Confirmação não é persistida. Somente hash é armazenado.
- Empresa: razão social obrigatória; nome fantasia opcional; CNPJ obrigatório e único; endereço opcional e preenchível depois.
- CNPJ texto com 14 posições, canônico sem máscara, compatível com numérico e alfanumérico; conferir algoritmo oficial antes da implementação.

## Abordagens

Escolhida: vínculo direto usuario.empresa_id e endereço dentro de empresa. Uma empresa por usuário e um endereço por empresa não justificam tabelas de associação/endereço. Sessão Spring Security escolhida em vez de JWT no navegador. DataSource singleton Spring com pool HikariCP, nunca Connection estática compartilhada.

## Banco aprovado

empresa: id BIGINT IDENTITY PK; razao_social VARCHAR(200) NOT NULL; nome_fantasia VARCHAR(200); cnpj VARCHAR(14) NOT NULL UNIQUE; cep VARCHAR(8); logradouro VARCHAR(200); numero VARCHAR(20), aceita S/N; complemento VARCHAR(120); bairro VARCHAR(120); cidade VARCHAR(120); uf VARCHAR(2); criado_em e atualizado_em TIMESTAMPTZ NOT NULL.

usuario: id BIGINT IDENTITY PK; empresa_id BIGINT FK empresa; nome VARCHAR(120) NOT NULL; email VARCHAR(254) NOT NULL, índice único lower(email); senha_hash VARCHAR(255) NOT NULL; nivel VARCHAR(20) NOT NULL com CHECK dos níveis; troca_senha_obrigatoria BOOLEAN NOT NULL; ativo BOOLEAN NOT NULL; criado_em e atualizado_em TIMESTAMPTZ NOT NULL. CHECK exige empresa nula para SUPERADMIN e não nula para demais. FK não apaga usuários em cascata. Índice empresa_id.

usuario_permissao: usuario_id BIGINT FK usuario; permissao VARCHAR(80); PK composta (usuario_id, permissao). Exclusão de conta não será disponibilizada nesta entrega.

## Limites

Sem consultorias, recuperação por email, convites, exclusão física, tela exclusiva global ou publicação. Não adicionar permissões de módulos futuros. Catálogo inicial somente para administração de usuários; criação de empresas pelo SUPERADMIN nesta entrega. Esses limites conservadores não concedem novos privilégios implícitos.

## Segurança e execução

PostgreSQL local embalagem_db para desenvolvimento. Testes destrutivos somente banco/cluster isolado. Migrations SQL explícitas via executor existente, nunca DDL Hibernate. Credenciais não versionadas; não registrar conteúdo de .env. Primeiro superadmin por procedimento local explícito e idempotente, senha informada fora do Git, sujeito à troca inicial. Sem cadastrar conta ou senha fictícia silenciosamente.

## Validação necessária

PostgreSQL real: constraints, unicidade, rollback, mapeamento. HTTP: sessão, CSRF, logout, usuário inativo, primeiro acesso, isolamento entre empresas, elevação de privilégio. Navegador: login/erro/troca/logout/criação de empresa e usuário, desktop e celular. Builds e testes das bases preservados. Documentar resultados e limitações no Notion.
