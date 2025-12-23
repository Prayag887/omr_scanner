#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#define LOG_TAG "ImageEnhancer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

static string APP_PACKAGE_PATH;

// ======================= PATH ==========================
static string getBasePath() {
    return "/data/data/" + APP_PACKAGE_PATH + "/files/";
}

// ================= SMART CONTRAST NORMALIZATION =================
static Mat normalizeLightingSmart(const Mat& gray) {
    Mat result = gray.clone();

    // 1. Global contrast check
    Scalar mean, stddev;
    meanStdDev(gray, mean, stddev);

    // 2. Edge density (screen images have high edge regularity)
    Mat edges;
    Canny(gray, edges, 80, 160);
    double edgeDensity = (double)countNonZero(edges) / gray.total();

    LOGI("Contrast stddev=%.2f, edgeDensity=%.4f", stddev[0], edgeDensity);

    // 3. Heuristics
    bool lowContrast = stddev[0] < 45.0;
    bool screenLike  = edgeDensity > 0.12;  // PC / monitor photos

    if (lowContrast && !screenLike) {
        // Use WEAK CLAHE only when truly needed
        Ptr<CLAHE> clahe = createCLAHE(2.0, Size(16, 16));
        clahe->apply(gray, result);
        LOGI("CLAHE applied (safe mode)");
    } else {
        // Safer fallback: gamma correction
        Mat lut(1, 256, CV_8UC1);
        double gamma = (lowContrast ? 0.9 : 1.0);

        for (int i = 0; i < 256; i++) {
            lut.at<uchar>(i) =
                    saturate_cast<uchar>(pow(i / 255.0, gamma) * 255.0);
        }
        LUT(gray, lut, result);
        LOGI("CLAHE skipped → gamma normalization");
    }

    imwrite(getBasePath() + "2_normalized.png", result);
    return result;
}


// =================== BLUR DETECTION ====================
static bool isBlurry(const Mat& gray) {
    Mat lap;
    Laplacian(gray, lap, CV_64F);
    Scalar mu, sigma;
    meanStdDev(lap, mu, sigma);
    return (sigma.val[0] < 30.0);  // empirically stable
}

// ================= OCR OPTIMAL PIPELINE =================
static Mat enhanceForOCR(const Mat& input) {
    Mat gray;

    // 1. Grayscale
    if (input.channels() == 3)
        cvtColor(input, gray, COLOR_BGR2GRAY);
    else
        gray = input.clone();

    imwrite(getBasePath() + "1_gray.png", gray);

    // Normalize lighting (very important)
    Mat norm = normalizeLightingSmart(gray);


    imwrite(getBasePath() + "2_clahe.png", norm);

    // 3. Blur-aware sharpening
    Mat sharp = norm.clone();
    if (isBlurry(norm)) {
        Mat blur;
        GaussianBlur(norm, blur, Size(0, 0), 1.5);
        addWeighted(norm, 1.8, blur, -0.8, 0, sharp);
        LOGI("Blur detected → sharpening applied");
    } else {
        LOGI("Image already sharp → skipping sharpening");
    }

    imwrite(getBasePath() + "3_sharp.png", sharp);


    // 5. Sauvola threshold (best for OCR)
    Mat binary(sharp.size(), CV_8UC1);
    const int win = 21;
    const double k = 0.25;
    const double R = 128;

    Mat padded;
    int p = win / 2;
    copyMakeBorder(sharp, padded, p, p, p, p, BORDER_REFLECT);

    for (int y = p; y < padded.rows - p; y++) {
        for (int x = p; x < padded.cols - p; x++) {
            Rect r(x - p, y - p, win, win);
            Mat roi = padded(r);

            Scalar m, s;
            meanStdDev(roi, m, s);

            double T = m[0] * (1 + k * ((s[0] / R) - 1));
            binary.at<uchar>(y - p, x - p) =
                    (sharp.at<uchar>(y - p, x - p) > T) ? 255 : 0;
        }
    }

    imwrite(getBasePath() + "5_sauvola.png", binary);

    // 6. Morph cleanup (OCR safe)
    Mat kernel = getStructuringElement(MORPH_RECT, Size(2, 2));
    morphologyEx(gray, binary, MORPH_OPEN, kernel);
    morphologyEx(gray, binary, MORPH_CLOSE, kernel);

    // 7. FORCE BLACK BG + WHITE TEXT
    // OCR engines prefer this strictly
    int whitePixels = countNonZero(binary);
    if (whitePixels > (binary.total() / 2)) {
        bitwise_not(gray, binary);
        LOGI("Binary inverted for OCR preference");
    }

    imwrite(getBasePath() + "6_final_binary.png", binary);

    return binary;
}

// ======================== JNI ==========================
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_prayag_omr_1scan_1aar_data_omrresult_repository_OMRRepositoryImpl_processOMR(
        JNIEnv* env,
        jobject /*thiz*/,
        jlong matAddr,
        jstring dir_path) {

    APP_PACKAGE_PATH = env->GetStringUTFChars(dir_path, nullptr);
    LOGI("App path: %s", APP_PACKAGE_PATH.c_str());

    vector<int> status(1, 0);

    try {
        Mat& input = *(Mat*) matAddr;

        if (input.empty()) {
            LOGE("Input image empty");
            status[0] = -1;
        } else {
            imwrite(getBasePath() + "0_original.png", input);

            Mat finalBinary = enhanceForOCR(input);

            bool ok = imwrite(getBasePath() + "ocr_ready.png", finalBinary);
            status[0] = ok ? 1 : -2;
        }
    }
    catch (const cv::Exception& e) {
        LOGE("OpenCV error: %s", e.what());
        status[0] = -3;
    }

    jintArray result = env->NewIntArray(1);
    env->SetIntArrayRegion(result, 0, 1, status.data());
    return result;
}
