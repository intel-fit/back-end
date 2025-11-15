package rto.intelfit.service.ocr;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ImageQualityEvaluator {

    static {
        OpenCV.loadShared();
    }

    private static final double BLUR_THRESHOLD = 120.0;
    private static final double SKEW_THRESHOLD_DEGREES = 3.0;

    public QualityReport evaluate(byte[] imageBytes) {
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_GRAYSCALE);
        if (image.empty()) {
            return new QualityReport(0.0, 0.0, true);
        }

        double blur = calculateLaplacianVariance(image);
        double skew = estimateSkewAngle(image);
        image.release();
        boolean lowQuality = blur < BLUR_THRESHOLD || Math.abs(skew) > SKEW_THRESHOLD_DEGREES;

        return new QualityReport(blur, skew, lowQuality);
    }

    private double calculateLaplacianVariance(Mat gray) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        org.opencv.core.Core.meanStdDev(laplacian, mean, std);
        double variance = Math.pow(std.get(0, 0)[0], 2);
        laplacian.release();
        mean.release();
        std.release();
        return variance;
    }

    private double estimateSkewAngle(Mat gray) {
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 100, 50, 10);

        if (lines.rows() == 0) {
            edges.release();
            lines.release();
            return 0.0;
        }

        List<Double> angles = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            Point pt1 = new Point(line[0], line[1]);
            Point pt2 = new Point(line[2], line[3]);
            double angle = Math.toDegrees(Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x));
            if (!Double.isNaN(angle)) {
                angles.add(angle);
            }
        }

        edges.release();
        lines.release();

        if (angles.isEmpty()) {
            return 0.0;
        }

        double sum = angles.stream().mapToDouble(Double::doubleValue).sum();
        return sum / angles.size();
    }

    public boolean isLowQuality(QualityReport report) {
        return report == null || report.lowQuality;
    }

    @Getter
    @RequiredArgsConstructor
    public static class QualityReport {
        private final double blurVariance;
        private final double skewAngle;
        private final boolean lowQuality;

        public String describe(String label) {
            return String.format("%s blur=%.2f, skew=%.2f°, lowQuality=%s",
                    label, blurVariance, skewAngle, lowQuality);
        }
    }
}
