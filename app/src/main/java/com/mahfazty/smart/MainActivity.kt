package com.mahfazty.smart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mahfazty.smart.ui.screens.AccountOpsScreen
import com.mahfazty.smart.ui.screens.ClientAccountsScreen
import com.mahfazty.smart.ui.screens.ClientsScreen
import com.mahfazty.smart.ui.screens.GoalsScreen
import com.mahfazty.smart.ui.screens.HomeScreen
import com.mahfazty.smart.ui.screens.SavingsScreen
import com.mahfazty.smart.ui.screens.SettingsScreen
import com.mahfazty.smart.ui.screens.SettingsViewModel
import com.mahfazty.smart.ui.screens.SplashScreen
import com.mahfazty.smart.ui.screens.TransactionsScreen
import com.mahfazty.smart.ui.theme.MahfaztyTheme
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import com.mahfazty.smart.ui.viewmodels.AccountOpsViewModel
import com.mahfazty.smart.ui.viewmodels.ClientAccountsViewModel
import com.mahfazty.smart.ui.viewmodels.ClientsViewModel
import com.mahfazty.smart.ui.viewmodels.GoalsViewModel
import com.mahfazty.smart.ui.viewmodels.HomeViewModel
import com.mahfazty.smart.ui.viewmodels.MainViewModel
import com.mahfazty.smart.ui.viewmodels.TransactionsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MahfaztyApp).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                    MainViewModel(app.container.walletRepository, app.container.settingsRepository)
                }
            })
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            // واجهة عربية RTL دائماً (تطبيق يمني 🇾🇪)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MahfaztyTheme(settings) {
                    // شاشة افتتاح متحركة تظهر مرة واحدة عند الإطلاق
                    val reduceMotion = rememberReduceMotion()
                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        // مهارة 4: الدخول أطول من الخروج — السائل يحتاج ~1.2ث + استقرار
                        delay(if (reduceMotion) 350 else 1400)
                        showSplash = false
                    }
                    Box(Modifier.fillMaxSize()) {
                        AppRoot(mainViewModel)
                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(tween(380)) + scaleOut(targetScale = 1.06f, animationSpec = tween(380)),
                        ) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }
}

// ============ مسارات التنقل ============

private object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val GOALS = "goals"
    const val SAVINGS = "savings"
    const val CLIENTS = "clients"
    const val SETTINGS = "settings"
    const val CLIENT_DETAIL = "client/{clientId}"
    const val ACCOUNT_DETAIL = "account/{clientId}/{accountId}"
    fun clientDetail(id: Long) = "client/$id"
    fun accountDetail(clientId: Long, accountId: Long) = "account/$clientId/$accountId"
}

private val topLevelRoutes = setOf(
    Routes.HOME, Routes.TRANSACTIONS, Routes.GOALS,
    Routes.SAVINGS, Routes.CLIENTS, Routes.SETTINGS,
)

