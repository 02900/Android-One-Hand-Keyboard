package com.onehandkeyboard.spell

import android.content.Context
import android.util.Log
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and keeps one [Suggester] per language.
 *
 * Dictionaries (~50-90k words) are parsed on a background thread the first time a language is
 * needed; until then [suggester] returns null and the keyboard simply does not correct.
 * Shared by the keyboard service and the settings screen (for "forget learned words").
 */
@Singleton
class SpellEngines
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val configurationFactory: KeyboardConfigurationFactory,
    ) {
        /** Background thread for dictionary loading, suggestion searches and file writes. */
        val executor: ExecutorService =
            Executors.newSingleThreadExecutor {
                Thread(
                    it,
                    "spell",
                ).apply { isDaemon = true }
            }

        private val suggesters = ConcurrentHashMap<Language, Suggester>()
        private val userDictionaries = ConcurrentHashMap<Language, UserDictionary>()
        private val loading = ConcurrentHashMap.newKeySet<Language>()

        fun suggester(language: Language): Suggester? =
            suggesters[language] ?: run {
                preload(language)
                null
            }

        fun userDictionary(language: Language): UserDictionary =
            userDictionaries.getOrPut(language) {
                UserDictionary(
                    File(context.filesDir, "user_words_${language.name.lowercase()}.tsv"),
                ) { executor.execute(it) }
            }

        /** Starts loading [language] in the background (no-op if loaded or loading). */
        fun preload(language: Language) {
            if (suggesters.containsKey(language) || !loading.add(language)) return
            // The layout factory is not thread-safe: read the letter cells here, on the caller's thread.
            val model = errorModelFor(language)
            executor.execute {
                try {
                    val dictionary =
                        context.assets.open("dict/${assetName(language)}.tsv").bufferedReader().useLines {
                            WordDictionary.parse(it)
                        }
                    suggesters[language] = Suggester(dictionary, userDictionary(language), model, allWords(language))
                } catch (e: IOException) {
                    Log.w(TAG, "Dictionary for $language could not be loaded", e)
                    loading.remove(language)
                }
            }
        }

        /** The language's full word list (dict/<lang>_words.bin), if it has one. */
        private fun allWords(language: Language): WordSet? =
            try {
                context.assets.open("dict/${assetName(language)}_words.bin").use(WordSet::read)
            } catch (_: IOException) {
                null // English has none: only its dictionary is used
            }

        /** Forgets every learned word in every language. */
        fun clearLearnedWords() {
            Language.values().forEach { userDictionary(it).clear() }
        }

        private fun errorModelFor(language: Language): ErrorModel =
            ErrorModel.fromLayout(configurationFactory.getConfiguration(language, LayoutMode.LETTERS))

        private fun assetName(language: Language) =
            when (language) {
                Language.SPANISH -> "es"
                Language.ENGLISH -> "en"
            }

        private companion object {
            const val TAG = "OneHandKeyboard"
        }
    }
