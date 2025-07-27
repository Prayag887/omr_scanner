#include <jni.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <numeric>
#include <android/log.h>
#include <fstream>

#define LOG_TAG "OMRProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

string APP_PACKAGE_PATH;

// Get the base path for file operations
string getBasePath() {
    return "/data/data/" + APP_PACKAGE_PATH + "/files/";
}

const int OPTIONS_PER_QUESTION = 4;
const double SELECTION_THRESHOLD = 0.5;
const int MIN_BUBBLE_AREA = 10;
const int MAX_BUBBLE_AREA = 2000;
const int MIN_BUBBLE_DIMENSION = 15;
const int MAX_BUBBLE_DIMENSION = 200;
const int COLUMN_PADDING = 10;

// this is for row grouping
const int VERTICAL_THRESHOLD = 15;

struct QuestionBubbles {
    vector<Rect> options;
    vector<float> fillPercentages;
    vector<vector<Rect>> columnBubbles;
};


Mat preprocessForOMR(Mat& gray) {
    Mat binary;
    GaussianBlur(gray, gray, Size(5, 5), 0);
    threshold(gray, binary, 0, 255, THRESH_BINARY_INV + THRESH_OTSU);

    // Create a copy for visualization
    Mat visual = binary.clone();
    cvtColor(visual, visual, COLOR_GRAY2BGR);  // Convert to color for debugging

    // Variables to store the longest line information
    int longestLineLength = 0;
    int startX = 0, endX = 0, lineY = 0;  // Coordinates for the longest line

    // Iterate over each row to detect continuous white pixels
    for (int i = 0; i < binary.rows; ++i) {
        int currentLineStartX = -1;
        int currentLineEndX = -1;
        int currentLineLength = 0;

        for (int j = 0; j < binary.cols; ++j) {
            if (binary.at<uchar>(i, j) == 255) {
                // Start of a new line
                if (currentLineStartX == -1) {
                    currentLineStartX = j;
                }
                currentLineEndX = j;
                currentLineLength = currentLineEndX - currentLineStartX + 1;
            } else {
                // End of the current line
                if (currentLineLength > longestLineLength) {
                    longestLineLength = currentLineLength;
                    startX = currentLineStartX;
                    endX = currentLineEndX;
                    lineY = i;
                }
                currentLineStartX = -1;  // Reset for the next potential line
                currentLineEndX = -1;
                currentLineLength = 0;
            }
        }

        // Check for the last line in the row (if it ends at the last column)
        if (currentLineLength > longestLineLength) {
            longestLineLength = currentLineLength;
            startX = currentLineStartX;
            endX = currentLineEndX;
            lineY = i;
        }
    }

    // If a longest line is detected, draw it on the image (in red)
    if (longestLineLength > 0) {
        line(visual, Point(startX, lineY), Point(endX, lineY), Scalar(0, 0, 255), 2);  // Red line
    }

    // Crop the region below the detected line
    if (lineY > 0) {
        Rect cropRegion(0, lineY, binary.cols, binary.rows - lineY);  // Region below the longest line
        Mat croppedBinary = binary(cropRegion);

        // Save the cropped binary image
        string a_binaryPath = getBasePath() + "binary.png";
        imwrite(a_binaryPath, croppedBinary);

        // Save the visualization for debugging
        string visualPath = getBasePath() + "visual_debug.png";
        imwrite(visualPath, visual);

        return croppedBinary;
    }

    // If no line is detected or other issues, return the original binary
    return binary;
}



vector<Rect> detectBubbles(Mat& binary) {
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(binary, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE); // Use RETR_CCOMP to get both filled and outlined

    vector<Rect> bubbles;
    for (const auto& contour : contours) {
        Rect r = boundingRect(contour);
        double area = contourArea(contour);

        LOGI("Found contour: area=%f, width=%d, height=%d, x=%d, y=%d",
             area, r.width, r.height, r.x, r.y);

        if (area > MIN_BUBBLE_AREA && area < MAX_BUBBLE_AREA &&
            r.width > MIN_BUBBLE_DIMENSION && r.width < MAX_BUBBLE_DIMENSION &&
            r.height > MIN_BUBBLE_DIMENSION && r.height < MAX_BUBBLE_DIMENSION) {
            bubbles.push_back(r);
            LOGI("Added bubble at (%d,%d)", r.x, r.y);
        }
    }
    return bubbles;
}


vector<Rect> filterDuplicates(vector<Rect>& bubbles, double minDist = 10.0) {
    vector<Rect> filtered;
    for (size_t i = 0; i < bubbles.size(); i++) {
        bool duplicate = false;
        Point2i center1(bubbles[i].x + bubbles[i].width/2,
                        bubbles[i].y + bubbles[i].height/2);

        for (const auto& f : filtered) {
            Point2i center2(f.x + f.width/2, f.y + f.height/2);
            if (norm(center1 - center2) < minDist) {
                duplicate = true;
                break;
            }
        }
        if (!duplicate) filtered.push_back(bubbles[i]);
    }
    return filtered;
}

