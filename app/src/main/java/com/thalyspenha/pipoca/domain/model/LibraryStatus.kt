package com.thalyspenha.pipoca.domain.model

/** Status pessoal de um filme na biblioteca. */
enum class MovieStatus { WANT_TO_WATCH, WATCHED }

/** Status pessoal de uma série na biblioteca. `PAUSED` e `DROPPED` reservados para fases futuras. */
enum class TvShowStatus { WANT_TO_WATCH, WATCHING, COMPLETED, PAUSED, DROPPED }
