#include <jni.h>
#include <string>

#include <gmp.h>

#include "HEAAN.h"

#include <NTL/BasicThreadPool.h>
#include <NTL/ZZ.h>

#include <cstdio>
#include <cassert>
#include <iostream>
#include <fstream>
#include <dirent.h>
#include <stdio.h>
#include <vector>
#include <android/log.h>


using namespace std;
using namespace NTL;

complex<double>* randomComplexArray(long n, double bound);
std::string serkeyname = "secret.key";

template <typename T>
inline string to_string(const T& t)
{
    std::ostringstream oss;
    oss << t;
    return oss.str();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_poseencryption_MainActivity_createNewKeys(
        JNIEnv* env,
        jobject, /* this */
        jstring jctextfname) {

    srand(time(NULL));
    SetNumThreads(4);

    jboolean isCopy;
    std::string path = (env)->GetStringUTFChars(jctextfname, &isCopy);
    std::string path_serkey = path + serkeyname;

    Ring ring;
    SecretKey secretKey(ring, path_serkey);
    SerializationUtils serutils;

    Scheme scheme(ring, true);
    scheme.init(path, 1);
    scheme.addEncKey(secretKey);            // EncKey.txt
    scheme.addMultKey(secretKey);           // MulKey.txt
    scheme.addConjKey(secretKey);           // ConjKey.txt
    scheme.addLeftRotKey(secretKey,1);   // RotKey_1.txt

    return env->NewStringUTF("done");
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_poseencryption_MainActivity_DoEncryptionCpp(
        JNIEnv* env,
        jobject, /* this */
        jint logN,
        jfloatArray inputarr,
        jint Num,
        jstring jctextfname,
        jstring jpath) {

    srand(time(NULL));
    SetNumThreads(4);

    long logq = 540; ///< Ciphertext Modulus ************ if > 1024  it returns nan !!!!
    long logp = 30;  ///< Real message will be quantized by multiplying 2^40
    long logn = (long)logN; // 9;   ///< log2(The number of slots)

    jboolean isCopy;
    jfloat *float_buf = env->GetFloatArrayElements(inputarr, NULL);
    std::string fname = (env)->GetStringUTFChars(jctextfname, &isCopy);
    std::string path = (env)->GetStringUTFChars(jpath, &isCopy);
    std::string path_serkey = path + serkeyname;

    Ring ring;
//    SecretKey secretKey(ring, path_serkey);
    SecretKey secretKey;
    secretKey.loadKey(path_serkey);

    SerializationUtils serutils;

    Scheme scheme(ring, true);
    scheme.init(path, 1);
    scheme.loadEncKey();
    scheme.loadMultKey();

//    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "path serkey: %s",path_serkey.c_str());
//    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "ctext path: %s",(path + fname).c_str());

    long n = (1 << logn);
    if (n < Num) {
        std::string out = "Smaller number of slots assigned!";
        return env->NewStringUTF(out.c_str());
    }

    complex<double>* mvec = new complex<double>[Num];
    for (long i = 0; i < n; ++i) {
        if (i < Num) {
            complex<double> res;
            res.real(float_buf[i]);
            res.imag(0.);
            mvec[i] = res;
        } else {
            complex<double> res;
            res.real(0.);
            res.imag(0.);
            mvec[i] = res;
        }
    }
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "assigning mvec done.","");
    Ciphertext cipher;


    scheme.encrypt(cipher, mvec, n, logp, logq);
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "encryption to cipher text done.","");

    serutils.writeCiphertext(cipher, path + fname);

    std:string out = path+fname;
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_poseencryption_MainActivity_DoDecryptionCpp(
        JNIEnv* env,
        jobject, /* this */
        jint logN,
        jint Num,
        jstring jpredictpath) {
    srand(time(NULL));
    SetNumThreads(4);
    ifstream ifile;
    long logq = 800; ///< Ciphertext Modulus ************ if > 1024  it returns nan !!!!
    long logp = 30;  ///< Real message will be quantized by multiplying 2^40
    long logn = (long) logN; // 9;   ///< log2(The number of slots)

    jboolean isCopy;
    std::string path = (env)->GetStringUTFChars(jpredictpath, &isCopy);
    std::string path_serkey = path + serkeyname;
    ifile.open(path_serkey);
    if (ifile) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", " %s exists.", path_serkey.c_str());
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", " %s does not exist.", path_serkey.c_str());
    }

    DIR *dir; struct dirent *diread;
    std::vector<std::string> files;

    if ((dir = opendir(path.c_str())) != nullptr) {
        while ((diread = readdir(dir)) != nullptr) {
            files.push_back(diread->d_name);
        }
        closedir (dir);
    } else {
        perror ("opendir");
        return EXIT_FAILURE;
    }

    for (auto file : files){
        FILE *f;
        f = fopen( (path + file).c_str(), "r");
        fseek(f, 0, SEEK_END);
        int fileLength = ftell(f);
        fclose(f);
//        int fileLength = 0;
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", " %s | %d", file.c_str(), fileLength);
    }


    SerializationUtils serutils;
    Ring ring;
    SecretKey secretKey;
    secretKey.loadKey(path_serkey);

    Scheme scheme(ring, true);
    scheme.init(path, 1);
    scheme.loadEncKey();
    scheme.loadMultKey();


    std::string predicts[5] = {"predict0.dat", "predict1.dat", "predict2.dat", "predict3.dat", "predict4.dat"};

    float probsum[5] = {0., 0., 0., 0., 0.};

    int argmax = 0; float maxval = 0.; int i;
    for (i=0;i<5;i++) {
        ifile.open(path + predicts[i]);
        if(ifile) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI:", " %s exists.", (path+predicts[i]).c_str());
        } else {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI:", " %s does not exist.", (path+predicts[i]).c_str());
        }

        Ciphertext cipher;
        serutils.readCiphertext(cipher, path + predicts[i]);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "Reading %s done.", (path + predicts[i]).c_str());
        complex<double>* dvec = scheme.decrypt(secretKey, cipher);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "decrypt done.");
        float sum = 0.;
        for (int j=0;j<Num;j++){
            sum += dvec[j].real();
        }
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "sum[%d]: %f",i,sum);
        probsum[i] = sum;
        if (sum > maxval) {
            argmax = i;
            maxval = sum;
        }
    }

    return argmax;
}


