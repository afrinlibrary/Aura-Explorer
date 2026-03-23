package com.aura.explorer.domain.model

import android.webkit.MimeTypeMap
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType {
    FOLDER,
    IMAGE, VIDEO, AUDIO,
    DOCUMENT, PDF, SPREADSHEET, PRESENTATION,
    ARCHIVE,
    APK,
    CODE,
    FONT,
    UNKNOWN;

    companion object {
        fun from(file: File): FileType {
            if (file.isDirectory) return FOLDER
            return when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg" -> IMAGE
                "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv"          -> VIDEO
                "mp3", "flac", "aac", "ogg", "wav", "m4a", "opus"         -> AUDIO
                "doc", "docx", "odt", "rtf", "txt", "md"                  -> DOCUMENT
                "pdf"                                                       -> PDF
                "xls", "xlsx", "ods", "csv"                               -> SPREADSHEET
                "ppt", "pptx", "odp"                                      -> PRESENTATION
                "zip", "7z", "rar", "tar", "gz", "bz2", "xz",
                "lz4", "zst", "br", "lzma", "cab", "iso"                 -> ARCHIVE
                "apk", "xapk", "apks"                                     -> APK
                "kt", "java", "py", "js", "ts", "json", "xml",
                "yaml", "yml", "toml", "sh", "c", "cpp",
                "h", "rs", "go", "dart", "swift", "cs"                   -> CODE
                "ttf", "otf", "woff", "woff2"                             -> FONT
                else                                                       -> UNKNOWN
            }
        }
    }
}

enum class SortOrder {
    NAME_ASC, NAME_DESC,
    DATE_ASC, DATE_DESC,
    SIZE_ASC, SIZE_DESC,
    TYPE_ASC;

    val label get() = when (this) {
        NAME_ASC  -> "Name (A → Z)"
        NAME_DESC -> "Name (Z → A)"
        DATE_ASC  -> "Date (oldest first)"
        DATE_DESC -> "Date (newest first)"
        SIZE_ASC  -> "Size (smallest first)"
        SIZE_DESC -> "Size (largest first)"
        TYPE_ASC  -> "Type"
    }
}

enum class ViewMode { LIST, GRID }

data class FileItem(
    val file        : File,
    val name        : String   = file.name,
    val path        : String   = file.absolutePath,
    val type        : FileType = FileType.from(file),
    val size        : Long     = if (file.isFile) file.length() else 0L,
    val lastModified: Long     = file.lastModified(),
    val isHidden    : Boolean  = file.isHidden,
    val extension   : String   = file.extension.lowercase(),
    val mimeType    : String?  = resolveMime(file),
) {
    val isDirectory: Boolean get() = type == FileType.FOLDER
    val isArchive  : Boolean get() = type == FileType.ARCHIVE

    val sizeFormatted: String
        get() = if (isDirectory) "" else formatSize(size)

    val dateFormatted: String
        get() = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
            .format(Date(lastModified))

    companion object {
        private fun resolveMime(file: File): String? {
            if (file.isDirectory) return null
            return MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase())
        }

        fun formatSize(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val idx = (Math.log10(bytes.toDouble()) / Math.log10(1024.0))
                .toInt().coerceIn(0, units.lastIndex)
            return "${DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, idx.toDouble()))} ${units[idx]}"
        }
    }
}

fun List<FileItem>.sortedWith(order: SortOrder): List<FileItem> {
    val (dirs, files) = partition { it.isDirectory }
    fun sort(list: List<FileItem>) = when (order) {
        SortOrder.NAME_ASC  -> list.sortedBy            { it.name.lowercase() }
        SortOrder.NAME_DESC -> list.sortedByDescending  { it.name.lowercase() }
        SortOrder.DATE_ASC  -> list.sortedBy            { it.lastModified }
        SortOrder.DATE_DESC -> list.sortedByDescending  { it.lastModified }
        SortOrder.SIZE_ASC  -> list.sortedBy            { it.size }
        SortOrder.SIZE_DESC -> list.sortedByDescending  { it.size }
        SortOrder.TYPE_ASC  -> list.sortedWith(compareBy({ it.type.name }, { it.name.lowercase() }))
    }
    return sort(dirs) + sort(files)
}
