/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ui.setup

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.osfans.trime.R
import com.osfans.trime.data.sync.RimeDataSync
import com.osfans.trime.databinding.ActivitySetupBinding
import com.osfans.trime.ui.main.MainActivity
import com.osfans.trime.ui.setup.SetupPage.Companion.firstUndonePage
import com.osfans.trime.ui.setup.SetupPage.Companion.isLastPage
import com.osfans.trime.util.appContext
import com.osfans.trime.util.createNotificationChannel
import com.osfans.trime.util.startActivity
import com.osfans.trime.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.systemservices.notificationManager

class SetupActivity : FragmentActivity() {
    private lateinit var viewPager: ViewPager2
    private var originalSource: Uri? = null
    private var importProgress: AlertDialog? = null

    private val originalSourcePicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            if (uri.authority != "com.osfans.trime.provider") {
                toast(R.string.original_source_required)
            } else {
                originalSource = uri
                toast(R.string.original_destination_required)
                originalDestinationPicker.launch(
                    android.provider.DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:rime",
                    ),
                )
            }
        }
    }

    private val originalDestinationPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val source = originalSource
        if (uri != null && source != null) {
            if (uri.authority != "com.android.externalstorage.documents" ||
                android.provider.DocumentsContract.getTreeDocumentId(uri) != "primary:rime"
            ) {
                toast(R.string.original_destination_required)
            } else {
                importOriginal(source, uri)
            }
        }
    }

    fun launchOriginalSync() {
        toast(R.string.original_source_required)
        originalSourcePicker.launch(android.provider.DocumentsContract.buildRootUri("com.osfans.trime.provider", "files"))
    }

    private fun importOriginal(source: Uri, destination: Uri) {
        if (!originalImportRunning.compareAndSet(false, true)) return
        val ctx = applicationContext
        com.osfans.trime.TrimeApplication.getInstance().coroutineScope.launch {
            val directory = java.io.File(ctx.cacheDir, "original-${java.util.UUID.randomUUID()}")
            val snapshot = java.io.File(ctx.cacheDir, "external-${java.util.UUID.randomUUID()}")
            val sessionName = "original-import-${java.util.UUID.randomUUID()}"
            var sessionCreated = false
            val mode = com.osfans.trime.data.prefs.AppPrefs.defaultInstance().profile.dataStorageMode
            val previousMode = mode.getValue()
            var completed = false
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    destination,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                withContext(Dispatchers.IO) {
                    com.osfans.trime.data.sync.ConfigurationTransfer.readOriginal(ctx, source, directory)
                    com.osfans.trime.data.sync.ConfigurationTransfer.mergeDestination(ctx, destination, directory)
                    com.osfans.trime.data.sync.ImportedThemeTuning.apply(ctx, directory)
                }
                val external = com.osfans.trime.data.sync.ExternalConfigurationInstall(
                    ctx,
                    destination,
                    directory,
                    snapshot,
                )
                val install = com.osfans.trime.data.sync.ConfigurationInstall(
                    directory,
                    com.osfans.trime.data.base.DataManager.userDataDir,
                    java.io.File(ctx.getExternalFilesDir(null), "backups/original-${System.currentTimeMillis()}.bak"),
                )
                // Bootstrap from app storage before committing the first external tree.
                mode.setValue(com.osfans.trime.data.sync.DataStorageMode.APP_STORAGE)
                val session = com.osfans.trime.daemon.RimeDaemon.createSession(sessionName)
                sessionCreated = true
                run {
                    session.runOnReady {
                        replaceConfiguration(
                            { install.install() },
                            {
                                try {
                                    external.rollback()
                                } finally {
                                    install.rollback()
                                }
                            },
                            {
                                external.prepare()
                                external.install()
                            },
                        )
                    }
                }
                RimeDataSync.persistTreeUri(ctx, destination)
                com.osfans.trime.data.prefs.AppPrefs.defaultInstance().profile.dataStorageMode
                    .setValue(com.osfans.trime.data.sync.DataStorageMode.EXTERNAL_SYNC)
                completed = true
                toast(R.string.done)
                if (!isDestroyed) {
                    refreshCurrentFragment()
                    updateButtons()
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Original configuration import failed")
                android.widget.Toast.makeText(ctx, getString(R.string.configuration_failed, e.message), android.widget.Toast.LENGTH_LONG).show()
            } finally {
                if (!completed) mode.setValue(previousMode)
                if (sessionCreated) com.osfans.trime.daemon.RimeDaemon.destroySession(sessionName)
                withContext(Dispatchers.IO) {
                    directory.deleteRecursively()
                    snapshot.deleteRecursively()
                }
                originalImportRunning.value = false
            }
        }
    }

    private lateinit var skipButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private val dataPathPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        RimeDataSync.persistTreeUri(this@SetupActivity, uri)
                        val name = "setup-import-${java.util.UUID.randomUUID()}"
                        val session = com.osfans.trime.daemon.RimeDaemon.createSession(name)
                        try {
                            session.runOnReady { deploy() }
                        } finally {
                            com.osfans.trime.daemon.RimeDaemon.destroySession(name)
                        }
                    }
                    refreshCurrentFragment()
                    toast(R.string.setup__data_path_imported)
                    skipButton.visibility = View.VISIBLE
                }.onFailure {
                    withContext(Dispatchers.IO) {
                        RimeDataSync.clearExternalTree(this@SetupActivity)
                    }
                    refreshCurrentFragment()
                    toast(R.string.setup__data_path_import_failed)
                }
            }
        }

    fun launchDataPathPicker() {
        dataPathPicker.launch(null as Uri?)
    }

    fun refreshCurrentFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
        (fragment as? SetupFragment)?.sync()
    }

    private fun completeSetup() {
        startActivity<MainActivity>()
        finish()
    }

    companion object {
        private val originalImportRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
        private var shown = false
        private const val CHANNEL_ID = "setup"
        private const val NOTIFY_ID = 87463

        fun shouldShowUp() = !shown && SetupPage.hasUndonePage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalSource = savedInstanceState?.getString("original_source")?.let(Uri::parse)
        enableEdgeToEdge()
        val binding = ActivitySetupBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val sysBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(
                sysBars.left,
                sysBars.top,
                sysBars.right,
                sysBars.bottom,
            )
            windowInsets
        }
        setContentView(binding.root)
        skipButton = binding.skipButton.apply {
            text = getString(R.string.setup__skip)
            setOnClickListener {
                AlertDialog
                    .Builder(this@SetupActivity)
                    .setMessage(R.string.setup__skip_hint)
                    .setPositiveButton(R.string.setup__skip_hint_yes) { _, _ ->
                        completeSetup()
                    }.setNegativeButton(R.string.setup__skip_hint_no, null)
                    .show()
            }
        }
        prevButton =
            binding.prevButton.apply {
                text = getString(R.string.setup__prev)
                setOnClickListener { viewPager.currentItem -= 1 }
            }
        nextButton =
            binding.nextButton.apply {
                setOnClickListener {
                    if (viewPager.currentItem != SetupPage.entries.size - 1) {
                        viewPager.currentItem += 1
                    } else {
                        completeSetup()
                    }
                }
            }
        viewPager = binding.viewpager
        viewPager.adapter = Adapter()
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateButtons()
            },
        )
        // Skip to undone page
        firstUndonePage()?.let { viewPager.currentItem = it.ordinal }
        updateButtons()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                originalImportRunning.collect { running ->
                    if (running && importProgress == null) {
                        importProgress = AlertDialog.Builder(this@SetupActivity)
                            .setMessage(R.string.configuration_working).setCancelable(false).show()
                    } else if (!running) {
                        importProgress?.dismiss()
                        importProgress = null
                        refreshCurrentFragment()
                        updateButtons()
                    }
                }
            }
        }
        shown = true
        createNotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.setup_channel),
        )
    }

    fun updateButtons() {
        val allDone = !SetupPage.hasUndonePage()
        val modeSetupDone = SetupPage.Mode.isDone()
        val isFirstPage = viewPager.currentItem == 0
        val isLastPage = viewPager.currentItem.isLastPage()

        viewPager.isUserInputEnabled = modeSetupDone

        prevButton.isGone = isFirstPage
        skipButton.isGone = !modeSetupDone || allDone
        nextButton.text = getString(if (isLastPage) R.string.done else R.string.setup__next)
        nextButton.isGone = isLastPage && !allDone
        nextButton.isEnabled = modeSetupDone
    }

    override fun onDestroy() {
        importProgress?.dismiss()
        importProgress = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("original_source", originalSource?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        for (fragment in supportFragmentManager.fragments) {
            if (fragment.isVisible) (fragment as? SetupFragment)?.sync()
        }
        updateButtons()
    }

    override fun onPause() {
        if (SetupPage.hasUndonePage()) {
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_trime_status)
                .setContentTitle(getText(R.string.trime_app_name))
                .setContentText(getText(R.string.setup__notify_hint))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, javaClass),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setAutoCancel(true)
                .build()
                .let { notificationManager.notify(NOTIFY_ID, it) }
        }
        super.onPause()
    }

    override fun onResume() {
        notificationManager.cancel(NOTIFY_ID)
        super.onResume()
    }

    private inner class Adapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = SetupPage.entries.size

        override fun createFragment(position: Int): Fragment = SetupFragment().apply {
            arguments = bundleOf("page" to SetupPage.entries[position])
        }
    }
}
