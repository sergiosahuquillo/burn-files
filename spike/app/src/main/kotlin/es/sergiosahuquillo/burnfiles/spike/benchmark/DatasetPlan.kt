package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.util.Locale
import kotlin.random.Random

data class DatasetProfile(
    val id: String,
    val targetFiles: Int,
    val targetFolders: Int,
    val topLevelFolders: Int,
    val maxDepth: Int,
    val smallShare: Double,
    val smallMaxBytes: Int,
    val largeMaxBytes: Int
) {
    companion object {
        /** research R6: 100k files, ~4k folders, depth 1–6, 97 % of 0–2 KiB and 3 % of 2–64 KiB. */
        val DEFAULT =
            DatasetProfile(
                id = "default-100k",
                targetFiles = 100_000,
                targetFolders = 4_000,
                topLevelFolders = 40,
                maxDepth = 6,
                smallShare = 0.97,
                smallMaxBytes = 2 * 1024,
                largeMaxBytes = 64 * 1024
            )
    }
}

/**
 * Deterministic description of the synthetic tree: same seed + same profile ⇒ same folders,
 * names and sizes. Pure: never touches the disk.
 */
class DatasetPlan private constructor(
    val seed: Long,
    val profile: DatasetProfile,
    private val folderParent: IntArray,
    private val folderDepth: IntArray,
    private val fileFolder: IntArray,
    private val fileSize: IntArray,
    private val fileExtension: ByteArray
) {
    val folderCount: Int get() = folderParent.size
    val fileCount: Int get() = fileFolder.size
    val maxDepth: Int get() = folderDepth.max()
    val totalBytes: Long by lazy { fileSize.sumOf { it.toLong() } }

    private val folderPaths: Array<String> by lazy {
        Array(folderCount) { "" }.also { paths ->
            // Parents always have a lower index than their children.
            for (index in 0 until folderCount) {
                val parent = folderParent[index]
                paths[index] = if (parent < 0) "d$index" else "${paths[parent]}/d$index"
            }
        }
    }

    fun folderPath(index: Int): String = folderPaths[index]

    fun folderDepth(index: Int): Int = folderDepth[index]

    fun fileSize(index: Int): Int = fileSize[index]

    fun filePath(index: Int): String =
        "${folderPaths[fileFolder[index]]}/f%06d%s".format(Locale.ROOT, index, EXTENSIONS[fileExtension[index].toInt()])

    /** Bytes actually used on disk, where every file takes at least one block. */
    fun diskBytes(blockSize: Int = DEFAULT_BLOCK): Long =
        fileSize.sumOf { size -> ((size + blockSize - 1) / blockSize).coerceAtLeast(1).toLong() * blockSize } +
            folderCount.toLong() * blockSize

    fun sameAs(other: DatasetPlan): Boolean = seed == other.seed &&
        profile == other.profile &&
        folderParent.contentEquals(other.folderParent) &&
        fileFolder.contentEquals(other.fileFolder) &&
        fileSize.contentEquals(other.fileSize) &&
        fileExtension.contentEquals(other.fileExtension)

    companion object {
        const val DEFAULT_SEED = 0xB0FF11E5L
        private const val DEFAULT_BLOCK = 4096
        private const val MAX_CHILDREN = 5
        private const val SHALLOW_DEPTH = 2

        /** Mixed extensions so the media index classifies the files in different ways. */
        val EXTENSIONS = listOf(".jpg", ".mp4", ".txt", ".apk", ".zip", ".tmp", "")

        fun create(seed: Long = DEFAULT_SEED, profile: DatasetProfile = DatasetProfile.DEFAULT): DatasetPlan {
            val random = Random(seed)
            val parents = ArrayList<Int>(profile.targetFolders)
            val depths = ArrayList<Int>(profile.targetFolders)
            repeat(profile.topLevelFolders) {
                parents += -1
                depths += 1
            }
            // Breadth-first growth: shallow levels always branch, deeper ones may stop.
            var cursor = 0
            while (parents.size < profile.targetFolders && cursor < parents.size) {
                val depth = depths[cursor]
                if (depth < profile.maxDepth) {
                    val minChildren = if (depth <= SHALLOW_DEPTH) 1 else 0
                    repeat(random.nextInt(minChildren, MAX_CHILDREN + 1)) {
                        if (parents.size < profile.targetFolders) {
                            parents += cursor
                            depths += depth + 1
                        }
                    }
                }
                cursor++
            }

            val folders = parents.size
            val fileFolder = IntArray(profile.targetFiles)
            val fileSize = IntArray(profile.targetFiles)
            val fileExtension = ByteArray(profile.targetFiles)
            for (index in 0 until profile.targetFiles) {
                fileFolder[index] = random.nextInt(folders)
                fileSize[index] =
                    if (random.nextDouble() < profile.smallShare) {
                        random.nextInt(1, profile.smallMaxBytes + 1)
                    } else {
                        random.nextInt(profile.smallMaxBytes + 1, profile.largeMaxBytes + 1)
                    }
                fileExtension[index] = random.nextInt(EXTENSIONS.size).toByte()
            }
            return DatasetPlan(
                seed = seed,
                profile = profile,
                folderParent = parents.toIntArray(),
                folderDepth = depths.toIntArray(),
                fileFolder = fileFolder,
                fileSize = fileSize,
                fileExtension = fileExtension
            )
        }
    }
}
