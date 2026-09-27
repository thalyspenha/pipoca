# IMPLEMENTAR FASE — BIBLIOTECA

Antes de começar:

1. Leia /docs/PROJECT.md
2. Leia /docs/ARCHITECTURE.md
3. Leia /docs/DATABASE.md
4. Leia /docs/ROADMAP.md
5. Leia /docs/DECISIONS.md
6. Analise todo o código já implementado.

NÃO recrie funcionalidades que já existem.

A Biblioteca será uma das telas principais do aplicativo.

==================================================

## OBJETIVO

==================================================

Criar uma tela "Biblioteca" onde o usuário consiga visualizar e organizar todos os filmes e séries que adicionou ao aplicativo.

A Biblioteca NÃO representa necessariamente a coleção física.

Exemplo:

Um filme pode estar na Biblioteca sem estar na coleção física.

Exemplo:

Predator

Biblioteca:
✅ Assistido

Coleção:
❌ Não possuo

Outro exemplo:

Alien

Biblioteca:
🕐 Quero assistir

Coleção:
💿 4K UHD Blu-ray

Esses dois conceitos devem permanecer independentes.

==================================================

## NAVEGAÇÃO

==================================================

Adicionar Biblioteca como uma das principais áreas do aplicativo.

Sugestão de navegação inferior:

🏠 Início
📚 Biblioteca
🔍 Buscar
💿 Coleção
⚙️ Configurações

Se a arquitetura atual utilizar outra navegação, adapte sem quebrar o que já existe.

==================================================

## ESTRUTURA DA BIBLIOTECA

==================================================

A tela deverá possuir:

Título:

"Minha Biblioteca"

E controles para:

FILMES | SÉRIES

Além disso, permitir filtrar por status.

==================================================

## FILTROS DE FILMES

==================================================

Criar filtros:

Todos
Quero assistir
Assistidos
Favoritos

Exemplo:

┌────────────────────────────────────┐
│ Minha Biblioteca                   │
│                                    │
│ FILMES        SÉRIES               │
│                                    │
│ [Todos] [Quero assistir] [Assistidos] │
│                                    │
│ ┌────────┐ ┌────────┐              │
│ │ poster │ │ poster │              │
│ │        │ │        │              │
│ │ Filme  │ │ Filme  │              │
│ └────────┘ └────────┘              │
│                                    │
└────────────────────────────────────┘

Utilizar LazyVerticalGrid para os posters.

==================================================

## FILTROS DE SÉRIES

==================================================

Para séries:

Todos
Quero assistir
Assistindo
Concluídas
Pausadas
Abandonadas
Favoritos

Os filtros devem refletir exatamente os estados existentes no banco.

==================================================

## CARD

==================================================

Criar um componente reutilizável:

MediaPosterCard

O card deverá mostrar:

* poster;
* título;
* ano;
* indicador de status;
* progresso para séries;
* indicador de favorito quando aplicável.

Para séries:

Exemplo:

Breaking Bad

██████████░░░░ 72%

36/50 episódios

Para filmes:

Predator

✅ Assistido

Não sobrecarregar o card com informações.

O poster deve ser o elemento visual principal.

==================================================

## DETALHES DE STATUS

==================================================

FILMES

Quero assistir:

🕐 Quero assistir

Assistido:

✅ Assistido

SÉRIES

Quero assistir:

🕐 Quero assistir

Assistindo:

▶️ Assistindo

Concluída:

✅ Concluída

Pausada:

⏸ Pausada

Abandonada:

❌ Abandonada

==================================================

## ORDENAÇÃO

==================================================

Criar opção de ordenar por:

* Adicionado recentemente
* Título A-Z
* Título Z-A
* Ano de lançamento
* Última atividade
* Nota pessoal

Para séries, quando possível:

* Progresso
* Último episódio assistido

A ordenação deve ser feita de forma eficiente.

Não carregue todos os registros desnecessariamente apenas para ordenar na UI.

==================================================

## VISUALIZAÇÃO

==================================================

Criar pelo menos:

1. Grid
2. Lista

Grid:

poster grande.

Lista:

┌─────────────────────────────────────┐
│ ┌──────┐                            │
│ │poster│  Predator                  │
│ │      │  1987                      │
│ └──────┘  ✅ Assistido              │
└─────────────────────────────────────┘

