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

### D-024 — Pipeline da busca no SearchViewModel
**Decisão:** `combine(texto.trim.distinct.debounce(400ms), tipo, tentativa)` → `distinctUntilChanged` → `flatMapLatest`. Assim: digitação rápida só dispara ao parar; texto vazio/curto (< 2) vira `Idle` sem rede; mesma pesquisa (texto + tipo) não se repete; pesquisa nova cancela a anterior; trocar FILMES/SÉRIES pesquisa na hora; `retry()` incrementa a tentativa para repetir de propósito. Estado próprio (`SearchUiState` + `SearchContent`: Idle, Loading, Results, Empty, Error com `DataError`) em vez do `UiState` genérico. Só página 1 por enquanto.
**Consequência:** `kotlinx-coroutines-test` 1.11.0 entra como dependência de teste (tempo virtual para testar debounce e cancelamento).

### D-025 — Tela de Busca e utilitários compartilhados
**Decisão:**
- `TmdbImageUrl` saiu de `data/remote` para `util/`: a UI monta URLs de poster e `presentation` não deve depender de `data`. Novo tamanho `POSTER_THUMB = w154` para listas.
- `PosterImage` (componente) mostra a inicial do título quando não há poster ou enquanto carrega.
- `DataError.toMessage()` e `isRetryable` em `presentation/components`: "Tentar novamente" só aparece para erros passageiros (`Network`, `Unknown`); chave ausente/inválida e não encontrado orientam a corrigir.
- Tela de Busca: campo com limpar, `SegmentedButton` FILMES | SÉRIES, lista com poster, título e "ano · tipo". Pesquisa ocorre enquanto digita; o botão Buscar do teclado só fecha o teclado.
**Consequência:** `SearchScreen(onResultClick)` já expõe o clique; a parte 3 liga à navegação.

### D-026 — Rotas de detalhes e aba marcada
**Decisão:** `MovieDetailsRoute(id)` e `TvShowDetailsRoute(id)` no mesmo `NavHost` (grafo plano), empilhadas sobre a aba atual. A bottom bar marca a aba da rota atual ou, em telas empilhadas, a última aba visitada. Telas de detalhes são placeholder com botão voltar (D-023). `SearchResultItem.detailsRoute()` escolhe a rota.
**Consequência:** a tela real de detalhes só troca o conteúdo dessas rotas; outras abas (Biblioteca etc.) reutilizam as mesmas rotas.
**Correções após teste no S25:** (1) `NavHost` usa `consumeWindowInsets(innerPadding)` — sem isso a `TopAppBar` dos detalhes aplicava o inset da status bar de novo (espaço extra no topo); (2) clicar numa aba marca essa aba na hora, porque o `restoreState` pode reabrir direto numa tela de detalhes (antes a bottom bar ficava marcando a aba anterior).

### D-027 — Fase 4 em 3 partes
**Contexto:** `fase4.md` (biblioteca pessoal) cobre entidades, migration, operações, Use Cases e Home com dados reais.
**Decisão:** Fase 4 em 3 partes (ver ROADMAP.md): (1) entidades, DAOs e migration, (2) Repository e Use Cases, (3) Home com dados reais. Uma por vez, só com pedido explícito.
**Consequência:** cada parte compila, passa em `./gradlew build` e termina com sugestão de commit.

