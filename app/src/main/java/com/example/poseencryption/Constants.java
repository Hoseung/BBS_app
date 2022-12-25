package com.example.poseencryption;

public final class Constants {
    public final static int IMG_SIZE = 224;
    public final static int[] MEAN_RGB = new int[] {104, 117, 124};
    public final static float[] MEAN_RGBF = new float[] {0.406f, 0.456f, 0.485f};
    public final static float[] STD_RGBF = new float[] {0.225f, 0.224f, 0.229f};
    public final static int SAMPLING_RATE = 10; // milisec
    public final static int PAUSE_TIME = 3000; // milisec
    public final static int T_DELCAY = 50; // milisec

    public final static int Nframes = 8;
    public final static int Nskeletons = 16;

    public final static int logN = 14;
    public static String dataDir = null;

    static void setDataDir(String path){
        dataDir = path;
    }

    public final static float [][] MeanPosXY = new float[][] {
            {0.5f, 0.5f}, // 1
            {0.5f, 0.5f}, // 2
            {0.5f, 0.5f}, // 3
            {0.5f, 0.5f}, // 4
            {0.5f, 0.5f}, // 5
            {0.5f, 0.5f}, // 6
            {0.5f, 0.5f}, // 7
            {0.5f, 0.5f}, // 8
            {0.5f, 0.5f}, // 9
            {0.5f, 0.5f}, // 10
            {0.5f, 0.5f}, // 11
            {0.5f, 0.5f}, // 12
            {0.5f, 0.5f}, // 13
            {0.5f, 0.5f}, // 14
    };

    public final static float [][] StdPosXY = new float[][] {
            {1.0f, 1.0f}, // 1
            {1.0f, 1.0f}, // 2
            {1.0f, 1.0f}, // 3
            {1.0f, 1.0f}, // 4
            {1.0f, 1.0f}, // 5
            {1.0f, 1.0f}, // 6
            {1.0f, 1.0f}, // 7
            {1.0f, 1.0f}, // 8
            {1.0f, 1.0f}, // 9
            {1.0f, 1.0f}, // 10
            {1.0f, 1.0f}, // 11
            {1.0f, 1.0f}, // 12
            {1.0f, 1.0f}, // 13
            {1.0f, 1.0f}, // 14
    };

    //    public final static int [] idxSkeleton = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    public final static int [] idxSkeleton = {
            15, // L Hand       > 0
            14, // L Elbow      > 1
            13, // L Shoulder   > 2
            12, // R Shoulder   > 3
            11, // R Elbow      > 4
            10, // R Hand       > 5
            9,  // Head         > 6
            8,  // Neck         > 7
            6,  // Pelvis       > 8
            5,  // L Foot       > 9
            4,  // L Knee       > 10
            3,  // L Hip        > 11
            2,  // R Hip        > 12
            1,  // R Knee       > 13
            0   // R Foot       > 14
    };




}
