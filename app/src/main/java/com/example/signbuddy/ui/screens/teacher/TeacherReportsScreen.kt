package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherReportsScreen(navController: NavController? = null) {
    Scaffold(topBar = { TopAppBar(title = { Text("AI Reports & Insights") }) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Individual Student Reports")
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accuracy per letter (Bar Chart)")
                    Text("Average response time (Line Chart)")
                    Text("Consistency trend (Sparkline)")
                    Text("Engagement (Session time)")
                }
            }

            Divider()
            Text("Class-Level Reports")
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Class accuracy distribution")
                    Text("Most common mistakes (Heatmap)")
                    Text("Average speed per difficulty level")
                    Text("Leaderboard insights (Top 5, gaps)")
                }
            }

            Divider()
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Assessment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("AI Suggestions")
                        AssistChip(onClick = { /* open suggestion */ }, label = { Text("Focus two-hand gestures (R, K)") })
                        AssistChip(onClick = { /* open detail */ }, label = { Text("Accuracy +20% vs last week") })
                    }
                }
            }

            Button(onClick = { navController?.popBackStack() }, shape = RoundedCornerShape(10.dp)) { Text("Back") }
        }
    }
}


