package org.ole.planet.myplanet.service

import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import org.ole.planet.myplanet.MainApplication.Companion.context
import java.io.File
import java.util.UUID

class AudioRecorderService {
    private var outputFile: String? = null
    private var myAudioRecorder: MediaRecorder? = null
    private var audioRecordListener: AudioRecordListener? = null

    fun forceStop() {
        myAudioRecorder?.apply {
            stop()
            release()
        }
        myAudioRecorder = null
        audioRecordListener?.onError("Recording stopped")
    }

    fun setAudioRecordListener(audioRecordListener: AudioRecordListener): AudioRecorderService {
        this.audioRecordListener = audioRecordListener
        return this
    }

    fun startRecording() {
        outputFile = createAudioFile()
        myAudioRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            try {
                prepare()
                start()
                audioRecordListener?.onRecordStarted()
            } catch (e: Exception) {
                myAudioRecorder = null
                audioRecordListener?.onError(e.message)
            }
        }
    }

    private fun createAudioFile(): String? {
        var audioFileName: String
        var audioFile: File
        var attempt = 0
        do {
            audioFileName = UUID.randomUUID().toString() + ".aac"
            audioFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                audioFileName
            )
            attempt++
        } while (audioFile.exists() && attempt < 100)
        if (attempt >= 100) {
            return null
        }
        return audioFile.absolutePath
    }

    fun isRecording(): Boolean {
        return myAudioRecorder != null
    }

    fun stopRecording() {
        if (myAudioRecorder != null) {
            myAudioRecorder?.stop()
            myAudioRecorder?.release()
            myAudioRecorder = null
            audioRecordListener?.onRecordStopped(outputFile)
        }
    }

    interface AudioRecordListener {
        fun onRecordStarted()
        fun onRecordStopped(outputFile: String?)
        fun onError(error: String?)
    }
}