package com.example.poseencryption

object API {
    const val UPLOAD_CTXT_URL : String = "upload/"
    const val UPLOAD_KEY_URL : String = "upload/"
    const val DOWNLOAD_URL : String = "result/"
        }

object Constants {
    const val IMG_SIZE = 224
    val MEAN_RGB = intArrayOf(104, 117, 124)
    val MEAN_RGBF = floatArrayOf(0.406f, 0.456f, 0.485f)
    val STD_RGBF = floatArrayOf(0.225f, 0.224f, 0.229f)
    const val SAMPLING_RATE = 10 // milisec
    const val PAUSE_TIME = 3000 // milisec
    const val T_DELCAY = 50 // milisec
    const val Nframes = 8
    const val Nskeletons = 16
    const val logN = 14
    var dataDir: String? = null
//
//    fun setDataDir(path: String?) {
//        dataDir = path
//    }

    val MeanPosXY = arrayOf(
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f),
        floatArrayOf(0.5f, 0.5f)
    )
    val StdPosXY = arrayOf(
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f)
    )

    //    public final static int [] idxSkeleton = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    val idxSkeleton = intArrayOf(
        15,  // L Hand       > 0
        14,  // L Elbow      > 1
        13,  // L Shoulder   > 2
        12,  // R Shoulder   > 3
        11,  // R Elbow      > 4
        10,  // R Hand       > 5
        9,  // Head         > 6
        8,  // Neck         > 7
        6,  // Pelvis       > 8
        5,  // L Foot       > 9
        4,  // L Knee       > 10
        3,  // L Hip        > 11
        2,  // R Hip        > 12
        1,  // R Knee       > 13
        0 // R Foot       > 14
    )
}