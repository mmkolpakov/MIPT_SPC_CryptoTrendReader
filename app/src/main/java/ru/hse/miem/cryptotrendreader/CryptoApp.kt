package ru.hse.miem.cryptotrendreader

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ru.hse.miem.cryptotrendreader.di.appModule
import timber.log.Timber

class CryptoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Timber.d("CryptoApp Timber planted.")

        startKoin {
            androidLogger()
            androidContext(this@CryptoApp)
            modules(appModule)
        }
        Timber.d("CryptoApp Koin started.")
    }
}