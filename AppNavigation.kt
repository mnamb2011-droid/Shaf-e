package com.shafii.encyclopedia.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shafii.encyclopedia.ui.screens.*
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class AppDestination(val titleAr: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Default.Home),
    BOOKS("أبواب الفقه", Icons.Default.MenuBook),
    SEARCH("البحث الشامل", Icons.Default.Search),
    COMPARISON("المقارنة بين الأقوال", Icons.Default.CompareArrows),
    SCHOLARS("العلماء", Icons.Default.People),
    LIBRARY("المكتبة والمصادر", Icons.Default.LibraryBooks),
    FAVORITES("المفضلة", Icons.Default.Bookmark),
    NOTES("الملاحظات", Icons.Default.EditNote),
    SETTINGS("إعدادات التطبيق", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShafiiRootScaffold(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    val selectedIssue by viewModel.selectedIssue.collectAsState()
    var showingIssueDetail by remember { mutableStateOf(false) }

    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage!!)
            viewModel.clearStatusMessage()
        }
    }

    val openIssue: (Int) -> Unit = { id ->
        viewModel.openIssue(id)
        showingIssueDetail = true
    }

    if (showingIssueDetail && selectedIssue != null) {
        IssueDetailScreen(
            detail = selectedIssue!!,
            viewModel = viewModel,
            onBack = { showingIssueDetail = false },
            onOpenRelatedIssue = { relatedId -> viewModel.openIssue(relatedId) },
            onOpenComparison = { issueId ->
                viewModel.selectIssueForComparison(issueId)
                showingIssueDetail = false
                currentDestination = AppDestination.COMPARISON
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "الموسوعة الشافعية الشاملة",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AppDestination.entries.forEach { dest ->
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.titleAr) },
                        selected = currentDestination == dest,
                        onClick = {
                            currentDestination = dest
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(currentDestination.titleAr) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "القائمة")
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentDestination = AppDestination.SEARCH }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val primaryTabs = listOf(
                        AppDestination.HOME,
                        AppDestination.BOOKS,
                        AppDestination.SEARCH,
                        AppDestination.COMPARISON,
                        AppDestination.LIBRARY
                    )
                    primaryTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination == tab,
                            onClick = { currentDestination = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.titleAr) },
                            label = { Text(tab.titleAr, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentDestination) {
                    AppDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigate = { currentDestination = it },
                        onOpenIssue = openIssue,
                        onGlobalSearch = { q ->
                            viewModel.updateSearch(q, recordToHistory = true)
                            currentDestination = AppDestination.SEARCH
                        }
                    )
                    AppDestination.BOOKS -> BooksScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.SEARCH -> SearchScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.COMPARISON -> ComparisonScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.SCHOLARS -> ScholarsScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.LIBRARY -> LibraryScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.FAVORITES -> FavoritesScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.NOTES -> NotesScreen(viewModel = viewModel, onOpenIssue = openIssue)
                    AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
