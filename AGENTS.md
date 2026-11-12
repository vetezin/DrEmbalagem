# AGENTS.md — Doutor Embalagem

## 1. Escopo e autoridade

Estas instruções valem para todo o projeto Doutor Embalagem e seus subdiretórios. Não são regras globais para outros projetos.

Decisões registradas em 12/11/2026, conforme alinhamento com o responsável pelo projeto. Em caso de conflito com documentos anteriores, estas decisões técnicas substituem as antigas. Novas decisões explícitas do responsável devem ser refletidas aqui.

Não interpretar aprovação da inicialização ou da stack como aprovação de todos os requisitos funcionais. Não inventar campos, fórmulas, permissões ou funcionalidades.

## 2. Produto e materiais existentes

- Sistema de consultoria para levantamento de ambientes e equipamentos em empresas e análise de resultados.
- Consultores usam o site pelo celular durante as visitas. Resultado final é visto e validado no computador. Quem valida e suas permissões ainda não foram definidos.
- Uma aplicação web online, com experiências adaptadas à coleta no celular e à análise no computador. Não apenas reduzir o dashboard para caber no celular.
- Sem aplicativo nativo, coleta offline, SQLite ou sincronização offline nesta fase.
- Cadastro de empresas, consultorias, ambientes e equipamentos, cálculos e exportação Excel aparecem nos materiais existentes. Detalhes e critérios de aceite precisam ser conferidos antes de implementar cada fluxo.
- Estoque, portal externo de clientes, assinaturas e compras automatizadas permanecem fora do MVP descrito no documento de iniciação, salvo nova aprovação.

Referências locais:

- `Doutor_Embalagem_Documento_Base_Iniciacao.md`: contexto e ideias iniciais; decisões Flutter/Serverpod, aplicativos e offline obrigatório estão superadas por este arquivo.
- `README.md` e `docs/plano-prototipo.md`: funcionamento, roteiro e limites da demonstração existente.
- `prototipo-dashboard/`, `prototipo-mobile/` e `compartilhado/mock.js`: referências de interface e fluxo, não código de produção nem especificação final.
- `planilhas-economia-Sicoob-Credivale.xlsx`: referência para descoberta e validação das regras, não especificação integral.

Preservar esses materiais. Não transportar limitações do protótipo para o produto por suposição, como uma única empresa por rodada, formulários preenchidos, bloqueio de edição após envio ou catálogo fictício. Quantidades coletadas no protótipo não determinam automaticamente preços ou consumo anual. Não tratar cálculos demonstrativos como regras finais homologadas.

## 3. Stack aprovada

### Backend e banco

- Java 25 LTS, sem recursos preview por padrão.
- Spring Boot.
- Spring Data JPA com Hibernate.
- PostgreSQL como banco central, acessado exclusivamente pelo backend.
- Lombok para reduzir código repetitivo.
- Migrations em SQL puro, versionadas e aplicadas ao banco por procedimento definido. Não usar Flyway.

### Frontend

- React com TypeScript.
- Vite para desenvolvimento e build; não é servidor de produção.
- Tailwind CSS para estilização, com CSS próprio quando simplificar.
- Frontend consome a API Java. Não contém credenciais do banco, segredos ou regras definitivas de negócio.

### Base técnica definida em 12/11/2026

Escolhas técnicas feitas sob autonomia concedida pelo responsável; não constituem homologação de requisitos funcionais.

- JDK: Eclipse Temurin 25 LTS, atualização de segurança estável mais recente da linha 25 na instalação; sem preview.
- Build backend: Maven 3.9.16 com Maven Wrapper (`mvnw` e `mvnw.cmd` versionados). Não usar Maven 4 enquanto estiver em RC.
- Spring Boot 4.1.2, com versões de Spring Data, Hibernate, driver PostgreSQL e Lombok gerenciadas pelo BOM do Boot, sem sobrescritas por antecipação. BOM consultado: Hibernate 7.4.13.Final, driver PostgreSQL 42.7.14 e Lombok 1.18.48.
- Configurar compilação para Java 25 e Lombok explicitamente como annotation processor; JDK 23+ não deve depender de descoberta implícita.
- PostgreSQL 18, inicialmente 18.7; manter atualizações corretivas da mesma linha.
- Frontend: React 19.3.1, TypeScript 7.0.2, Vite 8.3.8, `@vitejs/plugin-react` 6.3.0, Tailwind CSS e `@tailwindcss/vite` 4.3.5.
- Node.js 24 LTS e npm, sem adicionar outro gerenciador. Fixar patch de Node e versão de npm na criação do frontend; usar atualização de segurança vigente, não considerar o Node local automaticamente atualizado.
- Registrar dependências efetivamente instaladas em `package-lock.json`, versioná-lo e usar `npm ci` nas validações reproduzíveis. Build deve executar checagem TypeScript antes do Vite.
- Não selecionar snapshots, milestones ou release candidates. Conferir metadados e compatibilidade novamente na criação: esta pesquisa não substitui um build real.

