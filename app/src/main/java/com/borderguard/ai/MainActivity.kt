package com.borderguard.ai

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.borderguard.ai.ui.theme.BorderGuardAITheme

class MainActivity : ComponentActivity() {

    private var selectedImageUri by mutableStateOf<Uri?>(null)

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedImageUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BorderGuardAITheme {
                BorderGuardApp(
                    selectedImageUri = selectedImageUri,
                    onPickImage = {
                        imagePicker.launch("image/*")
                    },
                    onImageCaptured = { uri ->
                        selectedImageUri = uri
                    }
                )
            }
        }
    }
}

@Composable
fun BorderGuardApp(
    selectedImageUri: Uri?,
    onPickImage: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {

    // Controls which screen is currently visible
    var showCamera by remember {
        mutableStateOf(false)
    }
    var documentData by remember { mutableStateOf<DocumentData?>(null) }
    var validationResults by remember {
        mutableStateOf<List<ValidationResult>>(emptyList())
    }
    var ocrText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // CAMERA SCREEN
    if (showCamera) {

        CameraScreen(
            onImageCaptured = { uri ->
                onImageCaptured(uri)
                showCamera = false
            },
            onBack = {
                showCamera = false
            }
        )

        return
    }

    // HOME SCREEN
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "🛡️",
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "BorderGuard AI",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Intelligent Document Screening",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(30.dp))

                if (selectedImageUri == null) {

                    Text(
                        text = "DOCUMENT SCREENING",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showCamera = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "📷  Scan Document",
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onPickImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "🖼️  Upload Document",
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            Text(
                                text = "🤖  AI-Powered Verification",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            FeatureText("✓ OCR Information Extraction")
                            FeatureText("✓ Document Validation")
                            FeatureText("✓ AI Tampering Detection")
                            FeatureText("✓ Face Verification")
                            FeatureText("✓ Risk Assessment")
                        }
                    }

                } else {

                    Text(
                        text = "DOCUMENT SELECTED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected document",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (selectedImageUri != null) {
                                isAnalyzing = true

                                OcrProcessor.processImage(
                                    context = context,
                                    imageUri = selectedImageUri,
                                    onSuccess = { text ->
                                        ocrText = text

                                        val parsedData = DocumentParser.parsePassport(text)
                                        documentData = parsedData

                                        validationResults =
                                            DocumentValidator.validatePassport(parsedData)
                                        println("VALIDATION RESULTS: $validationResults")

                                        isAnalyzing = false
                                    },
                                    onFailure = { error ->
                                        ocrText = "OCR failed: ${error.message}"
                                        isAnalyzing = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "🤖  Analyze Document",
                            fontSize = 17.sp
                        )
                    }
                    if (isAnalyzing) {
                        Text(
                            text = "🔍 Analyzing document...",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    if (ocrText.isNotEmpty()) {
                        Text(
                            text = "Extracted Text",
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(300.dp)
                        ) {
                            item {
                                Text(text = ocrText)
                            }
                        }
                    }
                    documentData?.let { data ->

                        Text(
                            text = "📄 Document Information",
                            modifier = Modifier.padding(top = 20.dp)
                        )

                        Text(
                            text = """
            Document Type: ${data.documentType}
            Passport Number: ${data.passportNumber}
            Surname: ${data.surname}
            Given Names: ${data.givenNames}
            Nationality: ${data.nationality}
            Date of Birth: ${data.dateOfBirth}
            Sex: ${data.sex}
            Expiry Date: ${data.expiryDate}
        """.trimIndent(),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    if (validationResults.isNotEmpty()) {

                        Text(
                            text = "🔍 Document Validation",
                            modifier = Modifier.padding(top = 20.dp)
                        )

                        validationResults.forEach { result ->

                            val icon = when (result.status) {
                                Status.VALID -> "✓"
                                Status.WARNING -> "⚠"
                                Status.INVALID -> "✗"
                            }

                            Text(
                                text = "$icon ${result.field}: ${result.message}",
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onPickImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("🔄  Choose Another")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Prototype • AI-assisted document screening",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FeatureText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}