# DECISIONS

Registro de decisões arquiteturais. Formato: contexto → decisão → consequência. Decisões não são apagadas; se mudarem, marcar como **Substituída por D-XXX**.

---

### D-001 — Módulo único
**Contexto:** projeto pessoal, um desenvolvedor.
**Decisão:** um único módulo Gradle `:app`, com separação por pacotes.
**Consequência:** build simples. Se crescer, pacotes já separados facilitam extrair módulos.

### D-002 — MVVM + Clean Architecture leve
**Decisão:** camadas `presentation`, `domain`, `data`. `domain` em Kotlin puro. Sem módulos separados por camada.
**Consequência:** testabilidade sem cerimônia excessiva.

### D-003 — Room como fonte única da verdade
**Contexto:** requisito offline-first.
**Decisão:** UI observa Flows do Room; rede apenas atualiza o banco.
**Consequência:** offline funciona por padrão; toda tela de detalhe passa pelo banco.

### D-004 — kotlinx.serialization
**Decisão:** usar kotlinx.serialization para JSON (converter Retrofit oficial) e rotas type-safe do Navigation Compose. Gson descartado.
**Consequência:** uma biblioteca de serialização só; compatível com Kotlin sem reflexão.

### D-005 — ViewModel junto da tela
**Contexto:** prompt mestre sugere pasta `viewmodel/` global.
**Decisão:** organizar `presentation/screens/<feature>/` com Screen, ViewModel e UiState juntos.
**Consequência:** navegação por feature mais fácil.

### D-006 — Use cases apenas com regra de negócio
**Decisão:** criar use case quando houver lógica (progresso, status, marcações em lote). Leitura simples: ViewModel → interface de repositório.
**Consequência:** evita use cases "passa-recado".

### D-007 — Dados pessoais sem FK para o cache
**Decisão:** tabelas pessoais referenciam IDs TMDB sem foreign key; cache `tmdb_*` pode ser apagado/atualizado livremente.
**Consequência:** dado pessoal nunca some por atualização de cache. Integridade garantida no repositório; ao adicionar à biblioteca, garantir que o cache do item exista.

### D-008 — Busca não é persistida
**Decisão:** resultados de busca vêm direto da rede (debounce 400 ms). Só itens abertos/adicionados vão para o cache.
**Consequência:** banco não enche de dados irrelevantes; busca exige internet (aceito pelo prompt mestre).

### D-009 — Favorite e Rating como colunas
**Contexto:** prompt mestre sugere entidades Favorite e Rating.
**Decisão:** `is_favorite` e `rating` em `user_movie`/`user_tv_show`.
**Consequência:** menos JOINs; relação é 1:1. Favoritar sem status definido — resolver na fase de biblioteca (provável: favoritar adiciona à biblioteca).

### D-010 — minSdk 26
**Decisão:** `minSdk 26` (Android 8.0), `compileSdk/targetSdk` último estável.
**Consequência:** `java.time` nativo, sem desugaring; cobre qualquer celular atual.

### D-011 — Nome: Pipoca
**Contexto:** o projeto começou numa pasta `tv_time`, nome de marca de terceiros.
**Decisão:** app se chama **Pipoca**. `applicationId`/pacote base: `com.thalyspenha.pipoca`. Repositório: `git@github.com:thalyspenha/pipoca.git`, pasta `~/Projetos/Pessoal/pipoca`.
**Consequência:** `applicationId` não deve mudar depois da primeira instalação.

### D-012 — Autenticação TMDB por Bearer token
**Decisão:** usar API Read Access Token (v4) no header `Authorization`, via interceptor, lido de `local.properties` → `BuildConfig.TMDB_API_TOKEN`.
**Consequência:** chave fora das URLs/logs de query. Obs.: chave em `BuildConfig` fica no APK — aceitável para app pessoal não publicado.

