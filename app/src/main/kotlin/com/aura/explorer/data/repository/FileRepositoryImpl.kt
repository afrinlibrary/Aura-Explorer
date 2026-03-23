package com.aura.explorer.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.aura.explorer.domain.model.FileItem
import com.aura.explorer.domain.repository.FileRepository
import com.aura.explorer.domain.repository.StorageVolume
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileRepository {

    override fun observeDirectory(path: String): Flow<Result<List<FileItem>>> = flow {
        var last: List<FileItem> = emptyList()
        while (true) {
            val result = listDirectory(path)
            if (result.isSuccess) {
                val items = result.getOrThrow()
                if (items != last) { last = items; emit(result) }
            } else {
                emit(result)
            }
            delay(1_500)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listDirectory(path: String): Result<List<FileItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(path)
                require(dir.exists() && dir.isDirectory) { "Not a directory: $path" }
                dir.listFiles()?.map { FileItem(it) } ?: emptyList()
            }
        }

    override suspend fun delete(items: List<FileItem>): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { items.forEach { it.file.deleteRecursively() } } }

    override suspend fun rename(item: FileItem, newName: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dest = File(item.file.parent, newName)
                check(item.file.renameTo(dest)) { "Rename failed" }
                FileItem(dest)
            }
        }

    override suspend fun copy(items: List<FileItem>, destination: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val destDir = File(destination)
                items.forEach { item ->
                    item.file.copyRecursively(File(destDir, item.name), overwrite = true)
                }
            }
        }

    override suspend fun move(items: List<FileItem>, destination: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                copy(items, destination).getOrThrow()
                delete(items).getOrThrow()
            }
        }

    override suspend fun createDirectory(parent: String, name: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(parent, name)
                check(dir.mkdirs()) { "Could not create directory" }
                FileItem(dir)
            }
        }

    override fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else true

    override fun getStorageVolumes(): List<StorageVolume> {
        val sm = context.getSystemService<StorageManager>() ?: return emptyList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return emptyList()
        return sm.storageVolumes.mapNotNull { vol ->
            val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                vol.directory?.absolutePath else null
            path?.let {
                val stat = StatFs(it)
                StorageVolume(
                    name        = vol.getDescription(context),
                    path        = it,
                    totalBytes  = stat.totalBytes,
                    freeBytes   = stat.availableBytes,
                    isRemovable = vol.isRemovable,
                )
            }
        }
    }
}
