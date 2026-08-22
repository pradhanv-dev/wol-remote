package com.pradhanv.wolremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PcStore(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppRoot(store)
            }
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
    var toast by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("WoL Remote") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) { Text("+") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            if (pcs.isEmpty()) {
                Text("No PCs saved yet. Tap + to add one.\n\nFor waking from anywhere:\n• IPv4: forward UDP port (e.g. 9) on your router to the PC, enter your public IP or DDNS name.\n• IPv6: enter the PC's global IPv6 address directly — no port-forward needed if its firewall allows UDP 9.", style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(pcs, key = { it.id }) { pc ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pc.name, style = MaterialTheme.typography.titleMedium)
                                Text("${pc.mac} • ${pc.host}:${pc.port}" + (if (pc.useIpv6) " • IPv6" else ""), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    when (val r = WolEngine.wake(pc)) {
                                        is WolEngine.WakeResult.Success -> toast = "✅ ${pc.name}: ${r.detail}"
                                        is WolEngine.WakeResult.Failure -> toast = "❌ ${r.message}"
                                    }
                                }
                            }) { Text("Wake") }
                            IconButton(onClick = { editing = pc; showEditor = true }) { Text("✎") }
                        }
                    }
                }
            }
            if (!toast.isNullOrBlank()) {
                AssistChip(onClick = {}, label = { Text(toast!!) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showEditor) {
        PcEditor(
            initial = editing,
            onSave = { entry ->
                scope.launch { store.upsert(entry) }
                showEditor = false
            },
            onDelete = editing?.let { pc -> ({ scope.launch { store.remove(pc.id) }; showEditor = false }) },
            onCancel = { showEditor = false }
        )
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
        title = { Text(if (initial == null) "Add PC" else "Edit PC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(mac, { mac = it }, label = { Text("MAC address (e.g. AA-BB-CC-DD-EE-FF)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                OutlinedTextField(host, { host = it; useIpv6 = it.contains(":") }, label = { Text("Host / IP (public IPv4, DDNS name, or global IPv6 address)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                OutlinedTextField(port, { port = it }, label = { Text("UDP port (default 9)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(secureOn, { secureOn = it }, label = { Text("SecureOn password (optional, hex)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useIpv6, { useIpv6 = it })
                    Text("Prefer IPv6 target")
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (WolEngine.parseMac(mac) == null) { err = "Invalid MAC address"; return@TextButton }
                if (host.isBlank()) { err = "Host/IP required"; return@TextButton }
                onSave(PcEntry(initial?.id ?: 0L, name.ifBlank { "PC" }, host.trim(), mac.trim(), port.toIntOrNull() ?: 9, secureOn.trim(), useIpv6))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                onDelete?.let { del -> TextButton(onClick = del) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    )
}
