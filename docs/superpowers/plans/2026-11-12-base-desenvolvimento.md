# Base de desenvolvimento — plano

**Objetivo:** base local aprovada na conversa, com index simples conectada à API. Sem acessos nem empresas.

**Arquitetura:** backend Spring Boot 4.1.2/Java 25 e frontend React/TypeScript/Vite/Tailwind separados. API somente loopback. PostgreSQL isolado do serviço global; migrations explícitas. JDK Temurin portable em `.tools/jdk-25`, sem alterações globais.

## Restrições
- Preservar protótipos, documentos e alterações anteriores.
- Segredos, ferramentas locais e dados fora do Git.
- Não publicar nem implementar domínio de negócio.
- Maven Wrapper 3.9.16; versões frontend conforme AGENTS.md, confirmar disponibilidade.

## Etapas
- [ ] Conferir versões e baixar JDK portable com SHA-256.
- [ ] Criar backend mínimo e teste HTTP de `GET /api/status`, resposta `{ "status": "ok" }`.
- [ ] Criar frontend com carregamento, sucesso, erro e nova tentativa; proxy `/api` local, sem CORS amplo.
- [ ] Preparar executor SQL e testes com PostgreSQL isolado: vazio, atualização, repetição, divergência, falha e concorrência.
- [ ] Documentar comandos locais e pré-requisitos reais.
- [ ] Executar testes/builds e validar página no navegador, incluindo API indisponível.
- [ ] Revisar mudanças e commitar somente arquivos próprios quando validações essenciais passarem.

## Riscos a verificar
Java global permanece intacto; falha da API não pode aparecer como sucesso; porta ocupada deve falhar claramente; checksum divergente bloqueia migrations; concorrência deve serializar histórico.
