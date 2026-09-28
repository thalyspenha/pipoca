package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.CacheClearResult
import com.thalyspenha.pipoca.domain.model.DatabaseInfo
import kotlinx.coroutines.flow.Flow

/** Informações do banco e limpeza de cache (Configurações, D-057). */
interface MaintenanceRepository {
    fun observeDatabaseInfo(): Flow<DatabaseInfo>

    /**
     * Apaga imagens em cache e o cache TMDB de títulos que não estão na biblioteca, na coleção
     * nem no histórico. Dados pessoais nunca são tocados; o que o offline usa fica.
     */
    suspend fun clearCache(): CacheClearResult
}
