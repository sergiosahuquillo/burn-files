package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import org.json.JSONObject

enum class DatasetState { INCOMPLETE, COMPLETE, CLEANING }

/** `BurnFilesSpike/dataset/dataset.json`: detects interrupted generations (data-model.md). */
data class DatasetMarker(
    val seed: Long,
    val profile: String,
    val targetFiles: Int,
    val createdFiles: Int,
    val folders: Int,
    val totalBytes: Long,
    val state: DatasetState
) {
    fun matches(plan: DatasetPlan) = seed == plan.seed && profile == plan.profile.id

    fun write(datasetRoot: File) {
        datasetRoot.mkdirs()
        val json =
            JSONObject()
                .put("seed", seed)
                .put("profile", profile)
                .put("targetFiles", targetFiles)
                .put("createdFiles", createdFiles)
                .put("folders", folders)
                .put("totalBytes", totalBytes)
                .put("state", state.name)
        val tmp = File(datasetRoot, "$FILE_NAME.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(File(datasetRoot, FILE_NAME))) {
            File(datasetRoot, FILE_NAME).writeText(json.toString())
            tmp.delete()
        }
    }

    companion object {
        const val FILE_NAME = "dataset.json"

        fun forPlan(plan: DatasetPlan, createdFiles: Int, state: DatasetState) = DatasetMarker(
            seed = plan.seed,
            profile = plan.profile.id,
            targetFiles = plan.fileCount,
            createdFiles = createdFiles,
            folders = plan.folderCount,
            totalBytes = plan.totalBytes,
            state = state
        )

        fun read(datasetRoot: File): DatasetMarker? {
            val file = File(datasetRoot, FILE_NAME)
            if (!file.isFile) return null
            return runCatching {
                val json = JSONObject(file.readText())
                DatasetMarker(
                    seed = json.getLong("seed"),
                    profile = json.getString("profile"),
                    targetFiles = json.getInt("targetFiles"),
                    createdFiles = json.getInt("createdFiles"),
                    folders = json.getInt("folders"),
                    totalBytes = json.getLong("totalBytes"),
                    state = enumValueOf(json.getString("state"))
                )
            }.getOrNull()
        }
    }
}
