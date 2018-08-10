package org.vitrivr.vitrivrapp.features.query.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
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
import org.vitrivr.vitrivrapp.utils.checkAndRequestPermission
import org.vitrivr.vitrivrapp.utils.showToast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

@SuppressLint("ViewConstructor")
/**
 * Tools for constructing an audio query
 */
class AudioQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                wasChecked: Boolean,
                                                toolsContainer: ViewGroup,
                                                context: Context,
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

    private var mediaPlayer: MediaPlayer? = null

    init {
        /**
         * inflate the audio_query_tools layout to this view
         */
        LayoutInflater.from(context).inflate(R.layout.audio_query_tools, toolsContainer, true)

        /**
         * initializing views
         */
        recordAudio = toolsContainer.findViewById(R.id.recordAudio)
        removeAudio = toolsContainer.findViewById(R.id.removeAudio)
        loadAudio = toolsContainer.findViewById(R.id.loadAudio)
        playAudio = toolsContainer.findViewById(R.id.playAudio)
        audioName = toolsContainer.findViewById(R.id.audioName)
        audioDuration = toolsContainer.findViewById(R.id.audioDuration)
        fingerprintHummingBalance = toolsContainer.findViewById(R.id.fingerprintHummingBalance)

        recordAudio.setOnClickListener {
            removeAudio.performClick()
            recordAudio.setColorFilter(Color.RED)

            (context as Activity).checkAndRequestPermission(android.Manifest.permission.RECORD_AUDIO, RECORD_REQUEST_CODE) {
                startRecordingAudio()
            }
        }

        removeAudio.setOnClickListener {
            stopPlayback()

            val recordedAudioFile = getRecordedAudioFile()
            val loadedAudioFile = getLoadedAudioFile()

            if (recordedAudioFile.exists())
                recordedAudioFile.delete()

            if (loadedAudioFile.exists())
                loadedAudioFile.delete()

            recordAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
            loadAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
            audioDuration.text = context.getString(R.string.audio_duration_00)
            audioName.text = context.getString(R.string.no_audio_available)
        }

        loadAudio.setOnClickListener {
            removeAudio.performClick()
            loadAudio.setColorFilter(Color.RED)

            (context as Activity).checkAndRequestPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, LOAD_AUDIO_REQUEST_CODE) {
                startPickingFile()
            }
        }

        playAudio.setOnClickListener {
            when (playAudio.tag) {
                "play" -> {
                    val recordedAudioFile = getRecordedAudioFile()
                    val loadedAudioFile = getLoadedAudioFile()

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

        /**
         * If it was checked before, implies previous state exists, then restore it
         */
        if (wasChecked) {
            restoreState()
        } else {
            /**
             * else add a new AUDIO query term to query container model
             */
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.AUDIO)
        }
    }

    /**
     * start recording audio activity
     */
    fun startRecordingAudio() {
        val filePath = getUnformattedRecordedAudioFile().absolutePath
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

    /**
     * start picking file activity
     */
    fun startPickingFile() {
        FilePickerBuilder.getInstance()
                .setMaxCount(1)
                .setActivityTheme(R.style.LibAppTheme)
                .addFileSupport("Audio", arrayOf("aac", "mp3", "m4a", "wma", "wav", "flac"))
                .pickFile(context as Activity, LOAD_AUDIO_RESULT)
    }

    /**
     * handle recorded audio result. Use FFmpeg to change sampling rate to 22050Hz and the channel to mono
     */
    fun handleRecordedAudioResult() {
        loadAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
        recordAudio.setColorFilter(Color.RED)

        val loadedAudio = getLoadedAudioFile()
        if (loadedAudio.exists()) loadedAudio.delete()

        val unformattedAudioFile = getUnformattedRecordedAudioFile()
        val audioFile = getRecordedAudioFile()

        if (FFmpeg.getInstance(context).isSupported) {
            FFmpeg.getInstance(context).execute(arrayOf("-i", unformattedAudioFile.absolutePath, "-ar", "22050", "-map_channel", "0.0.0", audioFile.absolutePath), object : FFcommandExecuteResponseHandler {

                override fun onStart() {
                    audioName.text = context.getString(R.string.processing_audio)
                }

                override fun onSuccess(message: String?) {
                    if (audioFile.exists()) {
                        audioName.text = context.getString(R.string.audio_recorded)
                        setDurationFromFile(audioFile.absolutePath)
                        unformattedAudioFile.delete()

                        /**
                         * get base64 string from audioFile
                         */
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
                    }
                }

                override fun onFailure(message: String?) {
                    "Failed".showToast(context)
                }

                override fun onFinish() {}
                override fun onProgress(message: String?) {}
            })
        } else {
            "FFmpeg not supported".showToast(context)
            recordAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
        }
    }

    /**
     * handle loaded audio result. Use FFmpeg to change sampling rate to 22050Hz and the channel to mono
     * @param filePath file path of the selected audio file
     */
    fun handleLoadedAudioResult(filePath: String) {
        recordAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
        loadAudio.setColorFilter(Color.RED)

        val recordedAudio = getRecordedAudioFile()
        if (recordedAudio.exists()) recordedAudio.delete()

        val loadedAudioFile = getLoadedAudioFile()

        if (FFmpeg.getInstance(context).isSupported) {
            FFmpeg.getInstance(context).execute(arrayOf("-i", filePath, "-ar", "22050", "-map_channel", "0.0.0", loadedAudioFile.absolutePath), object : FFcommandExecuteResponseHandler {

                override fun onStart() {
                    audioName.text = context.getText(R.string.processing_audio)
                }

                override fun onSuccess(message: String?) {
                    if (loadedAudioFile.exists()) {
                        audioName.text = context.getText(R.string.audio_loaded)
                        setDurationFromFile(loadedAudioFile.absolutePath)

                        /**
                         * get base64 string from loaded audio file
                         */
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
                    }
                }

                override fun onFailure(message: String?) {
                    "Failed".showToast(context)
                }

                override fun onFinish() {}
                override fun onProgress(message: String?) {}
            })
        } else {
            "FFmpeg not supported".showToast(context)
            loadAudio.setColorFilter(ContextCompat.getColor(context, R.color.tileIconSelected))
        }
    }

    /**
     * stop media playback
     */
    fun stopPlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer = null
            playAudio.setImageResource(R.drawable.icon_play_audio)
            playAudio.tag = "play"
        }
    }

    private fun restoreState() {
        // restore state
        fingerprintHummingBalance.progress = queryViewModel.getBalance(queryViewModel.currContainerID, QueryTermType.AUDIO)

        val recordedAudioFile = getRecordedAudioFile()
        val loadedAudioFile = getLoadedAudioFile()

        if (recordedAudioFile.exists()) {
            recordAudio.setColorFilter(Color.RED)
            audioName.text = context.getString(R.string.audio_recorded)
            setDurationFromFile(recordedAudioFile.absolutePath)
        } /*else {
            recordAudio.setColorFilter(ContextCompat.getColor((context as Activity), R.color.tileIconSelected))
        }*/

        if (loadedAudioFile.exists()) {
            loadAudio.setColorFilter(Color.RED)
            audioName.text = context.getString(R.string.audio_loaded)
            setDurationFromFile(loadedAudioFile.absolutePath)
        } /*else {
            loadAudio.setColorFilter(ContextCompat.getColor(context as Activity, R.color.tileIconSelected))
        }*/
    }

    /**
     * given file path of an audio file, set the audio duration to audioDuration TextView
     * @param filePath absolute file path of the audio file
     */
    private fun setDurationFromFile(filePath: String) {
        if (!File(filePath).exists()) return

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(filePath)
        val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        val minutes = (durationStr.toLong() / 1000) / 60
        val seconds = (durationStr.toLong() / 1000) % 60
        val duration = "$minutes:${String.format("%02d", seconds)}"
        audioDuration.text = duration
    }

    private fun getUnformattedRecordedAudioFile(): File {
        return File(context.filesDir, "audioQuery_${queryViewModel.currContainerID}_recorded_audio_unformatted.wav")
    }

    private fun getRecordedAudioFile(): File {
        return File(context.filesDir, "audioQuery_${queryViewModel.currContainerID}_recorded_audio.wav")
    }

    private fun getLoadedAudioFile(): File {
        return File(context.filesDir, "audioQuery_${queryViewModel.currContainerID}_loaded_audio.wav")
    }

}