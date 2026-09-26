package com.thalyspenha.pipoca

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

    // Coil usa o ImageLoader configurado no NetworkModule (cache em disco, OkHttp compartilhado).
    @Inject lateinit var imageLoader: Provider<ImageLoader>

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
