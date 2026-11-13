# Usuários e permissões — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Estrutura inicial do módulo de acessos: tabelas `usuario` + `usuario_permissao`, domínio backend com CRUD e gestão de permissões, e bootstrap do superadmin.

**Architecture:** Migration SQL aplicada pelo executor `database/migrate.ps1`; domínio `usuario` no backend seguindo controller/service/repository/model/dto; permissões como chaves de enum Java em tabela por usuário; PostgreSQL real em cluster isolado para testes.

**Tech Stack:** Java 25 (Temurin local `.tools/jdk-25`), Spring Boot 4.1.2, Spring Data JPA, Lombok, spring-security-crypto (bcrypt), PostgreSQL 18, PowerShell para scripts.

**Spec:** `docs/superpowers/specs/2026-11-12-usuarios-permissoes-design.md`

## Global Constraints

- Sem `spring.jpa.hibernate.ddl-auto` além de `validate`; SQL migration é fonte do schema.
- Sem Flyway/Liquibase; migration em `database/migrations/V001__criar_usuario.sql`, transacional, sem `BEGIN`/`COMMIT` explícitos.
- DTOs em `record`; conversões triviais por método estático/fábrica, sem mapper.
- Injeção por construtor, dependências `final`; Lombok `@Getter` nas entidades, sem setter de ID; `@NoArgsConstructor(access = PROTECTED)` para JPA.
- Sem `@Data` em entidades; sem expor entidades na API (só DTOs).
- `open-in-view=false` (já configurado); dados carregados dentro do Service.
- Nada publicado; sem push; commits locais conforme AGENTS.md (base técnica pendente de commit por decisão do usuário — commits deste plano tocam somente arquivos próprios do módulo).
- Autorização por ator autenticado fica para o marco de autenticação; endpoints desta entrega rodam apenas localmente (sem dados reais).
- Lista de níveis (`SUPERADMIN`, `ADMIN`, `CONSULTOR`) e catálogo de permissões são provisórios e abertos a mudança.

## Review Focus

- Divergência entidade↔migration (nome de tabela/coluna/tipo): `validate` deve falhar se a entidade não casar com o SQL — testado no Task 2.
- Email duplicado: constraint UNIQUE no banco decide; violação vira erro HTTP 4xx claro, nunca 500 ou sucesso — testado no Task 6.
- Senha pura: nenhum log, DTO ou resposta pode conter senha ou hash — testado no Task 5.
- Chave de permissão inválida no `PUT /permissoes`: rejeitada pelo Service contra o enum — testado no Task 6.
- Bootstrap repetido: rodar duas vezes não cria segundo superadmin nem falha — testado no Task 7.

---

### Task 1: Migration V001

**Files:**
- Create: `database/migrations/V001__criar_usuario.sql`

**Interfaces:**
- Produces: tabelas `usuario` e `usuario_permissao` exatamente com o schema abaixo; Tasks 2–7 mapeiam sobre ele.

- [ ] **Step 1: Escrever `V001__criar_usuario.sql`**

```sql
CREATE TABLE usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    senha_hash    VARCHAR(100) NOT NULL,
    nivel         VARCHAR(20)  NOT NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_nivel CHECK (nivel IN ('SUPERADMIN', 'ADMIN', 'CONSULTOR'))
);

CREATE TABLE usuario_permissao (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    permissao  VARCHAR(80) NOT NULL,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, permissao)
);
```

Sem `BEGIN`/`COMMIT` no arquivo (o executor controla a transação).

- [ ] **Step 2: Validar com o executor em cluster novo**

Iniciar cluster isolado (padrão de `database/test-migrations.ps1`: `initdb` em `.tools/postgres-test/`, porta exclusiva, loopback, SCRAM-SHA-256), rodar `database/migrate.ps1` apontando para ele. Esperado: `schema_migrations` registra V001; segunda execução não reaplica (histórico válido); encerrar o cluster.

- [ ] **Step 3: Commit**

```bash
git add database/migrations/V001__criar_usuario.sql
git commit -m "feat: migration inicial de usuario e usuario_permissao"
```

### Task 2: Enums e entidades JPA

