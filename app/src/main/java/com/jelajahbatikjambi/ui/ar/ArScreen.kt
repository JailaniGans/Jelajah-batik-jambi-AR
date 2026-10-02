package com.jelajahbatikjambi.ui.ar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jelajahbatikjambi.ar.ArState
import com.jelajahbatikjambi.ar.Quaternion
import com.jelajahbatikjambi.ar.Vector3
import com.jelajahbatikjambi.ar.normalized
import com.jelajahbatikjambi.ar.plus
import com.jelajahbatikjambi.ar.times
import com.jelajahbatikjambi.camera.CameraAnalyzer
import com.jelajahbatikjambi.camera.CameraController
import com.jelajahbatikjambi.render.NativeSupport
import com.jelajahbatikjambi.ui.common.withClickSound
import com.jelajahbatikjambi.ui.theme.Dimensions

/** Radians of rotation per pixel of one-finger drag — tuned so a comfortable swipe spins the object roughly a third of a turn. */
private const val ROTATION_SENSITIVITY = 0.01f

/** Meters of drag-and-drop offset per pixel of two-finger pan — tuned against the ~0.2m assumed motif size (§ [com.jelajahbatikjambi.ar.ImageTargetDetector]). */
private const val PAN_SENSITIVITY = 0.0008f

/** Pinch-to-zoom bounds so the object can't be shrunk to nothing or blown up past reason. */
private const val MIN_USER_SCALE = 0.3f
private const val MAX_USER_SCALE = 3f

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

/**
 * AR scanner screen: camera permission flow, full-screen CameraX preview,
 * the Filament 3D overlay following the tracked marker, and the AR
 * information panel that appears once a marker is confirmed (§7, §9).
 */
