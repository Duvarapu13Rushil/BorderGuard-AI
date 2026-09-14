package com.borderguard.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.borderguard.ai.ui.theme.BorderGuardAITheme

class MainActivity : ComponentActivity() {

    private var selectedImageUri by mutableStateOf<Uri?>(null)
    private var selectedPersonImageUri by mutableStateOf<Uri?>(null)

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedImageUri = uri
        }

    private val personImagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedPersonImageUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BorderGuardAITheme {

                BorderGuardApp(
                    selectedImageUri = selectedImageUri,
                    selectedPersonImageUri = selectedPersonImageUri,

                    onPickImage = {
                        imagePicker.launch("image/*")
                    },

                    onPickPersonImage = {
                        personImagePicker.launch("image/*")
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
    selectedPersonImageUri: Uri?,
    onPickImage: () -> Unit,
    onPickPersonImage: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {

    var showCamera by remember {
        mutableStateOf(false)
    }

    var documentData by remember {
        mutableStateOf<DocumentData?>(null)
    }

    var validationResults by remember {
        mutableStateOf<List<ValidationResult>>(emptyList())
    }

    var ocrText by remember {
        mutableStateOf("")
    }

    var isAnalyzing by remember {
        mutableStateOf(false)
    }

    var forgeryProbability by remember {
        mutableStateOf<Float?>(null)
    }

    var riskAssessment by remember {
        mutableStateOf<RiskAssessment?>(null)
    }

    var detectedFaceBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var personFaceBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var faceSimilarity by remember {
        mutableStateOf<Float?>(null)
    }

    var isFaceVerifying by remember {
        mutableStateOf(false)
    }

    var faceModelReady by remember {
        mutableStateOf(false)
    }

    var faceVerificationResult by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current


    /*
     * ---------------------------------------------------------
     * INITIALIZE FORGERY DETECTOR
     * ---------------------------------------------------------
     */

    val forgeryDetector =
        remember {
            ForgeryDetector(context)
        }


    /*
     * ---------------------------------------------------------
     * INITIALIZE FACE AI MODEL
     * ---------------------------------------------------------
     */

    LaunchedEffect(Unit) {

        try {

            FaceEmbeddingProcessor.initialize(context)

            faceModelReady = true

            println("FACE MODEL READY")

        } catch (error: Exception) {

            println(
                "FACE MODEL INITIALIZATION ERROR: " +
                        error.message
            )

            faceVerificationResult =
                "Face model initialization failed: ${error.message}"
        }
    }


    /*
     * ---------------------------------------------------------
     * CAMERA SCREEN
     * ---------------------------------------------------------
     */

    if (showCamera) {

        CameraScreen(
            onImageCaptured = { uri ->

                showCamera = false

                onImageCaptured(uri)
            },

            onBack = {

                showCamera = false
            }
        )

        return
    }


    /*
     * ---------------------------------------------------------
     * MAIN SCREEN
     * ---------------------------------------------------------
     */

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                /*
                 * =================================================
                 * HEADER
                 * =================================================
                 */

                Text(
                    text = "🛂 BorderGuard AI",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "AI-Powered Document Screening",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )


                Spacer(
                    modifier = Modifier.height(25.dp)
                )


                /*
                 * =================================================
                 * NO DOCUMENT SELECTED
                 * =================================================
                 */

                if (selectedImageUri == null) {

                    Text(
                        text =
                            "Screen passports and travel documents using OCR, AI forgery detection and biometric verification.",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )

                    Spacer(
                        modifier = Modifier.height(25.dp)
                    )


                    /*
                     * SCAN DOCUMENT
                     */

                    Button(
                        onClick = {

                            showCamera = true
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),

                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text = "📷 Scan Document",
                            fontSize = 17.sp
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    /*
                     * UPLOAD DOCUMENT
                     */

                    Button(
                        onClick = {

                            onPickImage()
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),

                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text = "🖼️ Upload Document",
                            fontSize = 17.sp
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )


                    /*
                     * FEATURES
                     */

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                            )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {

                            Text(
                                text =
                                    "🔍 Screening Modules",

                                fontSize = 19.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    "• OCR information extraction\n" +
                                            "• Passport/MRZ validation\n" +
                                            "• AI-powered forgery detection\n" +
                                            "• Face verification\n" +
                                            "• Risk assessment",

                                fontSize = 15.sp
                            )
                        }
                    }

                } else {


                    /*
                     * =================================================
                     * DOCUMENT PREVIEW
                     * =================================================
                     */

                    Text(
                        text = "📄 Selected Document",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription =
                            "Selected document",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),

                        contentScale =
                            ContentScale.Fit
                    )


                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    /*
                     * =================================================
                     * ANALYZE BUTTON
                     * =================================================
                     */

                    Button(

                        onClick = {

                            if (selectedImageUri != null) {

                                isAnalyzing = true

                                forgeryProbability =
                                    null

                                riskAssessment =
                                    null

                                detectedFaceBitmap =
                                    null

                                personFaceBitmap =
                                    null

                                faceSimilarity =
                                    null

                                faceVerificationResult =
                                    null


                                /*
                                 * OCR
                                 */

                                OcrProcessor.processImage(

                                    context = context,

                                    imageUri =
                                        selectedImageUri,

                                    onSuccess = { text ->

                                        ocrText = text


                                        /*
                                         * PARSE DOCUMENT
                                         */

                                        val parsedData =
                                            DocumentParser
                                                .parsePassport(
                                                    text
                                                )

                                        documentData =
                                            parsedData


                                        /*
                                         * VALIDATE DOCUMENT
                                         */

                                        validationResults =
                                            DocumentValidator
                                                .validatePassport(
                                                    parsedData
                                                )


                                        /*
                                         * LOAD BITMAP
                                         */

                                        try {

                                            val inputStream =
                                                context
                                                    .contentResolver
                                                    .openInputStream(
                                                        selectedImageUri
                                                    )

                                            val bitmap =
                                                inputStream?.use {

                                                    BitmapFactory
                                                        .decodeStream(
                                                            it
                                                        )
                                                }


                                            if (bitmap != null) {


                                                /*
                                                 * -----------------------------------------
                                                 * FACE DETECTION
                                                 * -----------------------------------------
                                                 */

                                                FaceDetectorProcessor
                                                    .detectFace(

                                                        bitmap = bitmap,

                                                        onSuccess = { detectedFace ->

                                                            detectedFaceBitmap =
                                                                detectedFace
                                                                    .croppedBitmap

                                                            println(
                                                                "FACE DETECTED"
                                                            )

                                                            println(
                                                                "FACE BOUNDS: " +
                                                                        detectedFace
                                                                            .boundingBox
                                                            )

                                                            println(
                                                                "FACE CROP SIZE: " +
                                                                        "${detectedFace.croppedBitmap.width}x${detectedFace.croppedBitmap.height}"
                                                            )
                                                        },

                                                        onFailure = { error ->

                                                            println(
                                                                "FACE DETECTION ERROR: " +
                                                                        error.message
                                                            )
                                                        }
                                                    )


                                                /*
                                                 * -----------------------------------------
                                                 * FORGERY DETECTION
                                                 * -----------------------------------------
                                                 */

                                                val probability =
                                                    forgeryDetector
                                                        .detect(
                                                            bitmap
                                                        )

                                                forgeryProbability =
                                                    probability


                                                /*
                                                 * -----------------------------------------
                                                 * RISK ASSESSMENT
                                                 * -----------------------------------------
                                                 */

                                                val assessment =
                                                    RiskAssessor
                                                        .calculateRisk(

                                                            forgeryProbability =
                                                                probability,

                                                            validationResults =
                                                                validationResults
                                                        )

                                                riskAssessment =
                                                    assessment


                                                println(
                                                    "AI REAL PROBABILITY: " +
                                                            probability
                                                )

                                                println(
                                                    "RISK SCORE: " +
                                                            assessment.score
                                                )

                                                println(
                                                    "RISK LEVEL: " +
                                                            assessment.level
                                                )
                                            }

                                        } catch (error: Exception) {

                                            println(
                                                "FORGERY DETECTION ERROR: " +
                                                        error.message
                                            )
                                        }


                                        println(
                                            "VALIDATION RESULTS: " +
                                                    validationResults
                                        )

                                        isAnalyzing = false
                                    },

                                    onFailure = { error ->

                                        ocrText =
                                            "OCR failed: ${error.message}"

                                        isAnalyzing = false
                                    }
                                )
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),

                        shape =
                            RoundedCornerShape(14.dp),

                        enabled = !isAnalyzing
                    ) {

                        Text(

                            text =
                                if (isAnalyzing) {

                                    "🔍 Analyzing..."

                                } else {

                                    "🔍 Analyze Document"
                                },

                            fontSize = 17.sp
                        )
                    }


                    /*
                     * ANALYZING MESSAGE
                     */

                    if (isAnalyzing) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "Running OCR, AI forgery detection and face detection...",
                            textAlign =
                                TextAlign.Center
                        )
                    }


                    /*
                     * =================================================
                     * DOCUMENT FACE
                     * =================================================
                     */

                    detectedFaceBitmap?.let { faceBitmap ->

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(

                                    text =
                                        "👤 Detected Document Face",

                                    fontSize = 19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(14.dp)
                                )

                                Image(

                                    bitmap =
                                        faceBitmap
                                            .asImageBitmap(),

                                    contentDescription =
                                        "Detected passport face",

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(220.dp),

                                    contentScale =
                                        ContentScale.Fit
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * PRESENTED PERSON
                     * =================================================
                     */

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(

                                text =
                                    "🧑 Presented Person",

                                fontSize = 19.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )


                            if (
                                selectedPersonImageUri ==
                                null
                            ) {

                                Text(

                                    text =
                                        "Upload a photo of the person presenting the document.",

                                    textAlign =
                                        TextAlign.Center,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(14.dp)
                                )

                                Button(

                                    onClick =
                                        onPickPersonImage,

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),

                                    shape =
                                        RoundedCornerShape(14.dp)
                                ) {

                                    Text(
                                        text =
                                            "🖼️ Upload Person Photo"
                                    )
                                }

                            } else {

                                AsyncImage(

                                    model =
                                        selectedPersonImageUri,

                                    contentDescription =
                                        "Presented person",

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(250.dp),

                                    contentScale =
                                        ContentScale.Fit
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Button(

                                    onClick =
                                        onPickPersonImage,

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),

                                    shape =
                                        RoundedCornerShape(14.dp)
                                ) {

                                    Text(
                                        text =
                                            "🔄 Choose Another Person"
                                    )
                                }
                            }
                        }
                    }


                    /*
                     * =================================================
                     * PERSON FACE PREVIEW
                     * =================================================
                     */

                    personFaceBitmap?.let { faceBitmap ->

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(

                                    text =
                                        "🧑 Detected Person Face",

                                    fontSize = 19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(14.dp)
                                )

                                Image(

                                    bitmap =
                                        faceBitmap
                                            .asImageBitmap(),

                                    contentDescription =
                                        "Detected person face",

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(220.dp),

                                    contentScale =
                                        ContentScale.Fit
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * VERIFY FACE BUTTON
                     * =================================================
                     */

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(

                        onClick = {

                            /*
                             * DOCUMENT FACE CHECK
                             */

                            if (detectedFaceBitmap == null) {

                                faceVerificationResult =
                                    "Document face not detected"

                                return@Button
                            }


                            /*
                             * PERSON IMAGE CHECK
                             */

                            if (selectedPersonImageUri == null) {

                                faceVerificationResult =
                                    "Please upload the presented person's photo"

                                return@Button
                            }


                            /*
                             * FACE MODEL CHECK
                             */

                            if (!faceModelReady) {

                                faceVerificationResult =
                                    "Face AI model is still loading. Please wait."

                                return@Button
                            }


                            isFaceVerifying =
                                true

                            faceSimilarity =
                                null

                            faceVerificationResult =
                                null


                            try {


                                /*
                                 * LOAD PERSON IMAGE
                                 */

                                val inputStream =
                                    context
                                        .contentResolver
                                        .openInputStream(
                                            selectedPersonImageUri
                                        )

                                val personBitmap =
                                    inputStream?.use {

                                        BitmapFactory
                                            .decodeStream(
                                                it
                                            )
                                    }


                                if (personBitmap == null) {

                                    throw Exception(
                                        "Could not load person image"
                                    )
                                }


                                /*
                                 * DETECT PERSON FACE
                                 */

                                FaceDetectorProcessor
                                    .detectFace(

                                        bitmap =
                                            personBitmap,

                                        onSuccess = { detectedPersonFace ->

                                            personFaceBitmap =
                                                detectedPersonFace
                                                    .croppedBitmap


                                            try {


                                                /*
                                                 * DOCUMENT EMBEDDING
                                                 */

                                                val documentEmbedding =
                                                    FaceEmbeddingProcessor
                                                        .getEmbedding(
                                                            detectedFaceBitmap!!
                                                        )


                                                /*
                                                 * PERSON EMBEDDING
                                                 */

                                                val personEmbedding =
                                                    FaceEmbeddingProcessor
                                                        .getEmbedding(
                                                            detectedPersonFace
                                                                .croppedBitmap
                                                        )


                                                /*
                                                 * COSINE SIMILARITY
                                                 */

                                                val similarity =
                                                    FaceEmbeddingProcessor
                                                        .cosineSimilarity(

                                                            documentEmbedding,

                                                            personEmbedding
                                                        )


                                                faceSimilarity =
                                                    similarity


                                                /*
                                                 * PROTOTYPE THRESHOLD
                                                 */

                                                val isMatch =
                                                    similarity >= 0.60f

                                                faceVerificationResult =
                                                    if (isMatch) {
                                                        "MATCH"
                                                    } else {
                                                        "MISMATCH"
                                                    }
                                                /*
 * Update overall risk score using
 * the face verification result.
 */

                                                forgeryProbability?.let { probability ->

                                                    val updatedRisk =
                                                        RiskAssessor.calculateRisk(

                                                            forgeryProbability =
                                                                probability,

                                                            validationResults =
                                                                validationResults,

                                                            faceMatch =
                                                                isMatch
                                                        )

                                                    riskAssessment =
                                                        updatedRisk

                                                    println(
                                                        "UPDATED RISK SCORE: " +
                                                                updatedRisk.score
                                                    )

                                                    println(
                                                        "UPDATED RISK LEVEL: " +
                                                                updatedRisk.level
                                                    )

                                                    println(
                                                        "UPDATED RECOMMENDATION: " +
                                                                updatedRisk.recommendation
                                                    )
                                                }


                                                println(
                                                    "FACE SIMILARITY: " +
                                                            similarity
                                                )

                                            } catch (error: Exception) {

                                                faceVerificationResult =
                                                    "Face embedding failed: ${error.message}"

                                                println(
                                                    "FACE EMBEDDING ERROR: " +
                                                            error.stackTraceToString()
                                                )
                                            }


                                            isFaceVerifying =
                                                false
                                        },


                                        onFailure = { error ->

                                            faceVerificationResult =
                                                "Person face detection failed: ${error.message}"

                                            println(
                                                "PERSON FACE DETECTION ERROR: " +
                                                        error.stackTraceToString()
                                            )

                                            isFaceVerifying =
                                                false
                                        }
                                    )

                            } catch (error: Exception) {

                                faceVerificationResult =
                                    "Face verification failed: ${error.message}"

                                println(
                                    "FACE VERIFICATION ERROR: " +
                                            error.stackTraceToString()
                                )

                                isFaceVerifying =
                                    false
                            }
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),

                        shape =
                            RoundedCornerShape(14.dp),

                        enabled =
                            !isFaceVerifying &&
                                    faceModelReady
                    ) {

                        Text(

                            text =
                                when {

                                    isFaceVerifying ->
                                        "🔍 Verifying Face..."

                                    !faceModelReady ->
                                        "⏳ Loading Face AI..."

                                    else ->
                                        "👤 Verify Face"
                                },

                            fontSize = 17.sp
                        )
                    }


                    /*
                     * =================================================
                     * FACE VERIFICATION RESULT
                     * =================================================
                     *
                     * IMPORTANT:
                     * We display this based on faceVerificationResult,
                     * NOT faceSimilarity.
                     *
                     * This means errors will now actually appear
                     * on screen instead of looking like "nothing happened".
                     */

                    faceVerificationResult?.let { result ->

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(

                                    text =
                                        "👤 Face Verification",

                                    fontSize = 19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(

                                    text =
                                        result,

                                    fontSize = 22.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    textAlign =
                                        TextAlign.Center
                                )


                                faceSimilarity?.let {
                                        similarity ->

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )

                                    Text(

                                        text =
                                            "Similarity: %.4f"
                                                .format(
                                                    similarity
                                                ),

                                        fontSize =
                                            16.sp
                                    )
                                }
                            }
                        }
                    }


                    /*
                     * =================================================
                     * OCR OUTPUT
                     * =================================================
                     */

                    if (ocrText.isNotEmpty()) {

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                            ) {

                                Text(

                                    text =
                                        "📝 OCR Extracted Text",

                                    fontSize =
                                        19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    text =
                                        ocrText,
                                    fontSize =
                                        14.sp
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * DOCUMENT INFORMATION
                     * =================================================
                     */

                    documentData?.let { data ->

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                            ) {

                                Text(

                                    text =
                                        "📋 Document Information",

                                    fontSize =
                                        19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    text =
                                        "Document Type: ${data.documentType}"
                                )

                                Text(
                                    text =
                                        "Passport Number: ${data.passportNumber}"
                                )

                                Text(
                                    text =
                                        "Surname: ${data.surname}"
                                )

                                Text(
                                    text =
                                        "Given Names: ${data.givenNames}"
                                )

                                Text(
                                    text =
                                        "Nationality: ${data.nationality}"
                                )

                                Text(
                                    text =
                                        "Date of Birth: ${data.dateOfBirth}"
                                )

                                Text(
                                    text =
                                        "Sex: ${data.sex}"
                                )

                                Text(
                                    text =
                                        "Expiry Date: ${data.expiryDate}"
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * VALIDATION RESULTS
                     * =================================================
                     */

                    if (
                        validationResults.isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                            ) {

                                Text(

                                    text =
                                        "✅ Document Validation",

                                    fontSize =
                                        19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                validationResults
                                    .forEach { result ->

                                        Text(

                                            text =
                                                "${result.status}: ${result.field} — ${result.message}",

                                            fontSize =
                                                14.sp
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(6.dp)
                                        )
                                    }
                            }
                        }
                    }


                    /*
                     * =================================================
                     * FORGERY DETECTION
                     * =================================================
                     */

                    forgeryProbability?.let {
                            probability ->

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(18.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(

                                    text =
                                        "🤖 AI Forgery Detection",

                                    fontSize =
                                        19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(

                                    text =
                                        "Real Probability: %.2f%%"
                                            .format(
                                                probability * 100f
                                            ),

                                    fontSize =
                                        18.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(

                                    text =
                                        if (
                                            probability >=
                                            0.5f
                                        ) {

                                            "Document appears REAL"

                                        } else {

                                            "Document appears FAKE"
                                        },

                                    fontSize =
                                        20.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    textAlign =
                                        TextAlign.Center
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * RISK ASSESSMENT
                     * =================================================
                     */

                    /*
 * =================================================
 * FINAL BORDERGUARD SCREENING RESULT
 * =================================================
 */

                    riskAssessment?.let { assessment ->

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                /*
                                 * HEADER
                                 */

                                Text(
                                    text = "🛂 BorderGuard Screening Result",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(
                                    modifier = Modifier.height(18.dp)
                                )


                                /*
                                 * RISK SCORE
                                 */

                                Text(
                                    text = "FINAL RISK SCORE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "${assessment.score}/100",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = assessment.level,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )


                                /*
                                 * RECOMMENDATION
                                 */

                                Text(
                                    text = assessment.recommendation,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )


                                Spacer(
                                    modifier = Modifier.height(22.dp)
                                )


                                /*
                                 * SCREENING SIGNALS
                                 */

                                Text(
                                    text = "Screening Signals",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )


                                /*
                                 * AI FORGERY SIGNAL
                                 */

                                forgeryProbability?.let { probability ->

                                    val aiPassed =
                                        probability >= 0.5f

                                    Text(
                                        text =
                                            if (aiPassed) {
                                                "🟢 AI document authenticity — PASS"
                                            } else {
                                                "🔴 AI document authenticity — SUSPICIOUS"
                                            },

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        fontSize = 15.sp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )
                                }


                                /*
                                 * DOCUMENT VALIDATION SIGNAL
                                 */

                                val invalidCount =
                                    validationResults.count {
                                        it.status == Status.INVALID
                                    }

                                val warningCount =
                                    validationResults.count {
                                        it.status == Status.WARNING
                                    }

                                if (invalidCount == 0) {

                                    Text(
                                        text =
                                            "🟢 Document validation — PASS",

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        fontSize = 15.sp
                                    )

                                } else {

                                    Text(
                                        text =
                                            "🔴 Document validation — $invalidCount issue(s)",

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        fontSize = 15.sp
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )


                                /*
                                 * MRZ SIGNAL
                                 */

                                val mrzResult =
                                    validationResults.find {
                                        it.field == "MRZ"
                                    }

                                if (mrzResult != null) {

                                    Text(
                                        text =
                                            if (
                                                mrzResult.status ==
                                                Status.VALID
                                            ) {

                                                "🟢 MRZ validation — PASS"

                                            } else {

                                                "🔴 MRZ validation — FAILED"
                                            },

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        fontSize = 15.sp
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )


                                /*
                                 * FACE SIGNAL
                                 */

                                when {

                                    faceVerificationResult ==
                                            "MATCH" -> {

                                        Text(
                                            text =
                                                "🟢 Face verification — MATCH",

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            fontSize = 15.sp
                                        )
                                    }

                                    faceVerificationResult ==
                                            "MISMATCH" -> {

                                        Text(
                                            text =
                                                "🔴 Face verification — MISMATCH",

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            fontSize = 15.sp
                                        )
                                    }

                                    else -> {

                                        Text(
                                            text =
                                                "🟡 Face verification — NOT PERFORMED",

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            fontSize = 15.sp
                                        )
                                    }
                                }


                                /*
                                 * DETAILS
                                 */

                                if (
                                    invalidCount > 0 ||
                                    warningCount > 0
                                ) {

                                    Spacer(
                                        modifier =
                                            Modifier.height(18.dp)
                                    )

                                    Text(
                                        text = "Risk Factors",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(10.dp)
                                    )


                                    validationResults
                                        .filter {
                                            it.status ==
                                                    Status.INVALID ||
                                                    it.status ==
                                                    Status.WARNING
                                        }
                                        .forEach { result ->

                                            Text(
                                                text =
                                                    if (
                                                        result.status ==
                                                        Status.INVALID
                                                    ) {
                                                        "⚠️ ${result.field}: ${result.message}"
                                                    } else {
                                                        "🟡 ${result.field}: ${result.message}"
                                                    },

                                                modifier =
                                                    Modifier.fillMaxWidth(),

                                                fontSize = 14.sp
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(6.dp)
                                            )
                                        }


                                    /*
                                     * FACE MISMATCH RISK FACTOR
                                     */

                                    if (
                                        faceVerificationResult ==
                                        "MISMATCH"
                                    ) {

                                        Text(
                                            text =
                                                "⚠️ Face mismatch: presented person does not match document owner",

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            fontSize = 14.sp
                                        )
                                    }
                                }


                                /*
                                 * FOOTER
                                 */

                                Spacer(
                                    modifier =
                                        Modifier.height(18.dp)
                                )

                                Text(
                                    text =
                                        "AI-assisted screening • Final decision requires officer review",

                                    fontSize = 12.sp,

                                    textAlign =
                                        TextAlign.Center,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }


                    /*
                     * =================================================
                     * CHOOSE ANOTHER DOCUMENT
                     * =================================================
                     */

                    Spacer(
                        modifier =
                            Modifier.height(25.dp)
                    )

                    Button(

                        onClick = {

                            onPickImage()
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text =
                                "🔄 Choose Another Document"
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}