### D-028 — Tabelas pessoais e migrações explícitas
**Contexto:** `fase4.md` pede entidades `UserMovie`, `UserTvShow`, `Favorite`, `WatchHistory`, `Rating` e migrations corretas; o app já está instalado no S25 (o banco ainda não é aberto pela UI, mas será na parte 3); daqui em diante qualquer instalação pode ter dados pessoais.
**Decisão:**
- `user_movie`, `user_tv_show` e `watch_history` conforme DATABASE.md. Favorite e Rating continuam colunas (`is_favorite`, `rating`), não tabelas (D-009). Sem FK para o cache (D-007). Nota 1–10 validada no use case (Room não gera CHECK).
- `user_episode` e `collection_item` ficam para as fases de episódios e coleção.
- Fim da exceção de D-018: toda mudança de schema a partir da versão 1 tem `Migration` explícita (SQL copiado do schema exportado), registrada em `ALL_MIGRATIONS`; nunca `fallbackToDestructiveMigration`.
- `room-testing` 2.8.5 (mesma versão do Room) em `androidTestImplementation` para `MigrationTestHelper`; schemas entram automaticamente nos assets do teste pelo plugin do Room.
**Consequência:** testes de DAO e migração são instrumentados. Rodar com `./gradlew installDebug installDebugAndroidTest` + `adb shell am instrument -w com.thalyspenha.pipoca.test/androidx.test.runner.AndroidJUnitRunner`; evitar `connectedDebugAndroidTest`, que desinstala o app ao final e apaga os dados do aparelho.

### D-029 — Repository e use cases da biblioteca
**Decisão:**
- `LibraryRepository` (domain) só persiste; `LibraryRepositoryImpl` mapeia entity ↔ `LibraryMovie`/`LibraryTvShow` (datas como `Instant`). Operações com mais de uma tabela são `@Transaction` no `UserLibraryDao`.
- Use cases em `domain/usecase/library`, com `Clock` injetado: adicionar/remover, status, favorito e nota para filmes e séries.
- Regras: adicionar item já existente não muda nada; favoritar ou dar nota a item fora da biblioteca adiciona-o como `WANT_TO_WATCH`; remover favorito/nota de item ausente não adiciona; nota fora de 1–10 lança `IllegalArgumentException` (UI só oferece 1–10); toda alteração atualiza `updatedAt`, marcar o status atual não altera nada.
- Filme: passar a `WATCHED` registra evento em `watch_history` (reassistir soma eventos); voltar a `WANT_TO_WATCH` mantém o histórico; remover o filme da biblioteca apaga o histórico dele (remoção é intencional, e estatísticas não devem contar título removido).
- Série: só status nesta fase; histórico de episódios fica para a fase de episódios.
- "Nota pessoal" = `rating`; o campo `notes` (observações) ainda não tem operação.
**Consequência:** a Home (parte 3) observa `LibraryRepository` diretamente para leitura (D-006) e usa os use cases para ações.

### D-030 — Home com dados da biblioteca
**Decisão:**
- `HomeViewModel` observa `LibraryRepository.observeMovieItems()`/`observeTvShowItems()` (leitura direta, D-006) e monta `HomeContent` com função pura `buildHomeContent`: seções Assistindo (séries `WATCHING`), Quero assistir, Favoritos e Assistidos recentemente (filmes `WATCHED` + séries `COMPLETED`), mais recentes primeiro, até 20 por seção, e contadores.
- Itens vêm do LEFT JOIN com o cache TMDB. Item sem cache (cache limpo) dispara `refreshMovieDetails`/`refreshTvShowDetails` uma vez por item (só com token); até lá mostra "Carregando…".
- Biblioteca vazia: mensagem + botão Buscar (abre a aba Busca) ou aviso de chave ausente. Toque no pôster abre a rota de detalhes.
- `uiState` via `stateIn(WhileSubscribed(5s))`; `load()` saiu (não há o que recarregar: o Room reemite).
**Consequência:** a Home só terá conteúdo quando existir UI para adicionar à biblioteca (tela de detalhes, fase futura).

### D-031 — Fase 5 em 3 partes; coleção e progresso como placeholder
**Contexto:** `fase5.md` pede, no filme, "coleção" e "Adicionar à coleção", e na série, "progresso"; ainda não existem `collection_item` nem episódios.
**Decisão:** Fase 5 em 3 partes (ver ROADMAP.md): (1) ViewModel de detalhes do filme e testes, (2) tela do filme e componentes reutilizáveis, (3) série. Coleção e progresso aparecem como placeholder "em breve" (opção A, escolha do usuário); nada de `collection_item` nem migração nesta fase.
**Consequência:** fases de coleção física e episódios trocam os placeholders pelo conteúdo real.

