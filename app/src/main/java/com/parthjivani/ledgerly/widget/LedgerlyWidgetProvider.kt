package com.parthjivani.ledgerly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.parthjivani.ledgerly.MainActivity
import com.parthjivani.ledgerly.R
import com.parthjivani.ledgerly.data.LedgerlyRepository
import com.parthjivani.ledgerly.ui.formatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LedgerlyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == ACTION_UPDATE_WIDGET_DATA) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, LedgerlyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        } else if (intent.action == ACTION_TOGGLE_PRIVACY) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentMask = prefs.getBoolean(KEY_PRIVACY_MASKED, false)
            prefs.edit().putBoolean(KEY_PRIVACY_MASKED, !currentMask).apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, LedgerlyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.parthjivani.ledgerly.ACTION_REFRESH_WIDGET"
        const val ACTION_UPDATE_WIDGET_DATA = "com.parthjivani.ledgerly.ACTION_UPDATE_WIDGET_DATA"
        const val ACTION_TOGGLE_PRIVACY = "com.parthjivani.ledgerly.ACTION_TOGGLE_PRIVACY"
        private const val PREFS_NAME = "ledgerly_widget_prefs"
        private const val KEY_PRIVACY_MASKED = "privacy_masked"

        private val widgetScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            widgetScope.launch {
                val repository = LedgerlyRepository.getInstance(context)
                val allTxs = try {
                    repository.allTransactions.first()
                } catch (e: Exception) {
                    emptyList()
                }

                val expBal = LedgerlyRepository.calculateExpenseBalance(allTxs)
                val savBal = LedgerlyRepository.calculateSavingsBalance(allTxs)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isMasked = prefs.getBoolean(KEY_PRIVACY_MASKED, false)

                val views = RemoteViews(context.packageName, R.layout.widget_ledgerly)
                if (isMasked) {
                    views.setTextViewText(R.id.widget_tv_expense_balance, "₹ ••••••")
                    views.setTextViewText(R.id.widget_tv_savings_balance, "₹ ••••••")
                } else {
                    views.setTextViewText(R.id.widget_tv_expense_balance, formatCurrency(expBal))
                    views.setTextViewText(R.id.widget_tv_savings_balance, formatCurrency(savBal))
                }

                // Balance cards click -> Toggle privacy mask
                val privacyIntent = Intent(context, LedgerlyWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_PRIVACY
                }
                val privacyPending = PendingIntent.getBroadcast(
                    context, 4, privacyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_expense_card, privacyPending)
                views.setOnClickPendingIntent(R.id.widget_savings_card, privacyPending)

                // Header click -> open Main App
                val mainIntent = Intent(context, MainActivity::class.java)
                val mainPending = PendingIntent.getActivity(
                    context, 0, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_header, mainPending)

                // Refresh button click
                val refreshIntent = Intent(context, LedgerlyWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context, 1, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

                // + Expense button click -> QuickAddTransactionActivity
                val expIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                    putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, "PERSONAL_EXPENSE")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val expPending = PendingIntent.getActivity(
                    context, 2, expIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add_expense, expPending)

                // + Income button click -> QuickAddTransactionActivity
                val incIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                    putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, "PERSONAL_INCOME")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val incPending = PendingIntent.getActivity(
                    context, 3, incIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add_income, incPending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, LedgerlyWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET_DATA
            }
            context.sendBroadcast(intent)
        }
    }
}
