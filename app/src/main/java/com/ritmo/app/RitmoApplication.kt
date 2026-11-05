package com.ritmo.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.ritmo.app.updates.UpdateManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RitmoApplication : Application() {
    @Inject lateinit var updates: UpdateManager
    override fun onCreate() { super.onCreate(); CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { updates.check(notify = true) } }
}
