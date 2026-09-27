# DATABASE

Room. Status: **cache TMDB** (Fase 2): `tmdb_genre`, `tmdb_movie`, `tmdb_movie_genre`, `tmdb_tv_show`, `tmdb_tv_show_genre`, `tmdb_season` (resumo vindo dos detalhes da série), `tmdb_person`, `tmdb_credit`, acesso por `TmdbCacheDao`; `tmdb_episode` (Fase 6) por `TmdbEpisodeDao` via `SeasonRepository`. **Dados pessoais** (Fase 4, parte 1): `user_movie`, `user_tv_show`, `watch_history`, acesso por `UserLibraryDao` via `LibraryRepository` (Fase 4, parte 2). Listas para a UI usam LEFT JOIN com o cache (`observeMoviesWithCache`/`observeTvShowsWithCache`): título/poster nulos quando o cache não existe, sem nunca perder o item pessoal (D-030). `user_episode` (Fase 6) também em `UserLibraryDao`. `collection_item` (Fase 7) por `CollectionDao` via `CollectionRepository`. `AppDatabase` versão 4, schema em `app/schemas/`.

## Migrações

A partir da versão 1 toda mudança de schema tem `Migration` explícita em `data/local/migration/Migrations.kt`, registrada em `ALL_MIGRATIONS` e testada em `MigrationTest` (`MigrationTestHelper` valida contra o schema exportado). Nunca `fallbackToDestructiveMigration` (D-028).

| Versão | Mudança |
|---|---|
| 1 | Cache TMDB |
| 2 | `user_movie`, `user_tv_show`, `watch_history` (+ índices) |
| 3 | `tmdb_episode` (FK → `tmdb_season`, CASCADE), `user_episode` (+ índices) |
| 4 | `collection_item` (+ índice `(media_type, tmdb_id)`) |

## Princípios

1. Duas famílias de tabelas separadas:
   - **Cache TMDB** (`tmdb_*`): pode ser atualizado ou apagado a qualquer momento.
   - **Dados pessoais** (`user_*`, `collection_item`, `watch_history`): nunca apagados pelo cache.
2. IDs do TMDB são a chave de identidade de filmes, séries, temporadas e episódios.
3. Tabelas pessoais **não** têm foreign key para o cache (D-007). Assim, limpar/atualizar cache nunca apaga dado pessoal. Integridade é garantida no repositório.
4. Nada de duplicar dado TMDB nas tabelas pessoais (título, poster etc. vêm do cache via JOIN).
5. Progresso, contagens e estatísticas são **calculados**, não armazenados.
6. Datas: `Long` epoch millis (UTC) para instantes; `LocalDate` via converter (ISO string) para datas sem hora (lançamento, aquisição).
7. `exportSchema = true`; migrações explícitas desde a versão 1 (ver Migrações).

## Cache TMDB

### tmdb_movie
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK | TMDB ID |
| title | String | |
| original_title | String | |
| overview | String? | |
| poster_path | String? | só o path |
| backdrop_path | String? | |
| release_date | LocalDate? | ano derivado |
| runtime_minutes | Int? | |
| vote_average | Double? | nota TMDB |
| directors | List<String> | nomes dos diretores (JSON), extraídos dos créditos (D-022) |
| fetched_at | Long | controle de cache |

### tmdb_tv_show
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK | |
| name | String | |
| original_name | String | |
| overview | String? | |
| poster_path / backdrop_path | String? | |
| first_air_date | LocalDate? | |
| tmdb_status | String? | "Returning Series", "Ended"... (não é status pessoal) |
| number_of_seasons | Int? | |
| number_of_episodes | Int? | |
| episode_run_time | Int? | média, para horas estimadas |
| vote_average | Double? | |
| creators | List<String> | criadores (JSON) (D-022) |
| fetched_at | Long | |

### tmdb_season
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK | TMDB season id |
| show_id | Long FK → tmdb_tv_show (CASCADE) | índice |
| season_number | Int | 0 = especiais |
| name | String | |
| overview | String? | |
| poster_path | String? | |
| air_date | LocalDate? | |
| episode_count | Int | |
| fetched_at | Long | |

Índice único `(show_id, season_number)`.

