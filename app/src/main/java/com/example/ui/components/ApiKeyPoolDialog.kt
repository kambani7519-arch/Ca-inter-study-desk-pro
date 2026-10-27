package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiApiClient
import kotlinx.coroutines.launch

@Composable
fun ApiKeyPoolDialog(
  apiClient: GeminiApiClient,
  onDismiss: () -> Unit
) {
  val activeKeyIdx by apiClient.activeKeyIndexFlow.collectAsState()
  val poolSize by apiClient.poolSizeFlow.collectAsState()
  val lastNotice by apiClient.lastRotationNotice.collectAsState()
  val keys = remember(poolSize) { apiClient.getKeyPool() }

  var newKeyInput by remember { mutableStateOf("") }
  var isTesting by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.VpnKey,
          contentDescription = "API Keys",
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Gemini Key Auto-Rotation Pool", style = MaterialTheme.typography.titleMedium)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Text(
          text = "Zero-Interruption Multi-Key System: If an API key hits HTTP 429 quota limits during evaluation, the app automatically rotates to the next key without failing.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Active Key: #${activeKeyIdx + 1} of $poolSize in pool",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        if (lastNotice != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "⚡ $lastNotice",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Key Pool Entries:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
        ) {
          itemsIndexed(keys) { index, key ->
            val isCurrent = index == activeKeyIdx
            val masked = if (key.length > 8) "${key.take(4)}...${key.takeLast(4)}" else key
            Surface(
              color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "#${index + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = masked,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                  )
                }
                if (isCurrent) {
                  Text(
                    text = "ACTIVE",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = newKeyInput,
          onValueChange = { newKeyInput = it },
          label = { Text("Add Personal Gemini API Key") },
          placeholder = { Text("AIzaSy...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_key_input")
        )

        if (testResult != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = testResult!!,
            style = MaterialTheme.typography.labelSmall,
            color = if (testResult!!.startsWith("Success")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (newKeyInput.isNotBlank()) {
            apiClient.addKeyToPool(newKeyInput.trim())
            newKeyInput = ""
          }
        },
        enabled = newKeyInput.isNotBlank(),
        modifier = Modifier.testTag("add_key_button")
      ) {
        Text("Add to Pool")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
