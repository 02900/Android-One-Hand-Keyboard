package com.onehandkeyboard.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.input.InputCommitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemeSearchBarTest {
    private val searches = mutableListOf<String>()

    private val bar =
        MemeSearchBar(ApplicationProvider.getApplicationContext<Context>()).apply {
            onSearch = { searches += it }
        }

    private val committer = InputCommitter()
    private val ic = bar.connection

    @Test
    fun `the keys write the query, delete and search with Enter`() {
        committer.commitText(ic, "enel")
        committer.space(ic, now = 0L, allowPeriodShortcut = InputCommitter.allowsPeriodShortcut(bar.editorInfo))
        committer.commitText(ic, "facx")
        committer.backspace(ic)
        committer.commitText(ic, "e 😂")
        committer.backspace(ic) // the emoji goes whole

        assertEquals("enel face ", bar.text)

        committer.enter(ic, bar.editorInfo)

        assertEquals(listOf("enel face"), searches) // trimmed
    }

    @Test
    fun `delete-all and the arrows work, and an empty query does not search`() {
        bar.start("chill guy")
        committer.moveCursor(ic, -4)
        committer.commitText(ic, "x")
        assertEquals("chillx guy", bar.text)

        committer.deleteAll(ic)
        committer.submit(ic, bar.editorInfo)

        assertEquals("", bar.text)
        assertTrue(searches.isEmpty())
    }

    @Test
    fun `the double-space period is off in the search box`() {
        committer.commitText(ic, "a")
        committer.space(ic, now = 0L, allowPeriodShortcut = InputCommitter.allowsPeriodShortcut(bar.editorInfo))
        committer.space(ic, now = 100L, allowPeriodShortcut = InputCommitter.allowsPeriodShortcut(bar.editorInfo))

        assertEquals("a  ", bar.text)
    }
}
