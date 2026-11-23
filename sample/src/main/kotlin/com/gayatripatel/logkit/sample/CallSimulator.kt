package com.gayatripatel.logkit.sample

import com.gayatripatel.logkit.LogKit
import java.io.IOException

/** Emits the kind of logs a VoIP calling app produces, to show levels, redaction and error traces. */
object CallSimulator {
    private val sip = LogKit.logger("SIP")
    private val call = LogKit.logger("Call")
    private val audio = LogKit.logger("Audio")

    fun simulateCall() {
        sip.d { "REGISTER sip:alice@voip.example.com -> 200 OK (expires 3600)" }
        call.i { "Dialing +1 (415) 555-0134 from contact sheet" }
        audio.v { "Codec negotiated: OPUS 48 kHz, ptime 20 ms" }
        audio.i { "Audio route changed: EARPIECE -> SPEAKER" }
        call.i { "Call connected in 820 ms" }
        audio.w { "Jitter 48 ms above 30 ms threshold, enabling FEC" }
        call.e(IOException("Socket closed by peer")) { "Call dropped after 01:42, reconnecting" }
        call.i { "Reconnected, sending summary to support@example.com" }
    }
}
