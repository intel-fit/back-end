package rto.intelfit.service.ocr;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class ImagePreprocessor {

    static {
        OpenCV.loadShared();
    }

    private static final int TARGET_MIN_DIMENSION = 2200;

    public PreprocessingResult preprocess(byte[] imageBytes, boolean aggressiveMode) {
        Mat source = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (source.empty()) {
            throw new IllegalArgumentException("이미지 디코딩에 실패했습니다");
        }

        Mat document = detectDocumentArea(source);
        Mat perspectiveCorrected = correctPerspective(source, document);
        Mat normalized = removeLighting(perspectiveCorrected);
        Mat denoised = removeNoise(normalized, aggressiveMode);
        Mat enhanced = enhanceContrast(denoised);
        Mat sharpened = applySharpen(enhanced);
        Mat resized = resample(sharpened);

        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", resized, buffer);
        byte[] processedBytes = buffer.toArray();

        releaseMats(source, document, perspectiveCorrected, normalized, denoised, enhanced, sharpened, resized, buffer);

        return PreprocessingResult.builder()
                .processedImage(processedBytes)
                .mode(aggressiveMode ? "AGGRESSIVE" : "NORMAL")
                .build();
    }

    private Mat detectDocumentArea(Mat source) {
        Mat gray = new Mat();
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        Mat edged = new Mat();
        Imgproc.Canny(gray, edged, 75, 200);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        contours.sort((c1, c2) -> Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true);

            if (approx.total() == 4) {
                Mat result = new Mat();
                approx.convertTo(result, CvType.CV_32F);
                releaseMats(gray, edged, hierarchy);
                approx.release();
                contour2f.release();
                return result;
            }
            approx.release();
            contour2f.release();
        }

        releaseMats(gray, edged, hierarchy);
        return null;
    }

    private Mat correctPerspective(Mat source, Mat quadPoints) {
        if (quadPoints == null) {
            return source.clone();
        }

        Point[] points = sortCorners(quadPoints);
        double widthA = distance(points[2], points[3]);
        double widthB = distance(points[1], points[0]);
        double maxWidth = Math.max(widthA, widthB);

        double heightA = distance(points[1], points[2]);
        double heightB = distance(points[0], points[3]);
        double maxHeight = Math.max(heightA, heightB);

        MatOfPoint2f src = new MatOfPoint2f(points);
        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(maxWidth - 1, 0),
                new Point(maxWidth - 1, maxHeight - 1),
                new Point(0, maxHeight - 1)
        );

        Mat M = Imgproc.getPerspectiveTransform(src, dst);
        Mat warped = new Mat();
        Imgproc.warpPerspective(source, warped, M, new Size(maxWidth, maxHeight));
        releaseMats(M, src, dst);
        return warped;
    }

    private Mat removeLighting(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        Mat background = new Mat();
        Imgproc.medianBlur(gray, background, 31);
        Mat diff = new Mat();
        Core.absdiff(gray, background, diff);

        Mat normalized = new Mat();
        Core.normalize(diff, normalized, 0, 255, Core.NORM_MINMAX);

        Mat thresholded = new Mat();
        Imgproc.adaptiveThreshold(normalized, thresholded, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, 35, 10);

        Mat restored = new Mat();
        Imgproc.cvtColor(thresholded, restored, Imgproc.COLOR_GRAY2BGR);
        releaseMats(gray, background, diff, normalized, thresholded);
        return restored;
    }

    private Mat removeNoise(Mat image, boolean aggressive) {
        Mat prepared = ensureSupportedChannels(image);
        Mat median = new Mat();
        Imgproc.medianBlur(prepared, median, aggressive ? 7 : 5);
        Mat bilateral = new Mat();
        Imgproc.bilateralFilter(median, bilateral, aggressive ? 15 : 9, 75, 75);
        if (prepared != image) {
            prepared.release();
        }
        median.release();
        return bilateral;
    }

    private Mat enhanceContrast(Mat image) {
        Mat lab = new Mat();
        Imgproc.cvtColor(image, lab, Imgproc.COLOR_BGR2Lab);
        List<Mat> labChannels = new ArrayList<>();
        Core.split(lab, labChannels);
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        Mat lChannel = labChannels.get(0);
        clahe.apply(lChannel, lChannel);
        Core.merge(labChannels, lab);
        Mat result = new Mat();
        Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2BGR);
        releaseMats(lab);
        labChannels.forEach(Mat::release);
        return result;
    }

    private Mat applySharpen(Mat image) {
        Mat kernel = new Mat(3, 3, CvType.CV_32F);
        float[] data = {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        };
        kernel.put(0, 0, data);
        Mat result = new Mat();
        Imgproc.filter2D(image, result, -1, kernel);
        kernel.release();
        return result;
    }

    private Mat resample(Mat image) {
        int width = image.width();
        int height = image.height();
        int minDim = Math.min(width, height);

        if (minDim >= TARGET_MIN_DIMENSION) {
            return image.clone();
        }

        double scale = (double) TARGET_MIN_DIMENSION / minDim;
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        Mat resized = new Mat();
        Imgproc.resize(image, resized, new Size(newWidth, newHeight), 0, 0, Imgproc.INTER_CUBIC);
        return resized;
    }

    private Point[] sortCorners(Mat quadPoints) {
        Point[] pts = new Point[4];
        for (int i = 0; i < 4; i++) {
            double[] coords = quadPoints.get(i, 0);
            pts[i] = new Point(coords[0], coords[1]);
        }

        Point[] sorted = new Point[4];
        java.util.Arrays.sort(pts, Comparator.comparingDouble(p -> p.x + p.y));
        sorted[0] = pts[0];
        sorted[2] = pts[3];

        java.util.Arrays.sort(pts, Comparator.comparingDouble(p -> p.x - p.y));
        sorted[1] = pts[0];
        sorted[3] = pts[3];

        return sorted;
    }

    private double distance(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    private void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null) {
                mat.release();
            }
        }
    }

    private Mat ensureSupportedChannels(Mat image) {
        if (image.channels() == 4) {
            Mat converted = new Mat();
            Imgproc.cvtColor(image, converted, Imgproc.COLOR_BGRA2BGR);
            return converted;
        }
        return image;
    }

    @Getter
    @Builder
    public static class PreprocessingResult {
        private final byte[] processedImage;
        private final String mode;
    }
}
