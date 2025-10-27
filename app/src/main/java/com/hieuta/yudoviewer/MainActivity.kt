package com.hieuta.yudoviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestSearchResult
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var videoListRecyclerView: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var videoListAdapter: VideoListAdapter
    private val videos = mutableListOf<SearchResultVideoDetails>()
    private lateinit var toolbar: Toolbar
    private val youtubeDownloader = YoutubeDownloader()
    private val pendingDownloads = mutableMapOf<Int, Pair<SearchResultVideoDetails, ProgressBar>>()

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_WRITE_STORAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        searchEditText = findViewById(R.id.search_edit_text)
        videoListRecyclerView = findViewById(R.id.video_list_recycler_view)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)

        videoListAdapter = VideoListAdapter(videos) { video, progressBar ->
            downloadVideo(video, progressBar)
        }
        videoListRecyclerView.layoutManager = LinearLayoutManager(this)
        videoListRecyclerView.adapter = videoListAdapter

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
        searchEditText.setText("warrior news")
    }

    private fun search(query: String) {
        lifecycleScope.launch {
            loadingProgressBar.visibility = View.VISIBLE
            videos.clear()
            videoListAdapter.notifyDataSetChanged()
            try {
                val searchResults = searchYoutube(query)
                if (searchResults.isNotEmpty()) {
                    videos.addAll(searchResults)
                    videoListAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@MainActivity, "No results found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                Toast.makeText(this@MainActivity, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loadingProgressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun searchYoutube(query: String): List<SearchResultVideoDetails> = withContext(Dispatchers.IO) {
        Log.d(TAG, "searchYoutube called with query: $query")
        try {
            val searchQuery = RequestSearchResult(query)
            val searchResult = youtubeDownloader.search(searchQuery)
            return@withContext searchResult.data().videos()
        } catch (e: Exception) {
            Log.e(TAG, "searchYoutube failed", e)
            return@withContext emptyList()
        }
    }

    private fun downloadVideo(video: SearchResultVideoDetails, progressBar: ProgressBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloads[REQUEST_WRITE_STORAGE] = video to progressBar
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
            return
        }

        startDownload(video, progressBar)
    }

    private fun startDownload(video: SearchResultVideoDetails, progressBar: ProgressBar) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
            try {
                Log.d(TAG, "Downloading video: Title: ${video.title()} ID: (${video.videoId()})")
                val response = withContext(Dispatchers.IO) {
                    youtubeDownloader.getVideoInfo(RequestVideoInfo(video.videoId()))
                }
                val videoInfo = response.data()
                if (videoInfo == null) {
                    progressBar.visibility = View.GONE
                    if (response.error()?.message?.contains("LOGIN_REQUIRED") == true) {
                        Toast.makeText(this@MainActivity, "This video is protected and cannot be downloaded.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to get video info: ${response.error()?.message ?: "Unknown error"}")
                        Toast.makeText(this@MainActivity, "Failed to get video info", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val format = videoInfo.bestVideoWithAudioFormat()
                if (format == null) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "No video with audio format found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Log.d(TAG, "Download URL: ${format.url()}")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val request = RequestVideoFileDownload(format)
                    .saveTo(downloadsDir)
                    .callback(object : YoutubeProgressCallback<File> {
                        override fun onDownloading(progress: Int) {
                            progressBar.progress = progress
                        }

                        override fun onFinished(data: File) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Download finished: ${data.name}", Toast.LENGTH_SHORT)
                                .show()
                        }

                        override fun onError(throwable: Throwable) {
                            progressBar.visibility = View.GONE
                            Log.e(TAG, "Download failed", throwable)
                            Toast.makeText(
                                this@MainActivity,
                                "Download failed: ${throwable.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                withContext(Dispatchers.IO) {
                    youtubeDownloader.downloadVideoFile(request)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Download failed", e)
                Toast.makeText(this@MainActivity, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingDownloads[REQUEST_WRITE_STORAGE]?.let { (video, progressBar) ->
                    startDownload(video, progressBar)
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
            pendingDownloads.remove(REQUEST_WRITE_STORAGE)
        }
    }
}
