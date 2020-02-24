package korablique.recipecalculator.outside.fcm

import androidx.annotation.WorkerThread
import com.google.firebase.messaging.RemoteMessage
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.InjectorHolder
import javax.inject.Inject

class FCMService : com.google.firebase.messaging.FirebaseMessagingService() {
    @Inject
    lateinit var fcmManager: FCMManager
    @Inject
    lateinit var mainThreadExecutor: MainThreadExecutor

    override fun onCreate() {
        super.onCreate()
        InjectorHolder.getInjector().inject(this)
    }

    @WorkerThread
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        mainThreadExecutor.execute {
            fcmManager.onFCMTokenChanged(token)
        }
    }

    @WorkerThread
    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        mainThreadExecutor.execute {
            fcmManager.onMessageReceived(msg.data)
        }
    }
}