### D-032 — ViewModel de detalhes do filme
**Decisão:**
- `MovieDetailsViewModel` combina `observeMovieDetails` (cache), `LibraryRepository.observeMovie` e o estado do refresh. Estado próprio `MovieDetailsUiState`: `Loading`/`Error` só sem cache; com cache sempre `Success`, e falha de refresh vira `refreshError` não bloqueante (dispensável). `refresh(force)` ao abrir e para "tentar novamente".
- ID lido do `SavedStateHandle` pela chave `id` (propriedade de `MovieDetailsRoute`), sem `toRoute()`, para testar sem Android.
- Ações pelos use cases da Fase 4; a tela atualiza pela reemissão do Room. Botões de status funcionam como alternância: tocar no status atual tira o filme da biblioteca (e apaga o histórico, D-029). Favorito alterna; nota 1–10 ou `null`.
**Consequência:** a parte 2 só desenha a tela a partir de `MovieDetailsUiState` e liga os eventos.

### D-033 — Tela de detalhes do filme e componentes reutilizáveis
**Decisão:**
- Componentes em `presentation/components/details`: `DetailsHeader` (backdrop 16:9 com degradê e pôster sobreposto 48 dp, sem espaço vazio abaixo), `DetailsSection`, `GenreChips`, `Overview` (4 linhas, toque expande), `CastRow`, `ComingSoonCard`, `StatusSelector<T>` (segmentado genérico), `FavoriteButton`, `RatingSelector` (10 estrelas, tocar na nota atual remove).
- `MovieDetailsScreen`: `Scaffold` com título e voltar; Loading/Error de tela cheia só sem cache; com cache, falha de refresh em snackbar com "Tentar"; barra de progresso fina durante o refresh. Coleção como "Em breve" (D-031).
- `util/DisplayFormat.kt`: `formatRuntime` ("2h 16min") e `formatVote` ("8,2", pt-BR).
- `MovieDetailsRoute` agora abre a tela real; a série segue placeholder até a parte 3.
**Consequência:** a série reutiliza os mesmos componentes; nenhuma dependência nova.

### D-034 — Detalhes da série e ajustes nos componentes
**Decisão:**
- `TvShowDetailsViewModel`/`TvShowDetailsScreen` seguem o contrato do filme (D-032). Status oferecidos: Quero ver, Assistindo, Concluída (`PAUSED`/`DROPPED` ficam para depois); tocar no atual tira da biblioteca. Série também tem nota pessoal (use case já existia), embora `fase5.md` não peça.
- Cabeçalho da série: ano · status de produção traduzido (`tmdbStatusLabel`), "N temporadas · M episódios", nota TMDB. Seção Criação (criadores) no lugar de Direção. Progresso como "Em breve".
- `DetailsScaffold` (barra + snackbar de refresh) extraído e usado por filme e série. `DetailsSection` ganhou slot `action`: o favorito foi para o título da seção de biblioteca, liberando a largura para 3 status. Segmentos sem ✓ (o preenchimento indica a seleção).
- `DetailsPlaceholderScreen` removido; rotas de detalhes usam as telas reais.
**Consequência:** fase de episódios troca o card de progresso; nenhuma dependência nova.

### D-035 — Fase 6 em 3 partes; regra de série concluída
**Contexto:** `fase6.md` pede episódios, progresso e conclusão automática da série.
**Decisão:**
- Fase 6 em 3 partes (ver ROADMAP.md): (1) dados e migração 2→3, (2) regras e use cases, (3) telas.
- Conclusão automática, opção (a) escolhida pelo usuário: vira `COMPLETED` quando todos os episódios exibidos (exceto temporada 0) estão assistidos **e** `tmdb_status` é `Ended` ou `Canceled`; série ainda no ar fica `WATCHING` e a UI mostra "Em dia".
- Transições automáticas: marcar episódio de série fora da biblioteca ou em `WANT_TO_WATCH` passa para `WATCHING`; desmarcar episódio de série `COMPLETED` volta para `WATCHING`.
- Estrutura do TMDB (`tv/{id}/season/{n}`) será conferida na parte 1 antes de fechar as regras.
**Consequência:** substitui a regra provisória de DATABASE.md ("sugerir COMPLETED, não forçar").

