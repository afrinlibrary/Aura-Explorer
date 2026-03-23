package com.aura.explorer.domain.repository

import com.aura.explorer.domain.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun observeDirectory(path: String): Flow<Result<List<FileItem>>>
    suspend fun listDirectory(path: String): Result<List<FileItem>>
    suspend fun delete(items: List<FileItem>): Result<Unit>
    suspend fun rename(item: FileItem, newName: String): Result<FileItem>
    suspend fun copy(items: List<FileItem>, destination: String): Result<Unit>
    suspend fun move(items: List<FileItem>, destination: String): Result<Unit>
    suspend fun createDirectory(parent: String, name: String): Result<FileItem>
    fun hasAllFilesAccess(): Boolean
    fun getStorageVolumes(): List<StorageVolume>
}

data class StorageVolume(
    val name       : String,
    val path       : String,
    val totalBytes : Long,
    val freeBytes  : Long,
    val isRemovable: Boolean,
)