### tmdb_episode
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK | TMDB episode id |
| show_id | Long | índice |
| season_id | Long FK → tmdb_season (CASCADE) | |
| season_number | Int | |
| episode_number | Int | |
| name | String | |
| overview | String? | |
| still_path | String? | |
| air_date | LocalDate? | nula = sem data anunciada; base de "episódio lançado" |
| runtime_minutes | Int? | 0 do TMDB vira nulo |
| fetched_at | Long | controle de cache; validade da temporada = `MIN(fetched_at)` dos episódios |

Índice único `(show_id, season_number, episode_number)`; índice em `season_id`. Gravação transacional (`TmdbEpisodeDao.saveSeason`): upsert da temporada (sem REPLACE, para não disparar CASCADE), episódios que sumiram saem antes do upsert. A linha de `tmdb_season` gravada é a que já veio de `tv/{id}` quando existe (o endpoint da temporada pode trazer pôster nulo em pt-BR, D-038).

Estrutura do TMDB conferida (D-036): `tv/{id}.seasons` inclui a temporada 0 (especiais); `number_of_episodes` já exclui especiais (Breaking Bad: 62, com 9 especiais à parte). Episódios futuros podem vir sem `air_date`/`runtime`/`still_path`.

### tmdb_genre
`id` Long PK, `name` String. Relações N:N:
- `tmdb_movie_genre(movie_id, genre_id)` PK composta
- `tmdb_tv_show_genre(show_id, genre_id)` PK composta

### tmdb_person / tmdb_credit
- `tmdb_person(id PK, name, profile_path)`
- `tmdb_credit(media_type, media_id, person_id, character, order)` PK `(media_type, media_id, person_id)` — só elenco principal (top 15). `media_type` = enum `MOVIE`/`TV`; sem FK (aponta para tabelas diferentes).

Gravação é transacional (`saveMovie`/`saveTvShow`): upsert (não REPLACE, para não disparar CASCADE), gêneros e elenco da obra são substituídos, temporadas que sumiram são removidas.

## Dados pessoais

### user_movie
| Coluna | Tipo | Nota |
|---|---|---|
| movie_id | Long PK | TMDB ID |
| status | MovieStatus | `WANT_TO_WATCH`, `WATCHED` |
| is_favorite | Boolean | (D-009) |
| rating | Int? | nota pessoal 1–10 (exibida como 0,5–5 estrelas ou 1–10; decidir na UI) |
| notes | String? | observações |
| added_at | Long | |
| updated_at | Long | |

Data em que assistiu vem de `watch_history` (permite reassistir). Passar a `WATCHED` grava filme + evento na mesma transação; voltar a `WANT_TO_WATCH` mantém o histórico; remover o filme apaga o histórico dele (D-029).

### user_tv_show
| Coluna | Tipo | Nota |
|---|---|---|
| show_id | Long PK | |
| status | TvShowStatus | `WANT_TO_WATCH`, `WATCHING`, `COMPLETED`, `PAUSED`, `DROPPED` |
| is_favorite | Boolean | |
| rating | Int? | |
| notes | String? | |
| added_at / updated_at | Long | |

### user_episode
| Coluna | Tipo | Nota |
|---|---|---|
| episode_id | Long PK | TMDB episode id |
| show_id | Long | índice |
| season_number | Int | redundante de propósito: permite progresso sem cache |
| episode_number | Int | idem |
| watched_at | Long | data de visualização (linha só existe se assistido) |

Linha existe = assistido. Desmarcar = apagar linha. Marcar grava também um evento `EPISODE` em `watch_history`; desmarcar apaga o evento; remover a série da biblioteca apaga os assistidos e o histórico dela (D-037). Só episódios já exibidos podem ser marcados (especiais inclusive).

### watch_history
Log de eventos de visualização (base para Histórico e estatísticas por ano).

| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK autoincrement | |
| media_type | MediaType | `MOVIE`, `EPISODE` |
| movie_id | Long? | |
| show_id | Long? | |
| episode_id | Long? | |
| watched_at | Long | |

