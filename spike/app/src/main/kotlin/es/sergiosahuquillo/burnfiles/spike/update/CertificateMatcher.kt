package es.sergiosahuquillo.burnfiles.spike.update

object CertificateMatcher {
    fun fingerprint(certificate: ByteArray): String = Sha256Verifier.digest(certificate)

    /** The candidate must be signed by exactly the same set of certificates as the installed app. */
    fun matches(installed: Set<String>, candidate: Set<String>): Boolean =
        installed.isNotEmpty() && installed == candidate
}
