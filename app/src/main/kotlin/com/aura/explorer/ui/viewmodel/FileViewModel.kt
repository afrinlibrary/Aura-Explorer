package com.aura.explorer.ui.viewmodel

import android.os.Environment
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.explorer.domain.model.*
import com.aura.explorer.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Stable
data class FileUiState(
    val currentPath   : String          = Environment.getExternalStorageDirectory().absolutePath,
    val items         : List<FileItem>  = emptyList(),
    val filteredItems : List<FileItem>  = emptyList(),
    val isLoading     : Boolean         = true,
    val error         : String?         = null,
    val sortOrder     : SortOrder       = SortOrder.NAME_ASC,
    val viewMode      : ViewMode        = ViewMode.LIST,
    val searchQuery   : String          = "",
    val isSearchActive: Boolean         = false,
    val selectedItems : Set<String>     = emptySet(),
    val breadcrumbs   : List<BreadCrumb> = emptyList(),
    val showHidden    : Boolean         = false,
    val clipboardItems: List<FileItem>  = emptyList(),
    val clipboardMode : ClipboardMode   = ClipboardMode.NONE,
)

data class BreadCrumb(val name: String, val path: String)

enum class ClipboardMode { NONE, COPY, CUT }

sealed interface FileEvent {
    data class ShowSnackbar(val message: String)  : FileEvent
    data object RequestAllFilesPermission         : FileEvent
    data class OpenFile(val item: FileItem)       : FileEvent
    data class ShowDetails(val item: FileItem)    : FileEvent
}

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileUiState())
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    private val _events = Channel<FileEvent>(Channel.BUFFERED)
    val events: Flow<FileEvent> = _events.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        if (!repository.hasAllFilesAccess()) {
            viewModelScope.launch { _events.send(FileEvent.RequestAllFilesPermission) }
        }
        observeSearch()
        loadDirectory(_uiState.value.currentPath)
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listDirectory(path).fold(
                onSuccess = { items ->
                    _uiState.update { state ->
                        state.copy(
                            currentPath   = path,
                            items         = items,
                            filteredItems = applyFilters(items),
                            isLoading     = false,
                            breadcrumbs   = buildBreadcrumbs(path),
                            selectedItems = emptySet(),
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _events.trySend(FileEvent.ShowSnackbar("Error: ${e.message}"))
                }
            )
        }
    }

    fun navigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val root    = Environment.getExternalStorageDirectory()
        if (current.absolutePath == root.absolutePath) return false
        val parent = current.parentFile ?: return false
        loadDirectory(parent.absolutePath)
        return true
    }

    fun onItemClick(item: FileItem) {
        if (_uiState.value.selectedItems.isNotEmpty()) {
            toggleSelection(item)
        } else if (item.isDirectory) {
            loadDirectory(item.path)
        } else {
            viewModelScope.launch { _events.send(FileEvent.OpenFile(item)) }
        }
    }

    fun onItemLongClick(item: FileItem) = toggleSelection(item)

    fun toggleSelection(item: FileItem) {
        _uiState.update { state ->
            val set = state.selectedItems.toMutableSet()
            if (item.path in set) set.remove(item.path) else set.add(item.path)
            state.copy(selectedItems = set)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedItems = it.filteredItems.map { f -> f.path }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet()) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            state.copy(sortOrder = order, filteredItems = applyFilters(state.items, sort = order))
        }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun toggleShowHidden() {
        _uiState.update { state ->
            val show = !state.showHidden
            state.copy(showHidden = show, filteredItems = applyFilters(state.items, showHidden = show))
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery.debounce(200).collect { query ->
                _uiState.update { state ->
                    state.copy(
                        searchQuery   = query,
                        filteredItems = applyFilters(state.items, query = query),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
        if (!active) onSearchQueryChange("")
    }

    fun copySelected() = setClipboard(ClipboardMode.COPY)
    fun cutSelected()  = setClipboard(ClipboardMode.CUT)

    private fun setClipboard(mode: ClipboardMode) {
        val state    = _uiState.value
        val selected = state.filteredItems.filter { it.path in state.selectedItems }
        _uiState.update { it.copy(clipboardItems = selected, clipboardMode = mode) }
        clearSelection()
        viewModelScope.launch {
            val verb = if (mode == ClipboardMode.COPY) "copied" else "cut"
            _events.send(FileEvent.ShowSnackbar("${selected.size} item(s) $verb"))
        }
    }

    fun paste() {
        val state = _uiState.value
        if (state.clipboardItems.isEmpty()) return
        viewModelScope.launch {
            val result = when (state.clipboardMode) {
                ClipboardMode.COPY -> repository.copy(state.clipboardItems, state.currentPath)
                ClipboardMode.CUT  -> repository.move(state.clipboardItems, state.currentPath)
                ClipboardMode.NONE -> return@launch
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clipboardItems = emptyList(), clipboardMode = ClipboardMode.NONE) }
                    loadDirectory(state.currentPath)
                    _events.send(FileEvent.ShowSnackbar("Pasted successfully"))
                },
                onFailure = { _events.send(FileEvent.ShowSnackbar("Paste failed: ${it.message}")) }
            )
        }
    }

    fun deleteSelected() {
        val state    = _uiState.value
        val toDelete = state.filteredItems.filter { it.path in state.selectedItems }
        viewModelScope.launch {
            repository.delete(toDelete).fold(
                onSuccess = {
                    clearSelection()
                    loadDirectory(state.currentPath)
                    _events.send(FileEvent.ShowSnackbar("Deleted ${toDelete.size} item(s)"))
                },
                onFailure = { _events.send(FileEvent.ShowSnackbar("Delete failed: ${it.message}")) }
            )
        }
    }

    fun rename(item: FileItem, newName: String) {
        viewModelScope.launch {
            repository.rename(item, newName).fold(
                onSuccess = { loadDirectory(_uiState.value.currentPath) },
                onFailure = { _events.send(FileEvent.ShowSnackbar("Rename failed: ${it.message}")) }
            )
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createDirectory(_uiState.value.currentPath, name).fold(
                onSuccess = { loadDirectory(_uiState.value.currentPath) },
                onFailure = { _events.send(FileEvent.ShowSnackbar("Failed: ${it.message}")) }
            )
        }
    }

    fun showDetails(item: FileItem) {
        viewModelScope.launch { _events.send(FileEvent.ShowDetails(item)) }
    }

    private fun applyFilters(
        items      : List<FileItem>,
        query      : String?    = null,
        sort       : SortOrder? = null,
        showHidden : Boolean?   = null,
    ): List<FileItem> {
        val s = _uiState.value
        val q = query      ?: s.searchQuery
        val o = sort       ?: s.sortOrder
        val h = showHidden ?: s.showHidden
        return items
            .filter { if (!h) !it.isHidden else true }
            .filter { if (q.isNotBlank()) it.name.contains(q, ignoreCase = true) else true }
            .sortedWith(o)
    }

    private fun buildBreadcrumbs(path: String): List<BreadCrumb> {
        val root   = Environment.getExternalStorageDirectory().absolutePath
        val crumbs = mutableListOf(BreadCrumb("Storage", root))
        if (path == root) return crumbs
        var acc = root
        path.removePrefix(root).split("/").filter { it.isNotBlank() }.forEach { seg ->
            acc += "/$seg"
            crumbs += BreadCrumb(seg, acc)
        }
        return crumbs
    }
}
