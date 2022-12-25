package com.example.poseencryption.fragment

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.poseencryption.Constants
import com.example.poseencryption.MainActivity
import com.example.poseencryption.PoseLandMark
import com.example.poseencryption.api.UrlManager
import com.example.poseencryption.databinding.FragmentCaptureBinding
import com.google.mediapipe.components.*
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.google.protobuf.InvalidProtocolBufferException
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import kotlin.math.abs
import kotlin.math.atan2


class CaptureFragment : Fragment() {

    private lateinit var binding: FragmentCaptureBinding

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

    private var viewGroup: ViewGroup? = null

    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private var previewDisplayView: SurfaceView? = null

    // Creates and manages an {@link EGLContext}.
    private var eglManager: EglManager? = null

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private var processor: FrameProcessor? = null

    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private var converter: ExternalTextureConverter? = null

    // ApplicationInfo for retrieving metadata defined in the manifest.
    // Handles camera access via the {@link CameraX} Jetpack support library.
    private var cameraHelper: CameraXPreviewHelper? = null

    private var poseList = arrayListOf<Float>()

    private var capturing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        binding = FragmentCaptureBinding.inflate(layoutInflater)

        viewGroup = binding.previewDisplayLayout

        previewDisplayView = SurfaceView(requireContext())
        setupPreviewDisplayView()
        AndroidAssetUtil.initializeNativeAssetManager(requireContext())
        eglManager = EglManager(null)
        processor = FrameProcessor(
            requireContext(),
            eglManager!!.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        )
        processor!!
            .videoSurfaceOutput
            .setFlipY(FLIP_FRAMES_VERTICALLY)

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
//        if (Log.isLoggable(TAG, Log.VERBOSE)) {
        processor!!.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet: Packet ->
            Log.v(TAG, "Received Pose landmarks packet.")
            if (capturing) {
                try {
//                        NormalizedLandmarkList poseLandmarks = PacketGetter.getProto(packet, NormalizedLandmarkList.class);
                    val landmarksRaw = PacketGetter.getProtoBytes(packet)
                    val poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw)
                    Log.v(
                        TAG,
                        "[TS:" + packet.timestamp + "] " + getPoseLandmarksDebugString(
                            poseLandmarks
                        )
                    )

                } catch (exception: InvalidProtocolBufferException) {
                    Log.e(TAG, "failed to get proto.", exception)
                }
            }
        }
        PermissionHelper.checkAndRequestCameraPermissions(requireActivity())

        initView()

        return binding.root
    }

    private fun initView() = with(binding) {
        downloadBtn.setOnClickListener {
            UrlManager.service?.downloadEncryption()?.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            writeResponseBodyToDisk(response.body()!!)
                            Toast.makeText(requireContext(), "다운로드 성공!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        println(response)
                    }
                    println(response.body().toString())
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    t.printStackTrace()
                }

            })
        }

        captureBtn.setOnClickListener {
            if (capturing) {
                val floatArray = FloatArray(poseList.size)
                capturing = false
                var i = 0
                for (j in poseList) {
                    floatArray[i++] = j
                }

                println(
                    MainActivity().DoEncryptionCpp(
                        Constants.logN,
                        floatArray,
                        floatArray.size,
                        java.lang.String.format(
                            "logN%s_Cat%s_Angle%s.dat",
                            Constants.logN,
                            MainActivity().CatIndex + 1,
                            MainActivity().CatIndex + 1,
                        ),
                        requireActivity().applicationContext.filesDir.toString() + "/"
                    )
                )
                val file = File(
                    requireActivity().applicationContext.filesDir.toString() + "/",
                    java.lang.String.format(
                        "logN%s_Cat%s_Angle%s.dat",
                        Constants.logN,
                        MainActivity().CatIndex + 1,
                        MainActivity().CatIndex + 1,
                    )
                )
                if (file.exists()) {
                    println("YES" + file.toString())
                }
                // finish
            } else {
                // start
                poseList = arrayListOf()
                capturing = true
            }
        }
        sendBtn.setOnClickListener {
            sendData()
        }
    }

    private fun sendData() {

        val file = File(
            requireActivity().applicationContext.filesDir.toString() + "/" + java.lang.String.format(
                "logN%s_Cat%s_Angle%s.dat",
                Constants.logN,
                MainActivity().CatIndex + 1,
                MainActivity().CatIndex + 1,
            )
        )

        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        UrlManager.service?.postEncryptionFile("ctxt", 3, body)
            ?.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    println(response.body())
                    println(response.code())
                    Toast.makeText(requireContext(), "성공적으로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    t.printStackTrace()
                }
            })

    }


    private fun writeResponseBodyToDisk(body: ResponseBody): Boolean {
        return try {
            // todo change the file location/name according to your needs
            val futureStudioIconFile: File =
                File(
                    requireContext().getExternalFilesDir(null)
                        .toString() + File.separator + "Download Data @#$%^"
                )
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                    Log.d("TAG", "file download: $fileSizeDownloaded of $fileSize")
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            false
        }
    }


    override fun onResume() {
        super.onResume()
        converter = ExternalTextureConverter(
            eglManager!!.context, 2
        )
        converter!!.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter!!.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(requireActivity())) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        converter!!.close()

        // Hide preview display until we re-open the camera again.
        previewDisplayView!!.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected fun onCameraStarted(surfaceTexture: SurfaceTexture?) {
        previewFrameTexture = surfaceTexture

        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView!!.visibility = View.VISIBLE
    }

    protected fun cameraTargetResolution(): Size? {
        return null // No preference and let the camera (helper) decide.
    }

    fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper!!.setOnCameraStartedListener { surfaceTexture: SurfaceTexture? ->
            onCameraStarted(
                surfaceTexture
            )
        }
        val cameraFacing = CameraHelper.CameraFacing.FRONT
        cameraHelper!!.startCamera(
            requireActivity(),
            cameraFacing,  /*unusedSurfaceTexture=*/
            null,
            cameraTargetResolution()
        )
    }

    protected fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height)
    }

    protected fun onPreviewDisplaySurfaceChanged(
        holder: SurfaceHolder?, format: Int, width: Int, height: Int
    ) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        val viewSize = computeViewSize(width, height)
        val displaySize = cameraHelper!!.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated = cameraHelper!!.isCameraRotated

        //displaySize.getHeight(); 핸드폰 디스플레이 사이즈를 의미
        //displaySize.getWidth();


        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter!!.setSurfaceTextureAndAttachToGLContext(
            previewFrameTexture,
            if (isCameraRotated) displaySize.height else displaySize.width,
            if (isCameraRotated) displaySize.width else displaySize.height
        )
    }

    private fun setupPreviewDisplayView() {
        previewDisplayView!!.visibility = View.GONE
        viewGroup!!.addView(previewDisplayView)
        previewDisplayView!!
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        processor!!.videoSurfaceOutput.setSurface(holder.surface)
                        Log.d("Surface", "Surface Created")
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        onPreviewDisplaySurfaceChanged(holder, format, width, height)
                        Log.d("Surface", "Surface Changed")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        processor!!.videoSurfaceOutput.setSurface(null)
                        Log.d("Surface", "Surface destroy")
                    }
                })
    }

    companion object {
        const val TAG = "CaptureFragment"
        private const val BINARY_GRAPH_NAME = "pose_tracking_gpu.binarypb"
        private const val INPUT_VIDEO_STREAM_NAME = "input_video"
        private const val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private const val OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks"
        private const val NUM_HANDS = 2
        private val CAMERA_FACING = CameraHelper.CameraFacing.BACK

        // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
        // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
        // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
        // corner, whereas MediaPipe in general assumes the image origin is at top-left.
        private const val FLIP_FRAMES_VERTICALLY = true

        init {
            // Load all native libraries needed by the app.
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }

        fun getAngle(
            firstPoint: PoseLandMark,
            midPoint: PoseLandMark,
            lastPoint: PoseLandMark
        ): Double {
            var result = Math.toDegrees(
                atan2(
                    (lastPoint.y - midPoint.y).toDouble(),
                    (lastPoint.x - midPoint.x).toDouble()
                )
                        - atan2(
                    (firstPoint.y - midPoint.y).toDouble(),
                    (firstPoint.x - midPoint.x).toDouble()
                )
            )
            result = abs(result) // Angle should never be negative
            if (result > 180) {
                result = 360.0 - result // Always get the acute representation of the angle
            }
            return result
        }
    }

    //해당 코드에서 landmark의 좌표를 추출해낼 수 있다.
    //[0.0 , 1.0] 으로 normazlized 된 coordinate -> image width, height
    private fun getPoseLandmarksDebugString(poseLandmarks: LandmarkProto.NormalizedLandmarkList): String {
        val poseLandmarkStr = """
                 Pose landmarks: ${poseLandmarks.landmarkCount}
                 
                 """.trimIndent()
        val poseMarkers = ArrayList<PoseLandMark>()
        var landmarkIndex = 0
        for (landmark in poseLandmarks.landmarkList) {
            val marker = PoseLandMark(landmark.x, landmark.y, landmark.visibility)
            //          poseLandmarkStr += "\tLandmark ["+ landmarkIndex+ "]: ("+ (landmark.getX()*720)+ ", "+ (landmark.getY()*1280)+ ", "+ landmark.getVisibility()+ ")\n";
            ++landmarkIndex
            poseMarkers.add(marker)
        }
        // Get Angle of Positions
        val rightAngle = getAngle(poseMarkers[16], poseMarkers[14], poseMarkers[12])
        val leftAngle = getAngle(poseMarkers[15], poseMarkers[13], poseMarkers[11])
        val rightKnee = getAngle(poseMarkers[24], poseMarkers[26], poseMarkers[28])
        val leftKnee = getAngle(poseMarkers[23], poseMarkers[25], poseMarkers[27])
        val rightShoulder = getAngle(poseMarkers[14], poseMarkers[12], poseMarkers[24])
        val leftShoulder = getAngle(poseMarkers[13], poseMarkers[11], poseMarkers[23])
        var landmarksString = "\n"
        for (landmark in poseLandmarks.landmarkList) {
            landmarksString += """		Landmark[${landmarkIndex - 33}]: (${landmark.x}, ${landmark.y}, ${landmark.z})
"""
            poseList.add((landmarkIndex - 33).toFloat())
            poseList.add(landmark.x)
            poseList.add(landmark.y)
            ++landmarkIndex
        }
        Log.v(TAG, poseList.toString())

//        Log.v(TAG,"======Degree Of Position]======\n"+
//                "rightAngle :"+rightAngle+"\n"+
//                "leftAngle :"+leftAngle+"\n"+
//                "rightHip :"+rightKnee+"\n"+
//                "leftHip :"+leftKnee+"\n"+
//                "rightShoulder :"+rightShoulder+"\n"+
//                "leftShoulder :"+leftShoulder+"\n");

//        Log.v(TAG, poseLandmarkStr.toString());
//        return poseLandmarkStr;
        /*
       16 오른 손목 14 오른 팔꿈치 12 오른 어깨 --> 오른팔 각도
       15 왼쪽 손목 13 왼쪽 팔꿈치 11 왼쪽 어깨 --> 왼  팔 각도
       24 오른 골반 26 오른 무릎   28 오른 발목 --> 오른무릎 각도
       23 왼쪽 골반 25 왼쪽 무릎   27 왼쪽 발목 --> 왼 무릎 각도
       14 오른 팔꿈 12 오른 어깨   24 오른 골반 --> 오른 겨드랑이 각도
       13 왼   팔꿈 11 왼  어깨   23  왼  골반 --> 왼쪽 겨드랑이 각도
    */return poseLandmarkStr
    }

}