extern "C" JNIEXPORT void JNICALL
Java_com_example_poseencryption_MainActivity_Test(
        JNIEnv* env,
        jobject,
        jstring jpath
        ) {
    srand(time(NULL));
    SetNumThreads(4);

    long logq = 800; ///< Ciphertext Modulus ************ if > 1024  it returns nan !!!!
    long logp = 30;  ///< Real message will be quantized by multiplying 2^40
    long logn = 9;   ///< log2(The number of slots)
    long n = (1 << logn);

    jboolean isCopy;
//    std::string path = "/data/data/";
    std::string path = (env)->GetStringUTFChars(jpath, &isCopy);

    std::string path_serkey = path + serkeyname;
    Ring ring, ring2;
    SecretKey secretKey(ring, path_serkey);
    SecretKey secretKey2;

    Ciphertext cipher, cipher2;

    SerializationUtils serutils;

    Scheme scheme(ring, true);
    scheme.init(path, 0);
    scheme.addEncKey(secretKey);
    scheme.addMultKey(secretKey);
    scheme.loadEncKey();
    scheme.loadMultKey();


    complex<double>* mvec = new complex<double>[n];
    for (long i = 0; i < n; ++i) {
        complex<double> res;
        res.real(i);
        res.imag(0.);
        mvec[i] = res;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "assigning mvec done.");

    scheme.encrypt(cipher, mvec, n, logp, logq);
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "encryption of mvec done.");


    serutils.writeCiphertext(cipher, path+"ciphertext");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "writing ciphertext done.");

    secretKey2.loadKey(path_serkey);
    Scheme scheme2(ring2, true);
    scheme2.init(path, 0);
    scheme2.addEncKey(secretKey2);
    scheme2.addMultKey(secretKey2);
    scheme2.loadEncKey();
    scheme2.loadMultKey();
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "loading scheme2 done.");

    serutils.readCiphertext(cipher2, path+"ciphertext");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "loading ciphertext2 done.");

    complex<double>* dvec = scheme2.decrypt(secretKey2, cipher2);
    __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "decryption done.");

    for (long i = 0; i < 5; ++i) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "---------------------","");
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "mval: %d : %f + %f i",i,mvec[i]);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "dval: %d : %f + %f i",i,dvec[i]);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "eval: %d : %f + %f i",i,mvec[i] - dvec[i]);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI:", "---------------------","");
    }
}