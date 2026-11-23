package raito.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme

/**
 * Progressive blur + surface fade over the content scrolling under the floating nav pill, ported from
 * DropSauce's BarBlur (itself from AirSync's ProgressiveBlurModifier). Android 13+ uses a RuntimeShader;
 * older versions get a plain gradient fade. With [enabled] off the content simply runs under the pill.
 *
 * [zoneHeight] is the height of the blurred strip at the bottom of the content.
 */
@Composable
fun BarBlurContainer(
    enabled: Boolean,
    zoneHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tint = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    var height by remember { mutableIntStateOf(0) }
    val zonePx = with(density) { zoneHeight.toPx() }

    Box(modifier = modifier.onSizeChanged { height = it.height }) {
        if (enabled && zonePx >= 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ShaderBlurLayer(tint = tint, height = height, zonePx = zonePx, content = content)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (enabled && zonePx >= 1f) {
                            Modifier.drawWithContent {
                                drawContent()
                                val h = zonePx.coerceAtMost(size.height)
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, tint.copy(alpha = 0.85f)),
                                        startY = size.height - h,
                                        endY = size.height,
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h),
                                    size = androidx.compose.ui.geometry.Size(size.width, h),
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
                content = content,
            )
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderBlurLayer(
    tint: Color,
    height: Int,
    zonePx: Float,
    content: @Composable BoxScope.() -> Unit,
) {
    val shader = remember(tint) {
        RuntimeShader(SHADER).apply { setColorUniform("bottomColor", tint.toArgb()) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("bottom", height.toFloat())
                shader.setFloatUniform("bottomHeight", zonePx)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            },
        content = content,
    )
}

private const val SHADER = """
uniform shader content;
uniform float bottom;
uniform float bottomHeight;
layout(color) uniform half4 bottomColor;

const float BLUR_RADIUS = 40.0;
const float BOTTOM_ALPHA = 0.65;
const int SAMPLES = 4;

half4 main(float2 fragCoord) {
    float t = bottomHeight < 1.0 ? 0.0 : 1.0 - clamp((bottom - fragCoord.y) / bottomHeight, 0.0, 1.0);
    if (t <= 0.0) {
        return content.eval(fragCoord);
    }
    float radius = pow(t, 1.5) * BLUR_RADIUS;
    float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
    float2 jitter = float2(dither - 0.5, fract(dither * 1.618) - 0.5);
    float offsetScale = radius / float(SAMPLES);
    float radiusSq = radius * radius;
    half4 accum = half4(0.0);
    float weightSum = 0.0;
    for (int x = -SAMPLES; x <= SAMPLES; x++) {
        for (int y = -SAMPLES; y <= SAMPLES; y++) {
            float2 offset = (float2(float(x), float(y)) + jitter) * offsetScale;
            float distSq = dot(offset, offset);
            if (distSq <= radiusSq) {
                float weight = exp(-3.0 * distSq / radiusSq);
                accum += content.eval(fragCoord + offset) * weight;
                weightSum += weight;
            }
        }
    }
    half4 blurred = weightSum > 0.0 ? accum / weightSum : content.eval(fragCoord);
    return mix(blurred, bottomColor, t * BOTTOM_ALPHA);
}
"""