### D-036 — Episódios: cache, dados pessoais e migração 2→3
**Contexto:** estrutura do TMDB conferida com respostas reais: `tv/{id}.seasons` traz a temporada 0 (especiais), `number_of_episodes` já exclui especiais, `tv/{id}/season/{n}` traz episódios com `air_date`/`runtime`/`still_path` que podem faltar em episódios futuros.
**Decisão:**
- `tmdb_episode` (FK → `tmdb_season`, CASCADE) com `fetched_at` por episódio; validade da temporada = `MIN(fetched_at)`, com a mesma política da série (`CachePolicy.tvShowTtl`). Nada muda em `tmdb_season`.
- `TmdbEpisodeDao` separado do `TmdbCacheDao` (interface, `saveSeason` transacional) e `SeasonRepository` separado do `TvShowRepository`, para não inflar DAO/fakes existentes. Sem a série em cache, `refreshSeason` busca a série antes (FK).
- `user_episode` conforme DATABASE.md, com `watched_at` não nulo (linha existe = assistido). Operações básicas em `UserLibraryDao`; regras (histórico, status automático) ficam para a parte 2.
- `MIGRATION_2_3` explícita; `MigrationTest` limpa o arquivo antes de cada teste (testes compartilhavam o arquivo e falhavam conforme a ordem).
**Consequência:** migração real 2→3 no S25 preservou a biblioteca. Especiais continuam no cache, mas as regras da parte 2 os excluem do progresso.

### D-037 — Regras de progresso e marcação de episódios
**Decisão:**
- `ShowProgressCalculator` (domain, puro): disponível = temporada ≠ 0 e `air_date` ≤ hoje; assistidos contam só entre os disponíveis; próximo episódio = primeiro disponível não assistido; "próximo a estrear" para série em dia; `isComplete` = todas as temporadas regulares com episódios no cache. `isCompleted` = tudo assistido **e** `Ended`/`Canceled` (D-035); `isCaughtUp` = tudo assistido e série no ar. "Hoje" = `LocalDate.now(clock)` (relógio UTC injetado).
- `LibraryRepository` ganhou episódios assistidos: `markEpisodesWatched` (user_episode + evento `EPISODE` no histórico, transação) e `unmarkEpisodes` (apaga os dois). `removeTvShow` agora apaga também os assistidos e o histórico da série, como filmes (D-029).
- Use cases em `domain/usecase/episodes`: `ObserveShowProgressUseCase`, `RefreshShowEpisodesUseCase` (série + todas as temporadas regulares; para na primeira falha), marcar/desmarcar episódio e temporada, e `SyncShowStatusUseCase` com as transições de D-035. Só episódio já exibido pode ser marcado; marcar de novo não duplica; marcar temporada mantém a data dos já marcados. `PAUSED`/`DROPPED` só mudam se a série ficar concluída.
**Consequência:** a parte 3 só observa `ObserveShowProgressUseCase` e chama os use cases; progresso aparece como parcial até `RefreshShowEpisodesUseCase` baixar todas as temporadas.