vector<vector<Rect>> organizeBubblesByQuestion(vector<Rect>& bubbles) {
    vector<vector<Rect>> questions;
    // Sort bubbles by Y first (top-to-bottom), then by X (left-to-right)
    sort(bubbles.begin(), bubbles.end(), [](const Rect &a, const Rect &b) {
        return (a.y == b.y) ? (a.x < b.x) : (a.y < b.y);
    });
    for (const auto& bubble : bubbles) {
        bool added = false;
        for (auto& question : questions) {
            if (abs(question[0].y - bubble.y) < VERTICAL_THRESHOLD) {
                question.push_back(bubble);
                added = true;
                break;
            }
        }
        if (!added) {
            questions.push_back({bubble});
        }
    }
    // Ensure bubbles within each question are sorted left-to-right
    for (auto& question : questions) {
        sort(question.begin(), question.end(), [](const Rect &a, const Rect &b) {
            return a.x < b.x;
        });
        // If 5 bubbles are detected, remove the first one
        if (question.size() == 5) {
            question.erase(question.begin());
        }
    }

    return questions;
}

QuestionBubbles processColumns(Mat& binary) {
    QuestionBubbles result;
    vector<Rect> bubbles = detectBubbles(binary);
    bubbles = filterDuplicates(bubbles);

    vector<float> x_coords;
    for (const auto& b : bubbles)
        x_coords.push_back(b.x + b.width / 2.0f);

    Mat data(x_coords.size(), 1, CV_32F, x_coords.data());
    Mat labels, centers;
    kmeans(data, 4, labels, TermCriteria(TermCriteria::EPS + TermCriteria::COUNT, 10, 1.0),
           3, KMEANS_PP_CENTERS, centers);

    vector<int> sortedIndices(4);
    iota(sortedIndices.begin(), sortedIndices.end(), 0);
    sort(sortedIndices.begin(), sortedIndices.end(), [&](int a, int b) {
        return centers.at<float>(a) < centers.at<float>(b);
    });

    vector<vector<Rect>> columns(4);
    for (size_t i = 0; i < bubbles.size(); ++i)
        columns[labels.at<int>(i)].push_back(bubbles[i]);

    for (int col = 0; col < 4; ++col) {
        int actualCol = sortedIndices[col];
        auto& colBubbles = columns[actualCol];
        if (colBubbles.empty()) continue;

        int min_x = INT_MAX, max_x = 0, min_y = INT_MAX, max_y = 0;
        for (const auto& b : colBubbles) {
            min_x = min(min_x, b.x);
            max_x = max(max_x, b.x + b.width);
            min_y = min(min_y, b.y);
            max_y = max(max_y, b.y + b.height);
        }

        const int TOP_PADDING = 100;
        const int BOTTOM_PADDING = 100;

        int x = max(0, min_x - COLUMN_PADDING);
        int y = max(0, min_y - TOP_PADDING);
        int w = min(binary.cols - x, max_x - min_x + 2 * COLUMN_PADDING);
        int h = min(binary.rows - y, max_y - min_y + 2 * COLUMN_PADDING + BOTTOM_PADDING);

        Rect roi(x, y, w, h);


        Mat columnImg = binary(roi);
        string colPath = getBasePath() + "column_" + to_string(col + 1) + ".png";
        imwrite(colPath, columnImg);

        sort(colBubbles.begin(), colBubbles.end(), [](const Rect& a, const Rect& b) {
            return a.y < b.y;
        });

        result.columnBubbles.push_back(colBubbles);
    }

    return result;
}

