# ROADMAP

Legenda: ✅ concluída · 🚧 em andamento · ⏳ planejada

## Estado atual

**Fase 1 concluída.** Fundação pronta: projeto compila, 10 testes unitários passando. Próxima: Fase 2 (aguardando "IMPLEMENTAR FASE 2" e permissão para ler `fase2.md`).

## Fases

### ✅ Fase 0 — Planejamento
- Documentação: PROJECT, ARCHITECTURE, DATABASE, TMDB, ROADMAP, DECISIONS.
- Arquitetura, modelo de dados, estratégia de cache e integração TMDB definidos.
- Nome definido: Pipoca, `com.thalyspenha.pipoca` (D-011).

### ✅ Fase 1 — Fundação (ver `fase1.md`)
- Projeto Android funcional: Kotlin, Compose, Material 3.
- Navigation Compose, Hilt, Room, Retrofit/OkHttp, Coil configurados.
- Estrutura de pacotes conforme ARCHITECTURE.md.
- Tema Dark/Light.
- Home inicial e navegação básica.
- Estado de UI genérico (Loading/Success/Error).
- `TMDB_API_TOKEN` via `local.properties` → `BuildConfig`; `.env.example`; `.gitignore`.
- Sem funcionalidade TMDB completa.
- `./gradlew build` passando.
- **Executada em 4 partes** (decisão do usuário, D-014). Uma parte por vez, só quando o usuário disser "PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Esqueleto Android — Gradle ≥ 9.1, Kotlin, Compose, Material 3, `MainActivity`, `local.properties` → `BuildConfig`, `.gitignore`, `.env.example` (objetivos 1–4, 15, 16).
  2. ✅ Tema e navegação — pacotes, tema Dark/Light, Navigation Compose, Home inicial (10–13).
  3. ✅ Hilt e estado da UI — Hilt, `UiState` Loading/Success/Error, ViewModel da Home (6, 14).
  4. ✅ Room, Retrofit/OkHttp, Coil — infraestrutura sem endpoints/tabelas finais; build, testes, docs (7–9).

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

- Obter token TMDB e colocar em `local.properties` para testar integração (em cada máquina; ver SETUP.md).
- Ainda não rodado no S25 nem em emulador; só build e testes unitários.
- ~~Verificar JDK/Android SDK~~ ✅ JDK 25 (JBR do Android Studio, `JAVA_HOME` no `~/.zshrc`), SDK em `~/Library/Android/sdk` (android-33…37.1). JDK 25 exige Gradle ≥ 9.1.
