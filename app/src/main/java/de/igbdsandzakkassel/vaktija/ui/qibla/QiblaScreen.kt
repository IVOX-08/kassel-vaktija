package de.igbdsandzakkassel.vaktija.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextButton
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Qibla compass. The bearing is the great-circle direction from the SELECTED mosque to the Kaaba
 * value, since the app is location-specific — no location permission needed). The device's heading
 * comes from the rotation-vector sensor; the dial rotates so the compass keeps pointing at true
 * directions, and a green marker shows where the Qibla is. Align that marker with the gold triangle
 * at the top and the top of the phone faces the Qibla.
 */
@Composable
fun QiblaScreen(
    modifier: Modifier = Modifier,
    viewModel: QiblaViewModel = hiltViewModel(),
) {
    val qiblaBearing by viewModel.bearing.collectAsStateWithLifecycle()
    val usingDeviceLocation by viewModel.usingDeviceLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Asked for only when the button is tapped, never on opening the screen: most people pray at
    // home, where the mosque's bearing is already right, and a permission prompt out of nowhere is
    // the kind of thing that makes people distrust an app.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            QiblaLocation.lastKnown(context)?.let { (lat, lon) ->
                viewModel.useDeviceLocation(lat, lon)
            }
        }
    }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (rotationSensor == null) {
            Text(
                text = stringResource(R.string.qibla_no_sensor),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        var azimuth by remember { mutableFloatStateOf(0f) }
        var lowAccuracy by remember { mutableStateOf(false) }
        DisposableEffect(sensorManager, rotationSensor) {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            var smoothed = Float.NaN
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                    smoothed = if (smoothed.isNaN()) deg else lowPass(smoothed, deg)
                    azimuth = smoothed
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Indoors/near metal the magnetometer often degrades — a prayer-DIRECTION
                    // feature must say so instead of showing a confidently wrong dial.
                    lowAccuracy = accuracy < android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                }
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }

        val aligned = abs(normalizeSigned(qiblaBearing - azimuth)) < 5f

        Text(
            text = stringResource(R.string.nav_qibla),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${qiblaBearing.roundToInt()}°",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        // Say what the reading is based on. A compass that is silently pointing from a mosque
        // 1500 km away is worse than one that admits it.
        Text(
            text = stringResource(
                if (usingDeviceLocation) R.string.qibla_from_device
                else R.string.qibla_from_mosque,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                if (usingDeviceLocation) {
                    viewModel.useMosqueLocation()
                } else if (QiblaLocation.hasPermission(context)) {
                    QiblaLocation.lastKnown(context)?.let { (lat, lon) ->
                        viewModel.useDeviceLocation(lat, lon)
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
        ) {
            Text(
                stringResource(
                    if (usingDeviceLocation) R.string.qibla_use_mosque
                    else R.string.qibla_use_device,
                ),
            )
        }
        Spacer(Modifier.height(20.dp))

        CompassDial(
            azimuth = azimuth,
            qiblaBearing = qiblaBearing,
            aligned = aligned,
            modifier = Modifier.size(300.dp),
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(if (aligned) R.string.qibla_facing else R.string.qibla_hint),
            textAlign = TextAlign.Center,
            color = if (aligned) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (aligned) FontWeight.Bold else FontWeight.Normal,
        )
        if (lowAccuracy) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.qibla_calibrate),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float, qiblaBearing: Float, aligned: Boolean, modifier: Modifier) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val northColor = Color(0xFFD32F2F)
    val qiblaColor = BrandGreen

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Rotating dial: ticks, cardinals and the Qibla marker, turned so it tracks true directions.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-azimuth),
        ) {
            val c = center
            val r = size.minDimension / 2f
            val edge = 4.dp.toPx()

            drawCircle(color = ringColor, radius = r - edge, style = Stroke(width = 2.dp.toPx()), center = c)

            for (deg in 0 until 360 step 5) {
                val major = deg % 30 == 0
                val len = if (major) 16.dp.toPx() else 8.dp.toPx()
                val rad = Math.toRadians((deg - 90).toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val outer = Offset(c.x + (r - edge) * cosA, c.y + (r - edge) * sinA)
                val inner = Offset(c.x + (r - edge - len) * cosA, c.y + (r - edge - len) * sinA)
                drawLine(
                    color = if (deg == 0) northColor else ringColor,
                    start = inner,
                    end = outer,
                    strokeWidth = if (major) 3.dp.toPx() else 1.dp.toPx(),
                )
            }

            val letterInset = 42.dp.toPx()
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 17.dp.toPx()
                    isFakeBoldText = true
                }
                listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W").forEach { (deg, label) ->
                    paint.color = if (deg == 0) northColor.toArgb() else onSurface.toArgb()
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val x = c.x + (r - letterInset) * cos(rad).toFloat()
                    val y = c.y + (r - letterInset) * sin(rad).toFloat()
                    canvas.nativeCanvas.drawText(label, x, y + paint.textSize / 3f, paint)
                }
            }

            // Qibla marker
            val qRad = Math.toRadians((qiblaBearing - 90).toDouble())
            val tip = Offset(
                c.x + (r - 24.dp.toPx()) * cos(qRad).toFloat(),
                c.y + (r - 24.dp.toPx()) * sin(qRad).toFloat(),
            )
            drawLine(color = qiblaColor, start = c, end = tip, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(color = qiblaColor, radius = 11.dp.toPx(), center = tip)
            drawCircle(color = BrandGold, radius = 11.dp.toPx(), center = tip, style = Stroke(width = 2.5.dp.toPx()))
        }

        // Fixed overlay: top reference triangle (align the Qibla marker here) + centre hub.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val halfW = 9.dp.toPx()
            val path = Path().apply {
                moveTo(c.x - halfW, 0f)
                lineTo(c.x + halfW, 0f)
                lineTo(c.x, 15.dp.toPx())
                close()
            }
            drawPath(path, color = if (aligned) BrandGreen else BrandGold)
            drawCircle(color = onSurface, radius = 4.dp.toPx(), center = c)
        }
    }
}

private fun lowPass(prev: Float, target: Float, alpha: Float = 0.12f): Float {
    var diff = target - prev
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return ((prev + alpha * diff) + 360f) % 360f
}

private fun normalizeSigned(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

