package dev.ghjkl.tally.ui

import org.apache.commons.text.similarity.FuzzyScore
import java.util.Locale

// Same scoring approach as Sublime Text/Atom/IntelliJ's fuzzy matchers: points per
// matched character, bonus for consecutive runs — so "rent" ranks "Expenses:Rent"
// far above a scattered match buried in "Expenses:Leisure:Computer:3dprint".
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
