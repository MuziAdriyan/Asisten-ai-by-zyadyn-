// app/src/main/java/com/muzidev/zyyai/ui/screens/HomeScreen.kt
package com.muzidev.zyyai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzidev.zyyai.data.local.ChatMessageEntity
import com.muzidev.zyyai.ui.components.CustomIcons
import com.muzidev.zyyai.ui.theme.*
import com.muzidev.zyyai.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: ChatViewModel) {
    val messages by viewModel.currentMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(messages.size, viewModel.streamingText.collectAsState().value) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ZYY AI", color = CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("by MUZI DEV", color = TextSecondary, fontSize = 11.sp)
            }

            if (isOffline) {
                Surface(
                    color = ErrorRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Offline Mode", color = ErrorRed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                }
            }
        }

        // Error Notice Bar
        errorMessage?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(err, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.retryLastMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Coba Lagi", fontSize = 10.sp)
                }
            }
        }

        // Chat Message Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                ChatBubbleView(msg = msg, onCopy = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ZYY AI", text))
                })
            }

            if (isGenerating) {
                item {
                    val streamText by viewModel.streamingText.collectAsState()
                    ChatBubbleView(
                        msg = ChatMessageEntity(
                            sessionId = "",
                            sender = "ASSISTANT",
                            content = streamText.ifEmpty { "Sedang berpikir..." },
                            timestamp = System.currentTimeMillis()
                        ),
                        onCopy = {}
                    )
                }
            }
        }

        // Bottom Input Area
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Tanyakan sesuatu...", color = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )

                if (isGenerating) {
                    IconButton(
                        onClick = { viewModel.stopGeneration() },
                        modifier = Modifier
                            .background(ErrorRed, CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(CustomIcons.Stop, contentDescription = "Stop", tint = Color.White)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .background(CyanAccent, CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(CustomIcons.Send, contentDescription = "Send", tint = BackgroundDark)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleView(msg: ChatMessageEntity, onCopy: (String) -> Unit) {
    val isUser = msg.sender == "USER"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) BlueAccent.copy(alpha = 0.3f) else SurfaceCard

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(bgColor)
                .border(1.dp, if (isUser) BlueAccent else CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                if (msg.content.contains("```")) {
                    FormattedCodeContent(msg.content)
                } else {
                    Text(msg.content, color = TextPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onCopy(msg.content) }, modifier = Modifier.size(18.dp)) {
                        Icon(CustomIcons.Copy, contentDescription = "Copy", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedCodeContent(content: String) {
    val parts = content.split("```")
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // Code Block
                val lines = part.trim().lines()
                val lang = if (lines.isNotEmpty() && !lines.first().contains(" ")) lines.first() else "code"
                val codeBody = if (lines.size > 1) lines.drop(1).joinToString("\n") else part

                Surface(
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(lang.uppercase(), color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(codeBody, color = Color(0xFFE6EDE3), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            } else {
                if (part.isNotBlank()) {
                    Text(part, color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}
