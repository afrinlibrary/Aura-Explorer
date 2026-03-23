package com.aura.explorer.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aura.explorer.domain.model.FileType

data class FileIconMeta(val icon: ImageVector, val tint: Color)

fun fileIconMeta(type: FileType, fallback: Color): FileIconMeta = when (type) {
    FileType.FOLDER       -> FileIconMeta(Icons.Rounded.Folder,          Color(0xFFFFD54F))
    FileType.IMAGE        -> FileIconMeta(Icons.Rounded.Image,           Color(0xFF81C784))
    FileType.VIDEO        -> FileIconMeta(Icons.Rounded.Videocam,        Color(0xFFE57373))
    FileType.AUDIO        -> FileIconMeta(Icons.Rounded.MusicNote,       Color(0xFFBA68C8))
    FileType.DOCUMENT     -> FileIconMeta(Icons.Rounded.Description,     Color(0xFF64B5F6))
    FileType.PDF          -> FileIconMeta(Icons.Rounded.PictureAsPdf,    Color(0xFFEF5350))
    FileType.SPREADSHEET  -> FileIconMeta(Icons.Rounded.TableChart,      Color(0xFF66BB6A))
    FileType.PRESENTATION -> FileIconMeta(Icons.Rounded.Slideshow,       Color(0xFFFFA726))
    FileType.ARCHIVE      -> FileIconMeta(Icons.Rounded.FolderZip,       Color(0xFFFFCC80))
    FileType.APK          -> FileIconMeta(Icons.Rounded.Android,         Color(0xFF69F0AE))
    FileType.CODE         -> FileIconMeta(Icons.Rounded.Code,            Color(0xFF80DEEA))
    FileType.FONT         -> FileIconMeta(Icons.Rounded.TextFields,      Color(0xFFCE93D8))
    FileType.UNKNOWN      -> FileIconMeta(Icons.Rounded.InsertDriveFile, fallback.copy(alpha = 0.6f))
}
