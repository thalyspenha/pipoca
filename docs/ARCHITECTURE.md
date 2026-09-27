# ARCHITECTURE

## Resumo

- Módulo único `:app` (D-001).
- MVVM + Clean Architecture leve: `presentation` → `domain` ← `data` (D-002).
- Room como fonte única da verdade para a UI; rede só atualiza o cache (D-003).
- Injeção de dependência com Hilt.

## Stack

| Área | Escolha | Observação |
|---|---|---|
| Linguagem | Kotlin | |
| UI | Jetpack Compose + Material 3 | Dark/Light, dynamic color opcional |
| Navegação | Navigation Compose | Rotas type-safe (`@Serializable`) |
| DI | Hilt | KSP |
| Banco | Room | KSP, schemas exportados em `app/schemas/` |
| Rede | Retrofit + OkHttp | Converter kotlinx.serialization |
| JSON | kotlinx.serialization | Também usado nas rotas de navegação (D-004) |
| Imagens | Coil 3 | Cache em disco para posters offline |
| Assíncrono | Coroutines + Flow | |
| Testes | JUnit, kotlinx-coroutines-test, AndroidX Test, Room in-memory | |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) | |

Versões exatas (AGP, Kotlin, bibliotecas) estão em `gradle/libs.versions.toml` (D-015 a D-018).

SDK: `minSdk 26`, `compileSdk`/`targetSdk` = 37 (D-010, D-015).

Nenhuma outra dependência deve ser adicionada sem registro em DECISIONS.md.

## Estrutura de pacotes

Pacote base: `com.thalyspenha.pipoca` (D-011).

```
app/src/main/java/com/thalyspenha/pipoca/
├── App.kt                      # @HiltAndroidApp
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   ├── entity/             # cache TMDB + dados pessoais
│   │   └── converter/
│   ├── remote/
│   │   ├── TmdbApi.kt          # interface Retrofit
│   │   ├── TmdbRemoteDataSource.kt
│   │   ├── dto/
│   │   └── interceptor/        # autenticação, idioma
│   ├── mapper/                 # DTO ↔ Entity ↔ Domain
│   └── repository/             # implementações
├── domain/
│   ├── model/                  # modelos puros Kotlin
│   ├── repository/             # interfaces
│   ├── progress/               # cálculo puro de progresso de séries (D-037)
│   └── usecase/                # library/, episodes/
├── presentation/
│   ├── navigation/
│   ├── theme/
│   ├── components/             # componentes reutilizáveis
│   │   └── details/            # blocos das telas de detalhes (D-033)
│   └── screens/
│       └── <feature>/          # Screen + ViewModel + UiState juntos (details/: movie, tv, season)
├── di/                         # módulos Hilt
└── util/
```

Adaptação em relação ao prompt mestre: ViewModels ficam junto da tela da feature (`screens/home/HomeViewModel.kt`) em vez de uma pasta `viewmodel/` global — facilita navegar por feature (D-005).

## Camadas e regras

**presentation**
- Composables sem lógica de negócio. Recebem `UiState` e emitem eventos.
- ViewModels expõem `StateFlow<UiState>`; dependem apenas de use cases ou interfaces de repositório do `domain`.
- Nunca chamam HTTP nem DAO diretamente.

**domain**
- Kotlin puro (sem Android, Room, Retrofit).
- Modelos, interfaces de repositório e use cases.
- Use cases só quando há regra de negócio real (ex.: calcular progresso, transição de status, marcar temporada inteira). Leitura simples pode ir direto ao repositório (D-006).

**data**
- Implementa os repositórios. Orquestra Room + RemoteDataSource.
- Mappers explícitos entre DTO, Entity e modelo de domínio.
- DTOs e Entities nunca vazam para `presentation`.

## Fluxo de dados

```
UI (Compose) ──evento──▶ ViewModel ──▶ UseCase/Repository
     ▲                                     │
     │ StateFlow<UiState>                  ├─▶ Room (Flow)  ◀── fonte da verdade
     └───────────── Flow ◀─────────────────┤
                                           └─▶ RemoteDataSource ─▶ TMDB
                                                 (grava no Room)
```

Leitura de detalhes:
1. Repositório emite o que está no Room (se houver).
2. Se ausente ou vencido (ver Cache), busca no TMDB e grava no Room.
3. O Flow do Room emite de novo automaticamente.
4. Falha de rede com cache presente: mantém cache e sinaliza erro não bloqueante.

Busca (search) é exceção: vai direto à rede, sem persistir resultados (D-008).

## Estado da UI e erros

- `sealed interface UiState<T> { Loading; Success(data); Error(message, cause) }` como padrão genérico; telas complexas podem ter data class própria.
- Camada `data` retorna `Result`/tipo de erro de domínio (`NetworkError`, `NotFound`, `MissingApiKey`, `Unknown`). Exceções de Retrofit/IO não sobem para a UI.
- `MissingApiKey` gera mensagem amigável com instrução de configuração, sem crash.

## Cache

Cada entidade de cache TMDB tem `fetchedAt` (epoch millis). Política stale-while-revalidate:

| Dado | Validade |
|---|---|
| Filme (detalhes, elenco) | 7 dias |
| Série em produção (detalhes) | 1 dia |
| Série encerrada/cancelada | 30 dias |
| Temporada/episódios | mesma validade da série: 1 dia em produção, 30 dias encerrada (D-036) |
| Gêneros | 30 dias |
| Busca | não persiste; debounce 400 ms + cancelamento da busca anterior |

- Dentro da validade: nenhuma chamada de rede.
- Vencido: mostra cache e atualiza em segundo plano.
- Pull-to-refresh força atualização.
- Cache TMDB **nunca** apaga dados pessoais (D-007).
- Imagens: guardamos só os paths TMDB; Coil mantém cache em disco de 250 MB, com cliente HTTP sem o token (D-018). Poster pode faltar offline se nunca foi exibido.

Valores são ponto de partida; ajustar se necessário e registrar.

## Navegação

- `NavHost` único com bottom bar: Home, Busca, Biblioteca, Coleção, Mais (Estatísticas, Histórico, Configurações).
- Detalhes (filme, série, temporada) empilhados sobre a aba atual (`MovieDetailsRoute(id)`, `TvShowDetailsRoute(id)`); a aba de origem continua marcada na bottom bar.
- Rotas type-safe com argumentos simples (IDs TMDB), nunca objetos grandes.
- Estrutura final das abas pode mudar na fase de UI; registrar.

## Testes

- Unitários: mappers, use cases (progresso, status), cálculo de validade do cache, ViewModels com repositórios fake.
- Instrumentados: DAOs com Room in-memory, migrações (`MigrationTestHelper`). Como rodar sem apagar dados do aparelho: `docs/SETUP.md`.
- Cada fase entrega testes da lógica que introduziu.
