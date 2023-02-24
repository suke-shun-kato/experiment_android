package xyz.goodistory.xxperiment_android

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import java.lang.Float.min

class Camera2PreviewSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {

    private lateinit var cameraCharacteristics: CameraCharacteristics
    /** カメラ画像サイズ、幅 */
    private var previewWidth: Int = 0
    /** カメラ画像サイズ、高さ */
    private var previewHeight: Int = 0

    companion object {
        /**
         * 画面回転角度（自動回転ONにして端末を横にしたら回転するやつ）を取得する
         * @return 0, 90, 180, 270
         */
        fun calcSurfaceRotationDegrees(context: Context): Int {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            return when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw java.lang.IllegalStateException("rotationの値が不正です")
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // サイズを固定
        holder.setFixedSize(previewWidth, previewHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // SurfaceViewのサイズを取得
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // 相対角度を計算する
        val relativeRotation = computeRelativeRotation(
            cameraCharacteristics, calcSurfaceRotationDegrees(context))

        // X軸とY軸のScaleを変更する
        if (previewWidth > 0f && previewHeight > 0f) {
            // X軸方向（width）の SurfaceView と カメラ画像サイズ の倍率を求める
            val scaleX: Float =
                if (relativeRotation % 180 == 0) {
                    width.toFloat() / previewWidth  // 相対角度が0°、180°のとき
                } else {
                    width.toFloat() / previewHeight // 相対角度が90°、270°のとき
                }

            // Y軸方向（height）の SurfaceView と カメラ画像サイズ の倍率を求める
            val scaleY: Float =
                if (relativeRotation % 180 == 0) {
                    height.toFloat() / previewHeight
                } else {
                    height.toFloat() / previewWidth
                }

            // カメラ画像サイズ を SurfaceView にフィットするための倍率を取得
            val finalScale = min(scaleX, scaleY)

            // フィットする軸は等倍、しない軸はアスペクト比を調整する
            setScaleX(1 / scaleX * finalScale)
            setScaleY(1 / scaleY * finalScale)
        }

        // SurfaceView のwidthとheightをセット（onMeasure()の最後に必要）
        setMeasuredDimension(width, height)
    }

    /**
     * カメラのセンサー角度と画面回転角度から相対角度を求める
     * @param surfaceRotationDegrees 画面回転角度（0, 90, 180, 270、自動回転ONにして端末を横にしたら回転するやつ）
     * @return 相対角度（0, 90, 180, 270）
     */
    private fun computeRelativeRotation(
        characteristics: CameraCharacteristics,
        surfaceRotationDegrees: Int
    ): Int {

        // カメラのセンサーの角度
        val sensorOrientationDegrees = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // 前カメラと後カメラで角度を反転させるための係数
        val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_FRONT
        ) 1 else -1

        // 相対角度を求める
        return (sensorOrientationDegrees - surfaceRotationDegrees * sign + 360) % 360
    }

    /**
     * 初期化、サイズ決定
     */
    fun initSurfaceView(size: Size, cameraCharacteristics: CameraCharacteristics) {
        previewWidth = size.width
        previewHeight = size.height
        this.cameraCharacteristics = cameraCharacteristics
    }
}