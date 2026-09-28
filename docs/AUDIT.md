# Auditoria — Fase 10, parte 1

> **Parte 2 (2026-09-28):** resolvidos #1, #2, #3 (D-055), #4 na parte de busca (Biblioteca usa `FetchMissingDetailsUseCase`; o que "Limpar cache" preserva fica para a P3), #5 e #7 (D-056; claro e escuro vistos no S25, com a biblioteca vazia), #17 (`key.txt` no `.gitignore`), #22 (pôsteres decorativos para leitor de tela, ordenação e estrela selecionadas anunciadas), #24 (ícone monocromático; lint sem avisos), #26 (testes das mudanças).

Data: 2026-09-28. Base: commit `8cdd94a` (Fase B concluída). ~10 mil linhas em `app/src/main`, 225 testes unitários, 32 instrumentados, lint com 1 aviso.

Severidade: **A** = bug ou requisito de `fase10.md` não atendido · **M** = melhora perceptível · **B** = cosmético/opcional.
Destino: **P2** = correções (parte 2) · **P3** = Configurações (parte 3) · **P4** = release (parte 4) · **—** = só registro, sem ação.

## Resumo

A base está saudável: camadas separadas (domain sem Android), Room com migrações testadas e sem `fallbackToDestructiveMigration`, dados pessoais nunca apagados pelo cache (D-030), exceções de rede convertidas em `DataError`, Flows reativos em todas as telas, listas Lazy com `key`. Nenhum problema de arquitetura que peça reescrita.

Os achados relevantes são de **offline** (avisos de rede quando há dados salvos), **tema** (Light Mode com cores padrão lilás) e itens que a própria fase pede (Sobre/TMDB, Configurações, release).

## Achados

