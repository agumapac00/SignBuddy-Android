package com.example.signbuddy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun SignBuddyUsernameScreen(navController: NavController, modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Student") }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFFFE5C2), Color(0xFFE1F5FE))
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Logo",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome to SignBuddy 👋",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Learn sign language in a fun way!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { if (it.length <= 20) username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select role",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Student Role
                RoleCard(
                    title = "Student",
                    icon = Icons.Filled.Person,
                    isSelected = selectedRole == "Student",
                    selectedColor = Color(0xFFBBDEFB)
                ) { selectedRole = "Student" }

                // Teacher Role
                RoleCard(
                    title = "Teacher",
                    icon = Icons.Filled.School,
                    isSelected = selectedRole == "Teacher",
                    selectedColor = Color(0xFFC8E6C9)
                ) {
                    selectedRole = "Teacher"
                    navController.navigate("teacherLogin")
                }

                // Admin Role
                RoleCard(
                    title = "Admin",
                    icon = Icons.Filled.AdminPanelSettings,
                    isSelected = selectedRole == "Admin",
                    selectedColor = Color(0xFFFFCDD2)
                ) {
                    selectedRole = "Admin"
                    navController.navigate("adminLogin")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ✅ Animated Continue Button
            ContinueButton(
                username = username,
                selectedRole = selectedRole,
                navController = navController
            )
        }
    }
}

@Composable
fun ContinueButton(
    username: String,
    selectedRole: String,
    navController: NavController
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "buttonScale"
    )

    Button(
        onClick = {
            when (selectedRole) {
                "Student" -> {
                    if (username.isNotBlank()) {
                        navController.navigate("studentDashboard/$username")
                    }
                }
                "Teacher" -> navController.navigate("teacherLogin")
                "Admin" -> navController.navigate("adminLogin")
            }
        },
        enabled = username.isNotBlank(),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (username.isNotBlank()) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF42A5F5), Color(0xFF1976D2))
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
                    },
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Continue",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(104.dp)
            .clickable { onClick() }
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedColor else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignBuddyUsernameScreen() {
    val navController = rememberNavController()
    SignBuddyUsernameScreen(navController = navController)
}