Índices em `watched_at`, `movie_id`, `show_id`. Desmarcar episódio remove o evento correspondente. Tela Histórico (D-046): `UserLibraryDao.observeHistory` junta o evento com `tmdb_movie`/`tmdb_tv_show`/`tmdb_episode` e, para temporada/número sem cache, `user_episode`; ordem `watched_at` e `id` decrescentes.

### collection_item
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK autoincrement | |
| media_type | CollectionMediaType | `MOVIE`, `TV_SHOW` |
| tmdb_id | Long | índice `(media_type, tmdb_id)` |
| format | MediaFormat | `UHD_4K_BLURAY`, `BLURAY`, `DVD`, `DIGITAL`, `OTHER` |
| edition | String? | ex.: "Steelbook" |
| region | String? | ex.: "B", "A/B", "Free" |
| quantity | Int | 1–99 (validado no use case) |
| notes | String? | |
| acquired_at | LocalDate? | opcional; não pode ser futura |
| added_at / updated_at | Long | |

Um título pode ter vários itens (ex.: 4K e DVD). Independente de `user_movie`/`user_tv_show`: adicionar/editar/remover item não mexe em status, favorito ou histórico, e vice-versa (D-039, D-040). Lista da tela usa LEFT JOIN em `tmdb_movie` ou `tmdb_tv_show` conforme `media_type` (título/pôster nulos sem cache). Filtros (Todos/4K/Blu-ray/DVD/Digital; `OTHER` só em Todos) e ordenação (título pt-BR sem acento/caixa, adicionado recentemente, data de aquisição com sem-data no fim) em `filterAndSort`, no domain.

## Ajustes em relação ao prompt mestre

| Sugerido | Decisão | Motivo |
|---|---|---|
| Favorite (tabela) | coluna `is_favorite` | relação 1:1 com user_movie/user_tv_show (D-009) |
| Rating (tabela) | coluna `rating` | idem |
| Movie/TvShow/Season/Episode | `tmdb_*` | prefixo deixa explícito que é cache |
| Genre, Person, Credit | adicionadas | exigidas pelos detalhes e estatística por gênero |

## Cálculos (não armazenados)

- **Progresso da série** (`ShowProgressCalculator`, D-037) = episódios assistidos **entre os disponíveis** / episódios disponíveis. Disponível = temporada ≠ 0 e `air_date <= hoje` (sem data = ainda não saiu). Percentual arredonda para baixo. Especiais assistidos e assistidos que sumiram do TMDB não contam. Progresso só é "completo" quando todas as temporadas regulares com episódios estão no cache; sem isso, nunca conclui.
- **Home (D-044)**: progresso de todas as séries `WATCHING` sai de consultas fixas com JOIN em `user_tv_show.status = 'WATCHING'` (`observeWatchingShowsEpisodes`/`Seasons` em `TmdbEpisodeDao`, `observeWatchingShowsWatchedEpisodes` em `UserLibraryDao`), sem consulta por série.
- **Próximo episódio** = primeiro disponível não assistido, em ordem (temporada, número), mesmo com posteriores assistidos. **Próxima temporada** = temporada dele. **Próximo a estrear** = primeiro regular não exibido (para "Em dia").
- **Status automático** (D-035): `COMPLETED` automático quando todos os episódios lançados (sem temporada 0) estão assistidos **e** `tmdb_status` é `Ended`/`Canceled`; no ar, fica `WATCHING` ("Em dia"). Marcar episódio de série fora da biblioteca ou em `WANT_TO_WATCH` passa a `WATCHING`; desmarcar episódio de `COMPLETED` volta a `WATCHING`.
- **Tempo assistido** (`StatsDao.observeWatchTime`, D-048) = soma por visualização em `watch_history` (reassistir conta): filme `runtime_minutes`, episódio `runtime_minutes` ou `episode_run_time` da série; sem duração ou sem cache fica fora dos minutos e é contado à parte. Demais estatísticas em `StatsDao` (consultas agregadas, sem tabela nova).

## Enums

Guardados como `String` (nome do enum, conversão nativa do Room), para não quebrar se a ordem mudar. `MovieStatus` e `TvShowStatus` ficam em `domain/model` (usados pela UI); `WatchMediaType` e `CreditMediaType` são da camada `data`.
