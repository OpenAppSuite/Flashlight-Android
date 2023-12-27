package io.github.lentolen.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.lentolen.flashlight.ui.theme.FlashlightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FlashlightTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier
                        .safeDrawingPadding()
                        .fillMaxSize()) {
                        Screen()
                    }
                }
            }
        }
    }
}

@Composable
fun Screen() {
    val context = LocalContext.current
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList.firstOrNull()
    val camInfo = cameraId?.let { cameraManager.getCameraCharacteristics(it) }
    val torchMax = camInfo?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 45
    val torchAvailable = camInfo?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
    val torchIntensity = rememberSaveable { mutableStateOf(torchMax) }
    val isChecked = remember { mutableStateOf(false) }
    val isError = remember { mutableStateOf(false) }

    val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isChecked.value = enabled
            if (enabled) {
                torchIntensity.value = cameraId.let { cameraManager.getTorchStrengthLevel(it) }
            }
        }
    }
    cameraManager.registerTorchCallback(torchCallback, null)

    Column (
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ToggleFlashlightButton(torchIntensity, isChecked, cameraManager, cameraId, torchAvailable, isError)
        BrightnessControl(torchIntensity, isChecked, cameraManager, cameraId, torchMax)
        BrightnessIcons()
    }
}

@Composable
fun ToggleFlashlightButton(torchIntensity: MutableState<Int>, isChecked: MutableState<Boolean>, cameraManager: CameraManager, cameraId: String?, torchAvailable: Boolean, isError: MutableState<Boolean>) {
    val context = LocalContext.current
    FilledIconToggleButton(
        modifier = Modifier
            .padding(bottom = 24.dp)
            .size(150.dp),
        checked = isChecked.value,
        onCheckedChange = {
            if (cameraId != null && torchAvailable) {
                if (isChecked.value) {
                    turnOffFlashlight(cameraManager, cameraId)
                    cameraManager.setTorchMode(cameraId, false)
                    isChecked.value = false
                } else {
                    turnOnFlashlight(cameraManager, cameraId, torchIntensity.value)
                    isChecked.value = true
                }
            } else {
                isError.value = true
            }
        }
    ) {
        if (isChecked.value) {
            Icon(
                Icons.Outlined.FlashlightOn,
                contentDescription = stringResource(R.string.icon_on),
                modifier = Modifier.size(80.dp)
            )
        } else {
            Icon(
                Icons.Outlined.FlashlightOff,
                contentDescription = stringResource(R.string.icon_off),
                modifier = Modifier.size(80.dp)
            )
        }
    }
    if (isError.value) {
        ErrorSnackbar(errorMessage = stringResource(R.string.not_found))
    }
}

@Composable
fun BrightnessControl(torchIntensity: MutableState<Int>, isChecked: MutableState<Boolean>, cameraManager: CameraManager, cameraId: String?, torchMax: Int) {
    Slider(
        value = torchIntensity.value.toFloat(),
        onValueChange = { newValue ->
            torchIntensity.value = newValue.toInt()
            if (cameraId != null && isChecked.value) {
                turnOnFlashlight(cameraManager, cameraId, torchIntensity.value)
            }
        },
        valueRange = 1f..torchMax.toFloat(),
        modifier = Modifier.padding(24.dp, 0.dp)
    )
}

@Composable
fun BrightnessIcons() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(24.dp, 0.dp)
            .fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_brightness_1_24),
            contentDescription = stringResource(R.string.low_brightness_icon),
            modifier = Modifier.size(40.dp)
        )
        Icon(
            Icons.Outlined.LightMode,
            contentDescription = stringResource(R.string.max_brightness_icon),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ErrorSnackbar(errorMessage: String) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = Color.Red,
        action = {},
        content = {
            Text(
                text = errorMessage,
            )
        }
    )
}

fun turnOnFlashlight(cameraManager: CameraManager, cameraId: String, intensity: Int) {
    try {
        cameraManager.turnOnTorchWithStrengthLevel(cameraId, intensity)
    } catch (e: CameraAccessException) {
        Log.e("Flashlight", "Failed to turn on flashlight", e)
    }
}

fun turnOffFlashlight(cameraManager: CameraManager, cameraId: String) {
    try {
        cameraManager.setTorchMode(cameraId, false)
    } catch (e: CameraAccessException) {
        Log.e("Flashlight", "Failed to turn off flashlight", e)
    }
}
