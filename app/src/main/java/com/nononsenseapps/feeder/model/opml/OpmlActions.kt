package com.nononsenseapps.feeder.model.opml

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.nononsenseapps.feeder.db.room.AppDatabase
import com.nononsenseapps.feeder.db.room.FeedDao
import com.nononsenseapps.feeder.model.requestFeedSync
import com.nononsenseapps.feeder.util.ToastMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlin.system.measureTimeMillis

/**
 * Exports OPML on a background thread
 */
suspend fun exportOpml(kodein: Kodein, uri: Uri) = withContext(Dispatchers.IO) {
    try {
        val time = measureTimeMillis {
            val contentResolver: ContentResolver by kodein.instance()
            val feedDao: FeedDao by kodein.instance()
            contentResolver.openOutputStream(uri)?.let {
                writeOutputStream(
                        it,
                        feedDao.loadTags()
                ) { tag ->
                    feedDao.loadFeeds(tag = tag)
                }
            }
        }
        Log.d("OPML", "Exported OPML in $time ms on ${Thread.currentThread().name}")
    } catch (e: Throwable) {
        e.printStackTrace()
        Log.e("OMPL", "Failed to export OMPL: $e")
        kodein.direct.instance<ToastMaker>().makeToast("Failed to export OMPL")
    }
}

/**
 * Imports OPML on a background thread
 */
@FlowPreview
@ExperimentalCoroutinesApi
suspend fun importOpml(kodein: Kodein, uri: Uri) = withContext(Dispatchers.IO) {
    val db: AppDatabase by kodein.instance()
    try {
        val time = measureTimeMillis {
            val parser = OpmlParser(OPMLToRoom(db))
            val contentResolver: ContentResolver by kodein.instance()
            contentResolver.openInputStream(uri).use {
                it?.let { stream ->
                    parser.parseInputStream(stream)
                }
            }
            requestFeedSync(kodein = kodein, ignoreConnectivitySettings = false)
        }
        Log.d("OPML", "Imported OPML in $time ms on ${Thread.currentThread().name}")
    } catch (e: Throwable) {
        Log.e("OMPL", "Failed to import OMPL: $e")
        kodein.direct.instance<ToastMaker>().makeToast("Failed to import OMPL")
    }
}
