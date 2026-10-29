package com.aether.expensevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = VaultRepository(this)

        setContent {
            // Modern Swiss/Nordic Fintech Dark Aesthetic
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E599),    // Mint Green
                    secondary = Color(0xFF536DFE),  // Electric Indigo
                    background = Color(0xFF090A0E), // Obsidian Dark
                    surface = Color(0xFF13151D),    // Elevated Graphite
                    surfaceVariant = Color(0xFF1B1E29),
                    onBackground = Color(0xFFF7F9FC),
                    onSurface = Color(0xFFF7F9FC)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AetherApp(repository = repository)
                }
            }
        }
    }
}

enum class NavigationTab {
    DASHBOARD,
    ANALYTICS,
    SUBSCRIPTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherApp(repository: VaultRepository) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var transactions by remember { mutableStateOf(repository.loadTransactions()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type != TransactionType.INCOME }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    Scaffold(
        containerColor = Color(0xFF090A0E),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10121A),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DASHBOARD,
                    onClick = { selectedTab = NavigationTab.DASHBOARD },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Vault") },
                    label = { Text("Vault", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E599),
                        indicatorColor = Color(0xFF182A24),
                        selectedTextColor = Color(0xFF00E599),
                        unselectedIconColor = Color(0xFF676A7D),
                        unselectedTextColor = Color(0xFF676A7D)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.ANALYTICS,
                    onClick = { selectedTab = NavigationTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF536DFE),
                        indicatorColor = Color(0xFF1E2342),
                        selectedTextColor = Color(0xFF536DFE),
                        unselectedIconColor = Color(0xFF676A7D),
                        unselectedTextColor = Color(0xFF676A7D)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SUBSCRIPTIONS,
                    onClick = { selectedTab = NavigationTab.SUBSCRIPTIONS },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Recurring") },
                    label = { Text("Recurring", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB300),
                        indicatorColor = Color(0xFF2E2716),
                        selectedTextColor = Color(0xFFFFB300),
                        unselectedIconColor = Color(0xFF676A7D),
                        unselectedTextColor = Color(0xFF676A7D)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF00E599),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    transactions = transactions,
                    netBalance = netBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    onDelete = { tx ->
                        transactions = transactions.filter { it.id != tx.id }
                        repository.saveTransactions(transactions)
                    }
                )
                NavigationTab.ANALYTICS -> AnalyticsScreen(transactions = transactions)
                NavigationTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                    transactions = transactions.filter { it.type == TransactionType.SUBSCRIPTION },
                    onDelete = { tx ->
                        transactions = transactions.filter { it.id != tx.id }
                        repository.saveTransactions(transactions)
                    }
                )
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newTx ->
                    transactions = listOf(newTx) + transactions
                    repository.saveTransactions(transactions)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(
    transactions: List<Transaction>,
    netBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    onDelete: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 36.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aether Vault",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8B8E9E),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Net Liquidity",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141721),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E599))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AES-256 Encrypted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9094A6)
                        )
                    }
                }
            }
        }

        item {
            // Hero Modern Card (Balance View)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF171B26),
                                    Color(0xFF10121A)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF787B8D),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", netBalance)}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF00E599),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "+$${String.format(Locale.US, "%,.2f", totalIncome)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E599)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expenses", fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "-$${String.format(Locale.US, "%,.2f", totalExpense)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${transactions.size} records",
                    fontSize = 12.sp,
                    color = Color(0xFF787B8D)
                )
            }
        }

        items(transactions, key = { it.id }) { tx ->
            TransactionRow(tx = tx, onDelete = { onDelete(tx) })
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, onDelete: () -> Unit) {
    val dateStr = remember(tx.date) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(tx.date))
    }

    val isIncome = tx.type == TransactionType.INCOME
    val isSub = tx.type == TransactionType.SUBSCRIPTION

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isIncome -> Color(0xFF152A20)
                    isSub -> Color(0xFF282315)
                    else -> Color(0xFF28181A)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isIncome -> Icons.Default.ArrowDownward
                            isSub -> Icons.Default.Autorenew
                            else -> Icons.Default.ArrowUpward
                        },
                        contentDescription = null,
                        tint = when {
                            isIncome -> Color(0xFF00E599)
                            isSub -> Color(0xFFFFB300)
                            else -> Color(0xFFFF5252)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tx.category} • $dateStr",
                    fontSize = 12.sp,
                    color = Color(0xFF7E8294)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}$${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isIncome) Color(0xFF00E599) else Color.White
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF555768),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(transactions: List<Transaction>) {
    val totalExpense = transactions.filter { it.type != TransactionType.INCOME }.sumOf { it.amount }
    val categories = transactions
        .filter { it.type != TransactionType.INCOME }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text("Spending Analytics", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("AI categorised cashflow visualization", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Canvas Chart (Smooth Bar Breakdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CATEGORY INTENSITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                if (categories.isEmpty()) {
                    Text("No expense transactions recorded yet.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    categories.forEach { (cat, amount) ->
                        val ratio = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 13.sp, color = Color.White)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", amount)} (${(ratio * 100).toInt()}%)",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E599),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                drawRoundRect(
                                    color = Color(0xFF222633),
                                    size = size,
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF536DFE), Color(0xFF00E599))
                                    ),
                                    size = Size(size.width * ratio, size.height),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(transactions: List<Transaction>, onDelete: (Transaction) -> Unit) {
    val totalMonthlySubs = transactions.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 36.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Active Subscriptions", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Recurring SaaS, memberships & services", fontSize = 12.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("MONTHLY RECURRING BURN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalMonthlySubs)}/mo",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        items(transactions, key = { it.id }) { tx ->
            TransactionRow(tx = tx, onDelete = { onDelete(tx) })
        }
    }
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onAdd: (Transaction) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Consulting") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181B26),
        shape = RoundedCornerShape(24.dp),
        title = { Text("Log Transaction", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth()) {
                    TransactionType.values().forEach { t ->
                        val selected = type == t
                        Button(
                            onClick = { type = t },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF00E599) else Color(0xFF222636),
                                contentColor = if (selected) Color.Black else Color.White
                            )
                        ) {
                            Text(t.name.take(3), fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Stripe, AWS, Coffee)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($ USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onAdd(
                            Transaction(
                                id = UUID.randomUUID().toString(),
                                title = title.trim(),
                                amount = amt,
                                category = category.trim(),
                                type = type,
                                date = System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E599)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Record", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
