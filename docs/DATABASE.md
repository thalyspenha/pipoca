# DATABASE

Room. Status: **planejado** (nenhuma tabela implementada ainda).

## Princípios

1. Duas famílias de tabelas separadas:
   - **Cache TMDB** (`tmdb_*`): pode ser atualizado ou apagado a qualquer momento.
   - **Dados pessoais** (`user_*`, `collection_item`, `watch_history`): nunca apagados pelo cache.
2. IDs do TMDB são a chave de identidade de filmes, séries, temporadas e episódios.
3. Tabelas pessoais **não** têm foreign key para o cache (D-007). Assim, limpar/atualizar cache nunca apaga dado pessoal. Integridade é garantida no repositório.
4. Nada de duplicar dado TMDB nas tabelas pessoais (título, poster etc. vêm do cache via JOIN).
5. Progresso, contagens e estatísticas são **calculados**, não armazenados.
6. Datas: `Long` epoch millis (UTC) para instantes; `LocalDate` via converter (ISO string) para datas sem hora (lançamento, aquisição).
7. `exportSchema = true`; migrações explícitas a partir da primeira versão usada de verdade.

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
| director | String? | nome(s) do diretor, extraído dos créditos |
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
| air_date | LocalDate? | usado para "próximos episódios" |
| runtime_minutes | Int? | |

Índice único `(show_id, season_number, episode_number)`.

### tmdb_genre
`id` Long PK, `name` String. Relações N:N:
- `tmdb_movie_genre(movie_id, genre_id)` PK composta
- `tmdb_tv_show_genre(show_id, genre_id)` PK composta

### tmdb_person / tmdb_credit
- `tmdb_person(id PK, name, profile_path)`
- `tmdb_credit(media_type, media_id, person_id, character, order)` PK `(media_type, media_id, person_id)` — só elenco principal (ex.: top 15).

Elenco pode ficar para fase posterior; tabela planejada desde já.

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

Data em que assistiu vem de `watch_history` (permite reassistir).

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
| watched_at | Long? | data de visualização (editável) |

Linha existe = assistido. Desmarcar = apagar linha.

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

Índices em `watched_at`, `movie_id`, `show_id`. Desmarcar episódio remove o evento correspondente.

### collection_item
| Coluna | Tipo | Nota |
|---|---|---|
| id | Long PK autoincrement | |
| media_type | MediaType | `MOVIE`, `TV_SHOW` |
| tmdb_id | Long | índice |
| format | MediaFormat | `UHD_4K_BLURAY`, `BLURAY`, `DVD`, `DIGITAL`, `OTHER` |
| edition | String? | ex.: "Steelbook" |
| region | String? | ex.: "B", "A/B", "Free" |
| quantity | Int | default 1 |
| notes | String? | |
| acquired_at | LocalDate? | opcional |
| added_at / updated_at | Long | |

Um título pode ter vários itens (ex.: 4K e DVD).

## Ajustes em relação ao prompt mestre

| Sugerido | Decisão | Motivo |
|---|---|---|
| Favorite (tabela) | coluna `is_favorite` | relação 1:1 com user_movie/user_tv_show (D-009) |
| Rating (tabela) | coluna `rating` | idem |
| Movie/TvShow/Season/Episode | `tmdb_*` | prefixo deixa explícito que é cache |
| Genre, Person, Credit | adicionadas | exigidas pelos detalhes e estatística por gênero |

## Cálculos (não armazenados)

- **Progresso da série** = episódios assistidos / episódios lançados, excluindo temporada 0 (especiais). Episódio lançado = `air_date <= hoje`.
- **Próximo episódio** = primeiro episódio lançado não assistido, em ordem (temporada, número).
- **Status automático**: ao marcar o último episódio lançado, sugerir `COMPLETED` (não forçar para séries em produção). Regra final definida na fase de episódios.
- **Horas assistidas** = soma de `runtime_minutes` dos filmes assistidos (considerando reassistidos) + episódios assistidos (fallback `episode_run_time`).

## Enums

Guardados como `String` (nome do enum) via converter, para não quebrar se a ordem mudar.
