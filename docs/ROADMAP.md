# ROADMAP

Legenda: ✅ concluída · 🚧 em andamento · ⏳ planejada

## Estado atual

**Fase 3 concluída.** Busca completa (filmes/séries) com navegação para detalhes placeholder; 63 testes unitários passando. Próxima: Fase 4 (aguardando permissão para ler `fase4.md`).

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

### ✅ Fase 2 — Integração TMDB (ver `fase2.md`)
- Busca de filmes e séries; detalhes de filme e série (com elenco).
- Retrofit/API service, DTOs, RemoteDataSource, mapeamento DTO → domínio, erros, URLs de imagem, Repository.
- Testes: parsing, mapeamento, erro básico. Docs em `TMDB.md`; informar endpoints usados.
- Sem UI nova (tela de Busca continua placeholder). Fora: histórico, favoritos, coleção, progresso de episódios.
- **Executada em 3 partes** (decisão do usuário, D-019). Uma por vez, só com "IMPLEMENTAR FASE 2 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ API e DTOs — endpoints `search/movie`, `search/tv`, `movie/{id}` e `tv/{id}` com `append_to_response=credits`; DTOs `@Serializable`; testes de parsing com JSON de exemplo.
  2. ✅ RemoteDataSource, erros e mapeamento — erros de domínio (rede, não encontrado, chave ausente/inválida, desconhecido); modelos de domínio; mappers DTO → domínio; testes de mapeamento e erro.
  3. ✅ Repository e cache — busca direto na rede sem persistir (D-008); detalhes com cache no Room (`tmdb_movie`, `tmdb_tv_show`, gêneros, elenco) e validade conforme ARCHITECTURE.md; docs `TMDB.md`/`DATABASE.md` e ROADMAP.

### ✅ Fase 3 — Busca (ver `fase3.md`)
- Tela Search completa: campo, debounce, loading, erro, vazio, resultados (poster, título, ano, tipo), alternância FILMES | SÉRIES, navegação para detalhes.
- Sem requisição com texto vazio, durante digitação rápida ou repetindo a pesquisa em andamento.
- ViewModel + StateFlow, sem lógica de API na UI; testes do ViewModel.
- Detalhes: só rota + tela placeholder (opção B, D-023); tela real de detalhes fica para fase futura.
- **Executada em 3 partes** (D-023). Uma por vez, só com "IMPLEMENTAR FASE 3 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ SearchViewModel e testes — debounce 400 ms, ignora texto vazio/curto, não repete a mesma pesquisa, cancela a anterior, alternância filmes/séries; `kotlinx-coroutines-test`.
  2. ✅ Tela de Busca — campo, alternância FILMES/SÉRIES, lista com poster (Coil), título, ano, tipo; loading, erro (chave ausente, sem conexão) e vazio.
  3. ✅ Navegação para detalhes — rotas `MovieDetailsRoute(id)`/`TvShowDetailsRoute(id)` com telas placeholder; docs e ROADMAP.

### ⏳ Fases 4–11
Definidas nos arquivos `fase4.md` … `fase11.md` (ainda não lidos). Esta seção será atualizada com o conteúdo real de cada fase quando forem analisadas.

Ordem provável derivada do prompt mestre, apenas como referência:
- Biblioteca de filmes (status, favorito, nota, data).
- Séries, temporadas, episódios e progresso.
- Coleção física.
- Histórico.
- Home completa.
- Estatísticas.
- Configurações, polimento de UI, offline.

## Pendências gerais

- Token TMDB (Read Access Token v4): ✅ validado e configurado no Mac (2026-09-26). Em outra máquina, repetir no `local.properties` (ver SETUP.md).
- ✅ Testado no S25 (Android 16) em 2026-09-26: 4 testes instrumentados do DAO passando (`./gradlew connectedDebugAndroidTest`); app instalado e validado manualmente (Home, Busca filmes/séries, pôsteres e fallback, debounce = 1 requisição por pesquisa, detalhes placeholder, voltar, troca de abas). Dois bugs de navegação achados e corrigidos (D-026).
- Polimento visual (fase de UI): indicador da bottom bar e botão segmentado usam o `secondaryContainer` padrão (lilás), fora da paleta amarelo/vermelho do `PipocaTheme`.
- ~~Verificar JDK/Android SDK~~ ✅ JDK 25 (JBR do Android Studio, `JAVA_HOME` no `~/.zshrc`), SDK em `~/Library/Android/sdk` (android-33…37.1). JDK 25 exige Gradle ≥ 9.1.
