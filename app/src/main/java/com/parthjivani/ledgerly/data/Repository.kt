package com.parthjivani.ledgerly.data

import android.content.Context
import com.parthjivani.ledgerly.utils.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LedgerlyRepository(
    private val familyMemberDao: FamilyMemberDao,
    private val transactionDao: TransactionDao,
    private val appSettingDao: AppSettingDao,
    private val budgetGoalDao: BudgetGoalDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val creditCardDao: CreditCardDao,
    private val customWalletDao: CustomWalletDao,
    private val contactDao: ContactDao,
    private val transactionSplitDao: TransactionSplitDao
) {
    val allFamilyMembers: Flow<List<FamilyMember>> = familyMemberDao.getAllFamilyMembers()
    val allContacts: Flow<List<Contact>> = contactDao.getAllContactsFlow()
    val allTransactionSplits: Flow<List<TransactionSplit>> = transactionSplitDao.getAllSplitsFlow()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allCustomWallets: Flow<List<CustomWallet>> = customWalletDao.getAllCustomWalletsFlow()
    val allBudgetGoals: Flow<List<BudgetGoal>> = budgetGoalDao.getAllBudgetGoalsFlow()
    val allSavingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoalsFlow()
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = recurringTransactionDao.getAllRecurringTransactionsFlow()
    val allCreditCards: Flow<List<CreditCard>> = creditCardDao.getAllCreditCardsFlow()

    suspend fun insertCreditCard(card: CreditCard): Long = creditCardDao.insertCreditCard(card)
    suspend fun deleteCreditCard(card: CreditCard) = creditCardDao.deleteCreditCard(card)
    suspend fun clearAllCreditCards() = creditCardDao.clearAllCreditCards()

    suspend fun insertCustomWallet(wallet: CustomWallet): Long = customWalletDao.insertCustomWallet(wallet)
    suspend fun deleteCustomWallet(wallet: CustomWallet) = customWalletDao.deleteCustomWallet(wallet)
    suspend fun clearAllCustomWallets() = customWalletDao.clearAllCustomWallets()

    // Settings helpers
    suspend fun getSetting(key: String, defaultValue: String): String {
        val raw = appSettingDao.getSettingByKey(key)?.value ?: return defaultValue
        return if (key == "supabase_access_token" || key == "supabase_refresh_token") {
            val dec = SecurityUtils.decrypt(raw)
            if (dec.isNotEmpty()) dec else defaultValue
        } else {
            raw
        }
    }

    suspend fun insertSetting(key: String, value: String) {
        val toSave = if (key == "supabase_access_token" || key == "supabase_refresh_token") {
            SecurityUtils.encrypt(value)
        } else {
            value
        }
        appSettingDao.insertSetting(AppSetting(key, toSave))
    }

    // Budget helper functions
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal) {
        budgetGoalDao.insertBudgetGoal(budgetGoal)
    }

    suspend fun deleteBudgetGoalByCategory(category: String) {
        budgetGoalDao.deleteBudgetGoalByCategory(category)
    }

    suspend fun clearAllBudgetGoals() {
        budgetGoalDao.clearAllBudgetGoals()
    }

    // Savings Goals helper functions
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long {
        return savingsGoalDao.insertSavingsGoal(savingsGoal)
    }

    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal) {
        savingsGoalDao.deleteSavingsGoal(savingsGoal)
    }

    suspend fun clearAllSavingsGoals() {
        savingsGoalDao.clearAllSavingsGoals()
    }

    // Recurring helper functions
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
        return recurringTransactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransaction(recurring: RecurringTransaction) {
        recurringTransactionDao.deleteRecurringTransaction(recurring)
    }

    suspend fun getActiveRecurringTransactions(): List<RecurringTransaction> {
        return recurringTransactionDao.getActiveRecurringTransactions()
    }

    suspend fun clearAllRecurringTransactions() {
        recurringTransactionDao.clearAllRecurringTransactions()
    }

    // Expense Wallet Balance: calculated reactively
    val expenseWalletBalance: Flow<Double> = allTransactions.map { transactions ->
        calculateExpenseBalance(transactions)
    }

    // Savings Wallet Balance: calculated reactively
    val savingsWalletBalance: Flow<Double> = allTransactions.map { transactions ->
        calculateSavingsBalance(transactions)
    }

    suspend fun insertFamilyMember(member: FamilyMember): Long =
        familyMemberDao.insertFamilyMember(member)

    suspend fun updateFamilyMember(member: FamilyMember) =
        familyMemberDao.updateFamilyMember(member)

    suspend fun deleteFamilyMember(member: FamilyMember) =
        familyMemberDao.deleteFamilyMember(member)

    suspend fun getFamilyMemberById(id: Long): FamilyMember? =
        familyMemberDao.getFamilyMemberById(id)

    suspend fun clearAllFamilyMembers() =
        familyMemberDao.clearAllFamilyMembers()

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun getPendingDeletedTransactionIds(): Set<Long> {
        val raw = getSetting("pending_deleted_tx_ids", "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    suspend fun addPendingDeletedTransactionId(id: Long) {
        val current = getPendingDeletedTransactionIds().toMutableSet()
        current.add(id)
        insertSetting("pending_deleted_tx_ids", current.joinToString(","))
    }

    suspend fun removePendingDeletedTransactionId(id: Long) {
        val current = getPendingDeletedTransactionIds().toMutableSet()
        current.remove(id)
        insertSetting("pending_deleted_tx_ids", current.joinToString(","))
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionSplitDao.deleteSplitsByTransactionId(transaction.id)
        transactionDao.deleteTransaction(transaction)
        addPendingDeletedTransactionId(transaction.id)
    }

    fun getTransactionsForFamilyMember(memberId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForFamilyMember(memberId)

    suspend fun clearAllTransactions() {
        transactionSplitDao.clearAllSplits()
        transactionDao.clearAllTransactions()
    }

    suspend fun insertContact(contact: Contact): Long = contactDao.insertContact(contact)
    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)
    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)
    suspend fun clearAllContacts() = contactDao.clearAllContacts()

    suspend fun insertTransactionSplit(split: TransactionSplit): Long = transactionSplitDao.insertSplit(split)
    suspend fun insertTransactionSplits(splits: List<TransactionSplit>) = transactionSplitDao.insertSplits(splits)
    suspend fun deleteTransactionSplit(split: TransactionSplit) = transactionSplitDao.deleteSplit(split)
    fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplit>> = transactionSplitDao.getSplitsForTransaction(transactionId)
    suspend fun settleSplitsForContact(contactId: Long) = transactionSplitDao.settleSplitsForContact(contactId)
    suspend fun settleSplitsForFamilyMember(familyMemberId: Long) = transactionSplitDao.settleSplitsForFamilyMember(familyMemberId)

    suspend fun clearAllLocalData() {
        familyMemberDao.clearAllFamilyMembers()
        transactionDao.clearAllTransactions()
        transactionSplitDao.clearAllSplits()
        creditCardDao.clearAllCreditCards()
        budgetGoalDao.clearAllBudgetGoals()
        savingsGoalDao.clearAllSavingsGoals()
        recurringTransactionDao.clearAllRecurringTransactions()
        appSettingDao.clearAllSettings()
        customWalletDao.clearAllCustomWallets()
        contactDao.clearAllContacts()
    }

    suspend fun pushAllPendingToSupabase(): Boolean {
        return try {
            val api = SupabaseClient.api ?: return false

            // 1. Process and execute pending deletions on Supabase
            val pendingDeletes = getPendingDeletedTransactionIds()
            for (delId in pendingDeletes) {
                try {
                    api.deleteTransaction("eq.$delId")
                    removePendingDeletedTransactionId(delId)
                } catch (_: Exception) {
                    // Retry on next sync cycle
                }
            }

            // 2. Push all transactions without filtering out historical/backdated transactions
            val allTxs = allTransactions.first()
            if (allTxs.isNotEmpty()) {
                allTxs.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                    api.upsertTransactions(chunk)
                }
            }

            // 3. Push savings goals
            val allSavings = allSavingsGoals.first()
            if (allSavings.isNotEmpty()) {
                allSavings.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                    api.upsertSavingsGoals(chunk)
                }
            }

            // 4. Push budget goals
            val allBudget = allBudgetGoals.first()
            if (allBudget.isNotEmpty()) {
                allBudget.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                    api.upsertBudgetGoals(chunk)
                }
            }

            // 5. Push recurring transactions
            val allRecur = allRecurringTransactions.first()
            if (allRecur.isNotEmpty()) {
                allRecur.map { it.toSupabase() }.chunked(500).forEach { chunk ->
                    api.upsertRecurringTransactions(chunk)
                }
            }

            // 6. Push categories
            val localCats = getSetting("categories_list", "")
            val localIncCats = getSetting("income_categories_list", "")
            val catList = mutableListOf<SupabaseCategory>()
            if (localCats.isNotEmpty()) {
                localCats.split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("[", "]") }
                    .filter { it.isNotEmpty() }
                    .forEach { catList.add(SupabaseCategory(it, "expense")) }
            }
            if (localIncCats.isNotEmpty()) {
                localIncCats.split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("[", "]") }
                    .filter { it.isNotEmpty() }
                    .forEach { catList.add(SupabaseCategory(it, "income")) }
            }
            if (catList.isNotEmpty()) {
                catList.chunked(500).forEach { chunk ->
                    try { api.upsertCategories(chunk) } catch (_: Exception) {}
                }
            }

            // 7. Push app settings if configured
            try {
                val theme = getSetting("app_theme", "dark")
                val expColor = getSetting("expense_wallet_color", "#1A56DB")
                val cardColor = getSetting("credit_card_wallet_color", "#0097A7")
                val profileName = getSetting("user_profile_name", "User")
                api.upsertAppSettings(listOf(
                    SupabaseAppSetting("app_theme", theme),
                    SupabaseAppSetting("expense_wallet_color", expColor),
                    SupabaseAppSetting("credit_card_wallet_color", cardColor),
                    SupabaseAppSetting("user_profile_name", profileName)
                ))
            } catch (_: Exception) {}
            insertSetting("last_supabase_sync_timestamp", System.currentTimeMillis().toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LedgerlyRepository? = null
        var appContext: Context? = null

        fun getInstance(context: Context): LedgerlyRepository {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = LedgerlyRepository(
                    db.familyMemberDao(),
                    db.transactionDao(),
                    db.appSettingDao(),
                    db.budgetGoalDao(),
                    db.savingsGoalDao(),
                    db.recurringTransactionDao(),
                    db.creditCardDao(),
                    db.customWalletDao(),
                    db.contactDao(),
                    db.transactionSplitDao()
                )
                INSTANCE = instance
                instance
            }
        }
        fun calculateExpenseBalance(transactions: List<Transaction>): Double {
            var balance = 0.0
            for (t in transactions) {
                when (t.type) {
                    TransactionType.PERSONAL_INCOME -> {
                        if (t.sourceWallet == WalletType.EXPENSE_WALLET) {
                            balance += t.amount
                        }
                    }
                    TransactionType.PERSONAL_EXPENSE -> {
                        if (t.sourceWallet == WalletType.EXPENSE_WALLET) {
                            balance -= t.amount
                        }
                    }
                    TransactionType.FAMILY_TRANSFER, TransactionType.BORROW_LEND -> {
                        if (t.sourceWallet == WalletType.EXPENSE_WALLET || t.destinationWallet == WalletType.EXPENSE_WALLET) {
                            if (t.direction == TransactionDirection.RECEIVED_FROM_THEM || t.direction == TransactionDirection.BORROWED) {
                                balance += t.amount
                            } else if (t.direction == TransactionDirection.GIVEN_TO_THEM || t.direction == TransactionDirection.LENT) {
                                balance -= t.amount
                            }
                        }
                    }
                    TransactionType.WALLET_TRANSFER -> {
                        if (t.sourceWallet == WalletType.EXPENSE_WALLET) {
                            balance -= t.amount
                        }
                        if (t.destinationWallet == WalletType.EXPENSE_WALLET) {
                            balance += t.amount
                        }
                    }
                }
            }
            return balance
        }

        fun calculateSavingsBalance(transactions: List<Transaction>): Double {
            var balance = 0.0
            for (t in transactions) {
                when (t.type) {
                    TransactionType.PERSONAL_INCOME -> {
                        if (t.sourceWallet == WalletType.SAVINGS_WALLET) {
                            balance += t.amount
                        }
                    }
                    TransactionType.PERSONAL_EXPENSE -> {
                        if (t.sourceWallet == WalletType.SAVINGS_WALLET) {
                            balance -= t.amount
                        }
                    }
                    TransactionType.FAMILY_TRANSFER, TransactionType.BORROW_LEND -> {
                        if (t.sourceWallet == WalletType.SAVINGS_WALLET || t.destinationWallet == WalletType.SAVINGS_WALLET) {
                            if (t.direction == TransactionDirection.RECEIVED_FROM_THEM || t.direction == TransactionDirection.BORROWED) {
                                balance += t.amount
                            } else if (t.direction == TransactionDirection.GIVEN_TO_THEM || t.direction == TransactionDirection.LENT) {
                                balance -= t.amount
                            }
                        }
                    }
                    TransactionType.WALLET_TRANSFER -> {
                        if (t.sourceWallet == WalletType.SAVINGS_WALLET) {
                            balance -= t.amount
                        }
                        if (t.destinationWallet == WalletType.SAVINGS_WALLET) {
                            balance += t.amount
                        }
                    }
                }
            }
            return balance
        }

        fun calculateAverageMonthlyExpenseSpending(transactions: List<Transaction>, currentMillis: Long): Double {
            // 3 months is exactly 90 days.
            val ninetyDaysAgo = currentMillis - (90L * 24L * 60L * 60L * 1000L)
            
            val expenseOutflows = transactions.filter { t ->
                t.date >= ninetyDaysAgo && (
                    (t.type == TransactionType.PERSONAL_EXPENSE && t.sourceWallet == WalletType.EXPENSE_WALLET) ||
                    (t.type == TransactionType.FAMILY_TRANSFER && t.sourceWallet == WalletType.EXPENSE_WALLET && t.direction == TransactionDirection.GIVEN_TO_THEM)
                )
            }
            
            val totalSpending = expenseOutflows.sumOf { it.amount }
            return totalSpending / 3.0
        }

        fun calculateTempWalletBalance(transactions: List<Transaction>, customWalletId: Long): Double {
            var balance = 0.0
            for (t in transactions) {
                val isSourceThisWallet = t.sourceWallet == WalletType.TEMPORARY_WALLET && t.customWalletId == customWalletId
                val isDestThisWallet = t.destinationWallet == WalletType.TEMPORARY_WALLET && t.destinationCustomWalletId == customWalletId

                when (t.type) {
                    TransactionType.PERSONAL_INCOME -> {
                        if (isSourceThisWallet) balance += t.amount
                    }
                    TransactionType.PERSONAL_EXPENSE -> {
                        if (isSourceThisWallet) balance -= t.amount
                    }
                    TransactionType.FAMILY_TRANSFER, TransactionType.BORROW_LEND -> {
                        if (isSourceThisWallet || isDestThisWallet) {
                            if (t.direction == TransactionDirection.RECEIVED_FROM_THEM || t.direction == TransactionDirection.BORROWED) {
                                balance += t.amount
                            } else if (t.direction == TransactionDirection.GIVEN_TO_THEM || t.direction == TransactionDirection.LENT) {
                                balance -= t.amount
                            }
                        }
                    }
                    TransactionType.WALLET_TRANSFER -> {
                        if (isSourceThisWallet) balance -= t.amount
                        if (isDestThisWallet) balance += t.amount
                    }
                }
            }
            return balance
        }
    }
}
