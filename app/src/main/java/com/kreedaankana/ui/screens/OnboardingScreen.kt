package com.kreedaankana.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kreedaankana.model.Sport
import com.kreedaankana.ui.components.SportChip
import com.kreedaankana.ui.theme.*
import com.kreedaankana.viewmodel.MainViewModel

private enum class AuthMode { CHOOSE, SIGNUP, LOGIN }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var mode by remember { mutableStateOf(AuthMode.CHOOSE) }
    val isLoading by viewModel.isLoading.collectAsState()
    val message   by viewModel.message.collectAsState()
    val snackbar   = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SportGreen, SportGreenMid, Color(0xFF1A2E1B))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // Logo / Title
                Text("🏟️", fontSize = 72.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kreeda-Ankana",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )
                Text(
                    "Sports Ground & Match Organizer",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "\"Turning Village Grounds into Organised Sports Hubs\"",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = SportOrangeLight,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(32.dp))

                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn() + slideInVertically { it / 4 } togetherWith
                        fadeOut() + slideOutVertically { -it / 4 }
                    },
                    label = "auth_mode"
                ) { currentMode ->
                    when (currentMode) {
                        AuthMode.CHOOSE -> ChooseCard(
                            onSignup = { mode = AuthMode.SIGNUP },
                            onLogin  = { mode = AuthMode.LOGIN }
                        )
                        AuthMode.SIGNUP -> SignupCard(
                            viewModel = viewModel,
                            isLoading = isLoading,
                            onBack    = { mode = AuthMode.CHOOSE }
                        )
                        AuthMode.LOGIN  -> LoginCard(
                            viewModel = viewModel,
                            isLoading = isLoading,
                            onBack    = { mode = AuthMode.CHOOSE }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeaturePill("📅 Book Slots")
                    FeaturePill("⚔️ Challenges")
                    FeaturePill("🏆 Live Rankings")
                }
                Spacer(Modifier.height(12.dp))
                FeaturePill("📱 Social · scores sync to everyone's app in real-time")
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Choose Card ───────────────────────────────────────────────────────────────
@Composable
private fun ChooseCard(onSignup: () -> Unit, onLogin: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Get Started", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = SportGreen)
            Spacer(Modifier.height(8.dp))
            Text(
                "Join Kreeda-Ankana to book grounds, post challenges & track your team's performance",
                style     = MaterialTheme.typography.bodySmall,
                color     = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            // Sign Up
            Button(
                onClick  = onSignup,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportGreenMid)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Up — Register New Team", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Log In
            OutlinedButton(
                onClick  = onLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = androidx.compose.foundation.BorderStroke(2.dp, SportGreenMid)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, tint = SportGreenMid)
                Spacer(Modifier.width(8.dp))
                Text("Log In — Existing Team", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = SportGreenMid)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Team names are unique — like Instagram usernames.\nOne team name per group.",
                style     = MaterialTheme.typography.bodySmall,
                color     = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Sign Up Card ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignupCard(viewModel: MainViewModel, isLoading: Boolean, onBack: () -> Unit) {
    var teamName      by remember { mutableStateOf("") }
    var captainName   by remember { mutableStateOf("") }
    var captainPhone  by remember { mutableStateOf("") }
    var village       by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf(Sport.CRICKET) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = SportGreenMid)
                }
                Column {
                    Text("Register Your Team", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, color = SportGreen)
                    Text("Team name must be unique — choose wisely!",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(16.dp))

            KreedaTextField(teamName, { teamName = it }, "Team Name *", Icons.Default.Group,
                "e.g., Sunrise XI  (unique, like a username)")
            Spacer(Modifier.height(12.dp))
            KreedaTextField(captainName, { captainName = it }, "Captain's Name *", Icons.Default.Person,
                "Your full name")
            Spacer(Modifier.height(12.dp))
            KreedaTextField(captainPhone, { captainPhone = it }, "Mobile Number *", Icons.Default.Phone,
                "+91 XXXXX XXXXX", KeyboardType.Phone)
            Spacer(Modifier.height(12.dp))
            KreedaTextField(village, { village = it }, "Village / Area *", Icons.Default.LocationOn,
                "e.g., Rajpur Village")

            Spacer(Modifier.height(16.dp))
            Text("Primary Sport", style = MaterialTheme.typography.labelLarge,
                color = SportGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sport.values().forEach { sport ->
                    SportChip(sport = sport, selected = selectedSport == sport,
                        onClick = { selectedSport = sport })
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.registerTeam(teamName.trim(), captainName.trim(),
                        captainPhone.trim(), village.trim(), selectedSport)
                },
                enabled  = teamName.isNotBlank() && captainName.isNotBlank()
                        && captainPhone.isNotBlank() && village.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportGreenMid)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.SportsCricket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Register Team", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Log In Card ───────────────────────────────────────────────────────────────
@Composable
private fun LoginCard(viewModel: MainViewModel, isLoading: Boolean, onBack: () -> Unit) {
    var teamName     by remember { mutableStateOf("") }
    var captainPhone by remember { mutableStateOf("") }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = SportGreenMid)
                }
                Column {
                    Text("Log In", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, color = SportGreen)
                    Text("Enter your team name to log in",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(20.dp))

            KreedaTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = "Team Name",
                icon  = Icons.Default.Group,
                placeholder = "Your exact team name"
            )
            Spacer(Modifier.height(12.dp))
            KreedaTextField(
                value = captainPhone,
                onValueChange = { captainPhone = it },
                label = "Mobile Number",
                icon  = Icons.Default.Phone,
                placeholder = "+91 XXXXX XXXXX",
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "💡 Your team name is your unique identifier — like an Instagram username. Phone numbers can be shared across teams.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick  = { viewModel.loginTeam(teamName.trim(), captainPhone.trim()) },
                enabled  = teamName.isNotBlank() && captainPhone.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SportGreenMid)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have a team? ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                TextButton(onClick = onBack) {
                    Text("Sign Up", color = SportGreenMid, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ── Shared Components ─────────────────────────────────────────────────────────

@Composable
internal fun KreedaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = Color.Gray) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = SportGreenMid) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = SportGreenMid,
            focusedLabelColor    = SportGreenMid,
            cursorColor          = SportGreenMid,
            // ── Fix: make input text always dark and readable ──
            focusedTextColor     = Color(0xFF111111),
            unfocusedTextColor   = Color(0xFF111111)
        ),
        singleLine = true
    )
}

@Composable
private fun FeaturePill(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Text(
            text, color = Color.White,
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style     = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
    }
}