| # | Área | Sev. | Achado | Proposta | Destino |
|---|------|------|--------|----------|---------|
| 1 | Offline / erros | A | Detalhes (filme/série) e temporada com cache **vencido** tentam atualizar ao abrir; sem internet aparece snackbar "Mostrando dados salvos. Sem conexão…" toda vez. `fase10.md`: avisar só quando a operação exigir conexão. | Com dados em cache, falha `Network` no refresh automático fica silenciosa; snackbar só em refresh pedido pelo usuário ou erros não-rede (chave inválida etc.). | P2 |
| 2 | Offline / erros | A | Série na biblioteca: `loadEpisodes` baixa temporadas que faltam ao abrir; offline, mesmo snackbar de erro, embora o progresso com o que já está salvo funcione. | Mesmo critério do #1: falha de rede automática silenciosa; o card de progresso já mostra "Carregando temporadas…"/incompleto. | P2 |
| 3 | Offline | M | Home: `fetchMissing` marca o id como "já pedido" antes do resultado; se falhar (offline), não tenta de novo até recriar o ViewModel. Itens sem cache ficam "Carregando…". | Remover do conjunto de pedidos quando o resultado for falha. | P2 |
| 4 | Offline | M | Biblioteca não busca detalhes de itens sem cache (D-052); a Coleção e a Home buscam. Hoje é raro (itens entram pelos detalhes), mas "Limpar cache" (P3) vai criar esse caso. | Extrair a busca de itens sem cache (Home/Coleção) para um use case e usar também na Biblioteca; ou "Limpar cache" preservar o cache de itens da biblioteca/coleção. Decidir na P3 junto com o #12. | P2/P3 |
| 5 | UI / tema | A | Light Mode: esquema claro só define `primary`, `primaryContainer`, `secondary`; o resto (inclusive `secondaryContainer`, usado no indicador da bottom bar e nos segmentos de status) é o lilás padrão do Material. No escuro o mesmo lilás aparece. Pendência antiga do ROADMAP. | Completar os esquemas claro/escuro com a paleta manteiga/vermelho (`secondaryContainer`, `tertiary`, `surface*` levemente quentes). Verificar contraste. | P2 |
| 6 | UI / tema | M | Tema segue só o sistema; `dynamicColor` existe mas sem opção. `fase10.md` pede Tema nas Configurações. | Preferência Sistema/Claro/Escuro em `PreferencesRepository` (D-052), aplicada em `MainActivity`. | P3 |
| 7 | UI | M | Não verifiquei o Light Mode no aparelho nesta parte (trocar o tema do S25 altera configuração do sistema). | Verificar todas as telas no claro e no escuro depois do #5. | P2 |
| 8 | Erros | M | Só Home e Estatísticas tratam exceção do Room (`catch`); nas demais telas uma falha de banco derruba o app. Improvável (banco local), mas sem rede de segurança. | Aceitar (banco local, migrações testadas). Registrar como limitação. | — |
| 9 | Performance | M | `observeLibraryShowsEpisodes` (Biblioteca, D-051) carrega todos os episódios em cache de todas as séries da biblioteca e reemite a cada episódio marcado. Com dezenas de séries longas são milhares de linhas por emissão. Home tem o mesmo padrão, só com séries assistindo. | Aceitável para o volume atual. Se ficar lento: agregar no SQL (contagem de disponíveis/assistidos por série) em vez de trazer episódios. Só medir, não mudar agora. | — |
| 10 | Performance | B | `observeMovieDetails`/`observeTvShowDetails` fazem 2 consultas extras (gêneros, elenco) por emissão, fora do Flow. Correto porque o refresh regrava a entidade; só não é reativo a mudanças isoladas de elenco. | Nenhuma. | — |
| 11 | Performance | — | Imagens: Coil com cache em disco de 250 MB, tamanhos TMDB adequados (`w154` listas, `w342` grade, `w500`/`w780` detalhes). Busca com debounce 400 ms, sem pesquisa repetida, cancela a anterior. Cache TMDB com validade (filme 7 d, série 1/30 d). Listas Lazy com `key` e `@Immutable` nos cards. | Ok. | — |
| 12 | Cache | M | O cache TMDB (filmes, séries, temporadas, episódios, elenco) nunca é limpo: tudo que foi aberto nos detalhes fica para sempre. Bom para offline, cresce sem limite. | "Limpar cache" nas Configurações: apagar imagens (Coil) e o cache TMDB de títulos **fora** da biblioteca e da coleção (manter o que o offline precisa). | P3 |
| 13 | Banco | — | 4 versões com migrações e `MigrationTest`; FKs e 10 índices; esquema exportado em `app/schemas/`. Remoção de título apaga histórico/episódios dele e não toca a coleção (conferido no DAO). | Ok. | — |
| 14 | Banco | M | Configurações pede "informações do banco". | Mostrar versão do schema, contagens (filmes, séries, episódios assistidos, itens da coleção, títulos em cache) e tamanho do arquivo. | P3 |
| 15 | Segurança | M | Token TMDB vai no `BuildConfig`: qualquer APK gerado contém o token em texto. Aceitável para app pessoal (D-012); não compartilhar o APK. Log HTTP só em debug, `BASIC`, com `Authorization` ocultado. | Registrar no `RELEASE_NOTES.md` e no SETUP. | P4 |
| 16 | Segurança | B | `android:allowBackup="true"` sem regras: o backup do Android inclui o banco (bom: preserva biblioteca ao trocar de aparelho) e as `SharedPreferences`. Não há segredo em arquivos do app. | Manter; documentar. Opcional: `dataExtractionRules` excluindo nada sensível. | — |
| 17 | Segurança / higiene | B | `key.txt` (token) solto na raiz, fora do `.gitignore`; risco de commit acidental. | Apagar o arquivo, ou adicionar `key.txt` ao `.gitignore`. | P2 |
| 18 | TMDB | A | Não há atribuição ao TMDB no app (termos de uso da API exigem). | Tela Configurações → Sobre com logo/texto "This product uses the TMDB API but is not endorsed or certified by TMDB". | P3 |
| 19 | Configurações | A | Não existe tela de Configurações (Sobre, Tema, Limpar cache, banco, versão). A aba "Mais" já prevê a entrada. | Criar. | P3 |
| 20 | Release | A | `versionName` 0.1.0, release sem assinatura e sem minificação. | Definir versão 1.0.0; assinatura com keystore fora do git (`keystore.properties` ignorado); R8 opcional (se ativar, testar Retrofit/serialization/Room). | P4 |
| 21 | Release | M | Chave de debug diferente por máquina: instalar de outra máquina exige desinstalar (aconteceu em 2026-09-28). | Mesmo `debug.keystore` nas duas máquinas (SETUP.md), ou instalar sempre o release assinado com a mesma chave. | P4 |
| 22 | Acessibilidade | M | Pôsteres com `contentDescription = null` (decorativos, o título está ao lado) — correto nas listas. Verificar se há pôster sem título visível (ex.: faixas horizontais da Home) e botões só com ícone sem descrição. 21 usos de `contentDescription = null` para revisar. | Revisar cada um; descrição onde o ícone é a única informação. | P2 |
| 23 | Acessibilidade | B | Estrelas da nota (10 × 32 dp) abaixo dos 48 dp recomendados de área de toque (escolha para caber na largura do S25, D-033). | Manter; registrar. | — |
| 24 | Lint | B | 1 aviso: `MonochromeLauncherIcon` (ícone temático do Android 13+). | Adicionar `<monochrome>` ao ícone adaptativo. | P2 |
| 25 | Textos | B | Textos fixos no código (só `app_name` em `strings.xml`). App pessoal, só pt-BR. | Manter; registrar. | — |
| 26 | Testes | M | Boa cobertura (DAOs, migrações, repositórios, use cases, ViewModels, progresso, estatísticas). Sem teste: `SharedPreferencesRepository`, `LibraryRepositoryImpl`/`CollectionRepositoryImpl`/`StatsRepositoryImpl` (cobertos indiretamente pelos DAOs). | Testes para o que mudar nas P2/P3 (regra de erro silencioso, preferências de tema, limpeza de cache). | P2/P3 |

## Offline — situação atual

| Requisito (`fase10.md`) | Estado |
|---|---|
| Biblioteca funciona offline | ✅ Room; nenhuma chamada de rede. |
| Coleção funciona offline | ✅ Room. |
| Favoritos funcionam offline | ✅ Room. |
| Histórico funciona offline | ✅ Room. |
| Progresso funciona offline | ✅ com os episódios já em cache; ⚠ aviso de rede desnecessário (#2). |
| Detalhes carregados continuam disponíveis | ✅ cache nunca expira para leitura; ⚠ aviso de rede desnecessário com cache vencido (#1). |
| Aviso só quando a operação exige conexão | ⚠ Busca e primeiro acesso a detalhes avisam corretamente; refresh automático com cache também avisa (#1, #2). |

## Plano das próximas partes

- **Parte 2 — Correções:** #1, #2, #3, #4 (use case de busca de itens sem cache), #5, #7, #17, #22, #24; testes das mudanças.
- **Parte 3 — Configurações:** #6, #12 (e decisão do #4 sobre o que "Limpar cache" preserva), #14, #18, #19.
- **Parte 4 — Release:** #15, #20, #21, `RELEASE_NOTES.md`, `./gradlew clean build`, APKs.
