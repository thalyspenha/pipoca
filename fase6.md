# IMPLEMENTAR FASE 6 — SÉRIES E EPISÓDIOS

Implementar acompanhamento detalhado de séries.

Criar entidades:

Season
Episode
UserEpisode

Se necessário, adaptar o banco existente sem perder dados.

Implementar:

* lista de temporadas;
* lista de episódios;
* poster/imagem do episódio quando disponível;
* título;
* número;
* sinopse;
* duração;
* data de exibição;
* status assistido.

Ao marcar episódio como assistido:

* salvar no banco;
* registrar data;
* atualizar progresso da série;
* atualizar quantidade de episódios assistidos.

Permitir:

* marcar episódio;
* desmarcar episódio;
* marcar temporada inteira;
* desmarcar temporada inteira.

Calcular:

episódios assistidos / episódios disponíveis

e mostrar:

"37 / 62 episódios"

e percentual.

Determinar automaticamente:

* próxima temporada;
* próximo episódio;
* série concluída.

Não considerar episódios especiais de forma incorreta.

Analise a estrutura retornada pelo TMDB antes de definir a regra final.

Criar testes específicos para cálculo de progresso.

Executar build/testes.

Atualizar DATABASE.md, ARCHITECTURE.md e ROADMAP.md.
