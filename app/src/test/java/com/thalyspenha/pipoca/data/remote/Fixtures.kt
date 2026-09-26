package com.thalyspenha.pipoca.data.remote

/** Lê respostas reais do TMDB (reduzidas) de `src/test/resources/tmdb/`. */
fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource("tmdb/$name")) { "fixture $name não encontrada" }
        .readText()
