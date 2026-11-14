package su.catlean.yellow

/** A single rotation sample written by the recorder and consumed by NeuroRotation. */
internal data class NeuroSample(
    val targetDeltaYaw: Float,
    val targetDeltaPitch: Float,
    val distance: Float,
    val actualDeltaYaw: Float,
    val actualDeltaPitch: Float
)

/** Keeps dataset names inside the neuro_datasets folder and avoids invalid file names. */
internal fun sanitizeDatasetName(name: String): String? {
    val sanitized = name.trim()
        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        .trim('_', '-', '.')
        .take(64)

    return sanitized.takeIf { it.isNotBlank() }
}
