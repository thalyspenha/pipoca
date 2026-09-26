# ROADMAP

Legenda: ✅ concluída · 🚧 em andamento · ⏳ planejada

## Estado atual

**Fase 0 concluída.** Apenas documentação. Nenhum código Android existe ainda.

## Fases

### ✅ Fase 0 — Planejamento
- Documentação: PROJECT, ARCHITECTURE, DATABASE, TMDB, ROADMAP, DECISIONS.
- Arquitetura, modelo de dados, estratégia de cache e integração TMDB definidos.
- Nome definido: Pipoca, `com.thalyspenha.pipoca` (D-011).

### ⏳ Fase 1 — Fundação (ver `fase1.md`)
- Projeto Android funcional: Kotlin, Compose, Material 3.
- Navigation Compose, Hilt, Room, Retrofit/OkHttp, Coil configurados.
- Estrutura de pacotes conforme ARCHITECTURE.md.
- Tema Dark/Light.
- Home inicial e navegação básica.
- Estado de UI genérico (Loading/Success/Error).
- `TMDB_API_TOKEN` via `local.properties` → `BuildConfig`; `.env.example`; `.gitignore`.
- Sem funcionalidade TMDB completa.
- `./gradlew build` passando.

### ⏳ Fases 2–11
Definidas nos arquivos `fase2.md` … `fase11.md` (ainda não lidos). Esta seção será atualizada com o conteúdo real de cada fase quando forem analisadas.

Ordem provável derivada do prompt mestre, apenas como referência:
- Integração TMDB (busca + detalhes) e cache.
- Biblioteca de filmes (status, favorito, nota, data).
- Séries, temporadas, episódios e progresso.
- Coleção física.
- Histórico.
- Home completa.
- Estatísticas.
- Configurações, polimento de UI, offline.

## Pendências gerais

- Obter token TMDB e colocar em `local.properties` para testar integração.
- Verificar JDK/Android SDK no ambiente de build.
