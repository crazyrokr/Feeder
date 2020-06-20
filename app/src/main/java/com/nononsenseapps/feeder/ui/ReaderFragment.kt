package com.nononsenseapps.feeder.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.BidiFormatter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.base.KodeinAwareFragment
import com.nononsenseapps.feeder.db.room.FeedItemWithFeed
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.model.FeedItemViewModel
import com.nononsenseapps.feeder.model.cancelNotification
import com.nononsenseapps.feeder.model.maxImageSize
import com.nononsenseapps.feeder.util.PREF_VAL_OPEN_WITH_WEBVIEW
import com.nononsenseapps.feeder.util.Prefs
import com.nononsenseapps.feeder.util.TabletUtils
import com.nononsenseapps.feeder.util.bundle
import com.nononsenseapps.feeder.util.openLinkInBrowser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

const val ARG_TITLE = "title"
const val ARG_CUSTOMTITLE = "customtitle"
const val ARG_LINK = "link"
const val ARG_ENCLOSURE = "enclosure"
const val ARG_IMAGEURL = "imageUrl"
const val ARG_ID = "dbid"
const val ARG_AUTHOR = "author"
const val ARG_DATE = "date"

@FlowPreview
class ReaderFragment : KodeinAwareFragment() {
    private val dateTimeFormat =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())

    private var _id: Long = ID_UNSET

    // All content contained in RssItem
    private var rssItem: FeedItemWithFeed? = null
    private lateinit var titleTextView: TextView

    private val viewModel: FeedItemViewModel by instance(arg = this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { arguments ->
            _id = arguments.getLong(ARG_ID, ID_UNSET)
        }

        if (_id > ID_UNSET) {
            val itemId = _id
            val appContext = context?.applicationContext
            appContext?.let {
                lifecycleScope.launchWhenResumed {
                    viewModel.markAsReadAndNotified(_id)
                    cancelNotification(it, itemId)
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val theLayout = if (TabletUtils.isTablet(activity)) {
            R.layout.fragment_reader_tablet
        } else {
            R.layout.fragment_reader
        }
        val rootView = inflater.inflate(theLayout, container, false)

        titleTextView = rootView.findViewById(R.id.story_title)
        val bodyTextView = rootView.findViewById<TextView>(R.id.story_body)
        val authorTextView = rootView.findViewById<TextView>(R.id.story_author)
        val feedTitleTextView = rootView.findViewById<TextView>(R.id.story_feedtitle)

        lifecycleScope.launchWhenCreated {
            viewModel.getLiveItem(_id).observe(this@ReaderFragment, androidx.lifecycle.Observer {
                rssItem = it

                rssItem?.let { rssItem ->
                    titleTextView.text = rssItem.plainTitle

                    rssItem.feedId?.let { feedId ->
                        feedTitleTextView.setOnClickListener {
                            findNavController().navigate(R.id.action_readerFragment_to_feedFragment, bundle {
                                putLong(ARG_FEED_ID, feedId)
                            })
                        }
                    }

                    feedTitleTextView.text = rssItem.feedDisplayTitle

                    rssItem.pubDate.let { pubDate ->
                        rssItem.author.let { author ->
                            when {
                                author == null && pubDate != null ->
                                    authorTextView.text = getString(R.string.on_date,
                                            pubDate.format(dateTimeFormat))
                                author != null && pubDate != null ->
                                    authorTextView.text = getString(R.string.by_author_on_date,
                                            // Must wrap author in unicode marks to ensure it formats
                                            // correctly in RTL
                                            unicodeWrap(author),
                                            pubDate.format(dateTimeFormat))
                                else -> authorTextView.visibility = View.GONE
                            }
                        }
                    }

                    // Update state of notification toggle
                    activity?.invalidateOptionsMenu()
                }
            })

            viewModel.getLiveImageText(_id, activity!!.maxImageSize(), urlClickListener()).observe(
                    this@ReaderFragment,
                    androidx.lifecycle.Observer {
                        bodyTextView.text = it
                    }
            )
        }
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rssItem?.storeInBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reader, menu)

        // Set intent
        rssItem?.let { rssItem ->
            // Show/Hide buttons
            menu.findItem(R.id.action_open_enclosure).isVisible = rssItem.enclosureLink != null
            menu.findItem(R.id.action_open_in_webview).isVisible = rssItem.link != null
            menu.findItem(R.id.action_open_in_browser).isVisible = rssItem.link != null
            // Add filename to tooltip
            if (rssItem.enclosureLink != null) {
                val filename = rssItem.enclosureFilename
                if (filename != null) {
                    menu.findItem(R.id.action_open_enclosure).title = filename
                }

            }
        }

        // Don't forget super call here
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_open_in_webview -> {
                // Open in web view
                rssItem?.let { rssItem ->
                    rssItem.link?.let { link ->
                        findNavController().navigate(
                                R.id.action_readerFragment_to_readerWebViewFragment,
                                bundle {
                                    putString(ARG_URL, link)
                                    putString(ARG_ENCLOSURE, rssItem.enclosureLink)
                                }
                        )
                    }
                }
                true
            }
            R.id.action_open_in_browser -> {
                val link = rssItem?.link
                if (link != null) {
                    context?.let { context ->
                        openLinkInBrowser(context, link)
                    }
                }

                true
            }
            R.id.action_open_enclosure -> {
                val link = rssItem?.enclosureLink
                if (link != null) {
                    context?.let { context ->
                        openLinkInBrowser(context, link)
                    }
                }

                true
            }
            R.id.action_mark_as_unread -> {
                lifecycleScope.launch {
                    viewModel.markAsRead(_id, unread = true)
                }
                true
            }
            R.id.action_share -> {
                rssItem?.link?.let { link ->
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, link)

                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                }
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }
}

fun Fragment.unicodeWrap(text: String): String =
        BidiFormatter.getInstance(getLocale()).unicodeWrap(text)

fun Fragment.getLocale(): Locale? =
        context?.getLocale()

fun Context.unicodeWrap(text: String): String =
        BidiFormatter.getInstance(getLocale()).unicodeWrap(text)

fun Context.getLocale(): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

fun Fragment.urlClickListener(): (link: String) -> Unit = { link ->
    context?.let { context ->
        val kodein: Kodein by closestKodein()
        val prefs: Prefs by kodein.instance()

        when (prefs.openLinksWith) {
            PREF_VAL_OPEN_WITH_WEBVIEW -> {
                findNavController().navigate(R.id.action_readerFragment_to_readerWebViewFragment, bundle {
                    putString(ARG_URL, link)
                })
            }
            else -> {
                openLinkInBrowser(context, link)
            }
        }
    }
}