### Estrutura definida para a aplicação futura

- `backend/`: projeto Maven único, `artifactId` `doutor-embalagem-api`, pacote-base e `groupId` `br.com.doutorembalagem`. Namespace interno, sem pressupor titularidade de domínio na internet.
- `frontend/`: aplicação React/Vite única; `src/app/` para composição, `src/features/` por funcionalidade e `src/shared/` somente para elementos realmente compartilhados. Não criar pastas vazias por antecipação.
- `database/migrations/`: scripts SQL ordenados; `database/README.md`: procedimento executável e instruções de recuperação, a produzir junto do executor.
- `docs/`: documentação. Protótipos e materiais existentes permanecem nos locais atuais.
- Backend organizado por domínio dentro do pacote-base, conforme seção 4. Sem módulos Maven extras, microsserviços ou camadas artificiais.
- Configuração por ambiente; exemplos sem segredos. Arquivos locais com credenciais, builds, `node_modules` e dados do banco fora do Git.
- Adoção de Git definida para a inicialização efetiva: um repositório na raiz, após revisão dos arquivos e criação de `.gitignore`. Não executar `git init` como efeito colateral desta atualização documental. Remoto e publicação não fazem parte desta autorização.

## 4. Organização do backend

Organizar primeiro por domínio de negócio, depois por responsabilidade. Exemplo aprovado para ilustrar organização, sem definir campos ou contratos finais:

```text
usuario/
├── controller/
│   └── UsuarioController.java
├── service/
│   └── UsuarioService.java
├── repository/
│   └── UsuarioRepository.java
├── model/
│   └── Usuario.java
└── dto/
    ├── CriarUsuarioRequest.java
    └── UsuarioResponse.java
```

### Responsabilidades

- **Controller:** recebe Request DTO, aciona validação estrutural, chama Service, converte resultado em Response DTO e define status/headers HTTP. Sem regras de negócio, persistência direta ou transação manual.
- **Service:** concentra regras de negócio, normalização, autorização específica quando definida e coordenação do caso de uso. Define limites transacionais com `@Transactional`. Sem dependência de servlet, `ResponseEntity` ou escolha de status HTTP.
- **Repository:** consultas e persistência via Spring Data JPA. Sem regras de negócio ou respostas HTTP. Não criar DAO adicional duplicando essa responsabilidade.
- **Entity em `model/`:** representação persistida e relacionamentos JPA. Não receber nem devolver entidades diretamente pela API.
- **Request DTO:** somente campos aceitos na operação; formato, tamanho e obrigatoriedade com Bean Validation. Não permitir alteração de ID gerado, identidade autenticada ou campos controlados pelo servidor por exposição acidental.
- **Response DTO:** contrato explícito de saída; somente dados necessários e autorizados.

### Convenções

