package com.parthjivani.ledgerly.ui

import org.json.JSONObject
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

    // Sync threshold: skip Supabase startup pull if data was synced within last 30 minutes
    private val SYNC_THRESHOLD_MS = 30L * 60L * 1000L

    init {
        loadThemeFromSettings()
        loadCategoriesFromSettings()
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

                val totalOnline = supMembers.size + supCards.size + supTxs.size + supGoals.size + supSavingsGoals.size + supRecurrings.size
                if (totalOnline == 0) {
                    val localMembers = repository.allFamilyMembers.first()
                    val localTxs = repository.allTransactions.first()
                    if (localMembers.isEmpty() && localTxs.isEmpty()) {
                        _supabaseSyncSuccess.value = "Connected to Supabase (Database setup ready)"
                    } else {
                        // Supabase is empty, but local has data. Auto-push to avoid blank state or overwrites
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
                        
                        val localCats = repository.getSetting("categories_list", "Groceries,Entertainment,Bills,Travel,Medical,Other").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val localIncCats = repository.getSetting("income_categories_list", "Salary,Business,Investment,Freelance,Gift,Other").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val catPushList = localCats.map { SupabaseCategory(it, "expense") } + localIncCats.map { SupabaseCategory(it, "income") }
                        if (catPushList.isNotEmpty()) {
                            catPushList.chunked(autoBatchSize).forEach { chunk -> api.upsertCategories(chunk) }
                        }

                        _supabaseSyncSuccess.value = "Synced: Auto-uploaded ${localAllTxs.size} local transactions to your new Supabase database!"
                    }
                } else {
                    // ── Push local data first so offline records are saved to Supabase ──
                    // This ensures any records added while offline are not lost when we
                    // clear local storage and replace it with the server's data.
                    val autoBatchSize = 500
                    val localMembersForPush = repository.allFamilyMembers.first()
                    val localCardsForPush = repository.allCreditCards.first()
                    val localWalletsForPush = repository.allCustomWallets.first()
                    val localTxsForPush = repository.allTransactions.first()
                    val localGoalsForPush = repository.allBudgetGoals.first()
                    val localSavingsGoalsForPush = repository.allSavingsGoals.first()
                    val localRecurringsForPush = repository.allRecurringTransactions.first()

                    if (localMembersForPush.isNotEmpty()) localMembersForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertFamilyMembers(chunk) }
                    if (localCardsForPush.isNotEmpty()) localCardsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertCreditCards(chunk) }
                    if (localWalletsForPush.isNotEmpty()) localWalletsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertCustomWallets(chunk) }
                    if (localTxsForPush.isNotEmpty()) localTxsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertTransactions(chunk) }
                    if (localGoalsForPush.isNotEmpty()) localGoalsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertBudgetGoals(chunk) }
                    if (localSavingsGoalsForPush.isNotEmpty()) localSavingsGoalsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertSavingsGoals(chunk) }
                    if (localRecurringsForPush.isNotEmpty()) localRecurringsForPush.map { it.toSupabase() }.chunked(autoBatchSize).forEach { chunk -> api.upsertRecurringTransactions(chunk) }

                    // ── Now re-fetch from Supabase (which now includes our offline records) ──
                    val mergedTxs = buildList {
                        val pageSize = 1000
                        var offset = 0
                        while (true) {
                            val page = api.getTransactions(range = "$offset-${offset + pageSize - 1}")
                            addAll(page)
                            if (page.size < pageSize) break
                            offset += pageSize
                        }
                    }
                    val mergedMembers = api.getFamilyMembers()
                    val mergedCards = api.getCreditCards()
                    val mergedWallets = api.getCustomWallets()
                    val mergedGoals = api.getBudgetGoals()
                    val mergedSavingsGoals = api.getSavingsGoals()
                    val mergedRecurrings = api.getRecurringTransactions()

                    // ── Replace local DB with the fully merged server state ──
                    repository.clearAllFamilyMembers()
                    repository.clearAllCreditCards()
                    repository.clearAllCustomWallets()
                    repository.clearAllTransactions()
                    repository.clearAllBudgetGoals()
                    repository.clearAllSavingsGoals()
                    repository.clearAllRecurringTransactions()

                    mergedMembers.forEach { repository.insertFamilyMember(it.toRoom()) }
                    mergedCards.forEach { repository.insertCreditCard(it.toRoom()) }
                    mergedWallets.forEach { repository.insertCustomWallet(it.toRoom()) }
                    mergedTxs.forEach { repository.insertTransaction(it.toRoom()) }
                    mergedGoals.forEach { repository.insertBudgetGoal(it.toRoom()) }
                    mergedSavingsGoals.forEach { repository.insertSavingsGoal(it.toRoom()) }
                    mergedRecurrings.forEach { repository.insertRecurringTransaction(it.toRoom()) }

                    if (supCategories.isNotEmpty()) {
                        val newExpCats = supCategories.filter { it.type == "expense" }.map { it.name }.joinToString(",")
                        val newIncCats = supCategories.filter { it.type == "income" }.map { it.name }.joinToString(",")
                        repository.insertSetting("categories_list", newExpCats)
                        repository.insertSetting("income_categories_list", newIncCats)
                        _categories.value = newExpCats.split(",").filter { it.isNotEmpty() }
                        _incomeCategories.value = newIncCats.split(",").filter { it.isNotEmpty() }
                    }

                    // Save sync timestamp to avoid redundant fetches within threshold window
                    repository.insertSetting("last_synced_at", System.currentTimeMillis().toString())

                    val roomTxs = mergedTxs.map { it.toRoom() }
                    val totalExp = roomTxs.filter { it.type == TransactionType.PERSONAL_EXPENSE && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val totalInc = roomTxs.filter { it.type == TransactionType.PERSONAL_INCOME && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val famRec = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.RECEIVED_FROM_THEM }.sumOf { it.amount }
                    val famGiv = roomTxs.filter { it.type == TransactionType.FAMILY_TRANSFER && (it.sourceWallet == WalletType.EXPENSE_WALLET || it.destinationWallet == WalletType.EXPENSE_WALLET) && it.direction == TransactionDirection.GIVEN_TO_THEM }.sumOf { it.amount }
                    val walletSub = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.sourceWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val walletAdd = roomTxs.filter { it.type == TransactionType.WALLET_TRANSFER && it.destinationWallet == WalletType.EXPENSE_WALLET }.sumOf { it.amount }
                    val calculated = totalInc - totalExp + famRec - famGiv - walletSub + walletAdd

                    _supabaseSyncSuccess.value = "Txs: ${roomTxs.size}. Exp: $totalExp, Inc: $totalInc, FamRec: $famRec, FamGiv: $famGiv, WAdd: $walletAdd, WSub: $walletSub. Calculated Balance: $calculated"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                if (refreshSupabaseToken()) {
                    pullFromSupabaseOnStartup(isRetry = true)
                    return
                } else {
                    _supabaseSyncError.value = "Session expired. Please log in again."
                    logout()
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
                val defaults = "Groceries,Entertainment,Bills,Travel,Medical,Other"
                repository.insertSetting("categories_list", defaults)
                _categories.value = defaults.split(",")
            }
            val incCats = repository.getSetting("income_categories_list", "")
            if (incCats.isEmpty()) {
                val defaults = "Salary,Business,Investment,Freelance,Gift,Other"
                repository.insertSetting("income_categories_list", defaults)
                _incomeCategories.value = defaults.split(",")
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
                notes = "Settled up splits",
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
                notes = "Settled up splits",
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
            val tx = Transaction(
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
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertTransactions(listOf(tx.copy(id = id).toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
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
            try {
                SupabaseClient.api?.let { api ->
                    api.upsertTransactions(listOf(tx.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setExpenseWalletColor(colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _expenseWalletColor.value = colorHex
            repository.insertSetting("expense_wallet_color", colorHex)
        }
    }

    fun setCreditCardWalletColor(colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _creditCardWalletColor.value = colorHex
            repository.insertSetting("credit_card_wallet_color", colorHex)
        }
    }

    private fun loadCategoriesFromSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesStr = repository.getSetting("categories_list", "Groceries,Entertainment,Bills,Travel,Medical,Other")
                val list = categoriesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                _categories.value = list

                val incomeStr = repository.getSetting("income_categories_list", "Salary,Business,Investment,Freelance,Gift,Other")
                val incList = incomeStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                _incomeCategories.value = incList
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
                    repository.insertSetting("categories_list", currentList.joinToString(","))
                    _categories.value = currentList
                    try {
                        SupabaseClient.api?.upsertCategories(listOf(SupabaseCategory(trimmed, "expense")))
                    } catch (e: Exception) { e.printStackTrace() }
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
                    repository.insertSetting("categories_list", currentList.joinToString(","))
                    _categories.value = currentList
                    try {
                        SupabaseClient.api?.deleteCategory("eq.${category}", "eq.expense")
                    } catch (e: Exception) { e.printStackTrace() }
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
                    repository.insertSetting("income_categories_list", currentList.joinToString(","))
                    _incomeCategories.value = currentList
                    try {
                        SupabaseClient.api?.upsertCategories(listOf(SupabaseCategory(trimmed, "income")))
                    } catch (e: Exception) { e.printStackTrace() }
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
                    repository.insertSetting("income_categories_list", currentList.joinToString(","))
                    _incomeCategories.value = currentList
                    try {
                        SupabaseClient.api?.deleteCategory("eq.${category}", "eq.income")
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
    fun addSavingsGoal(name: String, targetAmount: Double, currentAmount: Double = 0.0, category: String = "Overall") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sg = SavingsGoal(name = name, targetAmount = targetAmount, currentAmount = currentAmount, category = category)
                val id = repository.insertSavingsGoal(sg)
                SupabaseClient.api?.let { api ->
                    api.upsertSavingsGoals(listOf(sg.copy(id = id).toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSavingsGoal(sg: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertSavingsGoal(sg)
                SupabaseClient.api?.let { api ->
                    api.upsertSavingsGoals(listOf(sg.toSupabase()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSavingsGoal(sg: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSavingsGoal(sg)
                SupabaseClient.api?.let { api ->
                    api.deleteSavingsGoal("eq.${sg.id}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                        repository.insertRecurringTransaction(
                            recurring.copy(
                                lastAppliedDate = now,
                                nextDueDate = nextDue
                            )
                        )
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
                supTxs.forEach { repository.insertTransaction(it.toRoom()) }
                supGoals.forEach { repository.insertBudgetGoal(it.toRoom()) }
                supSavingsGoals.forEach { repository.insertSavingsGoal(it.toRoom()) }
                supRecurrings.forEach { repository.insertRecurringTransaction(it.toRoom()) }

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

                _supabaseSyncSuccess.value = "Successfully pushed to Supabase: ${localTxs.size} transactions, ${localMembers.size} family members, ${localCards.size} cards, ${localGoals.size} goals, ${localRecurrings.size} recurring items!"
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is retrofit2.HttpException && e.code() == 401 && !isRetry) {
                    if (refreshSupabaseToken()) {
                        pushToSupabase(isRetry = true)
                        return@launch
                    } else {
                        _supabaseSyncError.value = "Session expired. Please log in again."
                        logout()
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

    fun updateUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSetting("user_profile_name", name)
            _userName.value = name
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

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            SupabaseClient.accessToken = null
            SupabaseClient.refreshToken = null
            repository.clearAllLocalData()
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