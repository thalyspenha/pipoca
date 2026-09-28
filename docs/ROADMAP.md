# ROADMAP

Legenda: ✅ concluída · 🚧 em andamento · ⏳ planejada

## Estado atual

**Fase B concluída:** tela "Minha Biblioteca" (Filmes/Séries, filtros com contagem, Grid/Lista salvo, ordenação, pesquisa local, ações rápidas no toque longo) e Pausada/Abandonada nos detalhes da série; 225 testes unitários e 32 instrumentados passando. **Fase 10 concluída:** versão 1.0.0 (APKs debug e release, `RELEASE_NOTES.md`); 238 testes unitários, 34 instrumentados, lint sem avisos. Projeto encerrado em 2026-09-28 na versão 1.0.0 (`fase11.md` está vazio; retomar só com novas instruções).

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

### ✅ Fase 4 — Biblioteca pessoal (ver `fase4.md`)
- Entidades Room separando dados do TMDB (`Movie`, `TvShow`) de dados do usuário (`UserMovie`, `UserTvShow`, `Favorite`, `WatchHistory`, `Rating`).
- Operações: adicionar/remover filme e série; filme assistido/quero assistir; série quero assistir/assistindo/concluída; favoritar/remover favorito; adicionar/remover nota pessoal.
- Repository e Use Cases; Home usando dados reais do banco.
- Migrations corretas, sem apagar banco automaticamente; testes dos principais casos de uso; atualizar DATABASE.md e ROADMAP.md.
- **Executada em 3 partes** (D-027). Uma por vez, só com "IMPLEMENTAR FASE 4 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Entidades, DAOs e migration — tabelas do usuário, migration para nova versão do banco sem destructive fallback, testes de DAO/migration, DATABASE.md.
  2. ✅ Repository e Use Cases — todas as operações listadas, com testes dos casos de uso.
  3. ✅ Home com dados reais — HomeViewModel lendo a biblioteca via Use Cases, estados vazio/lista, ROADMAP.

### ✅ Fase 5 — Detalhes (ver `fase5.md`)
- Filme: backdrop, poster, título, título original, ano, duração, gêneros, nota TMDB, sinopse, diretor, elenco, status pessoal, favorito, nota pessoal, coleção. Ações: quero assistir, marcar assistido, favoritar, adicionar à coleção, avaliar.
- Série: backdrop, poster, título, ano, gêneros, nota TMDB, sinopse, elenco, número de temporadas, progresso, status pessoal, favorito.
- Componentes Compose reutilizáveis; estados Loading/Success/Error; ações gravam no banco na hora; funciona com cache.
- Coleção e progresso da série: placeholder "em breve" nesta fase (opção A, D-031); implementados nas fases de coleção física e episódios.
- **Executada em 3 partes** (D-031). Uma por vez, só com "IMPLEMENTAR FASE 5 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Detalhes do filme: ViewModel e testes — observa cache + biblioteca, refresh, ações via use cases da Fase 4.
  2. ✅ Tela do filme e componentes reutilizáveis — cabeçalho backdrop/poster, chips de gênero, faixa de elenco, seletor de status, favorito, avaliação 1–10.
  3. ✅ Série — ViewModel + tela reusando componentes, status assistindo/concluída; docs e ROADMAP.

### ✅ Fase 6 — Séries e episódios (ver `fase6.md`)
- Entidades Season, Episode, UserEpisode; adaptar banco sem perder dados.
- Listas de temporadas e episódios (imagem, título, número, sinopse, duração, data, assistido).
- Marcar/desmarcar episódio e temporada inteira; ao marcar: salvar, registrar data, atualizar progresso e contagem.
- Progresso "37 / 62 episódios" + percentual; próxima temporada, próximo episódio, série concluída automaticamente; especiais (temporada 0) fora da conta.
- Testes de cálculo de progresso; atualizar DATABASE.md, ARCHITECTURE.md, ROADMAP.md.
- Série concluída: regra (a) — automática só se todos os episódios exibidos estiverem assistidos **e** o TMDB indicar fim (Ended/Canceled); se ainda no ar, fica Assistindo ("Em dia") (D-035).
- **Executada em 3 partes** (D-035). Uma por vez, só com "IMPLEMENTAR FASE 6 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Dados — endpoint/DTO de temporada, `tmdb_episode` + `user_episode`, migração 2→3, cache de episódios no repositório; testes de parsing, DAO e migração.
  2. ✅ Regras — cálculo puro de progresso, próximo episódio/temporada, conclusão; use cases de marcar/desmarcar episódio e temporada (com `watch_history`); testes de progresso.
  3. ✅ Telas — temporadas, episódios, navegação a partir dos detalhes da série; progresso real no lugar do "Em breve"; docs.

