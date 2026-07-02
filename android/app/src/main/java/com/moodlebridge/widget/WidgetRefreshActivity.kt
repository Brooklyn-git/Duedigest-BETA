package com.moodlebridge.widget

import android.app.Activity
import android.os.Bundle
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetRefreshActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.Main).launch {
            TaskWidget().updateAll(this@WidgetRefreshActivity)
            FetchTaskWidget().updateAll(this@WidgetRefreshActivity)
            finish()
        }
    }
}
