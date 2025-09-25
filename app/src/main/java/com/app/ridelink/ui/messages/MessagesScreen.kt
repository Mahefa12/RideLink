package com.app.ridelink.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Message(
    val id: String,
    val senderName: String,
    val lastMessage: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean = false,
    val rideId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onNavigateToChat: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Hardcoded sample messages
    val sampleMessages = remember {
        listOf(
            Message(
                id = "msg1",
                senderName = "Sarah Johnson",
                lastMessage = "Thanks for booking the ride! I'll pick you up at 2:30 PM sharp.",
                timestamp = LocalDateTime.now().minusHours(1),
                isRead = false,
                rideId = "ride1"
            ),
            Message(
                id = "msg2",
                senderName = "Mike Chen",
                lastMessage = "Hey! Are you still available for the ride to the mall?",
                timestamp = LocalDateTime.now().minusHours(3),
                isRead = true,
                rideId = "ride2"
            ),
            Message(
                id = "msg3",
                senderName = "Emma Wilson",
                lastMessage = "The ride has been confirmed. See you tomorrow at 10 AM!",
                timestamp = LocalDateTime.now().minusDays(1),
                isRead = true,
                rideId = "ride3"
            ),
            Message(
                id = "msg4",
                senderName = "David Brown",
                lastMessage = "Sorry, I need to cancel the ride. Something urgent came up.",
                timestamp = LocalDateTime.now().minusDays(2),
                isRead = true
            ),
            Message(
                id = "msg5",
                senderName = "Lisa Garcia",
                lastMessage = "Great ride! Thanks for the smooth trip to the airport.",
                timestamp = LocalDateTime.now().minusDays(3),
                isRead = true,
                rideId = "ride5"
            )
        )
    }
    
    val filteredMessages = sampleMessages.filter { message ->
        searchQuery.isEmpty() || message.senderName.contains(searchQuery, ignoreCase = true) ||
        message.lastMessage.contains(searchQuery, ignoreCase = true)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Messages",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search messages") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredMessages.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No messages found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a conversation with other riders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMessages) { message ->
                    MessageItem(
                        message = message,
                        onClick = { onNavigateToChat(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (message.isRead) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (message.rideId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ride: ${message.rideId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (!message.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val formatter = when {
        timestamp.toLocalDate() == now.toLocalDate() -> DateTimeFormatter.ofPattern("HH:mm")
        timestamp.toLocalDate() == now.toLocalDate().minusDays(1) -> DateTimeFormatter.ofPattern("'Yesterday'")
        else -> DateTimeFormatter.ofPattern("MMM dd")
    }
    return timestamp.format(formatter)
}