### D-038 — Telas de progresso, temporadas e episódios
**Decisão:**
- Detalhes da série: card de progresso ("N / M episódios", barra, percentual; "Próximo: T2E1" com botão "Assisti"; "Em dia · próximo em dd/mm/aaaa" ou "Série concluída") e lista de temporadas (regulares primeiro, especiais no fim, vazias fora) com "assistidos / exibidos" quando a temporada está em cache e ✓ quando completa.
- Todas as temporadas só são baixadas quando a série está (ou entra) na biblioteca, uma vez por abertura da tela (`RefreshShowEpisodesUseCase`); fora da biblioteca o card orienta e oferece "Calcular progresso". Evita dezenas de chamadas para séries longas só por abrir detalhes. Falha vira aviso não bloqueante.
- `SeasonRoute(showId, seasonNumber)` → `SeasonScreen`: imagem do episódio (16:9, inicial "T1E3" sem imagem), "N. Nome", data (dd/MM/aaaa) e duração, sinopse em 2 linhas (toque expande), checkbox; futuro/sem data mostra "Estreia …"/"Sem data" e não pode ser marcado. "Marcar todos"/"Desmarcar todos" no topo.
- O endpoint da temporada pode trazer `poster_path` nulo em pt-BR: o refresh da temporada mantém o resumo já gravado a partir de `tv/{id}` e só grava episódios.
- Fakes compartilhados de série/temporada em `test/.../domain/usecase/episodes/EpisodeFakes.kt`.
**Consequência:** Home e Biblioteca podem mostrar progresso/próximo episódio reutilizando `ObserveShowProgressUseCase`.

### D-039 — Fase 7 em 3 partes
**Contexto:** `fase7.md` pede coleção física/digital com CRUD, tela com filtros e ordenação, independente do status assistido.
**Decisão:** Fase 7 em 3 partes (ver ROADMAP.md): (1) dados e regras com migração 3→4, (2) tela "Minha Coleção", (3) formulário de adicionar/editar/remover e acesso pelos detalhes. Coleção em tabela própria (`collection_item`, sem FK para o cache, D-007), vários itens por título; não toca em `user_movie`/`user_tv_show`, o que garante a independência coleção × assistido.
**Consequência:** os cards "Em breve" de coleção (D-031) são substituídos na parte 3.

### D-040 — Coleção: dados, regras, filtros e ordenação
**Decisão:**
- `collection_item` conforme DATABASE.md, `MIGRATION_3_4` explícita; índice `(media_type, tmdb_id)`. Enums `MediaFormat` e `CollectionMediaType` no domain (a UI usa).
- `CollectionDao` com LEFT JOIN em filme ou série conforme o tipo; `CollectionRepository` só persiste.
- Use cases `Add`/`Update`/`RemoveCollectionItemUseCase` recebem `CollectionItemDraft`: quantidade 1–99, data de aquisição não futura (`IllegalArgumentException`; a UI valida antes), textos aparados e em branco → nulo. Editar não muda título nem tipo e mantém `addedAt`. Nenhum toca na biblioteca (independência coleção × assistido, testada nos dois sentidos).
- `filterAndSort` puro no domain: título com `Collator` pt-BR (ignora acento e caixa), sem título no fim; adicionado recentemente; data de aquisição mais recente primeiro, sem data no fim; desempate por adicionado e id. Filtro `OTHER` só aparece em "Todos".
**Consequência:** as partes 2 e 3 só montam telas sobre `CollectionRepository` e os use cases.

### D-041 — Tela "Minha Coleção"
**Decisão:**
- Aba Coleção deixa de ser placeholder: título "Minha Coleção" + contagem, menu de ordenação (Título, Adicionados recentemente, Data de aquisição), chips de filtro (Todos, 4K, Blu-ray, DVD, Digital) e lista com pôster, título, "formato · edição" e "Filme/Série · N unidades". Toque abre os detalhes do título (edição do item fica para a parte 3).
- Padrão: ordem por título. Filtro e ordenação no `SavedStateHandle`. Coleção vazia e filtro sem itens têm mensagens diferentes.
- Item sem cache TMDB busca detalhes uma vez (como a Home, D-030), só com token.
- Rótulos de formato/filtro/ordenação em `presentation/components/CollectionLabels.kt`.
- Problema conhecido: o lint do AGP às vezes falha com "Unexpected failure during lint analysis" no primeiro `./gradlew build` depois de muitas mudanças e passa ao repetir, sem alteração de código (3 ocorrências, não reproduzido isoladamente). Ver SETUP.md.
**Consequência:** a parte 3 adiciona o formulário e o acesso pelos detalhes; esta tela ganha edição por item.