### D-013 — Fase 0 só com documentação
**Contexto:** a seção 19 do `prompt_mestre.md` pede, já na etapa inicial, estrutura do projeto, primeira tela mínima e compilação.
**Decisão:** Fase 0 cobre apenas planejamento e documentação. Estrutura Android, tela mínima e `./gradlew build` passando ficam na Fase 1 (`fase1.md`).
**Consequência:** nenhum código Android até "IMPLEMENTAR FASE 1"; a Fase 1 entrega os itens 2, 8 e 9 da seção 19.

### D-014 — Fase 1 em 4 partes
**Contexto:** a Fase 1 tem 16 objetivos; fazer tudo de uma vez dificulta revisão.
**Decisão:** executar em 4 partes (ver ROADMAP.md), uma por vez, apenas quando o usuário disser "PARTE N". Cada parte compila e termina com sugestão de commit.
**Consequência:** nunca encadear partes sozinho; revisar e commitar entre elas.

### D-015 — Versões base do build
**Decisão:** Gradle 9.8.0, AGP 9.4.1 (Kotlin embutido no AGP 9, sem plugin `kotlin-android`), Kotlin 2.4.20 (plugin Compose), Compose BOM 2026.09.00, `compileSdk`/`targetSdk` 37, Java 17 como alvo de bytecode. Build roda com o JDK 25 do Android Studio.
**Consequência:** versões centralizadas em `gradle/libs.versions.toml`; atualizar em bloco e registrar aqui.

### D-016 — Tema, navegação e ícones
**Decisão:**
- Dependências novas: Navigation Compose 2.10.2, plugin + `kotlinx-serialization-json` 1.11.0 (rotas type-safe, D-004), `material-icons-core` 1.7.8.
- `material-icons-core` parou em 1.7.8 e fica fixado fora do BOM; só ícones básicos, sem `material-icons-extended` (pesado). Trocar por vetores próprios se faltar ícone.
- Tema `PipocaTheme` com paleta própria (amarelo manteiga + vermelho), segue claro/escuro do sistema; dynamic color existe mas desligado por padrão.
- Bottom bar com 5 abas (Início, Busca, Biblioteca, Coleção, Mais), cada uma com pilha própria (`saveState`/`restoreState`). Telas são placeholders.
**Consequência:** novas telas entram como rota `@Serializable` em `presentation/navigation/Routes.kt`.

### D-017 — Hilt, UiState e configuração TMDB injetada
**Decisão:**
- Hilt 2.60.1 com KSP 2.3.12; `hilt-lifecycle-viewmodel-compose` 1.4.0 para `hiltViewModel()` (substitui `hilt-navigation-compose`); `lifecycle-runtime-compose` para `collectAsStateWithLifecycle`.
- `UiState<T>` (Loading/Success/Error) em `util/`; `UiStateContent` em `presentation/components` padroniza loading/erro/retry.
- `BuildConfig.TMDB_API_TOKEN` só é lido em `di/AppModule`, que fornece `domain.model.TmdbConfig`. Home mostra aviso quando o token falta.
- `kotlinx-coroutines-test` fica para quando houver ViewModel com coroutine.
**Consequência:** ViewModels testáveis com construtor simples, sem Android.

### D-018 — Room, rede e imagens
**Decisão:**
- Room 2.8.5 com plugin `androidx.room` (schemas em `app/schemas/`). Room exige ao menos uma entidade: `tmdb_genre` (já definida em DATABASE.md) é a primeira; versão 1 ainda não é "usada de verdade", então pode mudar sem migração até a primeira instalação real.
- Retrofit 3.0.0 + `converter-kotlinx-serialization`, OkHttp 5.5.0 + `logging-interceptor` (só debug, `Authorization` oculto). Timeouts 15 s.
- Dois `OkHttpClient`: base (imagens) e `@TmdbClient` (base + auth + idioma), para o token nunca ir para o servidor de imagens.
- Coil 3.6.3 (`coil-compose`, `coil-network-okhttp`) com cache em disco de 250 MB; `App` implementa `SingletonImageLoader.Factory` com o `ImageLoader` do Hilt.
- Teste: `mockwebserver3` (só `testImplementation`).
**Consequência:** fases seguintes só adicionam endpoints em `TmdbApi`, entidades/DAOs em `AppDatabase` e repositórios.

