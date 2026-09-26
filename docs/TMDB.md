# TMDB

Integração com a API oficial do The Movie Database. Status: **planejado**.

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
