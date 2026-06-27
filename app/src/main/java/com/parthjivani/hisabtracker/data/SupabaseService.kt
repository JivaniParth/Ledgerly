package com.parthjivani.hisabtracker.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.util.Log
import com.parthjivani.hisabtracker.BuildConfig

// Retrofit Network Models mapped to Postgres DB columns

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val access_token: String?,
    @Json(name = "token_type") val token_type: String?,
    @Json(name = "expires_in") val expires_in: Int?,
    @Json(name = "refresh_token") val refresh_token: String?,
    @Json(name = "user") val user: SupabaseUser?
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseUpdateUserRequest(
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @Json(name = "refresh_token") val refresh_token: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFamilyMember(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String?,
    @Json(name = "relationship") val relationship: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseCreditCard(
    @Json(name = "id") val id: Long,
    @Json(name = "card_name") val cardName: String?,
    @Json(name = "owner_name") val ownerName: String?,
    @Json(name = "credit_limit") val creditLimit: Double?
)

@JsonClass(generateAdapter = true)
data class SupabaseTransaction(
    @Json(name = "id") val id: Long,
    @Json(name = "amount") val amount: Double?,
    @Json(name = "date") val date: Long?,
    @Json(name = "notes") val notes: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "source_wallet") val sourceWallet: String?,
    @Json(name = "destination_wallet") val destinationWallet: String?,
    @Json(name = "family_member_id") val familyMemberId: Long?,
    @Json(name = "direction") val direction: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "credit_card_id") val creditCardId: Long?,
    @Json(name = "recurring_transaction_id") val recurringTransactionId: Long?
)

@JsonClass(generateAdapter = true)
data class SupabaseSavingsGoal(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "target_amount") val targetAmount: Double,
    @Json(name = "current_amount") val currentAmount: Double,
    @Json(name = "category") val category: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBudgetGoal(
    @Json(name = "category") val category: String,
    @Json(name = "limit_amount") val limitAmount: Double?
)

@JsonClass(generateAdapter = true)
data class SupabaseRecurringTransaction(
    @Json(name = "id") val id: Long,
    @Json(name = "amount") val amount: Double?,
    @Json(name = "notes") val notes: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "source_wallet") val sourceWallet: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "interval_type") val intervalType: String?,
    @Json(name = "last_applied_date") val lastAppliedDate: Long?,
    @Json(name = "next_due_date") val nextDueDate: Long?,
    @Json(name = "is_active") val isActive: Boolean?
)

// Extension converters
fun FamilyMember.toSupabase() = SupabaseFamilyMember(id, name, relationship)
fun SupabaseFamilyMember.toRoom() = FamilyMember(id, name ?: "", relationship ?: "")

fun CreditCard.toSupabase() = SupabaseCreditCard(id, cardName, ownerName, creditLimit)
fun SupabaseCreditCard.toRoom() = CreditCard(id, cardName ?: "", ownerName ?: "", creditLimit ?: 0.0)

fun Transaction.toSupabase() = SupabaseTransaction(
    id = id,
    amount = amount,
    date = date,
    notes = notes,
    type = type.name,
    sourceWallet = sourceWallet.name,
    destinationWallet = destinationWallet?.name,
    familyMemberId = familyMemberId,
    direction = direction?.name,
    category = category,
    creditCardId = creditCardId,
    recurringTransactionId = recurringTransactionId
)
fun SupabaseTransaction.toRoom(): Transaction {
    val tType = try {
        val cleaned = (type ?: "PERSONAL_EXPENSE").trim().uppercase()
        if (cleaned.contains("FAMILY") || (cleaned.contains("TRANSFER") && !cleaned.contains("WALLET"))) {
            TransactionType.FAMILY_TRANSFER
        } else if (cleaned.contains("WALLET")) {
            TransactionType.WALLET_TRANSFER
        } else if (cleaned.contains("INCOME")) {
            TransactionType.PERSONAL_INCOME
        } else if (cleaned.contains("EXPENSE")) {
            TransactionType.PERSONAL_EXPENSE
        } else {
            TransactionType.valueOf(cleaned)
        }
    } catch (e: Exception) {
        TransactionType.PERSONAL_EXPENSE
    }

    val sWallet = try {
        val cleaned = (sourceWallet ?: "EXPENSE_WALLET").trim().uppercase()
        if (cleaned.contains("EXPENSE") || cleaned.contains("CASH")) {
            WalletType.EXPENSE_WALLET
        } else if (cleaned.contains("SAVING")) {
            WalletType.SAVINGS_WALLET
        } else if (cleaned.contains("CREDIT")) {
            WalletType.CREDIT_CARD
        } else {
            WalletType.valueOf(cleaned)
        }
    } catch (e: Exception) {
        WalletType.EXPENSE_WALLET
    }

    val dWallet = destinationWallet?.let {
        try {
            val cleaned = it.trim().uppercase()
            if (cleaned.contains("EXPENSE") || cleaned.contains("CASH")) {
                WalletType.EXPENSE_WALLET
            } else if (cleaned.contains("SAVING")) {
                WalletType.SAVINGS_WALLET
            } else if (cleaned.contains("CREDIT")) {
                WalletType.CREDIT_CARD
            } else {
                WalletType.valueOf(cleaned)
            }
        } catch (e: Exception) {
            null
        }
    }

    val dir = direction?.let {
        try {
            val cleaned = it.trim().uppercase()
            if (cleaned.contains("RECEIVED") || cleaned.contains("REC") || cleaned.contains("FROM") || cleaned == "IN") {
                TransactionDirection.RECEIVED_FROM_THEM
            } else if (cleaned.contains("GIVEN") || cleaned.contains("GIV") || cleaned.contains("TO") || cleaned == "OUT") {
                TransactionDirection.GIVEN_TO_THEM
            } else {
                TransactionDirection.valueOf(cleaned)
            }
        } catch (e: Exception) {
            null
        }
    }
    return Transaction(
        id = id,
        amount = java.lang.Math.abs(amount ?: 0.0),
        date = date ?: System.currentTimeMillis(),
        notes = notes ?: "",
        type = tType,
        sourceWallet = sWallet,
        destinationWallet = dWallet,
        familyMemberId = familyMemberId,
        direction = dir,
        category = category ?: "Other",
        creditCardId = creditCardId,
        recurringTransactionId = recurringTransactionId
    )
}

