package application;

import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

public class DetectFaceOld {
	
	public void run(Mat image, String imageName) {
		
		CascadeClassifier faceDetector = new CascadeClassifier("resources/lbpcascade_profileface.xml");
		
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);
		
		int noFaces = faceDetections.toArray().length;
		System.out.println("Detected " + noFaces + " faces");
		
		ArrayList<Point> points = new ArrayList<Point>();
		
		if (noFaces > 0) {
			Rect rect = faceDetections.toArray()[0];
			Point centerFace = new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
//			for (Rect rect: faceDetections.toArray()) {
//				Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
//			}
			
			//Get skin colour
			ArrayList<double[]> skinColours = new ArrayList<double[]>();
			boolean spiralBoolean = true;
			Point spiral = centerFace.clone();
			for (int i = 0; i < rect.width / 3; i++) {
				if (spiralBoolean) {
					for (int j = 0; j < i + 1; j++) {
						spiral = new Point(spiral.x + 1, spiral.y);
						skinColours.add(image.get((int)spiral.y, (int)spiral.x));
						//points.add(spiral.clone());
					}
					for (int j = 0; j < i + 1; j++) {
						spiral = new Point(spiral.x, spiral.y - 1);
						skinColours.add(image.get((int)spiral.y, (int)spiral.x));
						//points.add(spiral.clone());
					}
				} else {
					for (int j = 0; j < i + 1; j++) {
						spiral = new Point(spiral.x - 1, spiral.y);
						skinColours.add(image.get((int)spiral.y, (int)spiral.x));
						//points.add(spiral.clone());
					}
					for (int j = 0; j < i + 1; j++) {
						spiral = new Point(spiral.x, spiral.y + 1);
						skinColours.add(image.get((int)spiral.y, (int)spiral.x));
						//points.add(spiral.clone());
					}
				}
				spiralBoolean = !spiralBoolean;
			}
			
			double[] sumTotal = {0, 0, 0};
			for (double[] numberArray: skinColours) {
				for (int i = 0; i < numberArray.length; i++) {
					sumTotal[i] += numberArray[i];
				}
			}
			int size = skinColours.size();
			double[] average = {sumTotal[0] / size, sumTotal[1] / size, sumTotal[2] / size};
			
			double[] variance = {0, 0, 0};
			for (double[] numberArray: skinColours) {
				for (int i = 0; i < numberArray.length; i++) {
					variance[i] += (numberArray[i] - average[i]) * (numberArray[i] - average[i]);
				}
			}
			for (int i = 0; i < variance.length; i++) {
				variance[i] = variance[i] / size;
			}
			
			double[] standardDeviation = {Math.sqrt(variance[0]), Math.sqrt(variance[1]), Math.sqrt(variance[2])};
			System.out.println(standardDeviation[0] + " " + standardDeviation[1] + " " + standardDeviation[2]);
			
			double colourWeight = 0;
			for (int i = 0; i < variance.length; i++) {
				colourWeight += variance[i] / 4;
			}
			colourWeight = colourWeight / variance.length;
			
			//Find tip of nose
			Point fiducialC = centerFace.clone();
//			boolean found = false;
//			int c = (int)centerFace.x;
//			while (!found && c > 0) {
//				points.add(fiducialC.clone());
//				if (compare(image.get((int)fiducialC.y, (int)fiducialC.x - 1), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
//					fiducialC = new Point(fiducialC.x - 1, fiducialC.y);
//				} else if (compare(image.get((int)fiducialC.y + 1, (int)fiducialC.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
//					fiducialC = new Point(fiducialC.x, fiducialC.y + 1);
//				} else if (compare(image.get((int)fiducialC.y - 1, (int)fiducialC.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
//					fiducialC = new Point(fiducialC.x, fiducialC.y - 1);
//				} else {
//					found = true;
//				}
//				c--;
//			}
			for (double i = centerFace.x; i >= rect.x - (rect.width / 4); i--) {
				double yHalfHeight = centerFace.x - i;
				double topY = centerFace.y - yHalfHeight;
				if (topY < 0) topY = 0;
				double bottomY = centerFace.y + yHalfHeight;
				if (bottomY > image.height() - 1) bottomY = image.height() - 1; 
				for (double j = topY; j <= bottomY; j++) {
					if (compare(image.get((int)j, (int)i), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
						points.add(new Point(i, j));
						fiducialC = new Point(i, j);
					}
				}
			}
			
			//Find Fiducial B
			Point fiducialB = fiducialC.clone();
			Point finder = new Point(fiducialC.x, fiducialC.y);
			ArrayList<double[]> upperFacePoints = new ArrayList<double[]>();
			int direction = 0;
			for (int i = 0; i < 10000; i++) {
				if (compare(image.get((int)finder.y, (int)finder.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
					if (direction == 0) {
						direction = 3;
					} else {
						direction--;
					}
					double[] coords = {finder.x, finder.y};
					upperFacePoints.add(coords);
					points.add(finder.clone());
				} else {
					if (direction == 3) {
						direction = 0;
					} else {
						direction++;
					}
				}
				switch (direction) {
				case 0:
					finder = new Point(finder.x, finder.y - 1);
					break;
				case 1:
					finder = new Point(finder.x + 1, finder.y);
					break;
				case 2:
					finder = new Point(finder.x, finder.y + 1);
					break;
				case 3:
					finder = new Point(finder.x - 1, finder.y);
					break;
				}
			}
			
			int intervalSize = rect.width / 10;
			ArrayList<SimpleRegression> regressions = new ArrayList<SimpleRegression>();
			for (int j = 0; j < upperFacePoints.size(); j+= intervalSize) {
				SimpleRegression sr = new SimpleRegression();
				int sizeOfArray = upperFacePoints.size() - j;
				if (sizeOfArray > intervalSize) {
					sizeOfArray = intervalSize;
				}
				double[][] array = new double[sizeOfArray][];
				for (int i = 0; i < sizeOfArray; i++) {
					array[i] = upperFacePoints.get(j + i);
				}
				sr.addData(array);
				regressions.add(sr);
				Point start = new Point(array[0][0], sr.predict(array[0][0]));
				Point end = new Point(array[array.length - 1][0], sr.predict(array[array.length - 1][0]));
				//Core.line(image, start, end, new Scalar(255, 255, 0));
			}
			
//			SimpleRegression lastSr = null;
//			boolean found = false;
//			for (SimpleRegression sr: regressions) {
//				if (sr.getSlope() > 0 && !found) {
//					found = true;
//					RealMatrix coefficients = new Array2DRowRealMatrix(new double[][] { {lastSr.getSlope(), -1}, {sr.getSlope(), -1}}, false);
//					DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
//					RealVector constants = new ArrayRealVector(new double[] {-lastSr.getIntercept(), -sr.getIntercept()}, false);
//					RealVector solution = solver.solve(constants);
//					fiducialB = new Point((int)solution.toArray()[0], (int)solution.toArray()[1]);
//				}
//				lastSr = sr;
//			}
			
			//Find Ficucial D
			Point fiducialD = fiducialC.clone();
//			finder = new Point(fiducialC.x, fiducialC.y);
//			for (int i = 0; i < 300; i++) {
//				Point newPosition = new Point(finder.x, finder.y + 1);
//				//points.add(finder.clone());
//				if (compare(image.get((int)newPosition.y, (int)newPosition.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
//					finder = newPosition.clone();
//				} else {
//					finder = new Point(finder.x + 1, finder.y);
//				}
//			}
			intervalSize = intervalSize * 2;
			finder = new Point(fiducialC.x, fiducialC.y);
			ArrayList<double[]> lowerFacePoints = new ArrayList<double[]>();
			direction = 2;
			for (int i = 0; i < 10000; i++) {
				if (!compare(image.get((int)finder.y, (int)finder.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
					if (direction == 0) {
						direction = 3;
					} else {
						direction--;
					}
					double[] coords = {finder.x, finder.y};
					lowerFacePoints.add(coords);
					points.add(finder.clone());
				} else {
					if (direction == 3) {
						direction = 0;
					} else {
						direction++;
					}
				}
				switch (direction) {
				case 0:
					finder = new Point(finder.x, finder.y - 1);
					break;
				case 1:
					finder = new Point(finder.x + 1, finder.y);
					break;
				case 2:
					finder = new Point(finder.x, finder.y + 1);
					break;
				case 3:
					finder = new Point(finder.x - 1, finder.y);
					break;
				}
			}
			
			regressions = new ArrayList<SimpleRegression>();
			for (int j = 0; j < lowerFacePoints.size(); j+= intervalSize) {
				SimpleRegression sr = new SimpleRegression();
				int sizeOfArray = lowerFacePoints.size() - j;
				if (sizeOfArray > intervalSize) {
					sizeOfArray = intervalSize;
				}
				double[][] array = new double[sizeOfArray][];
				for (int i = 0; i < sizeOfArray; i++) {
					array[i] = lowerFacePoints.get(j + i);
				}
				sr.addData(array);
				regressions.add(sr);
				Point start = new Point(array[0][0], sr.predict(array[0][0]));
				Point end = new Point(array[array.length - 1][0], sr.predict(array[array.length - 1][0]));
				Core.line(image, start, end, new Scalar(255, 255, 0));
			}
			
//			lastSr = null;
//			found = false;
//			for (SimpleRegression sr: regressions) {
//				if (sr.getSlope() < 0 && !found) {
//					found = true;
//					RealMatrix coefficients = new Array2DRowRealMatrix(new double[][] { {lastSr.getSlope(), -1}, {sr.getSlope(), -1}}, false);
//					DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
//					RealVector constants = new ArrayRealVector(new double[] {-lastSr.getIntercept(), -sr.getIntercept()}, false);
//					RealVector solution = solver.solve(constants);
//					fiducialD = new Point((int)solution.toArray()[0], (int)solution.toArray()[1]);
//				}
//				lastSr = sr;
//			}

			//Debug points
			for (Point p: points) {
				Core.circle(image, p, 0, new Scalar(255, 0, 0));
			}
			
			//Draw boxes and circles
			Core.circle(image, centerFace, 3, new Scalar(0, 255, 0));
			Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
			Core.circle(image, fiducialC, 3, new Scalar(0, 0, 255));
			Core.circle(image, fiducialB, 3, new Scalar(0, 0, 255));
			Core.circle(image, fiducialD, 3, new Scalar(0, 0, 255));
			String filename = imageName.replace("images", "facedetects");
			Highgui.imwrite(filename, image);
		}
		
	}
	
	private boolean compare(double[] newPoint, double[] oldPoint, double weight) {
		
		int difference = 0;
		for (int i = 0; i < oldPoint.length; i++) {
			difference += Math.abs(oldPoint[i] - newPoint[i]);
		}
		
		if (difference < weight) {
			return true;
		}
		
		return false;
		
	}

}