- Preferir DTOs em `record`. DTO simples pode ser recebido pelo Service sem criar um objeto idêntico apenas por formalidade.
- Conversões triviais podem usar `fromModel`; não introduzir biblioteca ou camada de mapper sem necessidade.
- Injeção por construtor, dependências `final` e `@RequiredArgsConstructor` quando apropriado. Sem injeção em atributos.
- Lombok `@Getter` nas entidades e `@Setter` apenas nos campos que precisam ser alterados. Não gerar setter para ID por padrão.
- Usar construtor sem argumentos protegido para JPA, podendo gerá-lo com `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Evitar `@Data` em entidades JPA: `equals`, `hashCode` e `toString` exigem cuidado com identidade e relacionamentos.
- Não gerar construtores, setters, interfaces de Service, classes ou pastas sem necessidade concreta.
- Centralizar conversão de erros com `@RestControllerAdvice` e `@ExceptionHandler`. Formato público dos erros ainda precisa ser definido.
- Falha de persistência não pode virar sucesso ou lista vazia. Não expor stack trace, SQL ou detalhes internos ao cliente.

## 5. Persistência e evolução do banco

- JPA é o padrão de persistência, não uma proibição de SQL. Usar projeções, JPQL ou SQL nativo quando adequado às consultas e relatórios.
- Evitar carregamento `EAGER` indiscriminado, consultas N+1 e carregamento de entidades inteiras sem necessidade. Paginar listagens conforme o caso de uso.
- Proteger integridade no PostgreSQL com constraints, chaves estrangeiras e unicidade quando forem requisitos. Verificação prévia no Service não elimina corrida entre requisições.
- Usar consultas parametrizadas e lista de opções permitidas para identificadores dinâmicos, como ordenação.
- Migrations em `database/migrations/`, com versões numéricas únicas e ordenadas, por exemplo `V001__criar_usuario.sql`. Não usar Flyway, Liquibase ou execução automática pelo Spring Boot.
- Executor escolhido: `psql` 18, chamado por um comando explícito de preparação/deploy, separado da aplicação. Implementar e testar esse comando antes da primeira migration; esta documentação não é um executor pronto.
- O executor deve usar `-X` e `ON_ERROR_STOP=1`, validar ordem e SHA-256 dos arquivos e manter tabela técnica `schema_migrations` com versão, nome, checksum e instante de aplicação. Histórico aplicado ausente ou divergente no conjunto de arquivos deve bloquear execução.
- Manter uma única sessão `psql` com advisory lock de sessão durante leitura do histórico e aplicação dos scripts; todos os executores devem usar a mesma chave. Processos concorrentes não podem decidir sobre pendências antes de obter esse lock.
- Cada migration transacional e seu registro de histórico devem participar da mesma transação. Não combinar `--single-transaction` com scripts que controlem `BEGIN`/`COMMIT`; o executor será o responsável pelos limites transacionais.
- Interromper no primeiro erro e retornar falha ao processo chamador. Após rollback, corrigir a causa e repetir; migrations já concluídas continuam registradas. Não marcar aplicação com falha como concluída nem avançar ignorando erros.
- Não editar migration já aplicada em ambiente compartilhado; criar nova versão. Verificar checksum antes de pular uma versão registrada.
- Inicialmente permitir apenas migrations transacionais. Operações incompatíveis com transação, como `CREATE INDEX CONCURRENTLY`, exigem procedimento específico revisado, recuperação e registro definidos antes de executar. Não prometer rollback nesses casos.
- Criação do banco e das credenciais fica fora das migrations da aplicação. Credencial de execução de DDL separada da credencial de runtime, sem senha na linha de comando ou no repositório; usar mecanismo protegido do PostgreSQL.
- Não delegar criação ou alteração de schema ao Hibernate: usar `spring.jpa.hibernate.ddl-auto=validate` e desabilitar inicialização SQL automática do Boot. Scripts SQL são a fonte de evolução do schema. Usar `spring.jpa.open-in-view=false` e carregar dados necessários dentro dos limites dos Services.
- Verificar criação de banco vazio e atualização de banco existente. Não inserir credenciais ou dados pessoais reais em seeds.
- Valores monetários e cálculos decimais: `BigDecimal` no Java e `NUMERIC` no PostgreSQL. Precisão, escala e arredondamento dependem de regra validada.
- Definir semântica de datas, fusos, histórico e concorrência conforme os fluxos reais, sem assumir políticas ainda não aprovadas.

## 6. Frontend e experiência de uso

- Visual sóbrio, objetivo e legível, adequado a adultos de diferentes idades e familiaridade digital.
- Preservar identidade Doutor Embalagem; definir tokens compartilhados de cores, fontes e espaçamentos.
- Contraste forte, tipografia confortável, foco de teclado visível, rótulos persistentes e controles confortáveis ao toque.
- Não depender apenas de ícones, cor ou hover para comunicar ações e estados.
- Evitar animações decorativas, excesso de cartões e enfeites sem função.
- Reutilizar componentes quando houver repetição real; não espalhar blocos extensos e duplicados de classes Tailwind.
- Validar no frontend para experiência de uso, sem substituir validação no backend.
- Distinguir carregamento, salvamento, sucesso e erro. Nunca afirmar persistência sem confirmação do backend; tratar queda de conexão sem descarte silencioso do preenchimento.
- Salvamento progressivo foi recomendado, mas sua granularidade e comportamento ainda precisam de definição funcional.
- Vite não verifica tipos TypeScript sozinho: incluir checagem de tipos no processo de validação/build.
- Biblioteca de componentes, roteamento, formulários e estratégia de acesso/cache de dados ainda não escolhidos. Não adicionar dependências por antecipação.

## 7. Segurança e qualidade

- Adiamento da discussão de perfis não autoriza endpoints desprotegidos. Definir autenticação e autorização antes de disponibilizar dados reais na internet.
- Identidade e permissões verificadas no servidor; não confiar em usuário, papel ou proprietário enviados pelo frontend.
- Não confundir empresa atendida com eventual organização proprietária dos dados. Isolamento e escopo de acesso ainda precisam de definição.
- HTTPS, segredos fora do código, banco sem exposição pública e logs sem credenciais ou dados pessoais desnecessários.
- Definir CORS por origens autorizadas; CORS não substitui autenticação. Política CSRF depende do mecanismo de sessão escolhido.
- Testar regras e transações nos Services, contratos HTTP/validação, persistência com PostgreSQL real isolado e segurança conforme o fluxo.
- Mock de Repository não comprova SQL, mapeamentos ou constraints; build não comprova comportamento.
- Validar fluxos funcionais do frontend no navegador, incluindo celular e computador, erros e acessibilidade pertinente.
- Não executar testes destrutivos contra produção. Não declarar validações que não foram executadas.

## 8. Infraestrutura e operação

Discussão de infraestrutura está pausada. Direção aceita: frontend na Cloudflare Pages e backend + PostgreSQL juntos em uma VPS, buscando baixo custo. Provedor, plano, região e contratação não estão definidos.

Não tratar preços promocionais discutidos como orçamento permanente, nem estimativas de RAM como capacidade garantida. Não contratar, publicar ou alterar infraestrutura sem autorização.

Antes de produção, definir backups externos com recuperação testada, monitoramento, atualizações, gestão de segredos, implantação e responsabilidades operacionais. Backend e banco na mesma VPS constituem um ponto único de falha.

## 9. Pendências e próximos passos

Na execução da inicialização técnica (escolhas já definidas nas seções 3 e 5):

- Preparar JDK 25 e cliente/servidor PostgreSQL 18 em ambiente local isolado, com autorização antes de instalar ferramentas. Na pesquisa de 12/11/2026, Java ativo era Temurin 21 e `psql` não estava no PATH; isso não comprova ausência de outras instalações.
- Gerar backend/frontend na estrutura definida e fixar versões efetivamente usadas, inclusive Node/npm, sem alterar os protótipos.
- Implementar e testar executor SQL com banco vazio, atualização, repetição, checksum divergente, falha e concorrência. Procedimento escolhido não significa executor implementado.
- Inicializar Git na etapa de criação efetiva, após revisar arquivos sensíveis e exclusões. Não criar remoto nem publicar sem autorização.
- Executar build e testes reais do backend com JDK 25, checagem TypeScript e build do frontend. Compatibilidade documental foi pesquisada; conjunto ainda não foi compilado ou testado.

Antes de cada funcionalidade:

- Aproveitar ideias e materiais existentes; confirmar o que é requisito aprovado, hipótese ou detalhe demonstrativo.
- Delimitar primeiro fluxo e critérios de aceite, campos, relacionamentos e regras de alteração/exclusão.
- Definir contratos e erros da API, requisitos de concorrência e histórico conforme necessário.
- Para cálculos e Excel, validar fórmulas, premissas, unidades, arredondamento e exemplos de referência.

Antes da publicação:

- Resolver autenticação, permissões e escopo dos dados.
- Fechar infraestrutura, ambientes, backup/recuperação, monitoramento e processo de deploy.

A pasta atual contém um protótipo, não o produto implementado. Git inicializado em 12/11/2026 por autorização explícita, com branch `main` e remoto `origin` em `https://github.com/vetezin/DrEmbalagem.git`. Configurar remoto não significa publicar: push exige autorização explícita. Preservar protótipos e materiais existentes; não criar entidades ou requisitos apenas para preencher a arquitetura.