### D-042 — Formulário da coleção e acesso pelos detalhes
**Decisão:**
- Formulário em tela própria (`CollectionItemFormRoute(tmdbId, mediaType, itemId = 0)`; `mediaType` como nome do enum e `itemId` 0 = novo, para argumentos simples de rota). Campos: formato (chips), edição, região, quantidade (só dígitos, até 2), data de aquisição (`DatePicker` que só permite até hoje, com "Limpar") e observações. Erros aparecem só depois da primeira tentativa de salvar. Remover pede confirmação e lembra que status e favoritos não mudam. Item apagado em outro lugar fecha o formulário.
- Detalhes de filme e série: seção "Coleção" lista os itens do título (toque edita) e oferece "Adicionar à coleção"/"Adicionar outro formato"; substitui o "Em breve" (D-031).
- "Minha Coleção": toque no item abre a edição; a seta abre os detalhes do título.
- `org.gradle.jvmargs` de 2 GB para 4 GB: hipótese para a falha intermitente do lint (D-041), que só acontecia quando o mesmo build compilava, testava e analisava tudo. Build limpo completo passou depois da mudança.
**Consequência:** CRUD completo coberto por testes de use case, DAO (S25) e ViewModel do formulário.

### D-043 — Fase 8 em 3 partes; Favoritos e Histórico na aba "Mais"
**Contexto:** `fase8.md` pede Home como painel, tela de Favoritos e Histórico.
**Decisão:**
- Fase 8 em 3 partes (ver ROADMAP.md): (1) Home, (2) Favoritos, (3) Histórico.
- "Continuar assistindo" = próximo episódio de cada série `WATCHING`, pela série vista mais recentemente; "Séries em andamento" = séries `WATCHING` com barra de progresso.
- Progresso/próximo episódio de todas as séries a partir de uma consulta de episódios e uma de assistidos, sem consulta por série.
- Favoritos e Histórico ficam na aba "Mais", que vira lista de entradas (opção A, escolha do usuário); a Home leva a eles por "Ver todos". Aba "Biblioteca" continua placeholder.
**Consequência:** "Mais" recebe depois estatísticas e configurações.

### D-044 — Home como painel e consultas agregadas
**Decisão:**
- Seções na ordem de `fase8.md`: Continuar assistindo (cartão 16:9 com imagem do próximo episódio, "T2E1 · Nome" e botão "Assisti"), Séries em andamento (pôster, barra, "7 / 62", "Em dia"; "…" quando faltam temporadas no cache), Quero assistir, Assistidos recentemente, Favoritos, Adicionados recentemente à coleção (um cartão por título). Toque em série de Continuar/Andamento abre os detalhes. A antiga seção "Assistindo" foi substituída.
- `ObserveWatchingShowsUseCase`: quatro consultas fixas com JOIN em `user_tv_show.status = 'WATCHING'` (séries com cache, episódios, temporadas, assistidos), agrupadas em memória e passadas ao `ShowProgressCalculator`. Nenhuma consulta por série; número de consultas não cresce com a biblioteca. `LibraryTvShowItem` ganhou `tmdbStatus` (vem no mesmo JOIN).
- Ordem de Continuar assistindo: último episódio assistido mais recente primeiro; séries sem nada assistido no fim.
- `HomeViewModel` combina biblioteca, séries assistindo e coleção via Flow: qualquer marcação em outras telas atualiza a Home. Com token, busca uma vez o que falta (detalhes sem título, inclusive da coleção; temporadas de séries assistindo com progresso incompleto).
- A Home só fica "vazia" sem biblioteca **e** sem coleção.
**Consequência:** Favoritos e Histórico (partes 2 e 3) ganham "Ver todos" nas seções correspondentes.
