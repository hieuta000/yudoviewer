package com.hieuta.yudoviewer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.request.RequestSearchResult
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var videoListRecyclerView: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var videoListAdapter: VideoListAdapter
    private val videos = mutableListOf<SearchResultVideoDetails>()
    private lateinit var toolbar: Toolbar
    private val youtubeDownloader = YoutubeDownloader()


    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        searchEditText = findViewById(R.id.search_edit_text)
        videoListRecyclerView = findViewById(R.id.video_list_recycler_view)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)

        videoListAdapter = VideoListAdapter(videos)
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
            Log.d(TAG, "searchYoutube attempt: $searchQuery")
            val searchResult = youtubeDownloader.search(searchQuery)
            Log.d(TAG, "searchYoutube success: $searchResult")
            return@withContext searchResult.data().videos()
        } catch (e: Exception) {
            Log.e(TAG, "searchYoutube failed", e)
            return@withContext emptyList()
        }
    }
}
