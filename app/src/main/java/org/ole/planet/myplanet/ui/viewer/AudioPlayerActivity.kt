package org.ole.planet.myplanet.ui.viewer

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.jean.jcplayer.JcPlayerManagerListener
import com.example.jean.jcplayer.general.JcStatus
import com.example.jean.jcplayer.model.JcAudio
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.ActivityAudioPlayerBinding
import org.ole.planet.myplanet.utilities.Utilities
import java.io.File
import java.util.regex.Pattern

class AudioPlayerActivity : AppCompatActivity(), JcPlayerManagerListener {
    private var activityAudioPlayerBinding: ActivityAudioPlayerBinding? = null
    private lateinit var jcAudios: ArrayList<JcAudio>
    private var isFullPath = false
    private var filePath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAudioPlayerBinding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(activityAudioPlayerBinding!!.root)
        filePath = intent.getStringExtra("TOUCHED_FILE")
        jcAudios = ArrayList()
        isFullPath = intent.getBooleanExtra("isFullPath", false)
        if (filePath!!.matches(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}//.*".toRegex())) {
            playRecordedAudio()
        } else {
            playDownloadedAudio()
        }
    }

    private fun playDownloadedAudio() {
        val fullPath: String? = if (isFullPath) {
            filePath
        } else {
            val basePath = getExternalFilesDir(null)
            File(basePath, "ole/$filePath").absolutePath
        }
        jcAudios.add(JcAudio.createFromFilePath(fullPath!!))
        initializeJCPlayer()
    }

    private fun playRecordedAudio() {
        val uuidPattern = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/")
        val matcher = filePath?.let { uuidPattern.matcher(it) }
        if (matcher != null) {
            if (matcher.find()) {
                filePath = filePath!!.substring(matcher.group().length)
            }
        }
        jcAudios.add(JcAudio.createFromFilePath(filePath!!))
        initializeJCPlayer()
    }

    private fun initializeJCPlayer() {
        activityAudioPlayerBinding!!.jcplayer.initPlaylist(jcAudios, null)
        val rootView = activityAudioPlayerBinding!!.jcplayer.rootView
        rootView.findViewById<View>(R.id.btnNext).visibility = View.GONE
        rootView.findViewById<View>(R.id.btnPrev).visibility = View.GONE
        rootView.findViewById<View>(R.id.btnRepeatOne).visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (activityAudioPlayerBinding!!.jcplayer.isPlaying) {
            activityAudioPlayerBinding!!.jcplayer.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        activityAudioPlayerBinding!!.jcplayer.kill()
    }

    override fun onResume() {
        super.onResume()
        if (jcAudios.size > 0) {
            activityAudioPlayerBinding!!.jcplayer.playAudio(jcAudios[0])
        }
    }

    override fun onCompletedAudio() {}
    override fun onContinueAudio(status: JcStatus) {}
    override fun onJcpError(throwable: Throwable) {
        Utilities.toast(this, getString(R.string.unable_to_play_audio))
    }

    override fun onPaused(status: JcStatus) {}
    override fun onPlaying(status: JcStatus) {}
    override fun onPreparedAudio(status: JcStatus) {}
    override fun onStopped(status: JcStatus) {}
    override fun onTimeChanged(status: JcStatus) {}
}