### D-019 — Fase 2 em 3 partes, com cache de detalhes já no Room
**Contexto:** `fase2.md` pede Repository mas não fala de persistência; ARCHITECTURE.md (D-003) diz que detalhes vêm do Room.
**Decisão:** Fase 2 em 3 partes (ver ROADMAP.md), uma por vez: (1) API e DTOs, (2) RemoteDataSource, erros e mapeamento, (3) Repository com cache de detalhes no Room. Busca não persiste (D-008).
**Consequência:** Fase 2 cria as tabelas de cache `tmdb_movie`, `tmdb_tv_show`, gêneros e elenco; tabelas pessoais continuam para as fases seguintes.

### D-020 — DTOs enxutos e `TmdbJson` compartilhado
**Decisão:** DTOs só com campos usados, todos com default (tolerantes a `null`/ausência via `coerceInputValues`). Datas como `String` no DTO; conversão para `LocalDate` no mapeamento. `TmdbJson` é a única configuração JSON (Retrofit e testes). `include_adult=false` fixo nas buscas. Fixtures de teste são respostas reais do TMDB, reduzidas.
**Consequência:** campo novo necessário = adicionar ao DTO + fixture.

### D-021 — `DataResult`/`DataError` no domínio
**Decisão:** camada de dados devolve `DataResult<T>` (Success/Failure) com `DataError` (`Network`, `NotFound`, `MissingApiKey`, `InvalidApiKey`, `Unknown`), em `domain/model`. `TmdbRemoteDataSource` devolve DTOs (o repositório da parte 3 decide entre cache e domínio); mappers DTO → domínio são funções de extensão em `data/mapper`. Modelos de domínio usam `java.time.LocalDate`.
**Consequência:** UI traduz `DataError` em mensagem; exceções não passam da camada de dados.

### D-022 — Cache de detalhes: DAO único, observe + refresh
**Decisão:**
- Um único `TmdbCacheDao` (abstract class) para todo o cache TMDB; gravação em transação (`saveMovie`/`saveTvShow`) com `@Upsert`.
- Diretores (`tmdb_movie.directors`) e criadores (`tmdb_tv_show.creators`) como `List<String>` serializada em JSON; evita tabelas extras para dado só exibido. Muda o `director String?` planejado em DATABASE.md.
- `tmdb_season` já é preenchida com o resumo das temporadas que vem nos detalhes da série.
- Repositórios de detalhes expõem `observe…(id): Flow` (Room) + `refresh…(id, force): DataResult<Unit>` (rede → Room). ViewModel combina os dois.
- DTO → Entity passa pelo modelo de domínio para reaproveitar as regras dos mappers.
- `java.time.Clock` injetado (UTC) para testar validade do cache.
- Testes de repositório usam DAO fake em memória (herda as transações do DAO real); DAO real testado em `androidTest` (Room in-memory), com `androidx.test` 1.3.0/1.7.0.
**Consequência:** novas telas de detalhes só observam e pedem refresh; nada de chamada de rede direto da UI.

### D-023 — Fase 3 em 3 partes; detalhes como placeholder
**Contexto:** `fase3.md` pede navegação para detalhes, mas não a tela de detalhes.
**Decisão:** Fase 3 em 3 partes (ver ROADMAP.md): (1) SearchViewModel e testes, (2) tela de Busca, (3) rotas de detalhes com telas placeholder. Tela real de detalhes fica para fase futura (opção B, escolha do usuário).
**Consequência:** repositórios de detalhes da Fase 2 continuam sem uso na UI até essa fase.
