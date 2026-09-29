package eu.kanade.tachiyomi.extension.en.sacredtextslibrary

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

/**
 * Entry point declared in AndroidManifest.xml
 * (meta-data "tachiyomi.extension.class").
 */
class SacredTextsLibraryFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(SacredTextsLibrary())
}
