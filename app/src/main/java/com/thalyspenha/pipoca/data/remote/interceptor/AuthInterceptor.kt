package com.thalyspenha.pipoca.data.remote.interceptor

import com.thalyspenha.pipoca.domain.model.TmdbConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adiciona `Authorization: Bearer <token>`. Sem token, segue sem header (o TMDB responde 401). */
class AuthInterceptor @Inject constructor(
    private val tmdbConfig: TmdbConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!tmdbConfig.isConfigured) return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer ${tmdbConfig.apiToken}")
                .build(),
        )
    }
}
