package dev.ghjkl.tally.ui

import org.apache.commons.text.similarity.FuzzyScore
import java.util.Locale

// Scores consecutive matches higher, like Sublime Text/IntelliJ.
private val fuzzyScore = FuzzyScore(Locale.ENGLISH)

fun rankByFuzzyScore(query: String, candidates: List<String>, limit: Int): List<String> {
    if (query.isBlank()) return candidates.take(limit)
    return candidates
        .map { it to fuzzyScore.fuzzyScore(it, query) }
        .filter { (_, score) -> score > 0 }
        .sortedByDescending { (_, score) -> score }
        .take(limit)
        .map { (account, _) -> account }
}
