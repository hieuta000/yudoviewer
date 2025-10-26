package com.hieuta.yudoviewer

import android.app.Application

/**
 * Custom Application class for the YuDoViewer app.
 * This is the first code to be executed when the app starts.
 */
class YuDoViewerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // You can add app-wide initialization code here in the future.
        // For example: setting up dependency injection, logging, etc.
    }
}