### ✅ Fase 7 — Minha Coleção (ver `fase7.md`)
- `CollectionItem`: tmdbId, mediaType, formato (4K UHD Blu-ray, Blu-ray, DVD, Digital, Outro), edição, região, quantidade, data de aquisição, observações; um título pode ter vários itens.
- Adicionar filme/série, editar, remover. Tela "Minha Coleção" (pôster, título, formato, edição), filtros Todos/4K/Blu-ray/DVD/Digital, ordenação título/adicionado recentemente/data de aquisição.
- Coleção independente de status assistido (ex.: 4K + Quero assistir, 4K + Assistido). Testar CRUD completo; build; docs.
- **Executada em 3 partes** (D-039). Uma por vez, só com "IMPLEMENTAR FASE 7 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Dados e regras — `collection_item`, migração 3→4, consulta com título/pôster do cache, repositório e use cases (adicionar/editar/remover, validação), filtros e ordenação puros; testes de CRUD, DAO e migração.
  2. ✅ Tela "Minha Coleção" — aba Coleção com lista, filtros, ordenação e estado vazio.
  3. ✅ Formulário — adicionar/editar (formato, edição, região, quantidade, data, observações) e remover; acesso pelos detalhes de filme e série e pela tela da coleção; docs.

### ✅ Fase 8 — Home e organização (ver `fase8.md`)
- Home como painel: Continuar assistindo, Séries em andamento, Quero assistir, Assistidos recentemente, Favoritos, Adicionados recentemente à coleção; sem consultas excessivas; atualização automática via Flow.
- Tela Favoritos (filmes e séries, remover favorito).
- Histórico: título, episódio, data, pôster; mais recente primeiro; abrir o conteúdo; filtros básicos. Filme assistido e cada episódio geram histórico.
- Favoritos e Histórico acessados pela aba "Mais" (lista de entradas) e por "Ver todos" na Home (opção A, D-043).
- **Executada em 3 partes** (D-043). Uma por vez, só com "IMPLEMENTAR FASE 8 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Home — consultas agregadas (progresso/próximo episódio de todas as séries em uma consulta de episódios + uma de assistidos), seis seções, ViewModel e testes.
  2. ✅ Favoritos — tela com abas Filmes/Séries, remover favorito; aba "Mais" com entradas; "Ver todos" na Home.
  3. ✅ Histórico — consulta com cache (título, pôster, episódio), filtros Todos/Filmes/Séries, abrir conteúdo, testes de geração; ROADMAP.

### ✅ Fase 9 — Estatísticas (ver `fase9.md`)
- Só dados locais: filmes (assistidos, quero assistir, na coleção, favoritos), séries (total, em andamento, concluídas, quero ver), episódios (total, no mês, no ano), tempo (minutos, horas, dias equivalentes, com limitações documentadas), gêneros, notas pessoais, coleção por formato.
- Consultas eficientes no Room, sem duplicar dados; testes dos cálculos; interface limpa.
- Tempo: soma cada visualização do histórico (reassistir conta); filme = duração TMDB; episódio = duração do episódio ou média da série; sem duração → "N sem duração".
- Gêneros: opção (a) — filmes assistidos e séries com ≥ 1 episódio visto ou concluídas, cada título uma vez por gênero (D-047). Barras em Compose, sem biblioteca nova.
- **Executada em 2 partes** (D-047). Uma por vez, só com "IMPLEMENTAR FASE 9 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Consultas e cálculos — consultas agregadas (COUNT/GROUP BY), cálculo de tempo e distribuições no domain; testes.
  2. ✅ Tela — Estatísticas com seções e barras, entrada na aba "Mais"; docs (limitações em DATABASE.md).

