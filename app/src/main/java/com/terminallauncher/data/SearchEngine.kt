package com.terminallauncher.data

data class SearchResult(
    val app: AppInfo,
    val matchedIndices: List<Int>,
    val score: Int
)

class SearchEngine {

    fun search(query: String, apps: List<AppInfo>, screenTime: Map<String, Long> = emptyMap()): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val q = query.lowercase()
        val results = mutableListOf<SearchResult>()

        for (app in apps) {
            val label = app.label.lowercase()

            // Phase 1: full label starts with query (best match)
            if (label.startsWith(q)) {
                results.add(SearchResult(
                    app = app,
                    matchedIndices = q.indices.toList(),
                    score = 2000 - label.length
                ))
                continue
            }

            // Phase 2: any word in the label starts with query
            val wordMatch = findWordPrefixMatch(q, label)
            if (wordMatch != null) {
                results.add(SearchResult(
                    app = app,
                    matchedIndices = wordMatch,
                    score = 1500 - label.length
                ))
                continue
            }

            // Phase 3: label contains query as substring
            val containsIdx = label.indexOf(q)
            if (containsIdx >= 0) {
                results.add(SearchResult(
                    app = app,
                    matchedIndices = (containsIdx until containsIdx + q.length).toList(),
                    score = 1000 - label.length - containsIdx
                ))
                continue
            }

            // Phase 4: fuzzy subsequence match
            val fuzzy = fuzzyMatch(q, label)
            if (fuzzy != null) {
                results.add(SearchResult(
                    app = app,
                    matchedIndices = fuzzy.first,
                    score = fuzzy.second
                ))
            }
        }

        // Boost by screen time: each minute of usage adds 1 point to score
        return results.map { result ->
            val timeMs = screenTime[result.app.packageName] ?: 0L
            val minutes = (timeMs / 60_000).coerceAtMost(500).toInt()
            result.copy(score = result.score + minutes)
        }.sortedByDescending { it.score }
    }

    private fun findWordPrefixMatch(query: String, label: String): List<Int>? {
        var i = 0
        while (i < label.length) {
            // Find start of a word (after space, dash, or at position 0)
            if (i == 0 || label[i - 1] == ' ' || label[i - 1] == '-' || label[i - 1] == '_') {
                if (label.substring(i).startsWith(query)) {
                    return (i until i + query.length).toList()
                }
            }
            // Also match at uppercase transitions (camelCase): "Chrome" in "GoogleChrome"
            if (i > 0 && label[i].isUpperCase() && label[i - 1].isLowerCase()) {
                if (label.substring(i).lowercase().startsWith(query)) {
                    return (i until i + query.length).toList()
                }
            }
            i++
        }
        return null
    }

    private fun fuzzyMatch(query: String, label: String): Pair<List<Int>, Int>? {
        val matchedIndices = mutableListOf<Int>()
        var queryIdx = 0

        for (i in label.indices) {
            if (queryIdx < query.length && label[i] == query[queryIdx]) {
                matchedIndices.add(i)
                queryIdx++
            }
        }

        if (queryIdx != query.length) return null

        var score = 0
        for (i in matchedIndices.indices) {
            if (matchedIndices[i] == i) score += 10
            if (i > 0 && matchedIndices[i] == matchedIndices[i - 1] + 1) score += 5
            // Bonus for matching at word boundaries
            val idx = matchedIndices[i]
            if (idx == 0 || label[idx - 1] == ' ' || label[idx - 1] == '-') score += 8
        }
        score -= label.length

        return Pair(matchedIndices, score)
    }
}
