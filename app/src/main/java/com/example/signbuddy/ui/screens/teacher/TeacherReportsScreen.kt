package com.example.signbuddy.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.signbuddy.services.TeacherService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherReportsScreen(navController: NavController? = null, teacherId: String = "") {
    // Real data state
    var classStats by remember { mutableStateOf<TeacherService.ClassPerformanceStats?>(null) }
    var studentReports by remember { mutableStateOf<List<TeacherService.StudentReport>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReportType by remember { mutableStateOf("overview") }
    val teacherService = remember { TeacherService() }
    val scope = rememberCoroutineScope()
    
    // Fetch reports data
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                classStats = teacherService.getClassPerformanceStats(teacherId)
                studentReports = teacherService.getStudentReports(teacherId)
            } catch (e: Exception) {
                // Handle error - keep null data
            }
        }
        isLoading = false
    }
    
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE3F2FD), Color(0xFFFFF3E0), Color(0xFFF3E5F5))
    )
    
    Scaffold(topBar = { TopAppBar(title = { Text("AI Reports & Insights") }) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Report Type Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📊 Report Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { selectedReportType = "overview" },
                            label = { Text("Overview") },
                            selected = selectedReportType == "overview",
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            onClick = { selectedReportType = "individual" },
                            label = { Text("Individual") },
                            selected = selectedReportType == "individual",
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            onClick = { selectedReportType = "class" },
                            label = { Text("Class") },
                            selected = selectedReportType == "class",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (classStats == null || studentReports == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📈", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Reports Available",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reports will appear once students start learning!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                when (selectedReportType) {
                    "overview" -> OverviewReport(classStats!!, studentReports!!)
                    "individual" -> IndividualStudentReports(studentReports!!)
                    "class" -> ClassLevelReports(classStats!!)
                }
            }
            
            // AI Insights Section
            if (classStats != null && studentReports != null) {
                AIInsightsSection(classStats!!, studentReports!!)
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* Export reports */ },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📤 Export Reports")
                }
                
                Button(
                    onClick = { navController?.popBackStack() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun OverviewReport(classStats: TeacherService.ClassPerformanceStats, studentReports: List<TeacherService.StudentReport>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📈 Overview Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    title = "Total Students",
                    value = "${classStats.totalStudents}",
                    icon = Icons.Filled.Person,
                    color = Color(0xFF2196F3)
                )
                MetricCard(
                    title = "Avg Accuracy",
                    value = "${(classStats.averageAccuracy * 100).toInt()}%",
                    icon = Icons.Filled.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    title = "Total Sessions",
                    value = "${classStats.totalSessions}",
                    icon = Icons.Filled.TrendingUp,
                    color = Color(0xFF9C27B0)
                )
                MetricCard(
                    title = "Avg Speed",
                    value = "${classStats.averageSpeed.toInt()}s",
                    icon = Icons.Filled.Speed,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun IndividualStudentReports(studentReports: List<TeacherService.StudentReport>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "👥 Individual Student Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            studentReports.take(5).forEach { report ->
                StudentReportCard(report)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ClassLevelReports(classStats: TeacherService.ClassPerformanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🏫 Class-Level Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accuracy Distribution
            Text(
                text = "Accuracy Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            classStats.accuracyDistribution.forEach { (range, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(range)
                    Text("$count students", fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Common Mistakes
            Text(
                text = "Common Mistakes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            classStats.commonMistakes.entries.take(5).forEach { (letter, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Letter '$letter'")
                    Text("$count mistakes", color = Color(0xFFFF5722))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun AIInsightsSection(classStats: TeacherService.ClassPerformanceStats, studentReports: List<TeacherService.StudentReport>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Insights, contentDescription = null, tint = Color(0xFF9C27B0))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🤖 AI Insights & Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate AI suggestions based on data
            val suggestions = generateAISuggestions(classStats, studentReports)
            
            suggestions.forEach { suggestion ->
                AssistChip(
                    onClick = { /* Handle suggestion click */ },
                    label = { Text(suggestion) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StudentReportCard(report: TeacherService.StudentReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = report.studentName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Accuracy: ${(report.accuracy * 100).toInt()}%")
                Text("Level: ${report.level}")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sessions: ${report.totalSessions}")
                Text("Score: ${report.totalScore}")
            }
        }
    }
}

fun generateAISuggestions(classStats: TeacherService.ClassPerformanceStats, studentReports: List<TeacherService.StudentReport>): List<String> {
    val suggestions = mutableListOf<String>()
    
    if (classStats.averageAccuracy < 0.7f) {
        suggestions.add("Focus on accuracy improvement - class average is ${(classStats.averageAccuracy * 100).toInt()}%")
    }
    
    if (classStats.commonMistakes.isNotEmpty()) {
        val topMistake = classStats.commonMistakes.maxByOrNull { it.value }
        if (topMistake != null) {
            suggestions.add("Focus on letter '${topMistake.key}' - ${topMistake.value} students struggling")
        }
    }
    
    if (classStats.totalSessions < 50) {
        suggestions.add("Encourage more practice sessions - only ${classStats.totalSessions} total sessions")
    }
    
    val strugglingStudents = studentReports.count { it.accuracy < 0.5f }
    if (strugglingStudents > 0) {
        suggestions.add("$strugglingStudents students need extra attention (accuracy < 50%)")
    }
    
    if (suggestions.isEmpty()) {
        suggestions.add("Great job! Class is performing well overall")
    }
    
    return suggestions
}