@Composable
private fun AppRoot(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val reduceMotion = rememberReduceMotion()
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as MahfaztyApp).container

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                BottomBar(currentRoute) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                Routes.HOME,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        HomeViewModel(app.container.walletRepository, app.container.settingsRepository)
                    }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                val settings by mainViewModel.settings.collectAsStateWithLifecycle()
                val insufficient by vm.insufficient.collectAsStateWithLifecycle()
                val pending by vm.pending.collectAsStateWithLifecycle()
                val celebrate by vm.celebrate.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    settings = settings,
                    insufficient = insufficient,
                    pending = pending,
                    celebrate = celebrate,
                    onCelebrateDone = vm::celebrateDone,
                    toast = vm.toast,
                    onDismissInsufficient = vm::dismissInsufficient,
                    onQuickWithdrawGoal = vm::quickWithdrawGoal,
                    onQuickWithdrawSavings = vm::quickWithdrawSavings,
                    onAddTransaction = vm::addTransaction,
                    onTransfer = vm::transfer,
                    onSaveBankOpening = vm::setOpeningBank,
                    onClearPending = vm::clearPending,
                    onTxClick = { navController.navigate(Routes.TRANSACTIONS) },
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onOpenTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                )
            }

            composable(
                Routes.TRANSACTIONS,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm: TransactionsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        TransactionsViewModel(app.container.walletRepository, app.container.settingsRepository)
                    }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                val settings by mainViewModel.settings.collectAsStateWithLifecycle()
                val insufficient by vm.insufficient.collectAsStateWithLifecycle()
                val pending by vm.pending.collectAsStateWithLifecycle()
                TransactionsScreen(
                    state = state,
                    settings = settings,
                    insufficient = insufficient,
                    pending = pending,
                    toast = vm.toast,
                    onSetQuery = vm::setQuery,
                    onSetFilter = vm::setFilter,
                    onAdd = vm::addTransaction,
                    onUpdate = vm::updateTransaction,
                    onDelete = vm::deleteTransaction,
                    onDuplicate = vm::duplicateTransaction,
                    onDismissInsufficient = vm::dismissInsufficient,
                    onQuickWithdrawGoal = vm::quickWithdrawGoal,
                    onQuickWithdrawSavings = vm::quickWithdrawSavings,
                    onClearPending = vm::clearPending,
                )
            }

            composable(
                Routes.GOALS,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm: GoalsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        GoalsViewModel(app.container.walletRepository, app.container.settingsRepository)
                    }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                val insufficient by vm.insufficient.collectAsStateWithLifecycle()
                val pending by vm.pending.collectAsStateWithLifecycle()
                GoalsScreen(
                    state = state,
                    insufficient = insufficient,
                    pending = pending,
                    toast = vm.toast,
                    onAddGoal = vm::addGoal,
                    onContribute = vm::contribute,
                    onDeleteGoal = vm::deleteGoal,
                    onDismissInsufficient = vm::dismissInsufficient,
                    onQuickWithdrawGoal = vm::quickWithdrawGoal,
                    onQuickWithdrawSavings = vm::quickWithdrawSavings,
                    onClearPending = vm::clearPending,
                )
            }

            composable(
                Routes.SAVINGS,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm = viewModel<com.mahfazty.smart.ui.viewmodels.SavingsViewModel>(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        com.mahfazty.smart.ui.viewmodels.SavingsViewModel(app.container.walletRepository, app.container.settingsRepository)
                    }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                val insufficient by vm.insufficient.collectAsStateWithLifecycle()
                val pending by vm.pending.collectAsStateWithLifecycle()
                SavingsScreen(
                    state = state,
                    insufficient = insufficient,
                    pending = pending,
                    toast = vm.toast,
                    onAddToSavings = vm::addToSavings,
                    onWithdrawSavings = vm::withdrawSavings,
                    onDismissInsufficient = vm::dismissInsufficient,
                    onQuickWithdrawGoal = vm::quickWithdrawGoal,
                    onClearPending = vm::clearPending,
                )
            }

            composable(
                Routes.CLIENTS,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm: ClientsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        ClientsViewModel(app.container.clientsRepository)
                    }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                ClientsScreen(
                    state = state,
                    toast = vm.toast,
                    onSetQuery = vm::setQuery,
                    onAddClient = vm::addClient,
                    onOpenClient = { id -> navController.navigate(Routes.clientDetail(id)) },
                )
            }

            composable(
                Routes.CLIENT_DETAIL,
                // إصلاح العطل: تعريف الوسيط Long ضروري — بدونه يمر المعرف كنص ويفشل get<Long>
                arguments = listOf(navArgument("clientId") { type = NavType.LongType }),
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) { entry ->
                // السبب الجذري سابقاً: كان المعرف يُقرأ من SavedStateHandle الذي يحمل نصاً بدل رقم.
                // الحل: القراءة المباشرة من وسائط الوجهة (مصدر مضمون النوع Long).
                val clientId = entry.arguments?.getLong("clientId") ?: 0L
                val vm: ClientAccountsViewModel = viewModel(
                    key = "clientAccounts_$clientId",
                    factory = viewModelFactory {
                        initializer {
                            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                            ClientAccountsViewModel(app.container.clientsRepository, clientId)
                        }
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                ClientAccountsScreen(
                    data = state,
                    toast = vm.toast,
                    onBack = { navController.popBackStack() },
                    onAddAccount = vm::addAccount,
                    onUpdateClient = vm::updateClient,
                    onDeleteClient = vm::deleteClient,
                    onUpdateAccount = vm::updateAccount,
                    onDeleteAccount = vm::deleteAccount,
                    onOpenAccount = { id -> navController.navigate(Routes.accountDetail(clientId, id)) },
                )
            }

            composable(
                Routes.ACCOUNT_DETAIL,
                arguments = listOf(
                    navArgument("clientId") { type = NavType.LongType },
                    navArgument("accountId") { type = NavType.LongType },
                ),
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) { entry ->
                // نفس إصلاح شاشة العميل: القراءة من وسائط الوجهة مباشرة (نوع مضمون Long)
                val accountId = entry.arguments?.getLong("accountId") ?: 0L
                val vm: AccountOpsViewModel = viewModel(
                    key = "accountOps_$accountId",
                    factory = viewModelFactory {
                        initializer {
                            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                            AccountOpsViewModel(
                                app.container.clientsRepository,
                                app.container.walletRepository,
                                accountId,
                            )
                        }
                    },
                )
                val clientData by vm.client.collectAsStateWithLifecycle()
                val account by vm.account.collectAsStateWithLifecycle()
                val allAccounts by vm.allAccounts.collectAsStateWithLifecycle()
                val transfers by vm.transfers.collectAsStateWithLifecycle()
                val selection by vm.selection.collectAsStateWithLifecycle()
                val insufficientReal by vm.insufficientReal.collectAsStateWithLifecycle()
                val pendingOp by vm.pendingOp.collectAsStateWithLifecycle()
                val settings by mainViewModel.settings.collectAsStateWithLifecycle()
                val bank by vm.bank.collectAsStateWithLifecycle()
                val cash by vm.cash.collectAsStateWithLifecycle()
                val savings by vm.savings.collectAsStateWithLifecycle()
                val goalSources by vm.goalSources.collectAsStateWithLifecycle()
                AccountOpsScreen(
                    clientData = clientData,
                    account = account,
                    allAccounts = allAccounts,
                    transfers = transfers,
                    selection = selection,
                    insufficientReal = insufficientReal,
                    pendingOp = pendingOp,
                    settings = settings,
                    bank = bank,
                    cash = cash,
                    savings = savings,
                    goalSources = goalSources,
                    toast = vm.toast,
                    onBack = { navController.popBackStack() },
                    onToggleSelect = vm::toggleSelect,
                    onClearSelection = vm::clearSelection,
                    onAddOperation = vm::addOperation,
                    onUpdateOperation = vm::updateOperation,
                    onDeleteOperation = vm::deleteOperation,
                    onDeleteSelected = vm::deleteSelected,
                    onUpdateAccount = vm::updateAccount,
                    onDeleteAccount = vm::deleteAccount,
                    onFundReal = vm::fundReal,
                    onWithdrawReal = vm::withdrawReal,
                    onTransferReal = vm::transferReal,
                    onQuickFundReal = vm::quickFundReal,
                    onQuickTransferReal = vm::quickTransferReal,
                    onQuickFundSavings = vm::quickFundFromSavings,
                    onQuickFundGoal = vm::quickFundFromGoal,
                    onDismissInsufficientReal = vm::dismissInsufficientReal,
                    onClearPendingOp = vm::clearPendingOp,
                )
            }

            composable(
                Routes.SETTINGS,
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else fadeIn(tween(330, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = LinearOutSlowInEasing)) { -it / 8 } +
                        scaleIn(tween(330, easing = LinearOutSlowInEasing), initialScale = 0.96f)
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(230, easing = FastOutLinearInEasing)) { it / 8 } +
                        scaleOut(tween(220, easing = FastOutLinearInEasing), targetScale = 0.98f)
                },
            ) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MahfaztyApp
                        SettingsViewModel(
                            app.container.walletRepository,
                            app.container.settingsRepository,
                            app.container.clientsRepository,
                        )
                    }
                })
                SettingsScreen(vm = vm)
            }
        }
    }
}

// ============ الشريط السفلي ============

private data class BottomItem(val route: String, val icon: String, val label: String)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "🏠", "الرئيسية"),
    BottomItem(Routes.TRANSACTIONS, "📜", "العمليات"),
    BottomItem(Routes.GOALS, "🎯", "الأهداف"),
    BottomItem(Routes.SAVINGS, "🐷", "الادخار"),
    BottomItem(Routes.CLIENTS, "👥", "العملاء"),
    BottomItem(Routes.SETTINGS, "⚙️", "الإعدادات"),
)

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        bottomItems.forEach { item ->
            val selected = currentRoute == item.route
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.18f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "navIcon",
            )
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Text(item.icon, fontSize = 20.sp, modifier = Modifier.scale(iconScale)) },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