pair<vector<int>, vector<Rect>> analyzeColumn(Mat& columnImg, int colIndex) {
    vector<int> answers;
    vector<Rect> selectedBubbles;

    vector<Rect> bubbles = detectBubbles(columnImg);
    bubbles = filterDuplicates(bubbles, 15.0);

    if (bubbles.empty()) {
        LOGE("No bubbles detected in column %d", colIndex + 1);
        return {answers, selectedBubbles};
    }

    vector<vector<Rect>> questions = organizeBubblesByQuestion(bubbles);

    // Filter out invalid questions (1-2 bubbles) and validate question count
    vector<vector<Rect>> validQuestions;
    for (size_t q = 0; q < questions.size(); q++) {
        if (questions[q].size() >= 3 && questions[q].size() <= 4) {
            validQuestions.push_back(questions[q]);
        } else if (questions[q].size() == 1 || questions[q].size() == 2) {
            LOGI("Ignoring invalid question with %d bubbles (needs 3-4)", (int)questions[q].size());
        } else {
            LOGE("Question has %d bubbles, expected 3-4", (int)questions[q].size());
        }
    }

    // Validate we have exactly 50 questions
    if (validQuestions.size() != 50) {
        LOGE("Column %d has %d valid questions, expected 50", colIndex + 1, (int)validQuestions.size());
    }

    Mat debugImg;
    cvtColor(columnImg, debugImg, COLOR_GRAY2BGR);

    // Process only valid questions
    for (size_t q = 0; q < validQuestions.size(); q++) {
        auto& options = validQuestions[q];

        // Draw all options for this question
        for (size_t o = 0; o < options.size(); o++) {
            rectangle(debugImg, options[o], Scalar(255, 0, 0), 1);

            string label = "Q" + to_string(q+1) + char('A' + o);
            Point textPos(options[o].x + 5, options[o].y - 5);
            if (textPos.y < 5) textPos.y = options[o].y + 15;

            putText(debugImg, label, textPos, FONT_HERSHEY_SIMPLEX,
                    0.25, Scalar(0, 255, 255), 1);
        }

        // Calculate fill percentages for each option
        vector<float> fills;
        for (const auto& opt : options) {
            // Simply shrink the rectangle to focus on the center area
            // This preserves more bubble detail than a circular mask
            int shrinkX = opt.width * 0.1;  // Shrink by 10% on each side
            int shrinkY = opt.height * 0.1;

            Rect shrunkOpt(
                    opt.x + shrinkX,
                    opt.y + shrinkY,
                    max(1, opt.width - 2*shrinkX),
                    max(1, opt.height - 2*shrinkY)
            );

            // Ensure shrunk rectangle is within image bounds
            shrunkOpt = shrunkOpt & Rect(0, 0, columnImg.cols, columnImg.rows);

            Mat roi = columnImg(shrunkOpt);

            // Count white pixels (filled areas in inverted image)
            int whitePixels = countNonZero(roi);
            int totalPixels = roi.total();
            float fill = whitePixels / (float)totalPixels;
            fills.push_back(fill);

            // Debug info for first few bubbles
            if (q < 2) {
                LOGI("Debug Q%d-%c: original=%dx%d, shrunk=%dx%d, shrink=(%d,%d)",
                     (int)q+1, 'A' + fills.size() - 1,
                     opt.width, opt.height, shrunkOpt.width, shrunkOpt.height, shrinkX, shrinkY);

                // Save debug images
                string originalPath = getBasePath() + "debug_original_Q" + to_string(q+1) + "_" + char('A' + fills.size() - 1) + ".png";
                imwrite(originalPath, columnImg(opt));

                string shrunkPath = getBasePath() + "debug_shrunk_Q" + to_string(q+1) + "_" + char('A' + fills.size() - 1) + ".png";
                imwrite(shrunkPath, roi);
            }

            LOGI("Q%d-%c fill: %.2f (%d/%d pixels) [shrunk by %dx%d] %s",
                 (int)q+1, 'A' + fills.size() - 1, fill, whitePixels, totalPixels, shrinkX, shrinkY,
                 fill > 0.6 ? "FILLED" : "unfilled");
        }

        // Find the most filled bubble above threshold
        int selected = -1;
        float maxFill = 0;
        for (size_t o = 0; o < fills.size(); o++) {
            if (fills[o] > SELECTION_THRESHOLD && fills[o] > maxFill) {
                maxFill = fills[o];
                selected = o;
            }
        }

        answers.push_back(selected);

        if (selected != -1) {
            selectedBubbles.push_back(options[selected]);
            rectangle(debugImg, options[selected], Scalar(0, 255, 0), 2);
        } else {
            selectedBubbles.push_back(Rect(-1, -1, 0, 0));
        }
    }

    // Ensure we have exactly 50 answers (pad with -1 if needed)
    while (answers.size() < 50) {
        answers.push_back(-1);
        selectedBubbles.push_back(Rect(-1, -1, 0, 0));
    }

    string debugPath = getBasePath() + "debug_column_" + to_string(colIndex + 1) + ".png";
    imwrite(debugPath, debugImg);

    return {answers, selectedBubbles};
}

