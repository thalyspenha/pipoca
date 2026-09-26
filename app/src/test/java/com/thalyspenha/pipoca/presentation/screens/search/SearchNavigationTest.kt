package com.thalyspenha.pipoca.presentation.screens.search

import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNavigationTest {
    @Test
    fun `filme abre detalhes de filme`() {
        val item = SearchResultItem(603, SearchType.MOVIES, "Matrix", 1999, null)

        assertEquals(MovieDetailsRoute(603), item.detailsRoute())
    }

    @Test
    fun `serie abre detalhes de serie`() {
        val item = SearchResultItem(1396, SearchType.TV_SHOWS, "Breaking Bad", 2008, null)

        assertEquals(TvShowDetailsRoute(1396), item.detailsRoute())
    }
}
