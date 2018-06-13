package org.vitrivr.vitrivrapp.features.resultdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import kotlinx.android.synthetic.main.video_result_detail_activity.*
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.features.results.PathUtils
import org.vitrivr.vitrivrapp.features.results.ViewDetailsAdapter
import org.vitrivr.vitrivrapp.utils.px
import javax.inject.Inject


class VideoResultDetailActivity : AppCompatActivity() {

    @Inject
    lateinit var pathUtils: PathUtils
    val WRITE_REQUEST_CODE = 1

    lateinit var presenterObject: QueryResultPresenterModel
    lateinit var categoryInfo: HashMap<MediaType, HashSet<String>>
    lateinit var featureInfoDialog: AlertDialog
    lateinit var infoFileName: TextView
    lateinit var infoFilePath: TextView
    lateinit var infoObjectId: TextView
    lateinit var infoMediaType: TextView
    lateinit var allSegmentsRV: RecyclerView

    var currentWindow = 0
    var playbackPosition = 0L
    var playWhenReady = true
    var player: SimpleExoPlayer? = null

    val CURRENT_WINDOW = "CURRENT_WINDOW"
    val PLAYBACK_POSITION = "PLAYBACK_POSITION"
    val PLAY_WHEN_READY = "PLAY_WHEN_READY"

    lateinit var uri: Uri
    var isLocal = false

    init {
        App.daggerAppComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.video_result_detail_activity)

        if (savedInstanceState == null) {
            presenterObject = intent.getParcelableExtra(ViewDetailsAdapter.PRESENTER_OBJECT)
            categoryInfo = intent.getSerializableExtra(ViewDetailsAdapter.CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
        } else {
            presenterObject = savedInstanceState.getParcelable(ViewDetailsAdapter.PRESENTER_OBJECT)
            categoryInfo = savedInstanceState.getSerializable(ViewDetailsAdapter.CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
            currentWindow = savedInstanceState.getInt(CURRENT_WINDOW)
            playbackPosition = savedInstanceState.getLong(PLAYBACK_POSITION)
            playWhenReady = savedInstanceState.getBoolean(PLAY_WHEN_READY)
        }

        uri = pathUtils.getObjectURI(presenterObject)!!
        isLocal = pathUtils.isObjectPathLocal()!!

        val view = LayoutInflater.from(this).inflate(R.layout.object_info, null)

        featureInfoDialog = AlertDialog.Builder(this)
                .setTitle("Info")
                .setView(view)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                    initializePlayer(uri, isLocal)
                    player?.playWhenReady = playWhenReady
                }
                .create()

        infoFileName = view.findViewById(R.id.fileName)
        infoFilePath = view.findViewById(R.id.path)
        infoObjectId = view.findViewById(R.id.id)
        infoMediaType = view.findViewById(R.id.mediaType)
        allSegmentsRV = view.findViewById(R.id.allSegmentsRV)

        infoFileName.text = presenterObject.fileName
        infoFilePath.text = presenterObject.filePath
        infoObjectId.text = presenterObject.objectId
        infoMediaType.text = presenterObject.mediaType.name

        allSegmentsRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        allSegmentsRV.addItemDecoration(EqualSpacingItemDecoration(4.px, EqualSpacingItemDecoration.HORIZONTAL))
        allSegmentsRV.adapter = AllSegmentsAdapter(presenterObject.allSegments,
                presenterObject.objectId,
                presenterObject.mediaType,
                categoryInfo) {
            featureInfoDialog.dismiss()
            player?.seekTo((it * 1000).toLong())
            player?.playWhenReady = playWhenReady
            val mediaSource = if (isLocal) buildMediaSourceLocal(uri) else buildMediaSourceHttp(uri)
            player?.prepare(mediaSource, false, false)
        }
    }


    public override fun onResume() {
        super.onResume()
        initializePlayer(uri, isLocal)
    }

    public override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun initializePlayer(uri: Uri, isLocal: Boolean) {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this),
                    DefaultTrackSelector(), DefaultLoadControl())

            playerView.player = player
            player?.playWhenReady = playWhenReady
            player?.seekTo(currentWindow, playbackPosition)
        }

        val mediaSource = if (isLocal) buildMediaSourceLocal(uri) else buildMediaSourceHttp(uri)
        player?.prepare(mediaSource, false, false)
    }

    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            playWhenReady = player!!.playWhenReady
            player!!.stop()
            player!!.release()
            player = null
        }
    }

    private fun buildMediaSourceLocal(uri: Uri): MediaSource {
        val dataSpec = DataSpec(uri)
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: FileDataSource.FileDataSourceException) {
            e.printStackTrace()
        }

        val factory = DataSource.Factory { fileDataSource }
        return ExtractorMediaSource.Factory(factory)
                .createMediaSource(fileDataSource.uri)
    }

    private fun buildMediaSourceHttp(uri: Uri): MediaSource {
        return ExtractorMediaSource.Factory(
                DefaultHttpDataSourceFactory("exoplayer-codelab")).createMediaSource(uri)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.let {
            outState.putParcelable(ViewDetailsAdapter.PRESENTER_OBJECT, presenterObject)
            outState.putSerializable(ViewDetailsAdapter.CATEGORY_INFO, categoryInfo)
            outState.putInt(CURRENT_WINDOW, currentWindow)
            outState.putLong(PLAYBACK_POSITION, playbackPosition)
            outState.putBoolean(PLAY_WHEN_READY, playWhenReady)
        }
    }

    fun info(view: View) {
        player?.stop()
        featureInfoDialog.show()
    }

    fun save(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}