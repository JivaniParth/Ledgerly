package com.parthjivani.ledgerly.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WalletType {
    EXPENSE_WALLET,
    SAVINGS_WALLET,
    CREDIT_CARD,
    TEMPORARY_WALLET
}

enum class TransactionType {
    PERSONAL_INCOME,
    PERSONAL_EXPENSE,
    FAMILY_TRANSFER,
    WALLET_TRANSFER,
    BORROW_LEND
}

enum class TransactionDirection {
    RECEIVED_FROM_THEM,
    GIVEN_TO_THEM,
    BORROWED,
    LENT
}

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: Long, // timestamp in milliseconds
    val notes: String,
    val type: TransactionType,
    val sourceWallet: WalletType,
    val destinationWallet: WalletType? = null, // Used for WALLET_TRANSFER
    val familyMemberId: Long? = null,          // Used for FAMILY_TRANSFER
    val direction: TransactionDirection? = null, // Used for FAMILY_TRANSFER
    val category: String = "Other",             // Groceries, Entertainment, Bills, Travel, Medical, Other
    val creditCardId: Long? = null,            // Used for CREDIT_CARD
    val recurringTransactionId: Long? = null,  // Links to the recurring transaction that created this
    val customWalletId: Long? = null,          // Used for source TEMPORARY_WALLET
    val destinationCustomWalletId: Long? = null, // Used for destination TEMPORARY_WALLET
    val contactId: Long? = null                // Used for BORROW_LEND
)

@Entity(tableName = "custom_wallets")
data class CustomWallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String
)

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val ownerName: String, // owner name, e.g. "Self" or family member's name
    val creditLimit: Double = 0.0,
    val color: String = "#E91E63" // Default Pink color
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey val category: String, // Groceries, Entertainment, Bills, Travel, Medical, Overall
    val limitAmount: Double
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val category: String = "Overall" // Can be "Overall" or linked to specific expense category
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val notes: String,
    val type: TransactionType, // PERSONAL_EXPENSE or PERSONAL_INCOME
    val sourceWallet: WalletType,
    val category: String = "Other",
    val intervalType: String, // Daily, Weekly, Monthly
    val lastAppliedDate: Long,
    val nextDueDate: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "transaction_splits")
data class TransactionSplit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val amount: Double,
    val contactId: Long? = null,
    val familyMemberId: Long? = null,
    val isSettled: Boolean = false
)
