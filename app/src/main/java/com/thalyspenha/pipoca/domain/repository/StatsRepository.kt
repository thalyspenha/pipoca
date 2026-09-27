package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.stats.Statistics
import kotlinx.coroutines.flow.Flow

/** Estatísticas locais (D-048). Limites do mês/ano em epoch millis. */
interface StatsRepository {
    fun observeStatistics(monthStart: Long, yearStart: Long): Flow<Statistics>
}
