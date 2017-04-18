package asd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
    
    public static void findRectangle(Mat src) throws Exception {
        
        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve = null;
        MatOfPoint2f maxApproxCurve = null;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {
                    
                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    double area = Imgproc.contourArea(contour);
                    approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve, Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurve.total() == 4 && area >= maxArea) {
                        
                        double maxCosine = 0;
                        
                        List<Point> curves = approxCurve.toList();
                        for (int j = 2; j < 5; j++) {
                            double cosine = Math.abs(angle(curves.get(j % 4), curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.5) {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                            maxApproxCurve = approxCurve;
                        }
                    }
                }
            }
        }

       if (maxId >= 0) {           
           
           MatOfPoint corners = new MatOfPoint(maxApproxCurve.toArray());
           contours.add(corners);           
           
           Mat maskImage = new Mat(src.size(), CvType.CV_8U, new Scalar(0));
           Imgproc.drawContours(maskImage, contours, contours.size() - 1, new Scalar(255), Core.FILLED);
           
//           for (Point p :  corners.toList()) {
//               Imgproc.circle(src, p, 20, new Scalar(255, 0, 0));   
//           }
           
           List<Point> l = corners.toList();
           
           Point p0 = l.get(0);
           Point p1 = l.get(1);
           Point p2 = l.get(2);
           Point p3 = l.get(3);
           
           System.out.println(p0.x + ", " + p0.y);
           System.out.println(p1.x + ", " + p1.y);
           System.out.println(p2.x + ", " + p2.y);
           System.out.println(p3.x + ", " + p3.y);
           
           Double biggestX = null;
           Double lowestX = null;
           Double biggestY = null;
           Double lowestY = null;
           
           List<Point> l2 = new ArrayList<>(l);
           l2.sort((i, j) -> Double.compare(i.x, j.x));
           List<Point> l3 = new ArrayList<>(Arrays.asList(l2.get(0), l2.get(1)));
           l3.sort((i,j) -> Double.compare(i.y, j.y));
           System.out.println(l3);
           
           Point cornerLeft = l3.get(0);
           Integer index = l.indexOf(cornerLeft);
                      
           System.out.println(index);
                      
           for (Point p : l) {               
               
               if (null == biggestX || biggestX < p.x) {
                   biggestX = p.x;
               }
               if (null == lowestX || lowestX > p.x) {
                   lowestX = p.x;
               }
               if (null == biggestY || biggestY < p.y) {
                   biggestY = p.y;
               }
               if (null == lowestY || lowestY > p.y) {
                   lowestY = p.y;
               }               
           }
           
           Double height = biggestY - lowestY;
           Double width = biggestX - lowestX;
           
           l.sort((i, j) -> Double.compare(i.y, j.y));
           List<Point> tl = Arrays.asList(l.get(0), l.get(1));
           List<Point> bl = Arrays.asList(l.get(2), l.get(3));
           tl.sort((i, j) -> Double.compare(i.x, j.x));
           bl.sort((i, j) -> Double.compare(i.x, j.x));
           //Arrays.asList(tl.get(0), tl.get(1), bl.get(index))
           l = Arrays.asList(tl.get(0), bl.get(0), bl.get(1), tl.get(1));
           
           MatOfPoint2f start = new MatOfPoint2f();
           start.fromList(l);
           MatOfPoint2f dst = new MatOfPoint2f();
           dst.fromList(Arrays.asList(new Point(0, 0), new Point(0, height), new Point(width, height), new Point(width, 0)));           
           
           Mat perspective = new Mat(3, 3, CvType.CV_32FC1);
           perspective = Calib3d.findHomography(start, dst);
           
           Mat result = new Mat(width.intValue(), height.intValue(), CvType.CV_8UC4);
           Imgproc.warpPerspective(src, result, perspective, new Size(width, height), Imgproc.INTER_CUBIC);
           Imgproc.drawContours(src, contours, contours.size() - 1, new Scalar(255, 0, 0));
           
           Imgcodecs.imwrite("xyz.jpg", result);
           
           
//           MatOfInt mOi= new MatOfInt();
//           
//           Imgproc.convexHull(contours.get(contours.size() - 1), mOi); 
//           int[] intlist = mOi.toArray();
//           List<Point> l = new ArrayList<Point>();
//           l.clear();
//           for (int i = 0; i < intlist.length; i++) {
//               l.add(contours.get(contours.size() - 1).toList().get(mOi.toList().get(i)));
//           }
//           
//           Imgproc.drawContours(maskImage, ,0, new Scalar(100), 2);

           
       }
         
