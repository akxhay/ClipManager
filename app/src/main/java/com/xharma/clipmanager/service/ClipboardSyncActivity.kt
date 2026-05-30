package com.xharma.clipmanager.service

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.xharma.clipmanager.data.ClipDatabase
import com.xharma.clipmanager.data.ClipEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardSyncActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty()) {
                val db = ClipDatabase.getDatabase(this)
                CoroutineScope(Dispatchers.IO).launch {
                    db.clipDao().insert(ClipEntry(text = text))
                    // Trigger service update for notification/file
                    ClipboardMonitorService.refresh(this@ClipboardSyncActivity)
                    finish()
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
}