Permitir alternar entre os modos.

Salvar a preferência do usuário localmente.

==================================================

## PESQUISA NA BIBLIOTECA

==================================================

Adicionar pesquisa local.

Quando o usuário tocar na busca:

permitir pesquisar somente dentro da biblioteca.

Exemplo:

Biblioteca possui:

Predator
Predator 2
Alien
Aliens
Terminator

Pesquisa:

"predator"

Resultado:

Predator
Predator 2

Essa pesquisa NÃO deve consultar o TMDB.

Ela deve utilizar os dados locais.

==================================================

## EMPTY STATES

==================================================

Criar estados vazios específicos.

Exemplo:

Biblioteca vazia:

" sua biblioteca está vazia "

"Pesquise um filme ou série para começar."

Com botão:

"Buscar títulos"

Para:

Quero assistir vazio:

"Você ainda não adicionou títulos para assistir."

Assistidos vazio:

"Você ainda não marcou nenhum título como assistido."

==================================================

## INTERAÇÃO

==================================================

Ao tocar em um item:

abrir a tela de detalhes correspondente.

Não abrir diretamente o TMDB.

Os detalhes devem utilizar os dados locais quando disponíveis.

==================================================

## AÇÕES RÁPIDAS

==================================================

Avaliar a possibilidade de permitir ações rápidas no card:

* marcar como assistido;
* remover da biblioteca;
* favorito.

Não adicionar ações que deixem o card visualmente poluído.

Se necessário, utilizar menu contextual.

==================================================

## CONTAGEM

==================================================

Mostrar opcionalmente contagens:

Filmes

42 títulos

Séries

18 títulos

Ou:

Todos (60)
Quero assistir (23)
Assistidos (27)

As contagens devem vir do banco.

==================================================

## RELAÇÃO COM O BANCO

==================================================

Utilizar as entidades e Repository existentes.

NÃO criar uma segunda fonte de verdade.

A Biblioteca deve refletir:

UserMovie
UserTvShow
Favorite
UserEpisode

e demais entidades existentes.

O status do usuário deve ser derivado dos dados corretos.

==================================================

## REATIVIDADE

==================================================

Quando o usuário:

* marcar um filme como assistido;
* remover um filme;
* adicionar favorito;
* alterar status;
* atualizar progresso de série;

a Biblioteca deve atualizar automaticamente.

Utilizar Flow/StateFlow adequadamente.

Evitar refresh manual desnecessário.

==================================================

## PERFORMANCE

==================================================

A biblioteca poderá eventualmente conter centenas ou milhares de títulos.

Portanto:

* utilizar LazyVerticalGrid/LazyColumn;
* paginação local se realmente necessária;
* consultas eficientes;
* não carregar imagens gigantes;
* utilizar Coil;
* evitar recomposição desnecessária.

Não implementar complexidade prematuramente.

==================================================

## DESIGN

==================================================

Visual cinematográfico e moderno.

Dark Mode deve ser muito bem suportado.

Priorizar posters.

Utilizar Material 3.

Criar componentes reutilizáveis.

Não copiar visualmente TV Time.

==================================================

## ACESSIBILIDADE

==================================================

Adicionar contentDescription apropriado para imagens.

Garantir que textos importantes tenham contraste suficiente.

Botões devem possuir áreas de toque adequadas.

==================================================

## TESTES

==================================================

Criar testes para:

* filtros;
* ordenação;
* pesquisa local;
* contagens;
* status;
* atualização reativa;
* cálculo de progresso exibido para séries.

==================================================

## BUILD

==================================================

Ao terminar:

1. executar testes;
2. executar ./gradlew build;
3. corrigir erros;
4. corrigir warnings importantes;
5. verificar navegação;
6. verificar Dark Mode;
7. verificar estados vazios;
8. atualizar documentação;
9. atualizar ROADMAP.md.

Não implemente funcionalidades que pertencem a fases futuras.

Ao final informe:

* arquivos criados;
* arquivos modificados;
* funcionalidades implementadas;
* testes realizados;
* resultado do build;
* eventuais limitações.

# FIM DA FASE — BIBLIOTECA
