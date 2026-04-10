package com.example.kotlin_chatbot

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ai.edge.litertlm.*
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.CenterAlignedTopAppBar

// ---------- DATA ----------

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

// ---------- ACTIVITY ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen()
                }
            }
        }
    }
}

// ---------- GEMMA MANAGER ----------

class Gemma4Manager(private val context: Context) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun initialize() {
        if (conversation != null) return

        if (engine == null) {
            val config = EngineConfig(
                modelPath = "/data/local/tmp/llm/gemma-4-E2B-it.litertlm",
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )

            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
        }

        if (conversation == null) {
            conversation = engine!!.createConversation()
        }
    }

    fun reply(prompt: String): String {
        val currentConversation = conversation ?: error("Conversation not initialized")
        return currentConversation.sendMessage(prompt).toString()
    }

    fun close() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }
}

// ---------- UI ----------


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello! I am your on-device Gemma 4:E2B chatbot.", false)
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val gemmaManager = remember { Gemma4Manager(context) }

    DisposableEffect(Unit) {
        onDispose { gemmaManager.close() }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---------- HEADER (STICKY) ----------
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Gemma",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemma 4:E2B Chatbot")
                }
            }
        )

        // ---------- CHAT LIST ----------
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                if (message.isUser) {
                    UserMessageBubble(message)
                } else {
                    AgentMessageBubble(message.text)
                }
            }

            if (isLoading) {
                item {
                    AgentMessageBubble("Thinking...")
                }
            }
        }

        // ---------- INPUT ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = !isLoading,
                onClick = {
                    val userMessage = inputText.trim()
                    if (userMessage.isNotEmpty()) {
                        messages.add(ChatMessage(userMessage, true))
                        inputText = ""
                        isLoading = true

                        scope.launch {
                            val reply = withContext(Dispatchers.IO) {
                                try {
                                    gemmaManager.initialize()
                                    gemmaManager.reply(userMessage)
                                } catch (e: Exception) {
                                    "Model error: ${e.message}"
                                }
                            }

                            messages.add(ChatMessage(reply, false))
                            isLoading = false
                        }
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}

// ---------- BUBBLES ----------

@Composable
fun UserMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFDCF8C6), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(message.text)
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(Icons.Default.Person, contentDescription = "User")
    }
}

@Composable
fun AgentMessageBubble(markdown: String) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = "Gemma")

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .background(Color(0xFFEAEAEA), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            AndroidView(
                factory = { ctx -> TextView(ctx) },
                update = { tv -> markwon.setMarkdown(tv, markdown) }
            )
        }
    }
}
