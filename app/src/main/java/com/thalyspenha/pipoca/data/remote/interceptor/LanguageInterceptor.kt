package com.thalyspenha.pipoca.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adiciona `language=pt-BR` quando a chamada ainda não define idioma. */
class LanguageInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.queryParameter(PARAM) != null) return chain.proceed(request)
        val url = request.url.newBuilder().addQueryParameter(PARAM, DEFAULT_LANGUAGE).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }

    companion object {
        const val PARAM = "language"
        const val DEFAULT_LANGUAGE = "pt-BR"
    }
}