**Files:**
- Modify: `backend/pom.xml` (adicionar `org.springframework.security:spring-security-crypto`, versão do BOM do Boot)
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/model/NivelUsuario.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/model/Permissao.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/model/Usuario.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/model/UsuarioPermissao.java`

**Interfaces:**
- Consumes: schema do Task 1.
- Produces: `enum NivelUsuario { SUPERADMIN, ADMIN, CONSULTOR }`; `enum Permissao` com chaves provisórias `usuarios.listar`, `usuarios.criar`, `usuarios.permissao.definir` (método `getChave()` retornando a string minúscula com ponto); entidade `Usuario` com `id` (identity), `nome`, `email`, `senhaHash`, `nivel` (`@Enumerated(STRING)`, coluna `nivel`), `ativo`, `criadoEm`, `atualizadoEm`, coleção `Set<UsuarioPermissao> permissoes` (cascade ALL, orfan removal); entidade `UsuarioPermissao` com chave composta (`usuarioId` + `permissao` string, `@IdClass`), `criadoEm`.

- [ ] **Step 1: Adicionar spring-security-crypto ao `pom.xml`** (sem sobrescrever versão; gerida pelo BOM)

- [ ] **Step 2: Escrever enums e entidades** conforme Produces; nomes de colunas em snake_case via `@Column`.

- [ ] **Step 3: Compilar**

Run: `backend: .\mvnw.cmd -q compile` (JAVA_HOME de `.tools/jdk-25`)
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml backend/src/main/java/br/com/doutorembalagem/usuario
git commit -m "feat: entidades e enums de usuario"
```

### Task 3: Repository e DTOs

**Files:**
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/repository/UsuarioRepository.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/dto/CriarUsuarioRequest.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/dto/UsuarioResponse.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/dto/PermissoesRequest.java`

**Interfaces:**
- Produces:
  - `interface UsuarioRepository extends JpaRepository<Usuario, Long>` com `Optional<Usuario> findByEmail(String email)` e `@EntityGraph(attributePaths = "permissoes")` em `Optional<Usuario> findWithPermissoesById(Long id)` e `List<Usuario> findWithPermissoesByOrderByCriadoEmAsc()`.
  - `record CriarUsuarioRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Email @Size(max = 180) String email, @NotBlank @Size(min = 8, max = 72) String senha, @NotNull NivelUsuario nivel)`.
  - `record UsuarioResponse(Long id, String nome, String email, NivelUsuario nivel, boolean ativo, Instant criadoEm, Set<String> permissoes)` com fábrica estática `fromModel(Usuario)`.
  - `record PermissoesRequest(@NotNull Set<String> permissoes)`.
- Nunca expor `senhaHash` em `UsuarioResponse`.

- [ ] **Step 1: Escrever repository e DTOs**

- [ ] **Step 2: Compilar** (`.\mvnw.cmd -q compile`, expected BUILD SUCCESS)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/br/com/doutorembalagem/usuario
git commit -m "feat: repository e dtos de usuario"
```

### Task 4: Testes de integração com PostgreSQL isolado

**Files:**
- Create: `backend/test-integracao.ps1`
- Create: `backend/src/test/java/br/com/doutorembalagem/usuario/UsuarioHttpTest.java`
- Modify: `backend/README.md` (documentar `test-integracao.ps1`)

**Interfaces:**
- Produces: script `test-integracao.ps1` — inicia cluster Postgres isolado, aplica migrations via `database/migrate.ps1`, exporta `DATABASE_URL/DATABASE_USERNAME/DATABASE_PASSWORD`, executa `mvnw verify` com perfil `database` ativo nos testes (`@ActiveProfiles("database")`), encerra o cluster e propaga o código de saída. `UsuarioHttpTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("database")`) começa com: `GET /api/usuarios` em banco vazio retorna `200` e corpo `[]`.

- [ ] **Step 1: Escrever script + teste mínimo (lista vazia)**

- [ ] **Step 2: Rodar** `.\test-integracao.ps1`
Expected: cluster sobe, V001 aplica, teste passa, processo retorna 0.

- [ ] **Step 3: Commit**

```bash
git add backend/test-integracao.ps1 backend/src/test backend/README.md
git commit -m "test: integração de usuário com PostgreSQL isolado"
```

### Task 5: Service e Controller — criação e consulta

**Files:**
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/service/UsuarioService.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/controller/UsuarioController.java`
- Create: `backend/src/main/java/br/com/doutorembalagem/erro/TratamentoErros.java` (`@RestControllerAdvice`: Bean Validation → 400 com mensagem objetiva; `DataIntegrityViolationException` de email único → 409 sem expor SQL/stack)

**Interfaces:**
- Consumes: Task 3 (`UsuarioRepository`, DTOs).
- Produces:
  - `UsuarioService criarUsuario(CriarUsuarioRequest)` — `@Transactional`; hash bcrypt (`new BCryptPasswordEncoder().encode(...)` como bean `@PasswordEncoder` na config? — decisão: criar `PasswordEncoder` bean numa `@Configuration` de `usuario`, chamada só no Service); duplicidade tratada pela constraint (Service deixa estourar para o advice).
  - `UsuarioService listar()` / `obterPorId(Long)` (via `findWithPermissoes*`; ausente → `ResponseStatusException` 404).
  - Controller: `POST /api/usuarios` → 201 + `Location` + corpo `UsuarioResponse`; `GET /api/usuarios` → 200; `GET /api/usuarios/{id}` → 200/404.
  - `UsuarioResponse` jamais inclui senha/hash; Service não loga senha.

- [ ] **Step 1: Escrever Service, Controller e advice**

- [ ] **Step 2: Estender `UsuarioHttpTest`** — casos: criar usuário válido (201, corpo sem senha/hash, `Location` presente); criar email duplicado (409); payload inválido — nome em branco, email malformado, senha com 7 caracteres (400); `GET /{id}` inexistente (404); listar contém o criado.

- [ ] **Step 3: Rodar** `.\test-integracao.ps1`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src
git commit -m "feat: criação e consulta de usuários"
```

