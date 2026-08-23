package com.pradhanv.wolremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---- Palette ----
val Bg = Color(0xFF0B1220)
val Surface1 = Color(0xFF141C2B)
val Surface2 = Color(0xFF1B2537)
val Accent = Color(0xFF4FC3F7)
val AccentDim = Color(0xFF2A6E96)
val GoodGreen = Color(0xFF66BB6A)
val TextMain = Color(0xFFE3EAF5)
val TextSub = Color(0xFF8FA0B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PcStore(this)
        setContent {
            AppRoot(store)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(store: PcStore) {
    val scope = rememberCoroutineScope()
    val pcs by store.pcs.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<PcEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var snack by remember { mutableStateOf<String?>(null) }
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(snack) { snack?.let { snackHost.showSnackbar(it); snack = null } }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackHost) },
        topBar = {
            // Gradient header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF12314A), Bg)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(Accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("⚡", fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("WoL Remote", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Wake your PC from anywhere", color = TextSub, fontSize = 12.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = Accent,
                contentColor = Color(0xFF06131D),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+  Add PC", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (pcs.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pcs, key = { it.id }) { pc ->
                        PcCard(
                            pc = pc,
                            onWake = {
                                scope.launch {
                                    when (val r = WolEngine.wake(pc)) {
                                        is WolEngine.WakeResult.Success -> snack = "✓ Wake packet sent to ${pc.name} (${r.detail})"
                                        is WolEngine.WakeResult.Failure -> snack = "✗ ${r.message}"
                                    }
                                }
                            },
                            onEdit = { editing = pc; showEditor = true }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        PcEditor(
            initial = editing,
            onSave = { entry -> scope.launch { store.upsert(entry) }; showEditor = false },
            onDelete = editing?.let { pc -> ({ scope.launch { store.remove(pc.id) }; showEditor = false }) },
            onCancel = { showEditor = false }
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).background(Accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("⚡", fontSize = 40.sp) }
        Spacer(Modifier.height(20.dp))
        Text("No PCs yet", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap “Add PC” to save your machine.\n\n" +
            "• IPv4: forward a UDP port on your router, use public IP or DDNS name.\n" +
            "• IPv6: paste your PC’s global IPv6 address — no port-forward needed.",
            color = TextSub, fontSize = 13.sp, lineHeight = 19.sp
        )
    }
}

@Composable
fun PcCard(pc: PcEntry, onWake: () -> Unit, onEdit: () -> Unit) {
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(Surface2, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (pc.useIpv6) "6" else "4", color = Accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pc.name, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text("${pc.host}:${pc.port}", color = TextSub, fontSize = 12.sp)
                Text(
                    pc.mac.replace(":", "-").uppercase() + if (pc.secureOn.isNotBlank()) "  •  SecureOn" else "",
                    color = TextSub.copy(alpha = 0.7f), fontSize = 11.sp
                )
            }
            Button(
                onClick = onWake,
                colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.16f), contentColor = Accent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
            ) { Text("Wake", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun PcEditor(initial: PcEntry?, onSave: (PcEntry) -> Unit, onDelete: (() -> Unit)?, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var mac by remember { mutableStateOf(initial?.mac ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: 9).toString()) }
    var secureOn by remember { mutableStateOf(initial?.secureOn ?: "") }
    var useIpv6 by remember { mutableStateOf(initial?.useIpv6 ?: host.contains(":")) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Surface1,
        titleContentColor = TextMain,
        textContentColor = TextSub,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (initial == null) "Add PC" else "Edit PC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true,
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(mac, { mac = it }, label = { Text("MAC address (AA-BB-CC-DD-EE-FF)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(host, {
                    host = it
                    if (it.contains(":")) useIpv6 = true
                }, label = { Text("Public IPv4, DDNS name, or global IPv6") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(port, { port = it }, label = { Text("UDP port") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(secureOn, { secureOn = it }, label = { Text("SecureOn (optional)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        colors = fieldColors(), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useIpv6, { useIpv6 = it }, colors = CheckboxDefaults.colors(checkedColor = Accent))
                    Text("Prefer IPv6 target", color = TextMain, fontSize = 14.sp)
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (WolEngine.parseMac(mac) == null) { err = "Invalid MAC address"; return@TextButton }
                if (host.isBlank()) { err = "Host/IP required"; return@TextButton }
                onSave(PcEntry(initial?.id ?: 0L, name.ifBlank { "PC" }, host.trim(), mac.trim(),
                    port.toIntOrNull() ?: 9, secureOn.trim(), useIpv6))
            }) { Text("Save", color = Accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                onDelete?.let { del -> TextButton(onClick = del) { Text("Delete", color = Color(0xFFEF5350)) } }
                TextButton(onClick = onCancel) { Text("Cancel", color = TextSub) }
            }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Color(0xFF2A3850),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSub,
    cursorColor = Accent,
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain
)
