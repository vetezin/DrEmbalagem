# Banco de desenvolvimento

Banco local: `embalagem_db`, PostgreSQL em `127.0.0.1:5432`.
Credenciais ficam somente em `backend/.env`, ignorado pelo Git.

## Migrations

Scripts SQL versionados permanecem em `database/migrations/`. Ainda não há migration de negócio.
Os executores e testes PowerShell foram removidos por solicitação do responsável em 12/11/2026.
Não há comando automatizado substituto implementado. Hibernate continua com `ddl-auto=validate`;
a aplicação não cria nem altera o schema.

Próximas migrations serão aplicadas e conferidas no banco local, por comando explícito com `psql`.
O procedimento deverá ser definido junto da primeira migration, com interrupção em erro,
transação, histórico e conferência das versões aplicadas. Não considerar o executor anterior disponível.

## Segurança dos testes

`embalagem_db` é banco de desenvolvimento, não banco descartável.
Não executar limpeza, `DROP`, `TRUNCATE`, reset de schema ou inserção de contas fictícias
como teste automatizado nesse banco. Testes de falha, checksum e concorrência do executor antigo
não são transferidos automaticamente para ele. Conferir dados existentes e necessidade de backup
antes de aplicar mudanças. Nunca colocar senha na linha de comando ou no Git.
