package com.autodrive.app.core.designsystem.patterns.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveEmptyState
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveLoadingState
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSearchField
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

enum class SearchResultsState { Idle, Results, Empty, Loading, Error }

@Composable
fun <T> SearchResultsList(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    state: SearchResultsState,
    items: List<T>,
    emptyTitle: String,
    emptyBody: String,
    modifier: Modifier = Modifier,
    loadingLabel: String? = null,
    errorTitle: String = emptyTitle,
    errorBody: String = emptyBody,
    itemContent: @Composable (T) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG)) {
        AutoDriveSearchField(query, onQueryChange, placeholder, searching = state == SearchResultsState.Loading)
        when (state) {
            SearchResultsState.Idle -> Unit
            SearchResultsState.Loading -> AutoDriveLoadingState(label = loadingLabel)
            SearchResultsState.Empty -> AutoDriveEmptyState(emptyTitle, emptyBody)
            SearchResultsState.Error -> AutoDriveEmptyState(errorTitle, errorBody)
            SearchResultsState.Results -> for (item in items) itemContent(item)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun SearchResultsListPreview() = AutoDriveTheme {
    SearchResultsList(query = "", onQueryChange = {}, placeholder = "ابحث", state = SearchResultsState.Empty, items = emptyList<String>(), emptyTitle = "لا نتائج", emptyBody = "جرّب عبارة أخرى") { }
}