fun SavingsGoal.toSupabase() = SupabaseSavingsGoal(id, name, targetAmount, currentAmount, category)
fun SupabaseSavingsGoal.toRoom() = SavingsGoal(id, name, targetAmount, currentAmount, category)

fun BudgetGoal.toSupabase() = SupabaseBudgetGoal(category, limitAmount)
fun SupabaseBudgetGoal.toRoom() = BudgetGoal(category, limitAmount ?: 0.0)

fun RecurringTransaction.toSupabase() = SupabaseRecurringTransaction(
    id = id,
    amount = amount,
    notes = notes,
    type = type.name,
    sourceWallet = sourceWallet.name,
    category = category,
    intervalType = intervalType,
    lastAppliedDate = lastAppliedDate,
    nextDueDate = nextDueDate,
    isActive = isActive
)
fun SupabaseRecurringTransaction.toRoom(): RecurringTransaction {
    val tType = try {
        val cleaned = (type ?: "PERSONAL_EXPENSE").trim().uppercase()
        TransactionType.valueOf(cleaned)
    } catch (e: Exception) {
        TransactionType.PERSONAL_EXPENSE
    }

    val sWallet = try {
        val cleaned = (sourceWallet ?: "EXPENSE_WALLET").trim().uppercase()
        WalletType.valueOf(cleaned)
    } catch (e: Exception) {
        WalletType.EXPENSE_WALLET
    }
    return RecurringTransaction(
        id = id,
        amount = java.lang.Math.abs(amount ?: 0.0),
        notes = notes ?: "",
        type = tType,
        sourceWallet = sWallet,
        category = category ?: "Other",
        intervalType = intervalType ?: "Monthly",
        lastAppliedDate = lastAppliedDate ?: 0L,
        nextDueDate = nextDueDate ?: 0L,
        isActive = isActive ?: true
    )
}

interface SupabaseApi {
    // 1. Family Members
    @GET("rest/v1/family_members")
    suspend fun getFamilyMembers(@Query("select") select: String = "*"): List<SupabaseFamilyMember>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/family_members")
    suspend fun upsertFamilyMembers(@Body members: List<SupabaseFamilyMember>)

    @DELETE("rest/v1/family_members")
    suspend fun deleteFamilyMember(@Query("id") query: String)

    // 2. Credit Cards
    @GET("rest/v1/credit_cards")
    suspend fun getCreditCards(@Query("select") select: String = "*"): List<SupabaseCreditCard>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/credit_cards")
    suspend fun upsertCreditCards(@Body cards: List<SupabaseCreditCard>)

    @DELETE("rest/v1/credit_cards")
    suspend fun deleteCreditCard(@Query("id") query: String)

    // 3. Transactions
    @GET("rest/v1/transactions")
    suspend fun getTransactions(
        @Header("Range") range: String = "0-999",
        @Query("select") select: String = "*",
        @Query("order") order: String = "id.asc"
    ): List<SupabaseTransaction>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/transactions")
    suspend fun upsertTransactions(@Body txs: List<SupabaseTransaction>)

    @DELETE("rest/v1/transactions")
    suspend fun deleteTransaction(@Query("id") query: String)

    // 4. Budget Goals
    @GET("rest/v1/budget_goals")
    suspend fun getBudgetGoals(@Query("select") select: String = "*"): List<SupabaseBudgetGoal>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/budget_goals")
    suspend fun upsertBudgetGoals(@Body goals: List<@JvmSuppressWildcards SupabaseBudgetGoal>)

    @DELETE("rest/v1/budget_goals")
    suspend fun deleteBudgetGoal(@Query("category") query: String)

