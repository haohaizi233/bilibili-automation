package com.example.bilibiliautomation.util

object MediaUrls {
    // Bilibili image CDN serves the same /bfs/... path from any of these
    // hosts. They are interchangeable, so when one host fails to resolve
    // (e.g. emulator DNS flakiness for a specific iN.hdslb.com subdomain) we
    // fall back to the others.
    private val CDN_HOSTS = listOf("i0.hdslb.com", "i1.hdslb.com", "i2.hdslb.com")
    private val CDN_HOST_PATTERN = Regex("""https://(i\d\.hdslb\.com)/(.*)""")

    fun httpsFace(url: String?): String? {
        val raw = url?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return null
        return when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("http://") -> "https://${raw.removePrefix("http://")}"
            raw.startsWith("https://") -> raw
            else -> raw
        }
    }

    fun faceCandidates(url: String?): List<String> {
        val https = httpsFace(url) ?: return emptyList()
        val ordered = linkedSetOf(https)
        val base = https.substringBefore('?')
        if (!base.contains("hdslb.com/bfs/")) return ordered.toList()

        // Format variants (small jpg first — most reliably decoded by
        // BitmapFactory across API levels; then webp; then the raw url).
        val formatSuffixes = if ('@' in base) listOf("") else listOf("@120w_120h_1c.jpg", "@120w_120h_1c.webp", "")

        // Suffix-major order: for each format, try the original host first
        // then the other CDN hosts. This reaches a resolvable host fastest
        // when one subdomain's DNS is flaky (e.g. emulator can't resolve
        // i1.hdslb.com but can resolve i0.hdslb.com).
        val hosts = CDN_HOSTS.toList()
        for (suffix in formatSuffixes) {
            for (host in hosts) {
                val withHost = withHost(base, host) ?: continue
                ordered += withHost + suffix
            }
        }
        return ordered.toList()
    }

    private fun withHost(url: String, host: String): String? {
        val match = CDN_HOST_PATTERN.find(url) ?: return null
        return "https://$host/${match.groupValues[2]}"
    }
}