### Task 6: Permissões — consulta e definição

**Files:**
- Modify: `backend/src/main/java/br/com/doutorembalagem/usuario/service/UsuarioService.java`
- Modify: `backend/src/main/java/br/com/doutorembalagem/usuario/controller/UsuarioController.java`

**Interfaces:**
- Produces:
  - `static Set<String> Permissao.chavesValidas()` (enum values como chaves).
  - `UsuarioService definirPermissoes(Long usuarioId, Set<String> chaves)` — `@Transactional`: valida cada chave contra `Permissao.chavesValidas()` (inválida → 400 com a chave citada), substitui o conjunto do alvo; nunca aplicável a `SUPERADMIN` (regra fixa: superadmin tem tudo — tentativa → 400 explicando).
  - Controller: `PUT /api/usuarios/{id}/permissoes` com `PermissoesRequest` → 200 + `UsuarioResponse` atualizado.
  - `UsuarioResponse.fromModel` já inclui `permissoes` (Task 3).

- [ ] **Step 1: Implementar Service + endpoint**

- [ ] **Step 2: Estender `UsuarioHttpTest`** — casos: definir permissões válidas em usuário CONSULTOR (200, conjunto refletido); chave inexistente (400, mensagem cita a chave); conjunto vazio (200, limpa permissões); definir em SUPERADMIN (400); redefinir substitui conjunto anterior.

- [ ] **Step 3: Rodar** `.\test-integracao.ps1` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src
git commit -m "feat: gestão de permissões por usuário"
```

### Task 7: Bootstrap do superadmin

**Files:**
- Create: `backend/src/main/java/br/com/doutorembalagem/usuario/bootstrap/SuperadminBootstrap.java` (`ApplicationRunner`; ativado somente quando `SUPERADMIN_NOME`, `SUPERADMIN_EMAIL` e `SUPERADMIN_SENHA` estiverem definidos; cria SUPERADMIN se não existir nenhum; hash bcrypt; se já existir, registra log e não falha)
- Modify: `backend/README.md` (uso e aviso de não publicação)
- Modify: `backend/src/test/java/br/com/doutorembalagem/usuario/UsuarioHttpTest.java` ou novo teste de bootstrap

**Interfaces:**
- Consumes: Task 5 (`UsuarioService.criarUsuario` bypass? — decisão: bootstrap grava direto via Repository com hash, sem passar pelo Controller).

- [ ] **Step 1: Implementar bootstrap**

- [ ] **Step 2: Testar via `test-integracao.ps1`** — estender/criar teste: variáveis definidas → superadmin criado com nivel SUPERADMIN e zero permissões; variáveis definidas novamente (nova execução) → log, sem segundo superadmin.

- [ ] **Step 3: Rodar** `.\test-integracao.ps1` — Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add backend
git commit -m "feat: bootstrap do superadmin"
```

### Task 8: Documentação e Notion

**Files:**
- Modify: `docs/superpowers/specs/2026-11-12-usuarios-permissoes-design.md` (marcar implementado/validado)
- Modify: Notion páginas `2.1`, `3`, `4`, `6` (via MCP; links no `AGENTS.md`)

- [ ] **Step 1: Atualizar spec** (situação: implementado; validações executadas; pendências restantes)
- [ ] **Step 2: Atualizar Notion** — 2.1 (situação atual, fluxo, detalhes técnicos, pendências), 3 (contratos e persistência), 4 (comandos validados), 6 (registro da entrega). Confirmar gravação por re-fetch; se o MCP falhar, registrar em `docs/notion-pendencias.md`.
- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "docs: módulo de usuários e permissões"
```

---

## Explícitamente fora do escopo

- Autenticação/login, sessão, recuperação de senha (próximo marco).
- Telas de administração no frontend.
- Regras finais de edição, desativação e exclusão de usuários (responsável definiu que virão depois; estrutura aberta a mudança).
- Qualquer publicação ou push.