//       MatOfPoint maxMatOfPoint = contours.get(maxId);
//       MatOfPoint2f maxMatOfPoint2f = new MatOfPoint2f(maxMatOfPoint.toArray());

//       RotatedRect rect = Imgproc.minAreaRect(maxMatOfPoint2f);

//       Point points[] = new Point[4];
//       rect.points(points);
//       for (int i = 0; i < 4; ++i) 
//       {
//             Imgproc.line(src, points[i], points[(i + 1) % 4], new Scalar(255, 0, 0), 2);
//       }
    }
      

    public static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public static void main(String[] args) throws Exception {
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat image = Imgcodecs.imread("D:/workspace/eclipse_mobuss_web/asd/abc.jpg"); 
        findRectangle(image);
        //Imgcodecs.imwrite("xyz.jpg", image);
        
        //image = new Mat(image, new Rect(106, 106, 212, 212));
//        
//        Mat gray = new Mat();
//        Mat binary = new Mat();
//        Mat finalImage = new Mat();
//        
//        Mat hierarchy = new Mat();
//        List<MatOfPoint> contours = new ArrayList<>();
//        
//        MatOfPoint biggerContour = null;
//        
//        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);      
//        
//        //Imgproc.threshold(gray, binary, 150, 255, Imgproc.THRESH_BINARY);
//        //Imgproc.equalizeHist(gray, gray);
//        
//        Imgproc.Canny(gray, binary, 80, 255);
//        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
//                
//        for (MatOfPoint contour: contours) {      
//            if (biggerContour == null || Imgproc.contourArea(contour) > Imgproc.contourArea(biggerContour)) {
//                    biggerContour = new MatOfPoint(contour.toArray());   
//            }
//
//        }
//        
//        Mat maskImage = new Mat(image.size(), CvType.CV_8U, new Scalar(0));
//        Imgproc.drawContours(maskImage, contours, contours.indexOf(biggerContour), new Scalar(255), Core.FILLED);
//        
//        image.copyTo(finalImage, maskImage);     
//        finalImage.convertTo(finalImage, -1, 1.1, 0);
//        
//        Imgproc.connectedComponentsWithStats
//        Rect r = Imgproc.boundingRect(biggerContour);
//        Imgproc.rectangle(finalImage, r.tl(), r.br(), new Scalar(255));
//        
//        Imgcodecs.imwrite("xyz.jpg", finalImage);
        
       
    }
//    
//    public static Mat warp(Mat inputMat,Mat startM) {
//        
//        int resultWidth = 1000;
//        int resultHeight = 1000;
//
//        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);
//
//
//
//        Point ocvPOut1 = new Point(0, 0);
//        Point ocvPOut2 = new Point(0, resultHeight);
//        Point ocvPOut3 = new Point(resultWidth, resultHeight);
//        Point ocvPOut4 = new Point(resultWidth, 0);
//        List<Point> dest = new ArrayList<Point>();
//        dest.add(ocvPOut1);
//        dest.add(ocvPOut2);
//        dest.add(ocvPOut3);
//        dest.add(ocvPOut4);
//        Mat endM = Converters.vector_Point2f_to_Mat(dest);      
//
//        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);
//
//        Imgproc.warpPerspective(inputMat, 
//                                outputMat,
//                                perspectiveTransform,
//                                new Size(resultWidth, resultHeight), 
//                                Imgproc.INTER_CUBIC);
//
//        return outputMat;
//    }
    //}
}