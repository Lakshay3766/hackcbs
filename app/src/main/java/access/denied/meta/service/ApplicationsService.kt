package access.denied.meta.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import access.denied.meta.R
import access.denied.meta.models.InstalledApplication
import access.denied.meta.repository.ApplicationRepository
import access.denied.meta.utillies.ApplicationPermissionHelper
import access.denied.meta.utillies.Prefs
import access.denied.meta.utillies.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class ApplicationsService : Service() {
    private var receiver: BroadcastReceiver? = null
    private var TAG: String = this.javaClass.name
    private var NOTIFICATION_CHANNEL_ID = "PObY-A install"
    private var NOTIFICATION_CHANNEL_ID_service = "PObY-A service"

    private lateinit var notificationManager: NotificationManager

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationBuilderService: NotificationCompat.Builder


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Prefs.getInstance(baseContext)?.monitoringServiceStatus == false){
            stopSelf()
            stopForeground(true)
        }

        // Notification ID start with 2 (ID 1 for the foreground service's notifications)
        var notificationId = 2

        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        notificationBuilderService =
            NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID_service)

        val notificationBuilder = NotificationCompat.Builder(
            applicationContext, NOTIFICATION_CHANNEL_ID
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID_service,
                "PObY-A", NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Monitoring", NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val appRepository = ApplicationRepository.getInstance(application)

        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.pobya_shield_transparent)
            .setContentTitle(getString(R.string.monitoring_service_desc))
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(1, notification)

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        intentFilter.priority = 999

        receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "PObY-A",
                    NotificationManager.IMPORTANCE_HIGH
                )

                notificationChannel.description = "Applications actions"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                notificationChannel.enableVibration(true)
                notificationManager.createNotificationChannel(notificationChannel)

                var notificationBuilderAction =
                    NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

                val action = intent.action

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                when (action) {
                    // TODO: manage application update.
                    //      The current state when an update is performed it's considered as
                    //      uninstall and install (two notifications are shown)
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val app = intent.data?.schemeSpecificPart?.let {
                            ApplicationPermissionHelper(
                                applicationContext,
                                true
                            ).getAppByPackageName(it)
                        }
                        Utilities.populateScope.launch {
                            withContext(Dispatchers.IO) {
                                app?.let { appRepository.insert(it) }
                            }
                        }

                        notificationBuilderAction.setDefaults(Notification.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.drawable.pobya_shield_transparent)
                            .setTicker("PObY-A")
                            .setAutoCancel(true)
                            .setContentTitle(getString(R.string.monitoring_app_added))
                            .setContentText("" + app?.name)
                            .setContentInfo("Info")
                        notificationManager.notify(
                            notificationId,
                            notificationBuilderAction.build()
                        )
                        notificationId += 1
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {

                        var app: InstalledApplication

                        Utilities.dbScope.launch {
                            val applicationRepository =
                                ApplicationRepository.getInstance(application)
                            val currentDate = Calendar.getInstance().time
                            app = applicationRepository.getAppByPackageName(
                                intent.data?.schemeSpecificPart.toString()
                            )!!
                            app.uninstalled = true
                            app.uninstallDate = currentDate.time

                            applicationRepository.update(app)

                            withContext(Dispatchers.Main) {
                                notificationBuilderAction.setDefaults(Notification.DEFAULT_ALL)
                                    .setWhen(System.currentTimeMillis())
                                    .setSmallIcon(R.drawable.pobya_shield_transparent)
                                    .setAutoCancel(true)
                                    .setTicker("PObY-A")
                                    .setContentTitle(getString(R.string.monitoring_app_removed))
                                    .setContentText(app.name)
                                    .setContentInfo("Info")

                                notificationManager.notify(
                                    notificationId,
                                    notificationBuilderAction.build()
                                )
                                notificationId += 1
                            }
                        }
                    }
                }
            }
        }

        try {
            registerReceiver(receiver, intentFilter)
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
        }

        return START_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
            return super.onTaskRemoved(rootIntent)
        }

        if (Prefs.getInstance(applicationContext)?.monitoringServiceStatus == true) {
            val restartServiceIntent =
                Intent(applicationContext, ApplicationsService::class.java).also {
                    it.setPackage(packageName)
                }

            startForegroundService(restartServiceIntent)
        } else {
            stopForeground(true)
            stopSelf()
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.stackTrace.toString())
        }

        stopForeground(true)
        stopSelf()
    }


}