    // 5. Recurring Transactions
    @GET("rest/v1/recurring_transactions")
    suspend fun getRecurringTransactions(@Query("select") select: String = "*"): List<SupabaseRecurringTransaction>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/recurring_transactions")
    suspend fun upsertRecurringTransactions(@Body recurrings: List<SupabaseRecurringTransaction>)

    @DELETE("rest/v1/recurring_transactions")
    suspend fun deleteRecurringTransaction(@Query("id") query: String)

    // 6. Savings Goals
    @GET("rest/v1/savings_goals")
    suspend fun getSavingsGoals(@Query("select") select: String = "*"): List<SupabaseSavingsGoal>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/savings_goals")
    suspend fun upsertSavingsGoals(@Body goals: List<SupabaseSavingsGoal>)

    @DELETE("rest/v1/savings_goals")
    suspend fun deleteSavingsGoal(@Query("id") query: String)
}

// Normalizes JSON arrays so all objects have identical keys (fills missing keys with null).
// PostgREST HTTP 400 "All object keys must match" occurs when objects have different key sets.
private fun normalizeJsonArray(json: String): String {
    return try {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return json
        val arr = JSONArray(trimmed)
        if (arr.length() <= 1) return json
        // Collect every key used across all objects
        val allKeys = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            obj.keys().forEach { allKeys.add(it) }
        }
        // Fill missing keys with JSONObject.NULL so every object is identical
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            for (key in allKeys) {
                if (!obj.has(key)) {
                    obj.put(key, JSONObject.NULL)
                }
            }
        }
        arr.toString()
    } catch (e: Exception) {
        json // Return original if parsing fails
    }
}

private val jsonNormalizeInterceptor = Interceptor { chain ->
    val request = chain.request()
    val body = request.body
    if ((request.method == "POST" || request.method == "PATCH") && body != null) {
        val buffer = Buffer()
        body.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        val normalized = normalizeJsonArray(bodyString)
        val newBody = normalized.toRequestBody("application/json".toMediaType())
        chain.proceed(request.newBuilder().method(request.method, newBody).build())
    } else {
        chain.proceed(request)
    }
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    var accessToken: String? = null
    var refreshToken: String? = null

    val supabaseUrl: String
        get() = try {
            BuildConfig.SUPABASE_URL.trim().removeSurrounding("\"").removeSurrounding("'")
        } catch (e: Exception) { "" }

    val supabaseAnonKey: String
        get() = try {
            BuildConfig.SUPABASE_ANON_KEY.trim().removeSurrounding("\"").removeSurrounding("'")
        } catch (e: Exception) { "" }

    val isConfigured: Boolean
        get() {
            val url = supabaseUrl
            val key = supabaseAnonKey
            return url.isNotEmpty() &&
                    !url.contains("YOUR_SUPABASE_URL") &&
                    !url.contains("YOUR_") &&
                    url != "SUPABASE_URL" &&
                    key.isNotEmpty() &&
                    !key.contains("YOUR_SUPABASE_ANON_KEY") &&
                    !key.contains("YOUR_") &&
                    key != "SUPABASE_ANON_KEY"
        }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val tokenToUse = accessToken ?: supabaseAnonKey
                val request = chain.request().newBuilder()
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $tokenToUse")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(jsonNormalizeInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: SupabaseApi? by lazy {
        if (!isConfigured) {
            Log.e(TAG, "Supabase URL/Key is not configured. Returning null API.")
            null
        } else {
            try {
                var url = supabaseUrl.trim()
                if (url.endsWith("/rest/v1/")) {
                    url = url.removeSuffix("rest/v1/")
                } else if (url.endsWith("/rest/v1")) {
                    url = url.removeSuffix("rest/v1")
                } else if (url.endsWith("rest/v1/")) {
                    url = url.removeSuffix("rest/v1/")
                } else if (url.endsWith("rest/v1")) {
                    url = url.removeSuffix("rest/v1")
                }
                if (!url.endsWith("/")) {
                    url += "/"
                }
                Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseApi::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build Supabase Retrofit client", e)
                null
            }
        }
    }
    
    val authApi: SupabaseAuthApi? by lazy {
        if (!isConfigured) {
            null
        } else {
            try {
                var url = supabaseUrl.trim()
                if (url.endsWith("/rest/v1/")) {
                    url = url.removeSuffix("rest/v1/")
                } else if (url.endsWith("/rest/v1")) {
                    url = url.removeSuffix("rest/v1")
                } else if (url.endsWith("rest/v1/")) {
                    url = url.removeSuffix("rest/v1/")
                } else if (url.endsWith("rest/v1")) {
                    url = url.removeSuffix("rest/v1")
                }
                if (!url.endsWith("/")) {
                    url += "/"
                }
                Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseAuthApi::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signup(@Body request: SupabaseAuthRequest): SupabaseAuthResponse

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body request: SupabaseAuthRequest): SupabaseAuthResponse

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Body request: SupabaseRefreshTokenRequest): SupabaseAuthResponse

    @PUT("auth/v1/user")
    suspend fun updateUser(@Body request: SupabaseUpdateUserRequest): SupabaseUser
}