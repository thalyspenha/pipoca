# PROMPT MESTRE — APLICATIVO PESSOAL DE FILMES E SÉRIES

Você é o principal desenvolvedor deste projeto Android.

Quero construir um aplicativo Android pessoal para gerenciamento de filmes e séries, inspirado conceitualmente em aplicativos como TV Time, Letterboxd e gerenciadores de coleções, mas NÃO quero copiar identidade visual, código ou marca de nenhum deles.

O aplicativo será exclusivamente para uso pessoal/hobby. Não será publicado na Google Play inicialmente.

Seu trabalho será desenvolver o projeto de forma incremental, organizada, documentada e sustentável.

==================================================

## 1. OBJETIVO DO APLICATIVO

==================================================

O aplicativo deverá permitir que eu:

* pesquise filmes e séries;
* visualize capas e informações;
* adicione filmes/séries à minha biblioteca;
* marque filmes como assistidos;
* marque filmes como "quero assistir";
* acompanhe séries;
* marque episódios individuais como assistidos;
* acompanhe automaticamente o progresso de séries;
* favorite filmes e séries;
* dê uma nota pessoal;
* registre quando assisti;
* mantenha uma coleção física;
* diferencie 4K Blu-ray, Blu-ray, DVD etc.;
* veja estatísticas da minha biblioteca;
* utilize o aplicativo mesmo sem internet para consultar os dados já salvos.

A fonte principal de informações externas será a API do TMDB.

==================================================

## 2. TECNOLOGIAS

==================================================

Use tecnologias modernas e estáveis para Android.

Stack preferencial:

* Kotlin
* Jetpack Compose
* Material 3
* MVVM
* Clean Architecture em nível adequado ao tamanho do projeto
* Room
* Retrofit
* OkHttp
* Kotlin Coroutines
* Kotlin Flow
* Hilt
* Navigation Compose
* Coil
* kotlinx.serialization ou Gson, preferencialmente kotlinx.serialization se houver boa compatibilidade com a arquitetura escolhida
* JUnit
* AndroidX Test

Evite bibliotecas desnecessárias.

Não adicione dependências apenas por conveniência.

Priorize código simples, legível, testável e sustentável.

==================================================

## 3. ARQUITETURA

==================================================

Organize o projeto de maneira semelhante a:

app/
data/
local/
remote/
repository/
domain/
model/
repository/
usecase/
presentation/
navigation/
screens/
components/
viewmodel/
di/
util/

A estrutura pode ser adaptada se você encontrar uma solução tecnicamente melhor.

Não siga a estrutura cegamente.

Documente qualquer decisão arquitetural relevante.

==================================================

## 4. BANCO DE DADOS

==================================================

Use Room.

O banco deverá separar:

1. informações provenientes do TMDB;
2. informações pessoais do usuário.

Não dependa da API para determinar se algo foi assistido.

O estado pessoal do usuário deve permanecer local.

Considere entidades para:

* Movie
* TvShow
* Season
* Episode
* UserMovie
* UserTvShow
* UserEpisode
* CollectionItem
* WatchHistory
* Favorite
* Rating

Você pode ajustar essa modelagem se encontrar uma estrutura melhor.

IMPORTANTE:

Não duplicar desnecessariamente informações do TMDB.

Use IDs do TMDB como identificadores externos.

==================================================

## 5. TMDB

==================================================

Use a API oficial do TMDB.

O aplicativo deverá conseguir:

* pesquisar filmes;
* pesquisar séries;
* obter detalhes de filmes;
* obter detalhes de séries;
* obter temporadas;
* obter episódios;
* obter elenco quando disponível;
* obter gêneros;
* obter imagens;
* obter informações relevantes para a interface.

Crie uma camada RemoteDataSource separada.

Não coloque chamadas HTTP diretamente dentro das telas.

Não coloque lógica de API dentro dos ViewModels.

Use Repository + Use Cases quando fizer sentido.

==================================================

## 6. API KEY

==================================================

Nunca coloque a API key diretamente no código-fonte.

Use local.properties / BuildConfig ou mecanismo equivalente.

