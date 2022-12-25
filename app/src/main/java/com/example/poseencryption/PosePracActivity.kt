/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.poseencryption

import androidx.appcompat.app.AppCompatActivity
import android.graphics.SurfaceTexture
import android.view.SurfaceView
import com.google.mediapipe.glutil.EglManager
import android.os.Bundle
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import android.view.SurfaceHolder
import android.view.View
import com.google.protobuf.InvalidProtocolBufferException
import com.google.mediapipe.components.CameraHelper.CameraFacing
import android.view.ViewGroup
import com.google.mediapipe.components.*
import com.google.mediapipe.framework.Packet
import java.util.ArrayList

/**
 * Main activity of MediaPipe example apps.
 */
class PosePracActivity : AppCompatActivity() {
    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewLayoutResId)
        previewDisplayView = SurfaceView(this)
        setupPreviewDisplayView()
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Cannot find application info: $e")
        }
        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(this)
        eglManager = EglManager(null)
        processor = FrameProcessor(
            this,
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
            try {
//                        NormalizedLandmarkList poseLandmarks = PacketGetter.getProto(packet, NormalizedLandmarkList.class);
                val landmarksRaw = PacketGetter.getProtoBytes(packet)
                val poseLandmarks = NormalizedLandmarkList.parseFrom(landmarksRaw)
                Log.v(
                    TAG,
                    "[TS:" + packet.timestamp + "] " + getPoseLandmarksDebugString(poseLandmarks)
                )
                val srh = previewDisplayView!!.holder
                //
//                  -- this line cannot Running --
//                    Canvas canvas = null;
//                    try {
//                        canvas= srh.lockCanvas();
//                        synchronized(srh){
//                            Paint paint = new Paint();
//                            paint.setColor(Color.RED);
//                            canvas.drawCircle(10.0f,10.0f,10.0f,paint);
//                        }
//                    }finally{
//                        if(canvas != null){
//                            srh.unlockCanvasAndPost(canvas);
//                        }
//                    }
////                    processor.getVideoSurfaceOutput().setSurface(srh.getSurface());
            } catch (exception: InvalidProtocolBufferException) {
                Log.e(TAG, "failed to get proto.", exception)
            }
        }
        /*processor.addPacketCallback(
                "throttled_input_video_cpu",
                (packet) ->{
                    Log.d("Raw Image","Receive image with ts: "+packet.getTimestamp());
                    Bitmap image = AndroidPacketGetter.getBitmapFromRgba(packet);
                }
        );*/PermissionHelper.checkAndRequestCameraPermissions(this)
    }

    // Used to obtain the content view for this application. If you are extending this class, and
    // have a custom layout, override this method and return the custom layout.
    protected val contentViewLayoutResId: Int
        protected get() = R.layout.activity_main

    override fun onResume() {
        super.onResume()
        converter = ExternalTextureConverter(
            eglManager!!.context, 2
        )
        converter!!.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter!!.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(this)) {
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
        val cameraFacing = CameraFacing.BACK
        cameraHelper!!.startCamera(
            this, cameraFacing,  /*unusedSurfaceTexture=*/null, cameraTargetResolution()
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
//        previewDisplayView!!.visibility = View.GONE
//        val viewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
//        viewGroup.addView(previewDisplayView)
//        previewDisplayView!!
//            .getHolder()
//            .addCallback(
//                object : SurfaceHolder.Callback {
//                    override fun surfaceCreated(holder: SurfaceHolder) {
//                        processor!!.videoSurfaceOutput.setSurface(holder.surface)
//                        Log.d("Surface", "Surface Created")
//                    }
//
//                    override fun surfaceChanged(
//                        holder: SurfaceHolder,
//                        format: Int,
//                        width: Int,
//                        height: Int
//                    ) {
//                        onPreviewDisplaySurfaceChanged(holder, format, width, height)
//                        // 이곳에서 width , height 가 720,1280
//                        Log.d("Surface", "Surface Changed")
//                    }
//
//                    override fun surfaceDestroyed(holder: SurfaceHolder) {
//                        processor!!.videoSurfaceOutput.setSurface(null)
//                        Log.d("Surface", "Surface destroy")
//                    }
//                })
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val BINARY_GRAPH_NAME = "pose_tracking_gpu.binarypb"
        private const val INPUT_VIDEO_STREAM_NAME = "input_video"
        private const val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private const val OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks"
        private const val NUM_HANDS = 2
        private val CAMERA_FACING = CameraFacing.BACK

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

        //해당 코드에서 landmark의 좌표를 추출해낼 수 있다.
        //[0.0 , 1.0] 으로 normazlized 된 coordinate -> image width, height
        private fun getPoseLandmarksDebugString(poseLandmarks: NormalizedLandmarkList): String {
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
            var landmarksString = ""
            for (landmark in poseLandmarks.landmarkList) {
                landmarksString += """		Landmark[$landmarkIndex]: (${landmark.x}, ${landmark.y}, ${landmark.z})
"""
                ++landmarkIndex
            }
            Log.v(TAG, landmarksString)

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

        fun getAngle(
            firstPoint: PoseLandMark,
            midPoint: PoseLandMark,
            lastPoint: PoseLandMark
        ): Double {
            var result = Math.toDegrees(
                Math.atan2(
                    (lastPoint.y - midPoint.y).toDouble(),
                    (lastPoint.x - midPoint.x).toDouble()
                )
                        - Math.atan2(
                    (firstPoint.y - midPoint.y).toDouble(),
                    (firstPoint.x - midPoint.x).toDouble()
                )
            )
            result = Math.abs(result) // Angle should never be negative
            if (result > 180) {
                result = 360.0 - result // Always get the acute representation of the angle
            }
            return result
        }
    }
}