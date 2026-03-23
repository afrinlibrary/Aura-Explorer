package com.aura.explorer.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.explorer.domain.model.FileItem
import com.aura.explorer.ui.util.fileIconMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsBottomSheet(
    item       : FileItem,
    sheetState : SheetState,
    onDismiss  : () -> Unit,
    onRename   : (String) -> Unit,
    onDelete   : () -> Unit,
    onCopy     : () -> Unit,
    onMove     : () -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(item)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            QuickActionRow(
                onShare    = {},
                onOpen     = {},
                onCopy     = { onCopy(); onDismiss() },
                isFavorite = false,
                onFavorite = {},
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            SheetAction(Icons.Rounded.DriveFileRenameOutline, "Rename")   { showRenameDialog = true }
            SheetAction(Icons.Rounded.ContentCopy, "Copy to…")             { onCopy(); onDismiss() }
            SheetAction(Icons.Rounded.DriveFileMove, "Move to…")           { onMove(); onDismiss() }
            SheetAction(Icons.Rounded.Info, "Details", trailing = {
                Icon(Icons.Rounded.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }) {}

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            SheetAction(Icons.Rounded.Delete, "Delete",
                tint = MaterialTheme.colorScheme.error) { onDelete(); onDismiss() }

            if (!item.isDirectory) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                InfoRow("Size",     item.sizeFormatted)
                InfoRow("Modified", item.dateFormatted)
                InfoRow("Location", item.file.parent ?: "—")
                item.mimeType?.let { InfoRow("Type", it) }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != item.name) onRename(newName.trim())
                        showRenameDialog = false; onDismiss()
                    },
                    enabled = newName.isNotBlank(),
                ) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SheetHeader(item: FileItem) {
    val primary  = MaterialTheme.colorScheme.primary
    val iconMeta = remember(item.type) { fileIconMeta(item.type, primary) }
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier         = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium)
                .background(iconMeta.tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(iconMeta.icon, null, tint = iconMeta.tint, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                if (item.isDirectory) "Folder" else item.sizeFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    onShare: () -> Unit, onOpen: () -> Unit, onCopy: () -> Unit,
    isFavorite: Boolean, onFavorite: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickChip(Icons.Rounded.Share,        "Share",  onShare,    Modifier.weight(1f))
        QuickChip(Icons.Rounded.OpenInNew,    "Open",   onOpen,     Modifier.weight(1f))
        QuickChip(Icons.Rounded.ContentCopy,  "Copy",   onCopy,     Modifier.weight(1f))
        QuickChip(
            icon     = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label    = "Save",
            onClick  = onFavorite,
            tint     = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickChip(
    icon    : ImageVector,
    label   : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    tint    : Color    = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier              = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 10.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun SheetAction(
    icon    : ImageVector,
    label   : String,
    tint    : Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null,
    onClick : () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(key, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