void generateMarkedImage(Mat& columnImg, vector<Rect>& bubbles,
                         const vector<Rect>& selected, int colIndex) {
    Mat marked;
    cvtColor(columnImg, marked, COLOR_GRAY2BGR);

    vector<vector<Rect>> questions = organizeBubblesByQuestion(bubbles);

    for (size_t q = 0; q < questions.size(); q++) {
        auto& options = questions[q];

        for (size_t o = 0; o < options.size(); o++) {
            if (o >= OPTIONS_PER_QUESTION) break;

            rectangle(marked, options[o], Scalar(255, 0, 0), 1);

            string label = "" + static_cast<char>('A' + o);
            Point textPos(options[o].x + 5, options[o].y - 5);
            if (textPos.y < 10) textPos.y = options[o].y + 15;

            putText(marked, label, textPos, FONT_HERSHEY_SIMPLEX,
                    0.25, Scalar(0, 255, 255), 2, LINE_AA);
        }
    }

    for (const auto& sel : selected) {
        if (sel.x < 0 || sel.y < 0) continue;

        rectangle(marked, sel, Scalar(0, 255, 0), 2);

        Point checkPos(sel.x + 10, sel.y + sel.height / 2);
        putText(marked, "✓", checkPos, FONT_HERSHEY_SIMPLEX,
                0.7, Scalar(0, 255, 0), 2, LINE_AA);
    }

    string path = getBasePath() + "marked_column_" + to_string(colIndex + 1) + ".png";
    imwrite(path, marked);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_prayag_omr_1scan_1aar_data_omrresult_repository_OMRRepositoryImpl_processOMR(JNIEnv* env, jobject thiz, jlong matAddr, jstring dir_path){
    // Get the package name from the context
    APP_PACKAGE_PATH = env->GetStringUTFChars(dir_path, 0);
    LOGI("Using package path: %s", APP_PACKAGE_PATH.c_str());
    vector<int> finalAnswers;

    try {
        // Load the saved paper image instead of using matAddr
        string paperPath = getBasePath() + "paper.png";
        Mat input = imread(paperPath, IMREAD_COLOR);

        if (input.empty()) {
            LOGI("Failed to load isolated paper image.");
            finalAnswers = {-1};
        } else {
            // Get the dimensions of the input image
            int height = input.rows;
            int width = input.cols;

            // Crop 29% from the top, 10% from the bottom, and 5% from the left and right
            int cropTop = static_cast<int>(height * 0.20);  // 29% of the height
            int cropBottom = static_cast<int>(height * 0.02);  // 10% of the height
            int cropLeft = 0;  // 5% of the width
            int cropRight = 0;  // 5% of the width

            // Define the region of interest (ROI)
            Rect roi(cropLeft, cropTop, width - cropLeft - cropRight, height - cropTop - cropBottom);  // Crop top, bottom, and sides

            // Apply the cropping
            Mat croppedImage = input(roi);
            string cropPath = getBasePath() + "cropped.png";
            imwrite(cropPath, croppedImage);

            // Process the cropped image
            Mat gray, blurred, binary;

            // MODIFIED CODE: Preserve dark regions when converting to grayscale
            // First, identify very dark pixels in the original image
            Mat darkMask;
            inRange(croppedImage, Scalar(0, 0, 0), Scalar(40, 40, 40), darkMask);

            // Normal grayscale conversion
            cvtColor(croppedImage, gray, COLOR_BGR2GRAY);

            // Force the originally dark regions to stay black
            gray.setTo(0, darkMask);

            string grayPath = getBasePath() + "gray.png";
            imwrite(grayPath, gray);

            // Apply Gaussian blur to reduce noise and enhance edges (for clearer detection)
            GaussianBlur(gray, blurred, Size(5, 5), 0);

            // Keep dark regions dark even after blurring
            blurred.setTo(0, darkMask);

            string blurPath = getBasePath() + "blurred.png";
            imwrite(blurPath, blurred);

            // Perform adaptive thresholding to improve clarity for edge detection
            adaptiveThreshold(blurred, binary, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 5);

            // After thresholding, the darkest regions should be white (255), so invert the mask for binary image
            binary.setTo(0, darkMask);  // Ensure darkest areas from original stay black in binary

            string a_binaryPath = getBasePath() + "abinary.png";
            imwrite(a_binaryPath, binary);

            // Process the binary image
            binary = preprocessForOMR(binary);  // Additional preprocessing if needed

            QuestionBubbles qb = processColumns(binary);

            for (int col = 0; col < 4; col++) {
                string colPath = getBasePath() + "column_" + to_string(col + 1) + ".png";
                Mat columnImg = imread(colPath, IMREAD_GRAYSCALE);

                if (columnImg.empty()) {
                    LOGE("Failed to load column %d", col + 1);
                    continue;
                }

                vector<Rect> columnBubbles = detectBubbles(columnImg);
                columnBubbles = filterDuplicates(columnBubbles, 15.0);

                auto [answers, selected] = analyzeColumn(columnImg, col);

                generateMarkedImage(columnImg, columnBubbles, selected, col);

                finalAnswers.insert(finalAnswers.end(), answers.begin(), answers.end());
            }
        }
    }
    catch (const Exception& e) {
        LOGE("Processing failed: %s", e.what());
        finalAnswers = {-1};
    }

    jintArray result = env->NewIntArray(finalAnswers.size());
    env->SetIntArrayRegion(result, 0, finalAnswers.size(), finalAnswers.data());
    return result;
}
