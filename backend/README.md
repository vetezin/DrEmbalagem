# Doutor Embalagem API — base local

Java Temurin 25.0.5, Spring Boot 4.1.2 e Maven 3.9.16. Sem preview.
Dependências de JPA, PostgreSQL e Lombok gerenciadas pelo BOM do Boot.
Lombok configurado explicitamente como annotation processor.

Endpoint de diagnóstico `/api/status` e teste correspondente foram removidos em 12/11/2026.
A API não expõe endpoints de negócio neste momento; login e painel estão em implementação.

## Executar no Windows

Na raiz do projeto, com JDK local `.tools/jdk-25`:

```cmd
set "JAVA_HOME=%CD%\.tools\jdk-25"
cd backend
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

`JAVA_HOME` vale somente para o processo atual. Para encerrar, `Ctrl+C`.
Servidor escuta somente em `127.0.0.1:8080`.

## Persistência: perfil explícito

Sem o perfil `database`, a autoconfiguração de DataSource fica desabilitada.
Não existe banco em memória, conexão simulada ou persistência neste modo.

O perfil `database` habilita JPA/PostgreSQL e exige `DATABASE_URL`,
`DATABASE_USERNAME` e `DATABASE_PASSWORD`. Em desenvolvimento local, os valores
ficam em `backend/.env`, ignorado pelo Git, e devem ser carregados na configuração
de execução do IntelliJ. Nunca colocar senha em comandos, arquivos versionados ou na URL.

```cmd
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=database"
```

Em ambos os modos: `spring.jpa.hibernate.ddl-auto=validate`,
`spring.jpa.open-in-view=false` e `spring.sql.init.mode=never`.
A aplicação não cria nem altera o schema; migrations SQL ficam em `database/migrations/`
e continuam sem migration de negócio. Configuração faltante ou falha de conexão
não é convertida em sucesso.

## Primeiro SUPERADMIN (execução explícita)

Somente com autorização e banco/serviços confirmados pelo responsável: habilitar
`app.superadmin-bootstrap.enabled=true` na execução com perfil `database`.
Preparar `APP_SUPERADMIN_EMAIL` e `APP_SUPERADMIN_SENHA` por mecanismo local protegido,
sem senha em comandos, histórico, logs ou arquivos versionados. Não executar bootstrap
apenas para testar configuração.

Para o nome solicitado, configurar `APP_SUPERADMIN_NOME=Victor` no ambiente do processo.
Variável ausente mantém `Superadmin`. Nome explícito recebe trim; vazio ou acima de
120 caracteres após trim é rejeitado. Email exige `@` e domínio completo com ponto,
sem restringir a `.com`: `.com.br`, `.org` e demais sufixos sintaticamente válidos
são aceitos. Validação não consulta DNS nem comprova existência da caixa postal.
Se já existir SUPERADMIN, bootstrap não cria, renomeia nem atualiza a conta existente.
Credenciais continuam obrigatórias para habilitar o bootstrap. Após preparação,
desabilitar a opção de bootstrap e remover suas variáveis sensíveis do processo.
Este procedimento não foi executado nesta entrega; aguarda autorização/confirmação.

## Wrapper e verificações

`mvnw` e `mvnw.cmd` gerados pelo plugin oficial
`org.apache.maven.plugins:maven-wrapper-plugin:3.3.4`, tipo `only-script`.
Maven baixado de Maven Central via HTTPS. ZIP 3.9.16 conferido contra SHA-512
publicado em `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip.sha512`.
SHA-256 do mesmo ZIP fixado em `.mvn/wrapper/maven-wrapper.properties`.
Primeira execução requer internet e grava cache de ferramentas/dependências em `~/.m2`.

Teste HTTP `StatusHttpTest` removido junto do endpoint de diagnóstico.
Avisos de `sun.misc.Unsafe` (Lombok na compilação) e carregamento dinâmico de agente
(Byte Buddy/Mockito em testes) no JDK 25 não indicam falha de build.
Depois da remoção dos arquivos de status, o build precisa ser executado novamente
antes de qualquer afirmação de funcionamento.
