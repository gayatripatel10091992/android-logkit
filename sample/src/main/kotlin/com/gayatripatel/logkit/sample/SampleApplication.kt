package com.gayatripatel.logkit.sample

import android.app.Application
import com.gayatripatel.logkit.LogKit
import com.gayatripatel.logkit.LogLevel
import com.gayatripatel.logkit.LogcatSink
import com.gayatripatel.logkit.RingBufferSink
import com.gayatripatel.logkit.RollingFileSink
import java.io.File

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        fileSink = RollingFileSink(File(filesDir, "logs"))
        LogKit.install {
            minLevel = LogLevel.VERBOSE
            sink(LogcatSink())
            sink(inAppBuffer)
            sink(fileSink)
        }
        LogKit.logger("App").i { "LogKit installed with ${LogKit.minLevel} level" }
    }

    companion object {
        /** In-app log viewer source. */
        val inAppBuffer = RingBufferSink(capacity = 200)

        lateinit var fileSink: RollingFileSink
            private set
    }
}