@Composable
fun ArScreen(onBack: () -> Unit, onViewDetail: (Int) -> Unit, onStartQuiz: (Int) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var hasRequestedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        hasRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isPermanentlyDenied = !hasPermission && hasRequestedOnce &&
        activity != null &&
        !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )

    val arViewModel: ArViewModel = viewModel()
    val arState by arViewModel.state.collectAsStateWithLifecycle()
    val arPose by arViewModel.pose.collectAsStateWithLifecycle()
    val detectedRegion by arViewModel.detectedRegion.collectAsStateWithLifecycle()
    val detectedBatik by arViewModel.detectedBatik.collectAsStateWithLifecycle()

    // Resets automatically whenever the confirmed motif changes (or clears),
    // so dismissing the panel for one motif doesn't suppress it for the next.
    var panelDismissed by remember(detectedBatik?.id) { mutableStateOf(false) }

    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var hasFlash by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }

    // §27 3D interaction, extended per user request: one-finger drag spins
    // the object, two-plus-finger drag moves it (drag-and-drop), pinch
    // zooms it in/out, and double-tap resets all three. All reset
    // automatically whenever the tracked motif changes, so a manual
    // spin/move/zoom on one motif doesn't carry over and look wrong on the
    // next — and this state living in ArScreen rather than ArViewModel is
    // exactly why it survives a tracking dropout: it's independent of the
    // (now-sticky) [ArViewModel.pose]/[ArViewModel.detectedBatik] it's
    // layered on top of, only ever reset by an actual motif switch.
    var userRotation by remember(detectedBatik?.id) { mutableStateOf(Quaternion.IDENTITY) }
    var userOffset by remember(detectedBatik?.id) { mutableStateOf(Vector3.ZERO) }
    var userScale by remember(detectedBatik?.id) { mutableStateOf(1f) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            hasPermission -> {
                val analyzer = remember { CameraAnalyzer(onFrame = arViewModel::onCameraFrame) }
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = analyzer,
                    onCameraReady = { camera ->
                        hasFlash = camera.cameraInfo.hasFlashUnit()
                        applyCameraCharacteristics(camera, arViewModel)
                    },
                    onControllerReady = { controller -> cameraController = controller }
                )
                // The 3D overlay is optional: if the Filament native libraries
                // wouldn't load on this device (NativeSupport, resolved once at
                // process start) we skip the whole surface *and* its gesture
                // modifiers, and say so on screen — rather than dragging a
                // SurfaceView into existence that can only fail.
                if (NativeSupport.isAvailable) {
                    FilamentView(
                        modelPath = detectedBatik?.modelPath,
                        pose = arPose,
                        userRotation = userRotation,
                        userOffset = userOffset,
                        userScale = userScale,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectObjectManipulationGestures(
                                    onRotate = { dx, dy ->
                                        val yaw = -dx * ROTATION_SENSITIVITY
                                        val pitch = -dy * ROTATION_SENSITIVITY
                                        val delta = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), yaw) *
                                            Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), pitch)
                                        userRotation = (delta * userRotation).normalized()
                                    },
                                    onPan = { dx, dy ->
                                        userOffset = userOffset + Vector3(
                                            x = dx * PAN_SENSITIVITY,
                                            y = -dy * PAN_SENSITIVITY,
                                            z = 0f
                                        )
                                    },
                                    onZoom = { scaleFactor ->
                                        userScale = (userScale * scaleFactor).coerceIn(MIN_USER_SCALE, MAX_USER_SCALE)
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    userRotation = Quaternion.IDENTITY
                                    userOffset = Vector3.ZERO
                                    userScale = 1f
                                })
                            }
                    )
                }
                // Subtle top/bottom scrims so the status pill, back button,
                // flash toggle and info panel keep contrast against whatever
                // busy motif/scenery the camera happens to be pointed at —
                // decorative only, so they never intercept touch/drag input.
                ScanScrims(modifier = Modifier.fillMaxSize())

                MarkerReticle(state = arState, region = detectedRegion, modifier = Modifier.fillMaxSize())
                ScanHint(
                    state = arState,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = Dimensions.spacingXxl + 140.dp)
                )
                ArStatusIndicator(
                    state = arState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Dimensions.spacingXxl)
                )
                if (!NativeSupport.isAvailable) {
                    Render3DUnavailableNotice(
                        reason = NativeSupport.failureReason,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Dimensions.spacingXxl + 56.dp)
                    )
                }
                if (hasFlash) {
                    ArControls(
                        isFlashOn = isFlashOn,
                        onToggleFlash = {
                            isFlashOn = !isFlashOn
                            cameraController?.setTorchEnabled(isFlashOn)
                            Unit
                        }.withClickSound(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimensions.spacingLg)
                    )
                }
                ArInformationPanel(
                    batik = detectedBatik?.takeIf { !panelDismissed },
                    onLihatDetail = { batik -> onViewDetail(batik.id) },
                    onMulaiKuis = { batik -> onStartQuiz(batik.id) },
                    onTutup = { panelDismissed = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(Dimensions.spacingMd)
                )
            }

            hasRequestedOnce -> {
                CameraPermissionContent(
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            else -> {
                // Waiting on the initial system permission dialog.
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }

        ArTopBar(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * Feeds real per-device lens/sensor data into [ArViewModel] so
 * [com.jelajahbatikjambi.ar.CameraIntrinsics] doesn't have to fall back to a
 * rough estimate. Safe to skip — [CameraIntrinsics.estimate] covers devices
 * that don't report these Camera2 characteristics.
 */
private fun applyCameraCharacteristics(
    camera: androidx.camera.core.Camera,
    arViewModel: ArViewModel
) {
    val info = Camera2CameraInfo.from(camera.cameraInfo)
    arViewModel.setCameraCharacteristics(
        focalLengthMm = info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull(),
        sensorPhysicalSize = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE),
        sensorPixelArraySize = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    )
}

/**
 * Purely decorative vertical-gradient scrims at the top and bottom of the
 * camera preview. Plain [Box]es with no pointer-input modifiers, so they
 * never consume touch events — the drag-to-rotate/double-tap gestures on
 * [FilamentView] underneath keep working exactly as before.
 */
@Composable
private fun ScanScrims(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                    )
                )
        )
    }
}

/**
 * Small hint line under the scan bracket, only while actively searching —
 * gives a first-time user an explicit instruction instead of just a bracket
 * and a status pill to interpret on their own.
 */
@Composable
private fun ScanHint(state: ArState, modifier: Modifier = Modifier) {
    val isSearching = state is ArState.Searching || state is ArState.Initializing

    AnimatedVisibility(visible = isSearching, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            text = "Arahkan kamera ke motif Batik Jambi",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusSmall)
                )
                .padding(horizontal = Dimensions.spacingMd, vertical = Dimensions.spacingSm)
        )
    }
}

/**
 * Caption shown when the Filament native libraries wouldn't load, so a camera
 * preview that never grows an object on it is explained rather than silently
 * broken. Deliberately a caption and not an error screen: marker detection,
 * the info panel, the detail pages and the quiz all work without the 3D
 * overlay (§33), so blocking the screen would take away working features.
 * [reason] is the underlying exception from [NativeSupport], shown in small
 * type purely as a diagnostic aid.
 */
@Composable
private fun Render3DUnavailableNotice(reason: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = Dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tampilan 3D tidak tersedia di perangkat ini",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusSmall)
                )
                .padding(horizontal = Dimensions.spacingMd, vertical = Dimensions.spacingSm)
        )
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = Dimensions.spacingXs)
            )
        }
    }
}

@Composable
private fun ArTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(Dimensions.spacingSm)) {
        IconButton(
            onClick = onBack.withClickSound(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.35f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
    }
}
