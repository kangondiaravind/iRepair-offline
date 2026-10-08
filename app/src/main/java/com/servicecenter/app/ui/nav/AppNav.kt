package com.servicecenter.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.servicecenter.app.data.auth.AuthRepository
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.dao.RolePermissionDao
import com.servicecenter.app.data.local.entity.StaffEntity
import com.servicecenter.app.ui.auth.LoginScreen
import com.servicecenter.app.ui.auth.OwnerSetupScreen
import com.servicecenter.app.ui.common.LocalPermissions
import com.servicecenter.app.ui.common.can
import com.servicecenter.app.ui.customer.CustomerScreen
import com.servicecenter.app.ui.customer.NewCustomerScreen
import com.servicecenter.app.ui.enquiry.EnquiriesScreen
import com.servicecenter.app.ui.enquiry.NewEnquiryScreen
import com.servicecenter.app.ui.home.HomeScreen
import com.servicecenter.app.ui.job.JobDetailScreen
import com.servicecenter.app.ui.job.JobsScreen
import com.servicecenter.app.ui.job.NewJobScreen
import com.servicecenter.app.ui.reports.ReportsScreen
import com.servicecenter.app.ui.staff.StaffScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Who is logged in, what they may do, and whether the first-launch owner setup is still needed. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
    session: SessionManager,
    permissionDao: RolePermissionDao
) : ViewModel() {
    val current: StateFlow<StaffEntity?> = session.current

    @OptIn(ExperimentalCoroutinesApi::class)
    val permissions: StateFlow<Set<String>> = session.current
        .flatMapLatest { staff ->
            if (staff == null) flowOf(emptySet())
            else permissionDao.observeAllowed(staff.role).map { it.toSet() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** null while loading. */
    var hasOwner by mutableStateOf<Boolean?>(null)
        private set

    init {
        refreshOwner()
    }

    fun refreshOwner() {
        viewModelScope.launch { hasOwner = auth.hasOwner() }
    }

    fun logout() = auth.logout()
}

object Routes {
    const val HOME = "home"
    const val JOBS = "jobs"
    const val ENQUIRIES = "enquiries"
    const val REPORTS = "reports"
    const val STAFF = "staff"
    const val NEW_ENQUIRY = "enquiry/new"

    const val NEW_CUSTOMER = "newCustomer?enquiryId={enquiryId}"
    fun newCustomer(enquiryId: String?) =
        if (enquiryId == null) "newCustomer" else "newCustomer?enquiryId=$enquiryId"

    const val CUSTOMER = "customer/{customerId}"
    fun customer(id: String) = "customer/$id"

    const val NEW_JOB = "newJob/{customerId}?enquiryId={enquiryId}"
    fun newJob(customerId: String, enquiryId: String? = null) =
        if (enquiryId == null) "newJob/$customerId" else "newJob/$customerId?enquiryId=$enquiryId"

    const val JOB = "job/{jobId}"
    fun job(id: String) = "job/$id"
}

/** Decides which of the four states the app is in: loading, owner setup, PIN login, or the main app. */
@Composable
fun AppRoot(session: SessionViewModel = hiltViewModel()) {
    val staff by session.current.collectAsStateWithLifecycle()
    val permissions by session.permissions.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalPermissions provides permissions) {
        when {
            session.hasOwner == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            session.hasOwner == false -> OwnerSetupScreen(onDone = { session.refreshOwner() })
            staff == null -> LoginScreen()
            else -> MainNav(onLogout = { session.logout() })
        }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MainNav(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val canSeeReports = can(Permissions.VIEW_REPORTS)

    val tabs = buildList {
        add(BottomTab(Routes.HOME, "Home", Icons.Default.Home))
        add(BottomTab(Routes.JOBS, "Jobs", Icons.AutoMirrored.Filled.List))
        add(BottomTab(Routes.ENQUIRIES, "Enquiries", Icons.Default.Call))
        if (canSeeReports) add(BottomTab(Routes.REPORTS, "Reports", Icons.Default.DateRange))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenCustomer = { nav.navigate(Routes.customer(it)) },
                    onNewCustomer = { nav.navigate(Routes.newCustomer(null)) },
                    onNewEnquiry = { nav.navigate(Routes.NEW_ENQUIRY) },
                    onOpenJob = { nav.navigate(Routes.job(it)) },
                    onStaff = { nav.navigate(Routes.STAFF) },
                    onLogout = onLogout
                )
            }
            composable(Routes.JOBS) {
                JobsScreen(onOpenJob = { nav.navigate(Routes.job(it)) })
            }
            composable(Routes.ENQUIRIES) {
                EnquiriesScreen(
                    onNew = { nav.navigate(Routes.NEW_ENQUIRY) },
                    onConvertExisting = { customerId, enquiryId -> nav.navigate(Routes.newJob(customerId, enquiryId)) },
                    onConvertNew = { enquiryId -> nav.navigate(Routes.newCustomer(enquiryId)) }
                )
            }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.STAFF) { StaffScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.NEW_ENQUIRY) { NewEnquiryScreen(onBack = { nav.popBackStack() }) }

            composable(
                Routes.NEW_CUSTOMER,
                arguments = listOf(navArgument("enquiryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
                NewCustomerScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { customerId, enquiryId ->
                        nav.navigate(Routes.newJob(customerId, enquiryId)) {
                            popUpTo(Routes.NEW_CUSTOMER) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                Routes.CUSTOMER,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) {
                CustomerScreen(
                    onBack = { nav.popBackStack() },
                    onNewJob = { nav.navigate(Routes.newJob(it)) },
                    onOpenJob = { nav.navigate(Routes.job(it)) }
                )
            }
            composable(
                Routes.NEW_JOB,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.StringType },
                    navArgument("enquiryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                NewJobScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { jobId ->
                        nav.navigate(Routes.job(jobId)) { popUpTo(Routes.HOME) }
                    }
                )
            }
            composable(
                Routes.JOB,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) {
                JobDetailScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