### ✅ Fase B — Biblioteca (ver `biblioteca.md`)
- Tela "Minha Biblioteca": FILMES | SÉRIES, filtros por status com contagens do banco, Grid (LazyVerticalGrid) e Lista com preferência salva, `MediaPosterCard` (pôster, título, ano, status, progresso da série, favorito), ordenação, pesquisa local, estados vazios, ações rápidas em menu, reativa via Flow. Independente da coleção.
- Decisões (D-050): abas Início · Biblioteca · Busca · Coleção · Mais; status Pausada/Abandonada passam a ser selecionáveis nos detalhes da série; ordenação no SQL, exceto progresso (em memória sobre as consultas agregadas); pesquisa local em memória ignorando acento; preferência Grid/Lista em `SharedPreferences`.
- **Executada em 3 partes** (D-050). Uma por vez, só com "IMPLEMENTAR FASE B – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Dados — consultas de lista/contagens/ordenação no Room, progresso de todas as séries da biblioteca, pesquisa e filtros; testes.
  2. ✅ Tela — Grid/Lista, `MediaPosterCard`, filtros com contagem, ordenação, pesquisa, estados vazios, nova ordem das abas, preferência salva.
  3. ✅ Complementos — menu de ações rápidas, Pausada/Abandonada nos detalhes, polimento; docs e ROADMAP.

### ✅ Fase 10 — Polimento e release pessoal (ver `fase10.md`)
- Tema resolvido na parte 3 (D-057); release assinado com a chave de debug, sem R8 (D-058).
- **Executada em 4 partes** (D-054). Uma por vez, só com "IMPLEMENTAR FASE 10 – PARTE N"; cada parte compila e termina com sugestão de commit:
  1. ✅ Auditoria — 26 achados em `docs/AUDIT.md` com severidade e destino (P2 correções, P3 Configurações, P4 release).
  2. ✅ Correções — offline sem avisos desnecessários e busca de itens sem cache com nova tentativa (D-055), paleta completa claro/escuro, janela e ícone temático (D-056), acessibilidade, `key.txt` ignorado.
  3. ✅ Configurações — aba "Mais" → Configurações: Tema (Sistema/Claro/Escuro), Limpar cache (preserva o que o usuário usa), informações do banco, versão; Sobre com atribuição ao TMDB (D-057).
  4. ✅ Release — versão 1.0.0 (versionCode 2), APK debug e release (assinado com a chave de debug, sem R8), `RELEASE_NOTES.md`, SETUP (D-058).

### ⏸️ Fase 11
`fase11.md` lido em 2026-09-28: arquivo vazio. Sem fase definida; projeto encerrado na 1.0.0 por decisão do usuário. Ideias para o futuro em `RELEASE_NOTES.md` (backup JSON, logo TMDB, R8, lembrete de episódios).

Ajuste pós-1.0.0 (2026-09-28): aba "Mais" sempre reabre no menu (D-059).
Revisão de QA pós-1.0.0 (2026-09-28): fuso do "hoje", histórico duplicado por toque duplo, toque duplo no status e plural corrigidos (D-060); 239 testes unitários, 35 instrumentados. Pendentes de baixa prioridade listados em D-060.

## Pendências gerais

- Token TMDB (Read Access Token v4): ✅ validado e configurado no Mac (2026-09-26) e no Linux (2026-09-28). Em outra máquina, repetir no `local.properties` (ver SETUP.md).
- ✅ Testado no S25 (Android 16) em 2026-09-26: 4 testes instrumentados do DAO passando (`./gradlew connectedDebugAndroidTest`); app instalado e validado manualmente (Home, Busca filmes/séries, pôsteres e fallback, debounce = 1 requisição por pesquisa, detalhes placeholder, voltar, troca de abas). Dois bugs de navegação achados e corrigidos (D-026).
- ✅ 2026-09-28, a partir do Linux: app reinstalado no S25 (chave de debug diferente da do Mac; dados apagados, ver SETUP.md), 32 testes instrumentados passando, aba Biblioteca verificada (estado vazio, Dark Mode).
- ~~Polimento visual: lilás padrão do Material na bottom bar e nos segmentos~~ ✅ paleta completa (D-056).
- ~~Verificar JDK/Android SDK~~ ✅ JDK 25 (JBR do Android Studio, `JAVA_HOME` no `~/.zshrc`), SDK em `~/Library/Android/sdk` (android-33…37.1). JDK 25 exige Gradle ≥ 9.1.