Crie um arquivo:

.env.example

ou documentação equivalente mostrando como configurar a chave.

Nunca commite uma chave real.

Se a API key não estiver configurada, o aplicativo deverá apresentar uma mensagem amigável em vez de simplesmente quebrar.

==================================================

## 7. FUNCIONALIDADES

==================================================

### FILMES

Cada filme poderá ter:

* título;
* título original;
* poster;
* backdrop;
* sinopse;
* ano;
* duração;
* gêneros;
* nota TMDB;
* diretor;
* elenco;
* status pessoal;
* favorito;
* nota pessoal;
* data em que assisti;
* observações;
* posse física.

### SÉRIES

Cada série poderá ter:

* título;
* poster;
* backdrop;
* sinopse;
* ano;
* gêneros;
* temporadas;
* episódios;
* status pessoal;
* favorito;
* nota pessoal;
* progresso.

### EPISÓDIOS

Cada episódio poderá:

* ser marcado como assistido;
* ser desmarcado;
* possuir data de visualização;
* participar do cálculo de progresso.

==================================================

## 8. STATUS PESSOAIS

==================================================

Não trate o status do TMDB como status pessoal.

Crie estados próprios.

Para filmes:

* WANT_TO_WATCH
* WATCHED

Para séries:

* WANT_TO_WATCH
* WATCHING
* COMPLETED
* PAUSED
* DROPPED

Se houver uma razão técnica para alterar isso, documente.

==================================================

## 9. COLEÇÃO FÍSICA

==================================================

Essa é uma funcionalidade importante.

Um filme ou série poderá estar na coleção física.

Tipos:

* 4K UHD Blu-ray
* Blu-ray
* DVD
* Digital
* Outro

Também permitir:

* edição;
* região;
* observação;
* quantidade;
* data de aquisição opcional.

Exemplo:

Predator (1987)

TMDB ID: XXXXX

Coleção:

4K UHD Blu-ray
Edição: Steelbook
Região: B
Observação: edição importada

==================================================

## 10. TELAS

==================================================

Planeje pelo menos:

1. Home
2. Busca
3. Resultado da busca
4. Detalhes do filme
5. Detalhes da série
6. Temporadas
7. Episódios
8. Quero assistir
9. Assistidos
10. Minha coleção
11. Favoritos
12. Histórico
13. Estatísticas
14. Configurações

A navegação deve ser intuitiva.

==================================================

## 11. HOME

==================================================

A Home deverá mostrar informações úteis, por exemplo:

* continuar assistindo;
* adicionados recentemente;
* próximos episódios;
* filmes que quero assistir;
* séries em andamento;
* favoritos;
* estatísticas resumidas.

Não deixe a Home excessivamente carregada.

==================================================

## 12. PESQUISA

==================================================

A busca deverá permitir pesquisar:

* filmes;
* séries.

Mostrar:

* poster;
* título;
* ano;
* tipo.

Ao tocar em um resultado, abrir detalhes.

Evitar requisições desnecessárias.

Implementar debounce quando apropriado.

==================================================

## 13. OFFLINE-FIRST

==================================================

O aplicativo deverá funcionar parcialmente offline.

Quando informações já tiverem sido baixadas:

* detalhes devem continuar disponíveis;
* biblioteca deve continuar disponível;
* histórico deve continuar disponível;
* coleção deve continuar disponível;
* favoritos devem continuar disponíveis;
* progresso de séries deve continuar disponível.

A internet será necessária principalmente para:

* novas pesquisas;
* atualização de informações;
* download de novos dados.

==================================================

## 14. CACHE

==================================================

Não faça chamadas ao TMDB sempre que abrir uma tela.

Utilize cache local.

Defina uma estratégia razoável para atualização dos dados.

Documente a estratégia.

==================================================

## 15. UI/UX

==================================================

Visual moderno, limpo e cinematográfico.

Priorize:

* posters grandes;
* imagens de backdrop;
* cards;
* navegação simples;
* Material 3;
* Dark Mode;
* Light Mode.

