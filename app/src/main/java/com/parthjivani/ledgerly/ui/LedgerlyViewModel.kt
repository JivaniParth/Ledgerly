package com.parthjivani.ledgerly.ui

import android.content.Context
import java.io.File
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.parthjivani.ledgerly.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LedgerlyViewModel(private val repository: LedgerlyRepository) : ViewModel() {

    // Hot Flows from Database
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFamilyMembers: StateFlow<List<FamilyMember>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionSplits: StateFlow<List<TransactionSplit>> = repository.allTransactionSplits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Use Lazily so the last value is kept in cache indefinitely — prevents balance showing as
    // 0.0 after the app is killed and reopened (WhileSubscribed resets to initial on re-subscribe)
    val expenseWalletBalance: StateFlow<Double> = repository.expenseWalletBalance
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val savingsWalletBalance: StateFlow<Double> = repository.savingsWalletBalance
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    private val _lastSavedTransactionId = MutableStateFlow<Long?>(null)
    val lastSavedTransactionId: StateFlow<Long?> = _lastSavedTransactionId.asStateFlow()

    fun setLastSavedTransactionId(id: Long?) {
        _lastSavedTransactionId.value = id
    }

    // Current Miliseconds reference for 3-Month budget logic
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())

    // Average Monthly Expense Spending (Budget Projection)
    val averageMonthlyExpenseSpending: StateFlow<Double> = combine(
        allTransactions,
        _currentTimeMillis
    ) { transactions, currentMillis ->
        LedgerlyRepository.calculateAverageMonthlyExpenseSpending(transactions, currentMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Daily Logging Streak — number of consecutive days with at least one transaction logged
    val currentStreak: StateFlow<Int> = allTransactions.map { transactions ->
        calculateLoggingStreak(transactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    // Theme setting: "light", "dark", or "system"
    private val _appTheme = MutableStateFlow("system")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    // ── Transaction Screen Filter State (lives in ViewModel to survive navigation) ──────────────
    private val _txWalletFilter = MutableStateFlow<WalletType?>(null)
    val txWalletFilter: StateFlow<WalletType?> = _txWalletFilter.asStateFlow()

    private val _txCardFilter = MutableStateFlow<Long?>(null)
    val txCardFilter: StateFlow<Long?> = _txCardFilter.asStateFlow()

    private val _txMemberFilter = MutableStateFlow<Long?>(null)
    val txMemberFilter: StateFlow<Long?> = _txMemberFilter.asStateFlow()

    private val _txContactFilter = MutableStateFlow<Long?>(null)
    val txContactFilter: StateFlow<Long?> = _txContactFilter.asStateFlow()

    private val _txCategoryFilter = MutableStateFlow<String?>(null)
    val txCategoryFilter: StateFlow<String?> = _txCategoryFilter.asStateFlow()

    private val _txTypeFilter = MutableStateFlow<TransactionType?>(null)
    val txTypeFilter: StateFlow<TransactionType?> = _txTypeFilter.asStateFlow()

    private val _txCustomWalletFilter = MutableStateFlow<Long?>(null)
    val txCustomWalletFilter: StateFlow<Long?> = _txCustomWalletFilter.asStateFlow()

    private val _txSearchQuery = MutableStateFlow("")
    val txSearchQuery: StateFlow<String> = _txSearchQuery.asStateFlow()

    fun setTxWalletFilter(v: WalletType?) { _txWalletFilter.value = v }
    fun setTxCardFilter(v: Long?) { _txCardFilter.value = v }
    fun setTxMemberFilter(v: Long?) { _txMemberFilter.value = v }
    fun setTxContactFilter(v: Long?) { _txContactFilter.value = v }
    fun setTxCategoryFilter(v: String?) { _txCategoryFilter.value = v }
    fun setTxTypeFilter(v: TransactionType?) { _txTypeFilter.value = v }
    fun setTxCustomWalletFilter(v: Long?) { _txCustomWalletFilter.value = v }
    fun setTxSearchQuery(v: String) { _txSearchQuery.value = v }
    fun clearTxFilters() {
        _txWalletFilter.value = null
        _txCardFilter.value = null
        _txMemberFilter.value = null
        _txContactFilter.value = null
        _txCategoryFilter.value = null
        _txTypeFilter.value = null
        _txCustomWalletFilter.value = null
        _txSearchQuery.value = ""
    }

    val allBudgetGoals: StateFlow<List<BudgetGoal>> = repository.allBudgetGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurringTransactions: StateFlow<List<RecurringTransaction>> = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCards: StateFlow<List<CreditCard>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomWallets: StateFlow<List<CustomWallet>> = repository.allCustomWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _expenseWalletColor = MutableStateFlow<String>("")
    val expenseWalletColor: StateFlow<String> = _expenseWalletColor.asStateFlow()

    private val _creditCardWalletColor = MutableStateFlow<String>("")
    val creditCardWalletColor: StateFlow<String> = _creditCardWalletColor.asStateFlow()

    private val _incomeCategories = MutableStateFlow<List<String>>(emptyList())
    val incomeCategories: StateFlow<List<String>> = _incomeCategories.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow<String>("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // In-App Auto-Updater
    val updateManager = UpdateManager()
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    val downloadState = updateManager.downloadState
    private var downloadedApkFile: File? = null

    // Sync threshold: 10 seconds to allow responsive sync when returning to app while preventing excessive churn
    private val SYNC_THRESHOLD_MS = 10L * 1000L

    init {
        loadThemeFromSettings()
        loadCategoriesFromSettings()
        checkForUpdates(isManual = false)
        viewModelScope.launch(Dispatchers.IO) {
            val savedToken = repository.getSetting("supabase_access_token", "")
            val savedRefreshToken = repository.getSetting("supabase_refresh_token", "")
            if (savedToken.isNotEmpty()) {
                SupabaseClient.accessToken = savedToken
                SupabaseClient.refreshToken = savedRefreshToken
                _isLoggedIn.value = true
            }

            val savedName = repository.getSetting("user_profile_name", "User")
            _userName.value = savedName

            if (SupabaseClient.isConfigured && _isLoggedIn.value) {
                val lastSynced = repository.getSetting("last_synced_at", "0").toLongOrNull() ?: 0L
                val timeSinceSync = System.currentTimeMillis() - lastSynced
                if (timeSinceSync > SYNC_THRESHOLD_MS) {
                    pullFromSupabaseOnStartup()
                }
                // else: local Room DB is fresh enough — skip re-fetch
            } else if (!SupabaseClient.isConfigured) {
                prepopulateIfEmpty()
            }
            checkAndApplyRecurringTransactions()
        }
    }

    private suspend fun refreshSupabaseToken(): Boolean {
        val currentRefreshToken = SupabaseClient.refreshToken
        if (currentRefreshToken.isNullOrEmpty()) return false
        try {
            val authApi = SupabaseClient.authApi ?: return false
            val req = SupabaseRefreshTokenRequest(currentRefreshToken)
            val response = authApi.refreshToken(req)
            if (!response.access_token.isNullOrEmpty()) {
                SupabaseClient.accessToken = response.access_token
                repository.insertSetting("supabase_access_token", response.access_token)
                if (!response.refresh_token.isNullOrEmpty()) {
                    SupabaseClient.refreshToken = response.refresh_token
                    repository.insertSetting("supabase_refresh_token", response.refresh_token)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private suspend fun pullFromSupabaseOnStartup(isRetry: Boolean = false) {
        _supabasePulling.value = true
        _supabaseSyncing.value = true
        _supabaseSyncError.value = null
        _supabaseSyncSuccess.value = null
        try {
            val api = SupabaseClient.api
            if (api != null) {
                // 1. Pull user settings and profile metadata
                pullRemoteSettings()

                // 2. Fetch all online entities
                val supMembers = api.getFamilyMembers()
                val supCards = api.getCreditCards()
                val supCustomWallets = api.getCustomWallets()
                val supTxs = buildList {
                    val pageSize = 1000
                    var offset = 0
                    while (true) {
                        val page = api.getTransactions(range = "$offset-${offset + pageSize - 1}")
                        addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                }
                val supGoals = api.getBudgetGoals()
                val supSavingsGoals = api.getSavingsGoals()
                val supRecurrings = api.getRecurringTransactions()
                val supCategories = try { api.getCategories() } catch (e: Exception) { emptyList() }

                val totalOnline = supMembers.size + supCards.size + supTxs.size + supGoals.size + supSavingsGoals.size + supRecurrings.size + supCategories.size
                if (totalOnline == 0) {
                    val localMembers = repository.allFamilyMembers.first()
                    val localTxs = repository.allTransactions.first()
                    if (localMembers.isEmpty() && localTxs.isEmpty()) {
                        _supabaseSyncSuccess.value = "Connected to Supabase (Database setup ready)"
                    } else {
                        // Supabase is empty, but local has data. Auto-push to seed remote database
                        val autoBatchSize = 500
                        localMembers.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertFamilyMembers(chunk) }
                        val localCards = repository.allCreditCards.first()
                        if (localCards.isNotEmpty()) localCards.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertCreditCards(chunk) }
                        val localWallets = repository.allCustomWallets.first()
                        if (localWallets.isNotEmpty()) localWallets.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertCustomWallets(chunk) }
                        val localAllTxs = repository.allTransactions.first()
                        if (localAllTxs.isNotEmpty()) localAllTxs.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertTransactions(chunk) }
                        val localGoals = repository.allBudgetGoals.first()
                        if (localGoals.isNotEmpty()) localGoals.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertBudgetGoals(chunk) }
                        val localSavingsGoals = repository.allSavingsGoals.first()
                        if (localSavingsGoals.isNotEmpty()) localSavingsGoals.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertSavingsGoals(chunk) }
                        val localRecurrings = repository.allRecurringTransactions.first()
                        if (localRecurrings.isNotEmpty()) localRecurrings.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertRecurringTransactions(chunk) }

                        val catPushList = _categories.value.map { SupabaseCategory(it, "expense") } + _incomeCategories.value.map { SupabaseCategory(it, "income") }
                        if (catPushList.isNotEmpty()) {
                            catPushList.chunked(autoBatchSize).forEach { chunk -> try { api.upsertCategories(chunk) } catch (_: Exception) {} }
                        }
                        pushRemoteSettings()

                        _supabaseSyncSuccess.value = "Synced: Initialized your remote Supabase database with local data!"
                    }
                } else {
                    // Supabase has data: Remote is authoritative for cross-platform state.
                    // To prevent overwriting remote edits made on Web, only push locally created items that DO NOT exist on the server.
                    val autoBatchSize = 500

                    val remoteTxIds = supTxs.map { it.id }.toSet()
                    val localTxs = repository.allTransactions.first()
                    val offlineTxs = localTxs.filter { it.id !in remoteTxIds }
                    if (offlineTxs.isNotEmpty()) {
                        offlineTxs.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertTransactions(chunk) }
                    }

                    val remoteSavingsIds = supSavingsGoals.map { it.id }.toSet()
                    val localSavings = repository.allSavingsGoals.first()
                    val offlineSavings = localSavings.filter { it.id !in remoteSavingsIds }
                    if (offlineSavings.isNotEmpty()) {
                        offlineSavings.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertSavingsGoals(chunk) }
                    }

                    val remoteRecurrIds = supRecurrings.map { it.id }.toSet()
                    val localRecurrs = repository.allRecurringTransactions.first()
                    val offlineRecurrs = localRecurrs.filter { it.id !in remoteRecurrIds }
                    if (offlineRecurrs.isNotEmpty()) {
                        offlineRecurrs.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertRecurringTransactions(chunk) }
                    }

                    // Categories: If remote has categories, adopt remote. If remote is empty, seed from local.
                    if (supCategories.isEmpty()) {
                        val catPushList = _categories.value.map { SupabaseCategory(it, "expense") } + _incomeCategories.value.map { SupabaseCategory(it, "income") }
                        if (catPushList.isNotEmpty()) {
                            catPushList.chunked(autoBatchSize).forEach { chunk -> try { api.upsertCategories(chunk) } catch (_: Exception) {} }
                        }
                    } else {
                        val newExpCats = supCategories.filter { it.type == "expense" }.map { it.name }
                        val newIncCats = supCategories.filter { it.type == "income" }.map { it.name }
                        if (newExpCats.isNotEmpty()) {
                            repository.insertSetting("categories_list", encodeCategories(newExpCats))
                            _categories.value = newExpCats
                        }
                        if (newIncCats.isNotEmpty()) {
                            repository.insertSetting("income_categories_list", encodeCategories(newIncCats))
                            _incomeCategories.value = newIncCats
                        }
                    }

                    // If offline records were uploaded, re-fetch merged lists
                    val finalTxs = if (offlineTxs.isNotEmpty()) {
                        buildList {
                            val pageSize = 1000
                            var offset = 0
                            while (true) {
                                val page = api.getTransactions(range = "$offset-${offset + pageSize - 1}")
                                addAll(page)
                                if (page.size < pageSize) break
                                offset += pageSize
                            }
                        }
                    } else supTxs

                    val finalSavingsGoals = if (offlineSavings.isNotEmpty()) api.getSavingsGoals() else supSavingsGoals
                    val finalRecurrings = if (offlineRecurrs.isNotEmpty()) api.getRecurringTransactions() else supRecurrings

                    // Replace local Room database with authoritative server state
                    repository.clearAllFamilyMembers()
                    repository.clearAllCreditCards()
                    repository.clearAllCustomWallets()
                    repository.clearAllTransactions()
                    repository.clearAllBudgetGoals()
                    repository.clearAllSavingsGoals()
                    repository.clearAllRecurringTransactions()

                    supMembers.forEach { repository.insertFamilyMember(it.toRoom()) }
                    supCards.forEach { repository.insertCreditCard(it.toRoom()) }
                    supCustomWallets.forEach { repository.insertCustomWallet(it.toRoom()) }
                    finalTxs.forEach { repository.insertTransaction(it.toRoom()) }
                    supGoals.forEach { repository.insertBudgetGoal(it.toRoom()) }
                    finalSavingsGoals.forEach { repository.insertSavingsGoal(it.toRoom()) }
                    finalRecurrings.forEach { repository.insertRecurringTransaction(it.toRoom()) }

                    repository.insertSetting("last_synced_at", System.currentTimeMillis().toString())

                    val roomTxs = finalTxs.map { it.toRoom() }
                    val totalExp = roomTxs.filter { it.type == TransactionType.PERSONAL_EXPENSE && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val totalInc = roomTxs.filter { it.type == TransactionType.PERSONAL_INCOME && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val famRec = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.RECEIVED_FROM_THEM }.sumOf { it.amount }
                    val famGiv = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.GIVEN_TO_THEM }.sumOf { it.amount }
                    val walletSub = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val walletAdd = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.destinationWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val calculated = totalInc - totalExp + famRec - famGiv - walletSub + walletAdd

                    _supabaseSyncSuccess.value = "Synced with Supabase! Goals: ${finalSavingsGoals.size}, Txs: ${roomTxs.size}. Balance: $calculated"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is java.io.IOException) {
                _supabaseSyncError.value = "Offline mode: using local database"
            } else if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    pullFromSupabaseOnStartup(isRetry = true)
                    return
                } else {
                    _supabaseSyncError.value = "Session expired. Please log in again."
                }
            } else {
                _supabaseSyncError.value = getErrorMessage(e, "Startup sync failed")
            }
        } finally {
            _supabasePulling.value = false
            _supabaseSyncing.value = false
        }
    }

    private fun loadThemeFromSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val themeStr = repository.getSetting("app_theme", "system")
                _appTheme.value = themeStr
                _expenseWalletColor.value = repository.getSetting("expense_wallet_color", "")
                _creditCardWalletColor.value = repository.getSetting("credit_card_wallet_color", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prepopulateIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val expCats = repository.getSetting("categories_list", "")
            if (expCats.isEmpty()) {
                val defaults = listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other")
                repository.insertSetting("categories_list", encodeCategories(defaults))
                _categories.value = defaults
            }
            val incCats = repository.getSetting("income_categories_list", "")
            if (incCats.isEmpty()) {
                val defaults = listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other")
                repository.insertSetting("income_categories_list", encodeCategories(defaults))
                _incomeCategories.value = defaults
            }
        }
    }

    fun refreshTime() {
        _currentTimeMillis.value = System.currentTimeMillis()
    }

    // Family Member actions
    fun addFamilyMember(name: String, relationship: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val member = FamilyMember(name = name, relationship = relationship)
            val id = repository.insertFamilyMember(member)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertFamilyMembers(listOf(member.copy(id = id).toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateFamilyMember(member: FamilyMember) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFamilyMember(member)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertFamilyMembers(listOf(member.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFamilyMember(member: FamilyMember) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFamilyMember(member)
            try {
                SupabaseClient.api?.let { api ->
                    api.deleteFamilyMember("eq.${member.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Contact actions
    fun addContact(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact = Contact(name = name)
            repository.insertContact(contact)
            // Note: Add contact sync if contacts table is added to Supabase API
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateContact(contact)
            // Note: Add contact sync if contacts table is added to Supabase API
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
            // Note: Add contact sync if contacts table is added to Supabase API
        }
    }

    fun settleUpContact(contactId: Long, walletToReceive: WalletType, customWalletId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // Settle all splits
            val splits = repository.allTransactionSplits.first().filter { it.contactId == contactId && !it.isSettled }
            val amount = splits.sumOf { it.amount }
            if (amount <= 0) return@launch
            
            repository.settleSplitsForContact(contactId)
            
            // Add a RECEIVED_FROM_THEM transaction to the wallet
            addTransaction(
                amount = amount,
                notes = "Settled up splits (Received)",
                type = TransactionType.BORROW_LEND,
                sourceWallet = walletToReceive,
                direction = TransactionDirection.BORROWED,
                contactId = contactId,
                customWalletId = customWalletId
            )
        }
    }

    fun settleUpFamilyMember(familyMemberId: Long, walletToReceive: WalletType, customWalletId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // Settle all splits
            val splits = repository.allTransactionSplits.first().filter { it.familyMemberId == familyMemberId && !it.isSettled }
            val amount = splits.sumOf { it.amount }
            if (amount <= 0) return@launch
            
            repository.settleSplitsForFamilyMember(familyMemberId)
            
            // Add a RECEIVED_FROM_THEM transaction to the wallet
            addTransaction(
                amount = amount,
                notes = "Settled up splits (Received)",
                type = TransactionType.BORROW_LEND,
                sourceWallet = walletToReceive,
                direction = TransactionDirection.BORROWED,
                familyMemberId = familyMemberId,
                customWalletId = customWalletId
            )
        }
    }

    // Credit Card actions
    fun addCreditCard(cardName: String, ownerName: String, limit: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = CreditCard(cardName = cardName, ownerName = ownerName, creditLimit = limit)
            val id = repository.insertCreditCard(card)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertCreditCards(listOf(card.copy(id = id).toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateCreditCard(card: CreditCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCreditCard(card)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertCreditCards(listOf(card.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCreditCard(card)
            try {
                SupabaseClient.api?.let { api ->
                    api.deleteCreditCard("eq.${card.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Custom Wallet actions
    fun addCustomWallet(name: String, color: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wallet = CustomWallet(name = name, color = color)
            val id = repository.insertCustomWallet(wallet)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertCustomWallets(listOf(wallet.copy(id = id).toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateCustomWallet(wallet: CustomWallet) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCustomWallet(wallet)
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertCustomWallets(listOf(wallet.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCustomWallet(wallet: CustomWallet) {
        viewModelScope.launch(Dispatchers.IO) {
            val txs = repository.allTransactions.first().filter {
                (it.customWalletId == wallet.id || it.destinationCustomWalletId == wallet.id) &&
                it.type != TransactionType.WALLET_TRANSFER
            }
            txs.forEach { deleteTransaction(it) }

            repository.deleteCustomWallet(wallet)
            try {
                SupabaseClient.api?.let { api ->
                    api.deleteCustomWallet("eq.${wallet.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateTempWalletBalance(walletId: Long): Double {
        return LedgerlyRepository.calculateTempWalletBalance(allTransactions.value, walletId)
    }

    fun addTransactions(transactions: List<Transaction>) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalTxs = mutableListOf<Transaction>()
            for (t in transactions) {
                val id = repository.insertTransaction(t)
                finalTxs.add(t.copy(id = id))
            }
            try {
                SupabaseClient.api?.let { api ->
                    finalTxs.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                        api.upsertTransactions(chunk)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Transaction actions
    fun addTransaction(
        amount: Double,
        notes: String,
        type: TransactionType,
        sourceWallet: WalletType,
        destinationWallet: WalletType? = null,
        familyMemberId: Long? = null,
        direction: TransactionDirection? = null,
        date: Long = System.currentTimeMillis(),
        category: String = "Other",
        creditCardId: Long? = null,
        customWalletId: Long? = null,
        destinationCustomWalletId: Long? = null,
        contactId: Long? = null,
        splits: List<TransactionSplit> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = System.currentTimeMillis() * 1000L + kotlin.random.Random.nextLong(1000)
            val tx = Transaction(
                id = generatedId,
                amount = amount,
                date = date,
                notes = notes.trim(),
                type = type,
                sourceWallet = sourceWallet,
                destinationWallet = destinationWallet,
                familyMemberId = familyMemberId,
                direction = direction,
                category = category,
                creditCardId = creditCardId,
                customWalletId = customWalletId,
                destinationCustomWalletId = destinationCustomWalletId,
                contactId = contactId
            )
            val id = repository.insertTransaction(tx)
            
            if (splits.isNotEmpty()) {
                val splitsWithTxId = splits.map { it.copy(transactionId = id) }
                repository.insertTransactionSplits(splitsWithTxId)
            }
            
            _lastSavedTransactionId.value = id
            LedgerlyRepository.appContext?.let {
                com.parthjivani.ledgerly.widget.LedgerlyWidgetProvider.sendUpdateBroadcast(it)
            }
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertTransactions(listOf(tx.copy(id = id).toSupabase()))
                } ?: run {
                    LedgerlyRepository.appContext?.let { com.parthjivani.ledgerly.data.SyncWorker.enqueue(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LedgerlyRepository.appContext?.let { com.parthjivani.ledgerly.data.SyncWorker.enqueue(it) }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
            LedgerlyRepository.appContext?.let {
                com.parthjivani.ledgerly.widget.LedgerlyWidgetProvider.sendUpdateBroadcast(it)
            }
            try {
                SupabaseClient.api?.let { api ->
                    api.deleteTransaction("eq.${transaction.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun splitTransactionRetroactively(transactionId: Long, splits: List<TransactionSplit>) {
        viewModelScope.launch(Dispatchers.IO) {
            val splitsWithTxId = splits.map { it.copy(transactionId = transactionId) }
            repository.insertTransactionSplits(splitsWithTxId)
            LedgerlyRepository.appContext?.let {
                com.parthjivani.ledgerly.widget.LedgerlyWidgetProvider.sendUpdateBroadcast(it)
            }
        }
    }

    fun updateTransaction(
        id: Long,
        amount: Double,
        notes: String,
        type: TransactionType,
        sourceWallet: WalletType,
        destinationWallet: WalletType? = null,
        familyMemberId: Long? = null,
        direction: TransactionDirection? = null,
        date: Long = System.currentTimeMillis(),
        category: String = "Other",
        creditCardId: Long? = null,
        customWalletId: Long? = null,
        destinationCustomWalletId: Long? = null,
        contactId: Long? = null,
        splits: List<TransactionSplit> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = Transaction(
                id = id,
                amount = amount,
                date = date,
                notes = notes.trim(),
                type = type,
                sourceWallet = sourceWallet,
                destinationWallet = destinationWallet,
                familyMemberId = familyMemberId,
                direction = direction,
                category = category,
                creditCardId = creditCardId,
                customWalletId = customWalletId,
                destinationCustomWalletId = destinationCustomWalletId,
                contactId = contactId
            )
            val savedId = repository.insertTransaction(tx)
            _lastSavedTransactionId.value = savedId
            LedgerlyRepository.appContext?.let {
                com.parthjivani.ledgerly.widget.LedgerlyWidgetProvider.sendUpdateBroadcast(it)
            }
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertTransactions(listOf(tx.toSupabase()))
                } ?: run {
                    LedgerlyRepository.appContext?.let { com.parthjivani.ledgerly.data.SyncWorker.enqueue(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LedgerlyRepository.appContext?.let { com.parthjivani.ledgerly.data.SyncWorker.enqueue(it) }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllTransactions()
                repository.clearAllFamilyMembers()
                repository.clearAllCreditCards()
                repository.clearAllCustomWallets()
                repository.clearAllBudgetGoals()
                repository.clearAllSavingsGoals()
                repository.clearAllRecurringTransactions()
                repository.clearAllContacts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Settings actions
    fun setAppTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _appTheme.value = theme
                repository.insertSetting("app_theme", theme)
                pushRemoteSettings(theme = theme)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setExpenseWalletColor(colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _expenseWalletColor.value = colorHex
            repository.insertSetting("expense_wallet_color", colorHex)
            pushRemoteSettings(expenseColor = colorHex)
        }
    }

    fun setCreditCardWalletColor(colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _creditCardWalletColor.value = colorHex
            repository.insertSetting("credit_card_wallet_color", colorHex)
            pushRemoteSettings(cardColor = colorHex)
        }
    }

    private suspend fun pullRemoteSettings() {
        try {
            val authApi = SupabaseClient.authApi ?: return
            val responseBody = authApi.getUser()
            val jsonStr = responseBody.string()
            val userObj = JSONObject(jsonStr)
            val metadata = userObj.optJSONObject("user_metadata")
            if (metadata != null) {
                val remoteTheme = metadata.optString("app_theme", "")
                if (remoteTheme.isNotEmpty() && remoteTheme != _appTheme.value) {
                    _appTheme.value = remoteTheme
                    repository.insertSetting("app_theme", remoteTheme)
                }
                val remoteExpColor = metadata.optString("expense_wallet_color", "")
                if (remoteExpColor.isNotEmpty() && remoteExpColor != _expenseWalletColor.value) {
                    _expenseWalletColor.value = remoteExpColor
                    repository.insertSetting("expense_wallet_color", remoteExpColor)
                }
                val remoteCardColor = metadata.optString("credit_card_wallet_color", "")
                if (remoteCardColor.isNotEmpty() && remoteCardColor != _creditCardWalletColor.value) {
                    _creditCardWalletColor.value = remoteCardColor
                    repository.insertSetting("credit_card_wallet_color", remoteCardColor)
                }
                val remoteName = metadata.optString("name", "")
                if (remoteName.isNotEmpty() && remoteName != _userName.value) {
                    _userName.value = remoteName
                    repository.insertSetting("user_profile_name", remoteName)
                }
            }
        } catch (_: Exception) {
            // Fallback to app_settings table
            try {
                val api = SupabaseClient.api ?: return
                val settings = api.getAppSettings()
                for (s in settings) {
                    when (s.key) {
                        "app_theme" -> {
                            if (s.value.isNotEmpty()) {
                                _appTheme.value = s.value
                                repository.insertSetting("app_theme", s.value)
                            }
                        }
                        "expense_wallet_color" -> {
                            if (s.value.isNotEmpty()) {
                                _expenseWalletColor.value = s.value
                                repository.insertSetting("expense_wallet_color", s.value)
                            }
                        }
                        "credit_card_wallet_color" -> {
                            if (s.value.isNotEmpty()) {
                                _creditCardWalletColor.value = s.value
                                repository.insertSetting("credit_card_wallet_color", s.value)
                            }
                        }
                        "user_profile_name" -> {
                            if (s.value.isNotEmpty()) {
                                _userName.value = s.value
                                repository.insertSetting("user_profile_name", s.value)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun pushRemoteSettings(
        theme: String? = null,
        expenseColor: String? = null,
        cardColor: String? = null,
        userName: String? = null,
        isRetry: Boolean = false
    ) {
        val targetTheme = theme ?: _appTheme.value
        val targetExpColor = expenseColor ?: _expenseWalletColor.value
        val targetCardColor = cardColor ?: _creditCardWalletColor.value
        val targetName = userName ?: _userName.value

        try {
            val authApi = SupabaseClient.authApi
            if (authApi != null) {
                val dataObj = JSONObject().apply {
                    put("app_theme", targetTheme)
                    put("expense_wallet_color", targetExpColor)
                    put("credit_card_wallet_color", targetCardColor)
                    put("name", targetName)
                }
                val bodyObj = JSONObject().apply {
                    put("data", dataObj)
                }
                val requestBody = bodyObj.toString().toRequestBody("application/json".toMediaType())
                authApi.updateUserRaw(requestBody)
            }
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    pushRemoteSettings(theme, expenseColor, cardColor, userName, isRetry = true)
                    return
                }
            }
        }

        try {
            val api = SupabaseClient.api ?: return
            val list = listOf(
                SupabaseAppSetting("app_theme", targetTheme),
                SupabaseAppSetting("expense_wallet_color", targetExpColor),
                SupabaseAppSetting("credit_card_wallet_color", targetCardColor),
                SupabaseAppSetting("user_profile_name", targetName)
            )
            api.upsertAppSettings(list)
        } catch (_: Exception) {}
    }

    private fun encodeCategories(list: List<String>): String {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun decodeCategories(raw: String, defaults: List<String>): List<String> {
        if (raw.isBlank()) return defaults
        if (raw.startsWith("[")) {
            return try {
                val arr = org.json.JSONArray(raw)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                if (list.isNotEmpty()) list else defaults
            } catch (e: Exception) {
                defaults
            }
        }
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun loadCategoriesFromSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val defExp = listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other")
                val categoriesStr = repository.getSetting("categories_list", "")
                _categories.value = decodeCategories(categoriesStr, defExp)

                val defInc = listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other")
                val incomeStr = repository.getSetting("income_categories_list", "")
                _incomeCategories.value = decodeCategories(incomeStr, defInc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmed = category.trim()
                if (trimmed.isEmpty()) return@launch
                val currentList = _categories.value.toMutableList()
                if (!currentList.any { it.equals(trimmed, ignoreCase = true) }) {
                    currentList.add(trimmed)
                    repository.insertSetting("categories_list", encodeCategories(currentList))
                    _categories.value = currentList
                    pushCategoryWithRetry(trimmed, "expense")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = _categories.value.toMutableList()
                if (currentList.contains(category)) {
                    currentList.remove(category)
                    repository.insertSetting("categories_list", encodeCategories(currentList))
                    _categories.value = currentList
                    deleteCategoryWithRetry(category, "expense")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addIncomeCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmed = category.trim()
                if (trimmed.isEmpty()) return@launch
                val currentList = _incomeCategories.value.toMutableList()
                if (!currentList.any { it.equals(trimmed, ignoreCase = true) }) {
                    currentList.add(trimmed)
                    repository.insertSetting("income_categories_list", encodeCategories(currentList))
                    _incomeCategories.value = currentList
                    pushCategoryWithRetry(trimmed, "income")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteIncomeCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = _incomeCategories.value.toMutableList()
                if (currentList.contains(category)) {
                    currentList.remove(category)
                    repository.insertSetting("income_categories_list", encodeCategories(currentList))
                    _incomeCategories.value = currentList
                    deleteCategoryWithRetry(category, "income")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun pushCategoryWithRetry(name: String, type: String, isRetry: Boolean = false) {
        try {
            SupabaseClient.api?.upsertCategories(listOf(SupabaseCategory(name, type)))
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    pushCategoryWithRetry(name, type, isRetry = true)
                }
            } else {
                LedgerlyRepository.appContext?.let { SyncWorker.enqueue(it) }
            }
        }
    }

    private suspend fun deleteCategoryWithRetry(name: String, type: String, isRetry: Boolean = false) {
        try {
            SupabaseClient.api?.deleteCategory("eq.${name}", "eq.${type}")
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    deleteCategoryWithRetry(name, type, isRetry = true)
                }
            }
        }
    }

    // Budget actions
    fun addBudgetGoal(category: String, limitAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bg = BudgetGoal(category = category, limitAmount = limitAmount)
                repository.insertBudgetGoal(bg)
                SupabaseClient.api?.let { api ->
                    api.upsertBudgetGoals(listOf(bg.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteBudgetGoal(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteBudgetGoalByCategory(category)
                SupabaseClient.api?.let { api ->
                    api.deleteBudgetGoal("eq.$category")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllBudgetGoals() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllBudgetGoals()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Savings Goal actions
    fun depositToSavingsGoal(goal: SavingsGoal, amount: Double, sourceWallet: WalletType = WalletType.EXPENSE_WALLET) {
        viewModelScope.launch(Dispatchers.IO) {
            val roundedAmt = Math.round(amount * 100.0) / 100.0
            if (roundedAmt <= 0) return@launch
            try {
                val updatedGoal = goal.copy(currentAmount = goal.currentAmount + roundedAmt)
                repository.insertSavingsGoal(updatedGoal)

                val txId = System.currentTimeMillis() * 1000L + kotlin.random.Random.nextLong(1000)
                val transferTx = Transaction(
                    id = txId,
                    amount = roundedAmt,
                    date = System.currentTimeMillis(),
                    notes = "Savings Goal Deposit: ${goal.name}",
                    type = TransactionType.WALLET_TRANSFER,
                    sourceWallet = sourceWallet,
                    destinationWallet = WalletType.SAVINGS_WALLET,
                    category = "Savings"
                )
                repository.insertTransaction(transferTx)

                pushSavingsGoalWithRetry(updatedGoal)
                try {
                    SupabaseClient.api?.upsertTransactions(listOf(transferTx.toSupabase()))
                } catch (_: Exception) {
                    LedgerlyRepository.appContext?.let { SyncWorker.enqueue(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun withdrawFromSavingsGoal(goal: SavingsGoal, amount: Double, destinationWallet: WalletType = WalletType.EXPENSE_WALLET) {
        viewModelScope.launch(Dispatchers.IO) {
            val roundedAmt = Math.round(amount * 100.0) / 100.0
            if (roundedAmt <= 0) return@launch
            try {
                val newCurrent = maxOf(0.0, goal.currentAmount - roundedAmt)
                val updatedGoal = goal.copy(currentAmount = newCurrent)
                repository.insertSavingsGoal(updatedGoal)

                val txId = System.currentTimeMillis() * 1000L + kotlin.random.Random.nextLong(1000)
                val transferTx = Transaction(
                    id = txId,
                    amount = roundedAmt,
                    date = System.currentTimeMillis(),
                    notes = "Savings Goal Withdrawal: ${goal.name}",
                    type = TransactionType.WALLET_TRANSFER,
                    sourceWallet = WalletType.SAVINGS_WALLET,
                    destinationWallet = destinationWallet,
                    category = "Savings"
                )
                repository.insertTransaction(transferTx)

                pushSavingsGoalWithRetry(updatedGoal)
                try {
                    SupabaseClient.api?.upsertTransactions(listOf(transferTx.toSupabase()))
                } catch (_: Exception) {
                    LedgerlyRepository.appContext?.let { SyncWorker.enqueue(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSavingsGoal(name: String, targetAmount: Double, currentAmount: Double = 0.0, category: String = "Overall") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val generatedId = System.currentTimeMillis() * 1000L + kotlin.random.Random.nextLong(1000)
                val sg = SavingsGoal(id = generatedId, name = name, targetAmount = targetAmount, currentAmount = currentAmount, category = category)
                repository.insertSavingsGoal(sg)
                pushSavingsGoalWithRetry(sg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSavingsGoal(sg: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertSavingsGoal(sg)
                pushSavingsGoalWithRetry(sg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSavingsGoal(sg: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSavingsGoal(sg)
                deleteSavingsGoalWithRetry(sg.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun pushSavingsGoalWithRetry(sg: SavingsGoal, isRetry: Boolean = false) {
        try {
            SupabaseClient.api?.upsertSavingsGoals(listOf(sg.toSupabase()))
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    pushSavingsGoalWithRetry(sg, isRetry = true)
                }
            } else {
                LedgerlyRepository.appContext?.let { SyncWorker.enqueue(it) }
            }
        }
    }

    private suspend fun deleteSavingsGoalWithRetry(id: Long, isRetry: Boolean = false) {
        try {
            SupabaseClient.api?.deleteSavingsGoal("eq.$id")
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    deleteSavingsGoalWithRetry(id, isRetry = true)
                }
            }
        }
    }

    // Recurring actions
    fun addRecurringTransaction(
        amount: Double,
        notes: String,
        type: TransactionType,
        sourceWallet: WalletType,
        category: String,
        intervalType: String,
        nextDueDate: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val item = RecurringTransaction(
                    amount = amount,
                    notes = notes.trim(),
                    type = type,
                    sourceWallet = sourceWallet,
                    category = category,
                    intervalType = intervalType,
                    lastAppliedDate = now,
                    nextDueDate = nextDueDate,
                    isActive = true
                )
                val id = repository.insertRecurringTransaction(item)
                try {
                    SupabaseClient.api?.let { api ->
                        api.upsertRecurringTransactions(listOf(item.copy(id = id).toSupabase()))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Trigger immediate check in case it's due now
                checkAndApplyRecurringTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRecurringTransaction(
        id: Long,
        amount: Double,
        notes: String,
        type: TransactionType,
        sourceWallet: WalletType,
        category: String,
        intervalType: String,
        nextDueDate: Long,
        updatePastPayments: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.allRecurringTransactions.first().find { it.id == id } ?: return@launch
                val updatedRec = existing.copy(
                    amount = amount,
                    notes = notes.trim(),
                    type = type,
                    sourceWallet = sourceWallet,
                    category = category,
                    intervalType = intervalType,
                    nextDueDate = nextDueDate
                )
                repository.insertRecurringTransaction(updatedRec)

                if (updatePastPayments) {
                    val allTxs = repository.allTransactions.first()
                    val pastTxs = allTxs.filter { it.recurringTransactionId == id }
                    val updatedTxs = pastTxs.map { 
                        it.copy(
                            amount = amount,
                            notes = "${notes.trim()} (Recurring)",
                            type = type,
                            sourceWallet = sourceWallet,
                            category = category
                        )
                    }
                    for (t in updatedTxs) {
                        repository.insertTransaction(t)
                    }
                    SupabaseClient.api?.let { api ->
                        updatedTxs.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                            api.upsertTransactions(chunk)
                        }
                    }
                }

                SupabaseClient.api?.let { api ->
                    api.upsertRecurringTransactions(listOf(updatedRec.toSupabase()))
                }

                checkAndApplyRecurringTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecurringTransaction(recurring: RecurringTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteRecurringTransaction(recurring)
                SupabaseClient.api?.let { api ->
                    api.deleteRecurringTransaction("eq.${recurring.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllRecurringTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllRecurringTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAndApplyRecurringTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeRecurring = repository.getActiveRecurringTransactions()
                val now = System.currentTimeMillis()
                for (recurring in activeRecurring) {
                    var nextDue = recurring.nextDueDate
                    var updated = false
                    while (nextDue <= now) {
                        repository.insertTransaction(
                            Transaction(
                                amount = recurring.amount,
                                date = nextDue,
                                notes = "${recurring.notes} (Recurring)",
                                type = recurring.type,
                                sourceWallet = recurring.sourceWallet,
                                category = recurring.category,
                                recurringTransactionId = recurring.id
                            )
                        )
                        nextDue = calculateNextDueDate(nextDue, recurring.intervalType)
                        updated = true
                    }
                    if (updated) {
                        val updatedRec = recurring.copy(
                            lastAppliedDate = now,
                            nextDueDate = nextDue
                        )
                        repository.insertRecurringTransaction(updatedRec)
                        try {
                            SupabaseClient.api?.let { api ->
                                api.upsertRecurringTransactions(listOf(updatedRec.toSupabase()))
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateNextDueDate(currentDue: Long, intervalType: String): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = currentDue
        when (intervalType) {
            "Daily" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> cal.add(java.util.Calendar.MONTH, 1)
            else -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates how many consecutive days (going back from today) have at least one transaction.
     * Any transaction type counts (expense, income, transfer).
     */
    private fun calculateLoggingStreak(transactions: List<Transaction>): Int {
        if (transactions.isEmpty()) return 0
        val tz = java.util.TimeZone.getDefault()
        // Collect unique calendar day keys (year * 10000 + dayOfYear) in local timezone
        val loggedDays = transactions.map { tx ->
            val cal = java.util.Calendar.getInstance(tz)
            cal.timeInMillis = tx.date
            // Use year + dayOfYear to handle year boundaries correctly
            cal.get(java.util.Calendar.YEAR) * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
        }.toHashSet()

        val today = java.util.Calendar.getInstance(tz)
        val todayKey = today.get(java.util.Calendar.YEAR) * 1000 + today.get(java.util.Calendar.DAY_OF_YEAR)

        // Streak must start from today or yesterday (user may not have logged yet today)
        val startKey = when {
            loggedDays.contains(todayKey) -> todayKey
            else -> {
                val yesterday = java.util.Calendar.getInstance(tz).also { it.add(java.util.Calendar.DAY_OF_YEAR, -1) }
                val yesterdayKey = yesterday.get(java.util.Calendar.YEAR) * 1000 + yesterday.get(java.util.Calendar.DAY_OF_YEAR)
                if (loggedDays.contains(yesterdayKey)) yesterdayKey else return 0
            }
        }

        var streak = 0
        val checkCal = java.util.Calendar.getInstance(tz)
        // Set to the starting day
        val startDayOffset = todayKey - startKey // 0 if today, 1 if yesterday
        checkCal.add(java.util.Calendar.DAY_OF_YEAR, -startDayOffset)

        while (true) {
            val key = checkCal.get(java.util.Calendar.YEAR) * 1000 + checkCal.get(java.util.Calendar.DAY_OF_YEAR)
            if (!loggedDays.contains(key)) break
            streak++
            checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /**
     * Counts how many of the last [monthsBack] full calendar months had total spending
     * exceeding the "Overall" budget goal limit.
     */
    fun calculateMonthsOverBudget(
        transactions: List<Transaction>,
        budgetGoals: List<BudgetGoal>,
        monthsBack: Int = 6
    ): Int {
        val overallGoal = budgetGoals.find { it.category.equals("Overall", ignoreCase = true)
        } ?: return 0
        if (overallGoal.limitAmount <= 0) return 0

        val tz = java.util.TimeZone.getDefault()
        var overBudgetCount = 0

        for (i in 1..monthsBack) {
            val cal = java.util.Calendar.getInstance(tz)
            cal.add(java.util.Calendar.MONTH, -i)
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis

            val endCal = cal.clone() as java.util.Calendar
            endCal.add(java.util.Calendar.MONTH, 1)
            val monthEnd = endCal.timeInMillis

            val monthSpend = transactions.filter { t ->
                t.date >= monthStart && t.date < monthEnd && (
                    (t.type == TransactionType.PERSONAL_EXPENSE && t.sourceWallet != WalletType.TEMPORARY_WALLET) ||
                    (t.type == TransactionType.FAMILY_TRANSFER && t.direction == TransactionDirection.GIVEN_TO_THEM && t.sourceWallet != WalletType.TEMPORARY_WALLET)
                )
            }.sumOf { it.amount }

            if (monthSpend > overallGoal.limitAmount) overBudgetCount++
        }
        return overBudgetCount
    }

    private val _supabasePulling = MutableStateFlow(false)
    val supabasePulling = _supabasePulling.asStateFlow()

    private val _supabasePushing = MutableStateFlow(false)
    val supabasePushing = _supabasePushing.asStateFlow()

    private val _supabaseSyncing = MutableStateFlow(false)
    val supabaseSyncing = _supabaseSyncing.asStateFlow()

    private val _supabaseSyncError = MutableStateFlow<String?>(null)
    val supabaseSyncError = _supabaseSyncError.asStateFlow()

    private val _supabaseSyncSuccess = MutableStateFlow<String?>(null)
    val supabaseSyncSuccess = _supabaseSyncSuccess.asStateFlow()

    fun resetSupabaseSyncMessages() {
        _supabaseSyncError.value = null
        _supabaseSyncSuccess.value = null
    }

    fun pullFromSupabase(isRetry: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _supabasePulling.value = true
            _supabaseSyncing.value = true
            _supabaseSyncError.value = null
            _supabaseSyncSuccess.value = null
            try {
                val api = SupabaseClient.api
                if (api == null) {
                    _supabaseSyncError.value = "Supabase API was not initialized. Verify your URL and Key configuration."
                    _supabasePulling.value = false
                    _supabaseSyncing.value = false
                    return@launch
                }

                val supMembers = api.getFamilyMembers()
                val supCards = api.getCreditCards()
                val supCustomWallets = api.getCustomWallets()
                val supTxs = buildList {
                    val pageSize = 1000
                    var offset = 0
                    while (true) {
                        val page = api.getTransactions(range = "$offset-${offset + pageSize - 1}")
                        addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                }
                val supGoals = api.getBudgetGoals()
                val supSavingsGoals = api.getSavingsGoals()
                val supRecurrings = api.getRecurringTransactions()

                val totalOnlineCount = supMembers.size + supCards.size + supTxs.size + supGoals.size + supSavingsGoals.size + supRecurrings.size
                if (totalOnlineCount == 0) {
                    val localMembers = repository.allFamilyMembers.first()
                    val localTxs = repository.allTransactions.first()
                    if (localMembers.isNotEmpty() || localTxs.isNotEmpty()) {
                        _supabaseSyncError.value = "Your Supabase database is completely empty. To upload your current local data, click 'Push / Upload' instead! Pull was cancelled to protect your local data."
                        _supabasePulling.value = false
                        _supabaseSyncing.value = false
                        return@launch
                    }
                }

                val pendingDeletes = repository.getPendingDeletedTransactionIds()
                for (delId in pendingDeletes) {
                    try { api.deleteTransaction("eq.$delId") } catch (_: Exception) {}
                }
                val filteredSupTxs = supTxs.filter { it.id !in pendingDeletes }

                repository.clearAllFamilyMembers()
                repository.clearAllCreditCards()
                repository.clearAllCustomWallets()
                repository.clearAllTransactions()
                repository.clearAllBudgetGoals()
                repository.clearAllSavingsGoals()
                repository.clearAllRecurringTransactions()

                supMembers.forEach { repository.insertFamilyMember(it.toRoom()) }
                supCards.forEach { repository.insertCreditCard(it.toRoom()) }
                supCustomWallets.forEach { repository.insertCustomWallet(it.toRoom()) }
                filteredSupTxs.forEach { repository.insertTransaction(it.toRoom()) }
                supGoals.forEach { repository.insertBudgetGoal(it.toRoom()) }
                supSavingsGoals.forEach { repository.insertSavingsGoal(it.toRoom()) }
                supRecurrings.forEach { repository.insertRecurringTransaction(it.toRoom()) }

                val supCategories = try { api.getCategories() } catch (e: Exception) { emptyList() }
                if (supCategories.isNotEmpty()) {
                    val newExpCats = supCategories.filter { it.type == "expense" }.map { it.name }
                    val newIncCats = supCategories.filter { it.type == "income" }.map { it.name }
                    if (newExpCats.isNotEmpty()) {
                        repository.insertSetting("categories_list", encodeCategories(newExpCats))
                        _categories.value = newExpCats
                    }
                    if (newIncCats.isNotEmpty()) {
                        repository.insertSetting("income_categories_list", encodeCategories(newIncCats))
                        _incomeCategories.value = newIncCats
                    }
                }
                pullRemoteSettings()

                // Update sync timestamp
                repository.insertSetting("last_synced_at", System.currentTimeMillis().toString())

                val roomTxs = supTxs.map { it.toRoom() }
                val totalExp = roomTxs.filter { it.type == TransactionType.PERSONAL_EXPENSE && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                val totalInc = roomTxs.filter { it.type == TransactionType.PERSONAL_INCOME && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                val famRec = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.RECEIVED_FROM_THEM }.sumOf { it.amount }
                val famGiv = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.GIVEN_TO_THEM }.sumOf { it.amount }
                val walletSub = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                val walletAdd = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.destinationWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                val calculated = totalInc - totalExp + famRec - famGiv - walletSub + walletAdd

                _supabaseSyncSuccess.value = "Txs: ${roomTxs.size}. Exp: $totalExp, Inc: $totalInc, FamRec: $famRec, FamGiv: $famGiv, WAdd: $walletAdd, WSub: $walletSub. Calculated Balance: $calculated"
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                    if (refreshSupabaseToken()) {
                        pullFromSupabase(isRetry = true)
                        return@launch
                    } else {
                        _supabaseSyncError.value = "Session expired. Please log in again."
                        logout()
                    }
                } else {
                    _supabaseSyncError.value = getErrorMessage(e, "Download failed")
                }
            } finally {
                _supabasePulling.value = false
                _supabaseSyncing.value = false
            }
        }
    }

    fun pushToSupabase(isRetry: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _supabasePushing.value = true
            _supabaseSyncing.value = true
            _supabaseSyncError.value = null
            _supabaseSyncSuccess.value = null
            try {
                val api = SupabaseClient.api
                if (api == null) {
                    _supabaseSyncError.value = "Supabase API was not initialized. Verify your URL and Key configuration."
                    _supabasePushing.value = false
                    _supabaseSyncing.value = false
                    return@launch
                }

                val localMembers = repository.allFamilyMembers.first()
                val localCards = repository.allCreditCards.first()
                val localCustomWallets = repository.allCustomWallets.first()
                val localTxs = repository.allTransactions.first()
                val localGoals = repository.allBudgetGoals.first()
                val localSavingsGoals = repository.allSavingsGoals.first()
                val localRecurrings = repository.allRecurringTransactions.first()

                val batchSize = 500
                if (localMembers.isNotEmpty()) {
                    localMembers.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertFamilyMembers(chunk)
                    }
                }
                if (localCards.isNotEmpty()) {
                    localCards.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertCreditCards(chunk)
                    }
                }
                if (localCustomWallets.isNotEmpty()) {
                    localCustomWallets.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertCustomWallets(chunk)
                    }
                }
                if (localTxs.isNotEmpty()) {
                    localTxs.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertTransactions(chunk)
                    }
                }
                if (localGoals.isNotEmpty()) {
                    localGoals.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertBudgetGoals(chunk)
                    }
                }
                if (localSavingsGoals.isNotEmpty()) {
                    localSavingsGoals.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertSavingsGoals(chunk)
                    }
                }
                if (localRecurrings.isNotEmpty()) {
                    localRecurrings.map { it.toSupabase() }.chunked(batchSize).forEach { chunk ->
                        api.upsertRecurringTransactions(chunk)
                    }
                }

                val catPushList = _categories.value.map { SupabaseCategory(it, "expense") } + _incomeCategories.value.map { SupabaseCategory(it, "income") }
                if (catPushList.isNotEmpty()) {
                    catPushList.chunked(batchSize).forEach { chunk ->
                        try { api.upsertCategories(chunk) } catch (_: Exception) {}
                    }
                }
                pushRemoteSettings()

                _supabaseSyncSuccess.value = "Successfully pushed to Supabase: ${localTxs.size} transactions, ${localMembers.size} family members, ${localCards.size} cards, ${localGoals.size} goals, ${localSavingsGoals.size} savings goals, ${catPushList.size} categories!"
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is java.io.IOException) {
                    _supabaseSyncError.value = "Network unavailable. Changes saved locally."
                } else if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                    if (refreshSupabaseToken()) {
                        pushToSupabase(isRetry = true)
                        return@launch
                    } else {
                        _supabaseSyncError.value = "Session expired. Please log in again."
                    }
                } else {
                    _supabaseSyncError.value = getErrorMessage(e, "Upload failed")
                }
            } finally {
                _supabasePushing.value = false
                _supabaseSyncing.value = false
            }
        }
    }

    private fun getErrorMessage(e: Throwable, defaultPrefix: String): String {
        if (e is retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
            if (!errorBody.isNullOrEmpty()) {
                val messageVal = try {
                    val json = JSONObject(errorBody)
                    json.optString("message", errorBody)
                } catch (ex: Exception) {
                    errorBody
                }
                return "$defaultPrefix (HTTP ${e.code()}): $messageVal"
            }
            return "$defaultPrefix (HTTP ${e.code()}): ${e.message()}"
        }
        return "$defaultPrefix: ${e.localizedMessage ?: e.toString()}"
    }

    // Auth actions
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authApi = SupabaseClient.authApi
                if (authApi == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Supabase URL or Key not configured.") }
                    return@launch
                }
                val req = SupabaseAuthRequest(email, password)
                val response = authApi.login(req)
                if (!response.access_token.isNullOrEmpty()) {
                    SupabaseClient.accessToken = response.access_token
                    repository.insertSetting("supabase_access_token", response.access_token)
                    if (!response.refresh_token.isNullOrEmpty()) {
                        SupabaseClient.refreshToken = response.refresh_token
                        repository.insertSetting("supabase_refresh_token", response.refresh_token)
                    }
                    _isLoggedIn.value = true
                    pullFromSupabaseOnStartup() // Sync data after login
                    withContext(Dispatchers.Main) { onResult(true, null) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Login failed: No access token returned.") }
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) { onResult(false, "Invalid credentials.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    fun signup(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authApi = SupabaseClient.authApi
                if (authApi == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Supabase URL or Key not configured.") }
                    return@launch
                }
                val req = SupabaseAuthRequest(email, password)
                val response = authApi.signup(req)
                if (!response.access_token.isNullOrEmpty()) {
                    SupabaseClient.accessToken = response.access_token
                    repository.insertSetting("supabase_access_token", response.access_token)
                    if (!response.refresh_token.isNullOrEmpty()) {
                        SupabaseClient.refreshToken = response.refresh_token
                        repository.insertSetting("supabase_refresh_token", response.refresh_token)
                    }
                    _isLoggedIn.value = true
                    withContext(Dispatchers.Main) { onResult(true, null) }
                } else {
                    withContext(Dispatchers.Main) { onResult(true, "Signup successful. Please check your email to verify if required, or try logging in.") }
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) { onResult(false, "Signup failed. User may already exist.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authApi = SupabaseClient.authApi
                if (authApi == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Supabase URL or Key not configured.") }
                    return@launch
                }
                val req = SupabaseRecoverRequest(email.trim())
                val response = authApi.recoverPassword(req)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        onResult(true, "Password reset link sent to your email!")
                    } else {
                        onResult(false, "Failed to send reset link (HTTP ${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Failed to send password reset email.")
                }
            }
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSetting("user_profile_name", name)
            _userName.value = name
            pushRemoteSettings(userName = name)
        }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authApi = SupabaseClient.authApi
                if (authApi == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Supabase API not configured.") }
                    return@launch
                }
                authApi.updateUser(SupabaseUpdateUserRequest(newPassword))
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) { onResult(false, "Failed to update password.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    // In-App Auto-Updater actions
    fun checkForUpdates(isManual: Boolean = false, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = updateManager.checkForUpdates()
            _isCheckingUpdate.value = false
            result.onSuccess { info ->
                _updateInfo.value = info
                if (info.isUpdateAvailable) {
                    _showUpdateDialog.value = true
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(true, "Update found: ${info.latestVersion}")
                    }
                } else {
                    if (isManual) {
                        withContext(Dispatchers.Main) {
                            onResult?.invoke(false, "You're already on the latest version (v${com.parthjivani.ledgerly.BuildConfig.VERSION_NAME})")
                        }
                    }
                }
            }.onFailure { err ->
                if (isManual) {
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, "Could not check for updates: ${err.localizedMessage}")
                    }
                }
            }
        }
    }

    fun startUpdateDownload(context: Context) {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            val file = updateManager.downloadUpdate(context, info.downloadUrl)
            if (file != null) {
                downloadedApkFile = file
            }
        }
    }

    fun installDownloadedApk(context: Context) {
        val file = downloadedApkFile
        if (file != null && file.exists()) {
            updateManager.installApk(context, file)
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        updateManager.resetState()
    }

    fun openUpdateDialog() {
        if (_updateInfo.value?.isUpdateAvailable == true) {
            _showUpdateDialog.value = true
        }
    }

    fun logout(clearLocalData: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            SupabaseClient.accessToken = null
            SupabaseClient.refreshToken = null
            repository.insertSetting("supabase_access_token", "")
            repository.insertSetting("supabase_refresh_token", "")
            if (clearLocalData) {
                repository.clearAllLocalData()
            }
            _isLoggedIn.value = false
        }
    }
}

class LedgerlyViewModelFactory(private val repository: LedgerlyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerlyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LedgerlyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}