package org.vitrivr.vitrivrapp.features.query.tools

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder
import cafe.adriel.androidaudiorecorder.model.AudioChannel
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate
import cafe.adriel.androidaudiorecorder.model.AudioSource
import droidninja.filepicker.FilePickerBuilder
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class AudioQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                wasChecked: Boolean,
                                                toolsContainer: ViewGroup,
                                                context: Context,
                                                val openTerm: () -> Unit,
                                                attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0,
                                                defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val recordAudio: ImageView
    val removeAudio: ImageView
    val loadAudio: ImageView
    val playAudio: ImageView
    val audioName: TextView
    val audioDuration: TextView
    val fingerprintHummingBalance: SeekBar

    var mediaPlayer: MediaPlayer? = null

    init {
        // inflate the audio_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.audio_query_tools, toolsContainer, true)

        recordAudio = toolsContainer.findViewById(R.id.recordAudio)
        removeAudio = toolsContainer.findViewById(R.id.removeAudio)
        loadAudio = toolsContainer.findViewById(R.id.loadAudio)
        playAudio = toolsContainer.findViewById(R.id.playAudio)
        audioName = toolsContainer.findViewById(R.id.audioName)
        audioDuration = toolsContainer.findViewById(R.id.audioDuration)
        fingerprintHummingBalance = toolsContainer.findViewById(R.id.fingerprintHummingBalance)

        recordAudio.setOnClickListener {
            recordAudio.setColorFilter(Color.RED)

            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_REQUEST_CODE)
            } else {
                startRecordingAudio()
            }
        }

        removeAudio.setOnClickListener {
            val recordedAudioFile = File(context.filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
            val loadedAudioFile = File((context as Activity).filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")

            if (recordedAudioFile.exists())
                recordedAudioFile.delete()

            if (loadedAudioFile.exists())
                loadedAudioFile.delete()

            recordAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
            loadAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
            audioDuration.text = "0:00"
            audioName.text = "No Audio Available"
        }

        loadAudio.setOnClickListener {
            loadAudio.setColorFilter(Color.RED)

            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), LOAD_AUDIO_REQUEST_CODE)
            } else {
                startPickingFile()
            }
        }

        playAudio.setOnClickListener {
            when (playAudio.tag) {
                "play" -> {
                    val recordedAudioFile = File(context.filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
                    val loadedAudioFile = File((context as Activity).filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")

                    val audioFile = when {
                        recordedAudioFile.exists() -> recordedAudioFile
                        loadedAudioFile.exists() -> loadedAudioFile
                        else -> null
                    }

                    if (audioFile != null) {
                        if (mediaPlayer == null)
                            mediaPlayer = MediaPlayer.create(context, Uri.fromFile(audioFile))
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener {
                            playAudio.setImageResource(R.drawable.icon_play_audio)
                            playAudio.tag = "play"
                            mediaPlayer = null
                        }
                        playAudio.setImageResource(R.drawable.icon_pause_audio)
                        playAudio.tag = "pause"
                    }
                }
                "pause" -> {
                    mediaPlayer?.pause()
                    playAudio.setImageResource(R.drawable.icon_play_audio)
                    playAudio.tag = "play"
                }
            }
        }

        fingerprintHummingBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(queryViewModel.currContainerID, QueryTermType.AUDIO, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.AUDIO)
        }
    }

    fun startRecordingAudio() {
        val filePath = File((context as Activity).filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}_unformatted.wav").absolutePath
        val color = ContextCompat.getColor(context, R.color.colorPrimaryDark)

        AndroidAudioRecorder.with(context as Activity)
                .setFilePath(filePath)
                .setColor(color)
                .setRequestCode(RECORD_AUDIO_RESULT)
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                .setAutoStart(true)
                .setKeepDisplayOn(true)
                .record()
    }

    fun startPickingFile() {
        FilePickerBuilder.getInstance()
                .setMaxCount(1)
                .setActivityTheme(R.style.LibAppTheme)
                .addFileSupport("Audio", arrayOf("aac", "mp3", "m4a", "wma", "wav", "flac"))
                .pickFile(context as Activity, LOAD_AUDIO_RESULT)
    }

    private fun restoreState() {
        // restore state
        fingerprintHummingBalance.progress = queryViewModel.getBalance(queryViewModel.currContainerID, QueryTermType.AUDIO)

        val recordedAudioFile = File((context as Activity).filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
        val loadedAudioFile = File((context as Activity).filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")

        if (recordedAudioFile.exists()) {
            recordAudio.setColorFilter(Color.RED)
            audioName.text = "Audio Recorded"
            setDurationFromFile(recordedAudioFile.absolutePath)
        } else {
            recordAudio.setColorFilter(ContextCompat.getColor((context as Activity), R.color.tileIconSelected))
        }

        if (loadedAudioFile.exists()) {
            loadAudio.setColorFilter(Color.RED)
            audioName.text = "Audio Loaded"
            setDurationFromFile(loadedAudioFile.absolutePath)
        } else {
            loadAudio.setColorFilter(ContextCompat.getColor(context as Activity, R.color.tileIconSelected))
        }
    }

    private fun setDurationFromFile(filePath: String) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(filePath)
        val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        val minutes = (durationStr.toLong() / 1000) / 60
        val seconds = (durationStr.toLong() / 1000) % 60
        val duration = "$minutes:${String.format("%02d", seconds)}"
        audioDuration.text = duration
    }

    fun handleRecordedAudioResult() {
        loadAudio.setColorFilter(ContextCompat.getColor(context as Activity, R.color.tileIconSelected))
        val loadedAudio = File((context as Activity).filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")
        if (loadedAudio.exists()) loadedAudio.delete()

        val unformattedAudioFile = File((context as Activity).filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}_unformatted.wav")
        val audioFile = File((context as Activity).filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")

        if (FFmpeg.getInstance(context).isSupported) {
            FFmpeg.getInstance(context).execute(arrayOf("-i", unformattedAudioFile.absolutePath, "-ar", "22050", "-map_channel", "0.0.0", audioFile.absolutePath), object : FFcommandExecuteResponseHandler {

                override fun onFinish() {

                }

                override fun onSuccess(message: String?) {
                    unformattedAudioFile.delete()

                    val fileStream = FileInputStream(audioFile)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val byteArray = ByteArray(1024)
                    var x = fileStream.read(byteArray)
                    while (x != -1) {
                        byteArrayOutputStream.write(byteArray, 0, x)
                        x = fileStream.read(byteArray)
                    }
                    val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
                    queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.AUDIO, base64String)
                    openTerm()
                }

                override fun onFailure(message: String?) {

                }

                override fun onProgress(message: String?) {

                }

                override fun onStart() {

                }
            })
        } else {
            Toast.makeText(context, "FFmpeg not supported", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleLoadedAudioResult(filePath: String) {
        val loadedAudioFile = File((context as Activity).filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")

        if (FFmpeg.getInstance(context).isSupported) {
            FFmpeg.getInstance(context).execute(arrayOf("-i", filePath, "-ar", "22050", "-map_channel", "0.0.0", loadedAudioFile.absolutePath), object : FFcommandExecuteResponseHandler {
                override fun onFinish() {

                }

                override fun onSuccess(message: String?) {
                    recordAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
                    val recordedAudio = File((context as Activity).filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
                    if (recordedAudio.exists()) recordedAudio.delete()

                    val fileStream = FileInputStream(loadedAudioFile)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val byteArray = ByteArray(1024)
                    var x = fileStream.read(byteArray)
                    while (x != -1) {
                        byteArrayOutputStream.write(byteArray, 0, x)
                        x = fileStream.read(byteArray)
                    }
                    val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

                    queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.AUDIO, base64String)
                    openTerm()
                }

                override fun onFailure(message: String?) {

                }

                override fun onProgress(message: String?) {

                }

                override fun onStart() {

                }
            })
        } else {
            Toast.makeText(context, "FFmpeg not supported", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopPlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer = null
            playAudio.setImageResource(R.drawable.icon_play_audio)
            playAudio.tag = "play"
        }
    }

}