A interface deve funcionar bem em celulares.

Não copie visualmente o TV Time.

Crie identidade própria.

==================================================

## 16. ESTATÍSTICAS

==================================================

Posteriormente implementar:

* quantidade de filmes assistidos;
* quantidade de séries;
* episódios assistidos;
* horas aproximadas assistidas;
* quantidade de filmes na coleção;
* distribuição por gênero;
* filmes assistidos por ano;
* notas pessoais.

Essas estatísticas devem ser calculadas a partir dos dados locais.

==================================================

## 17. QUALIDADE DO CÓDIGO

==================================================

Regras obrigatórias:

* código limpo;
* funções pequenas;
* nomes claros;
* evitar duplicação;
* evitar lógica de negócio na UI;
* ViewModels sem chamadas HTTP diretas;
* Repository para acesso aos dados;
* tratamento de erros;
* estados de Loading/Success/Error;
* testes para lógica importante.

Não implemente "gambiarras temporárias" sem documentá-las.

==================================================

## 18. DOCUMENTAÇÃO PERMANENTE

==================================================

Crie e mantenha:

/docs/PROJECT.md
/docs/ARCHITECTURE.md
/docs/DATABASE.md
/docs/TMDB.md
/docs/ROADMAP.md
/docs/DECISIONS.md

Esses arquivos são extremamente importantes.

Sempre que uma decisão arquitetural relevante for tomada, registre em DECISIONS.md.

Sempre atualize ROADMAP.md.

==================================================

## 19. REGRA MAIS IMPORTANTE PARA O CLAUDE

==================================================

Antes de modificar qualquer código:

1. leia PROJECT.md;
2. leia ARCHITECTURE.md;
3. leia DATABASE.md;
4. leia ROADMAP.md;
5. examine o código existente;
6. entenda o estado atual do projeto.

NUNCA assuma que o projeto está no estado inicial.

NUNCA recrie arquivos existentes sem necessidade.

NUNCA apague funcionalidades existentes para implementar uma nova funcionalidade sem motivo técnico.

Preserve o que já funciona.

==================================================

## 20. DESENVOLVIMENTO INCREMENTAL

==================================================

Este projeto será desenvolvido em fases.

Você NÃO deve tentar implementar o aplicativo inteiro imediatamente.

Quando eu disser:

"IMPLEMENTAR FASE X"

você deverá:

1. analisar o estado atual;
2. identificar o que já existe;
3. implementar apenas a fase solicitada;
4. executar testes/build quando possível;
5. corrigir erros;
6. atualizar documentação;
7. atualizar ROADMAP.md;
8. informar exatamente o que foi implementado;
9. informar qualquer pendência.

==================================================

## 21. BUILD

==================================================

Ao terminar cada fase:

* execute o build;
* execute testes relevantes;
* corrija erros de compilação;
* não deixe código propositalmente quebrado.

Se o build não puder ser executado devido ao ambiente, explique claramente.

==================================================

## 22. GIT

==================================================

Se o projeto possuir Git:

não faça commits automaticamente sem que eu peça.

Você pode sugerir uma mensagem de commit ao final da fase.

==================================================

## 23. PRIMEIRA TAREFA

==================================================

Neste momento NÃO implemente todas as funcionalidades.

Sua tarefa agora é:

1. analisar este documento;
2. criar a estrutura inicial do projeto;
3. criar a documentação;
4. definir a arquitetura;
5. definir o modelo inicial de dados;
6. definir o roadmap;
7. preparar a integração com TMDB;
8. criar uma primeira tela mínima funcional;
9. garantir que o projeto compile.

Depois disso, pare.

Não implemente ainda:

* biblioteca completa;
* coleção;
* histórico;
* estatísticas;
* episódios;
* funcionalidades avançadas.

Primeiro vamos construir uma fundação sólida.

Ao terminar, apresente:

* estrutura de pastas;
* arquitetura escolhida;
* dependências;
* banco planejado;
* roadmap;
* como configurar TMDB API key;
* resultado do build;
* próximos passos.

# FIM DO PROMPT MESTRE
