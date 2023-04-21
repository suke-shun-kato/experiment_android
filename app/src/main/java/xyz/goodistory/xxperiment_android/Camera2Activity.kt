package xyz.goodistory.xxperiment_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import xyz.goodistory.xxperiment_android.databinding.ActivityCamera2Binding


class Camera2Activity : AppCompatActivity() {
    companion object {
        const val MAX_IMAGES = 1
        const val IMAGE_FORMAT = ImageFormat.YUV_420_888
        const val SIZE_INDEX = 0
        const val LENS_FACING = CameraMetadata.LENS_FACING_BACK
        const val NEED_PERMISSION: String = Manifest.permission.CAMERA
    }

    private lateinit var binding: ActivityCamera2Binding
    private lateinit var imageReader: ImageReader
    private lateinit var cameraId: String
    private val cameraManager: CameraManager
        get() = getSystemService(Context.CAMERA_SERVICE) as CameraManager


    /**
     * カメラとのセッションに関するコールバック
     */
    private val captureSessionStatusCallback = object: CameraCaptureSession.StateCallback() {
        /**
         * カメラとのセッションが確立が成功したときに呼ばれる
         */
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // カメラの設定をする
            val captureRequestBuilder: CaptureRequest.Builder = cameraCaptureSession
                .device
                // 引数はカメラのモード。例えばTEMPLATE_MANUALだとフォーカスなど手動設定になる。今回はプレビュー用のモードに設定。
                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            // レイアウトの SurfaceView の対象に設定
            captureRequestBuilder.addTarget(binding.camera2SurfaceView.holder.surface)
            captureRequestBuilder.addTarget(imageReader.surface)

            // 設定した対象（surfaceView） にカメラから動画を流す
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) = Unit
    }

    /**
     * カメラをopenしたりcloseしたときに呼ばれるコールバック
     */
    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
//            val surfaces: List<Surface> = listOf(binding.camera2SurfaceView.holder.surface)
            val surfaces: List<Surface> = listOf(binding.camera2SurfaceView.holder.surface, imageReader.surface)

            createCaptureSession(cameraDevice, surfaces)
        }

        /**
         * カメラとのセッションを接続する
         * @param cameraDevice 対象のカメラのcameraDevice
         * @param surfaces 対象のsurface（複数）
         */
        private fun createCaptureSession(cameraDevice: CameraDevice, surfaces: List<Surface>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val configurations: List<OutputConfiguration> = surfaces.map { OutputConfiguration(it) }
                cameraDevice.createCaptureSession(
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        configurations,
                        this@Camera2Activity.mainExecutor,
                        captureSessionStatusCallback
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                cameraDevice.createCaptureSession(surfaces, captureSessionStatusCallback, null)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
            Log.d("ssss", "fffff")
    }

    /**
     * パーミッション許可ダイアログでボタンを押した後の処理
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                onPermissionGranted()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View関連の設定
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        // カメラ関連の変数を取得
        cameraId = getFirstCameraIdFacing(cameraManager, LENS_FACING)!!
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val size: Size = getCameraImageSize(SIZE_INDEX, cameraCharacteristics)

        // SurfaceViewをセット
        binding.camera2SurfaceView.initSurfaceView(size, cameraCharacteristics)

        setImageReader(size, MAX_IMAGES, IMAGE_FORMAT)

        // パーミッションをチェックして許可OKの場合はカメラを起動
        when {
            // 許可OKの場合
            ContextCompat.checkSelfPermission(this, NEED_PERMISSION )
                    == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }

            // 許可NGで アプリが権限を必要とする理由を説明する 場合
            shouldShowRequestPermissionRationale(NEED_PERMISSION) -> {
                showRationaleDialog { _, _ -> requestPermissionLauncher.launch(NEED_PERMISSION) }
            }

            // 許可NGの場合
            else -> {
                requestPermissionLauncher.launch(NEED_PERMISSION)
            }
        }
    }

    /**
     * アプリが権限を必要とする理由を説明するダイアログを表示する
     * @param okListener ポジティブボタンを押したあとの処理
     */
    private fun showRationaleDialog(okListener: DialogInterface.OnClickListener?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.camera2_rationale_dialog_message)
            .setPositiveButton(R.string.camera2_rationale_dialog_ok_button, okListener)
            .create()
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun onPermissionGranted() {
        // カメラを起動する
        cameraManager.openCamera(cameraId, deviceStateCallback, null)
    }

    /**
     * 複数ある画像サイズからサイズを取得する
     * @param sizeIndex サイズ配列から何番目のサイズを取得するか
     * @param cameraCharacteristics カメラのCameraCharacteristics
     */
    @Suppress("SameParameterValue")
    private fun getCameraImageSize(sizeIndex: Int, cameraCharacteristics: CameraCharacteristics): Size {
        val streamConfigurationMap: StreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        val sizes: Array<Size> = streamConfigurationMap.getOutputSizes(SurfaceHolder::class.java)

        return sizes[sizeIndex]
    }


    @Suppress("SameParameterValue")
    private fun setImageReader(
        size: Size, maxImages: Int, imageFormat: Int) {

        // imageReaderを作成
        imageReader = ImageReader.newInstance(size.width, size.height, imageFormat, maxImages)
        imageReader.setOnImageAvailableListener(onImageAvailableListener, null)
    }


    /**
     * 指定のCameraIDを取得する
     * @param facing カメラがある場所（前、後ろなど、CameraMetadata.LENS_FACING_XXX の値）
     */
    @Suppress("SameParameterValue")
    private fun getFirstCameraIdFacing(
        cameraManager: CameraManager, facing: Int = CameraMetadata.LENS_FACING_BACK): String? {

        // デバイスにある最低限の機能があるカメラのcameraID（複数）を取得する
        val cameraIds: List<String> = cameraManager.cameraIdList.filter {
            // 各カメラの capability（機能）を取得
            val capabilities: IntArray? = cameraManager
                .getCameraCharacteristics(it)
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            // 最低限の機能があるカメラのみを抜き出す
            capabilities?.contains(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
        }

        // 指定したカメラがある場所の CameraID を返す
        cameraIds.forEach {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                // メソッドの戻り値
                return it
            }
        }

        // 指定したカメラがない場合は、配列の最初のカメラのCameraIDを返す（配列がない場合はnull）
        return cameraIds.firstOrNull()
    }
}
