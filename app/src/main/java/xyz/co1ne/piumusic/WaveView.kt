package xyz.co1ne.piumusic

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.Property
import android.view.MotionEvent
import android.view.View
import android.view.animation.*
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.util.rangeTo
import kotlin.random.Random

/**
 * @author Co1ne Yellow
 * Created on 2021/9/9.
 * description:
 */
class WaveView : View {
    companion object {
        val DEFAULT_MAX_WAVE_GAP = 48
        val DEFAULT_PATH_COUNT = 3
        private const val TAG = "WaveView"
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    val waveGroup = mutableListOf<WaveLine>()
    val animatorSets = mutableListOf<AnimatorSet>()
    val defaultColors =
        arrayOf(
            resources.getColor(R.color.wave_color_pink, context.theme),
            resources.getColor(R.color.wave_color_teal, context.theme),
            resources.getColor(R.color.wave_color_blue, context.theme),
            resources.getColor(R.color.wave_color_purple, context.theme)
        )

    private fun init() {
        for (i in 0..DEFAULT_PATH_COUNT) {
            Log.d(TAG, "init: i = $i")
            waveGroup.add(WaveLine(i, Path(), Paint().apply {
                color = defaultColors[i]
                strokeWidth = 4f
                style = Paint.Style.FILL_AND_STROKE
            }))
        }

    }

    data class WaveLine(
        val id: Int,
        val path: Path,
        val paint: Paint,
        var amplitude: Float = Random.nextInt(20, 50).toFloat(),
        val frequency: Double = Random.nextDouble(0.5, 1.0),
        var offset: Double = Random.nextDouble(0.1, 2.0) * Math.PI,
        var starty: Float = 0f
    )


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildWaveLines()
    }

    private fun buildWaveLines() {
        for (waveLine in waveGroup) {
            var startx = 0f
            waveLine.path.reset()
            if (waveLine.starty == 0f) {
                waveLine.starty =
                    Random.nextInt(height / 5, height / 5 + DEFAULT_MAX_WAVE_GAP).toFloat()
            }

            waveLine.path.moveTo(startx, waveLine.starty)
            waveLine.paint.shader = LinearGradient(
                startx,
                waveLine.starty,
                width.toFloat(),
                height.toFloat(),
                waveLine.paint.color,
                Color.WHITE,
                Shader.TileMode.CLAMP
            )
            while (startx < width) {
                //f(x) = Asin(ωx +Ψ ) + k
                val endY =
                    waveLine.amplitude * Math.sin(waveLine.frequency * Math.PI / 180 * startx + waveLine.offset)
                        .toFloat() + waveLine.starty
                waveLine.path.lineTo(startx, endY)
                startx++
            }
            waveLine.path.lineTo(width.toFloat(), height.toFloat())
            waveLine.path.lineTo(0f, height.toFloat())
            waveLine.path.lineTo(0f, waveLine.starty)
            waveLine.path.close()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        for (waveLine in waveGroup) {
            canvas?.drawPath(waveLine.path, waveLine.paint)
        }
    }

    open fun stopAnimation(): Boolean {
        var result = false
        if (animatorSets.isNotEmpty()) {
            for (animatorSet in animatorSets) {
                if (animatorSet.isRunning) {
                    animatorSet.cancel()
                }
            }
            animatorSets.clear()
            result = true
        }
        return result
    }

    open fun startAnimation() {
        var isRunning = false
        if (animatorSets.isNotEmpty()) {
            for (animatorSet in animatorSets) {
                if (animatorSet.isRunning) {
                    isRunning = true
                }
            }
        }
        if (isRunning) {
            return
        }
        for (waveLine in waveGroup) {
            val amplitudeAnimator =
                ValueAnimator.ofFloat(
                    waveLine.amplitude,
                    1.5f * waveLine.amplitude,
                    waveLine.amplitude
                )
                    .apply {
                        interpolator = LinearInterpolator()
                        setEvaluator(FloatEvaluator())
                        duration = 2000
                        repeatCount = ValueAnimator.INFINITE
                    }
            val offsetAnimator =
                ValueAnimator.ofFloat(
                    waveLine.offset.toFloat(),
                    (waveLine.offset + (2f * Math.PI) / waveLine.frequency).toFloat()
                ).apply {
                    interpolator = LinearInterpolator()
                    setEvaluator(FloatEvaluator())
                    duration = (1000 * (2f * Math.PI) / waveLine.frequency).toLong()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE

                }
            amplitudeAnimator.addUpdateListener {
                waveLine.amplitude = it.animatedValue as Float
            }
            offsetAnimator.addUpdateListener {
                waveLine.offset = (it.animatedValue as Float).toDouble()
                buildWaveLines()
                invalidate()
            }

            animatorSets.add(AnimatorSet().apply {
                play(amplitudeAnimator)
                    .with(offsetAnimator)
                postDelayed({ start() }, (waveLine.id * 100).toLong())
            })
        }
    }

}
