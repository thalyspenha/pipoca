# TMDB

Integração com a API oficial do The Movie Database. Status: **infraestrutura pronta** (Fase 1): Retrofit + OkHttp, `AuthInterceptor` (Bearer), `LanguageInterceptor` (`pt-BR`), logging só em debug com header de autorização ocultado, `TmdbImageUrl`. Fase 2 concluída: busca de filmes/séries e detalhes de filme/série, com cache de detalhes no Room.

## Autenticação

- Usar o **API Read Access Token** (v4, Bearer) nas chamadas v3 (D-012). Alternativa: `api_key` v3 via query parameter, se necessário.
- Header adicionado por um `Interceptor` OkHttp: `Authorization: Bearer <token>`.

### Configurar a chave

1. Criar conta em https://www.themoviedb.org e solicitar chave em *Settings → API*.
2. Copiar o **API Read Access Token**.
3. Em `local.properties` (raiz do projeto, já ignorado pelo Git):
   ```
   TMDB_API_TOKEN=seu_token_aqui
   ```
4. O Gradle lê a propriedade e gera `BuildConfig.TMDB_API_TOKEN`.
5. Um arquivo `.env.example` (sem chave real) documentará a variável.

Regras:
- Nunca colocar a chave no código-fonte nem commitar.
- Chave ausente/vazia: o build **não** falha; o app mostra mensagem amigável ("Configure sua chave TMDB...") nas telas que dependem de rede e continua funcionando com dados locais.
- Resposta 401 do TMDB é tratada como chave inválida, com mensagem própria.

## Configuração do cliente

- Base URL: `https://api.themoviedb.org/3/`
- Imagens: `https://image.tmdb.org/t/p/{size}{path}` — tamanhos típicos: poster `w342`/`w500`, backdrop `w780`/`w1280`, perfil `w185`, still `w300`. Montagem centralizada em um helper; apenas o `path` é salvo no banco.
- Idioma: `language=pt-BR` via interceptor. Se sinopse vier vazia, tentar `en-US` (definir na fase de detalhes).
- `region=BR` onde aplicável.
- Timeouts OkHttp padrão razoáveis (~15 s); logging interceptor apenas em debug.

## Endpoints em uso

| Uso | Endpoint | Método em `TmdbApi` |
|---|---|---|
| Buscar filmes | `GET search/movie?query=&page=&include_adult=false` | `searchMovies` |
| Buscar séries | `GET search/tv?query=&page=&include_adult=false` | `searchTvShows` |
| Detalhes filme + elenco | `GET movie/{id}?append_to_response=credits` | `getMovieDetails` |
| Detalhes série + elenco | `GET tv/{id}?append_to_response=credits` | `getTvShowDetails` |

Todos recebem `language=pt-BR` pelo `LanguageInterceptor` e `Authorization: Bearer` pelo `AuthInterceptor`.

DTOs: só os campos usados (ver DATABASE.md); o resto é ignorado (`TmdbJson`). Datas chegam como `String` (podem vir vazias) e são convertidas no mapeamento. Diretor = `credits.crew` com `job == "Director"`. `episode_run_time` costuma vir vazio.

Testes usam respostas reais reduzidas em `app/src/test/resources/tmdb/` (Matrix, Breaking Bad, erro 34).

## Erros

`TmdbRemoteDataSource` é o único ponto que chama `TmdbApi` e devolve `DataResult<DTO>`:

| Situação | `DataError` |
|---|---|
| Token vazio em `local.properties` (nenhuma chamada é feita) | `MissingApiKey` |
| HTTP 401 | `InvalidApiKey` |
| HTTP 404 | `NotFound` |
| `IOException` (sem conexão, timeout, DNS) | `Network` |
| Outros HTTP, JSON inesperado, exceções | `Unknown(cause)` |

Cancelamento de coroutine é repassado, nunca vira erro.

## Mapeamento DTO → domínio

`data/mapper/TmdbMappers.kt`: datas `""`/inválidas → `null`; strings vazias → `null`; `runtime` 0 → `null`; `episode_run_time` → média (ignorando 0); diretores = crew com `job == "Director"`; elenco ordenado por `order`, máximo 15; temporadas ordenadas por número.

## Repositórios

| Interface (`domain/repository`) | Comportamento |
|---|---|
| `SearchRepository` | `searchMovies`/`searchTvShows` direto na rede, sem persistir (D-008). Consulta com menos de 2 caracteres devolve página vazia sem chamar a rede. |
| `MovieRepository` | `observeMovieDetails(id)`: `Flow` do Room (`null` sem cache). `refreshMovieDetails(id, force)`: busca se não houver cache válido (7 dias) e grava. |
| `TvShowRepository` | Igual, para séries. Validade 30 dias se `Ended`/`Canceled`, senão 1 dia. |

Falha no refresh devolve `DataResult.Failure` e não apaga o cache: com cache, a UI mostra os dados e um aviso; sem cache, mostra erro. Validade em `data/cache/CachePolicy.kt`; hora vem de `Clock` injetado.

## Endpoints previstos

| Uso | Endpoint |
|---|---|
| Buscar filmes | `GET search/movie?query=` |
| Buscar séries | `GET search/tv?query=` |
| Busca combinada (opcional) | `GET search/multi?query=` (filtrar `movie`/`tv`) |
| Detalhes filme + elenco | `GET movie/{id}?append_to_response=credits` |
| Detalhes série | `GET tv/{id}?append_to_response=credits` |
| Temporada + episódios | `GET tv/{id}/season/{n}` |
| Gêneros | `GET genre/movie/list`, `GET genre/tv/list` |
| Imagens extras (opcional) | `GET movie/{id}/images`, `GET tv/{id}/images` |

`append_to_response` reduz o número de chamadas.

## Camadas

- `TmdbApi`: interface Retrofit, retorna DTOs (`@Serializable`).
- `TmdbRemoteDataSource`: encapsula `TmdbApi`, converte exceções (`IOException`, `HttpException`, falta de chave) em erros de domínio.
- Repositórios usam o data source e gravam no Room. ViewModels e telas nunca acessam a API.
- JSON: `Json { ignoreUnknownKeys = true; coerceInputValues = true }` — TMDB adiciona campos com frequência e manda `null` em campos inesperados.

## Limites e boas práticas

- Rate limit atual do TMDB é generoso, mas evitar abuso: debounce na busca (400 ms), mínimo de 2 caracteres, cancelar requisição anterior, cache local para detalhes (ver ARCHITECTURE.md).
- Paginação da busca: começar com página 1; paginação infinita avaliada depois.

## Atribuição

Os termos do TMDB exigem atribuição: exibir logo/texto "This product uses the TMDB API but is not endorsed or certified by TMDB" na tela de Configurações/Sobre.
