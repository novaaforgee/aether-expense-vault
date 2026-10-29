package com.aether.expensevault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

class VaultRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "aether_fintech_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TRANSACTIONS = "key_txs"
        private const val KEY_CURRENCY = "key_currency"
    }

    fun getCurrency(): String {
        return prefs.getString(KEY_CURRENCY, "$") ?: "$"
    }

    fun setCurrency(c: String) {
        prefs.edit().putString(KEY_CURRENCY, c).apply()
    }

    fun saveTransactions(list: List<Transaction>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("amount", item.amount)
            obj.put("category", item.category)
            obj.put("type", item.type.name)
            obj.put("date", item.date)
            obj.put("currency", item.currency)
            obj.put("note", item.note)
            array.put(obj)
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return getSampleInitialData()
        val list = mutableListOf<Transaction>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Transaction(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        amount = o.getDouble("amount"),
                        category = o.getString("category"),
                        type = TransactionType.valueOf(o.getString("type")),
                        date = o.getLong("date"),
                        currency = o.optString("currency", "USD"),
                        note = o.optString("note", "")
                    )
                )
            }
        } catch (_: Exception) {
            return getSampleInitialData()
        }
        return list
    }

    private fun getSampleInitialData(): List<Transaction> {
        val now = System.currentTimeMillis()
        val day = 86400000L
        return listOf(
            Transaction("1", "Client Retainer", 3200.0, "Consulting", TransactionType.INCOME, now - day * 1),
            Transaction("2", "AWS Cloud Infrastructure", 84.50, "Servers", TransactionType.SUBSCRIPTION, now - day * 2),
            Transaction("3", "Figma Professional", 15.00, "Tools", TransactionType.SUBSCRIPTION, now - day * 4),
            Transaction("4", "Blue Bottle Coffee", 6.80, "Dining", TransactionType.EXPENSE, now - day * 5),
            Transaction("5", "Apple Developer Program", 99.00, "License", TransactionType.EXPENSE, now - day * 8)
        )
    }
}
