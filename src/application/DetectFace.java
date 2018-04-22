package application;

import java.util.ArrayList;
import java.util.HashMap;

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
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class DetectFace {
	
	//Variable to store the image
	private Mat image;
	
	//Variable to store points that can be displayed for debugging
	private ArrayList<Point> points;
	
	//Variables to store the rectangles for the face, ear and eye
	private Rect faceBox;
	private Rect earBox;
	private Rect eyeBox;
	
	//Variables to store the centers of the face, ear and eye boxes
	private Point centerFace;
	private Point centerEar;
	private Point centerEye;
	
	//Varaibles to store the Fiducial points
	private Point fiducialB;
	private Point fiducialC;
	private Point fiducialD;
	
	
	//Main method for face identification
	public HashMap<String, Point> run(Mat inputimage, String imageName) {
		
		//Set image variable to input image
		this.image = inputimage;

		//Set image to grayscale, equalise the hist, and convert back to color image
		Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
		Imgproc.equalizeHist(image, image);
		Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGB);
		
		//Initialise the face detector, which will give the rough position of faces
		CascadeClassifier faceDetector = new CascadeClassifier("resources/lbpcascade_profileface.xml");
		
		//Get the rectangles of all the faces in the picture that were detected
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);
		
		//Check to see if there is a face in the photo, otherwise return null
		int noFaces = faceDetections.toArray().length;
		System.out.println("Detected " + noFaces + " faces");
		if (noFaces > 0) {
			
			//Initialise ArrayList for debug points
			points = new ArrayList<Point>();
			
			//For each rectangle in faceDetections, find the largest rectangle.
			for (Rect r: faceDetections.toArray()) {
				if (faceBox == null) {
					faceBox = r;
				} else if (r.width > faceBox.width) { 
					faceBox = r;
				}
			}
			
			//Calculate the center of the face
			centerFace = new Point(faceBox.x + (faceBox.width / 2), faceBox.y + (faceBox.height / 2));
			
			//Get the weight for the skinColour
			double colourWeight = getSkinWeight(centerFace, faceBox);
			
			//Debug code to check how much the face is within the colour weight
			for (int i = 0; i < image.width(); i++) {
				for (int j = 0; j < image.height(); j++) {
					if (compare(image.get(j, i), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
						//points.add(new Point(i, j));
					}
				}
			}
			
			//Initialise Hash Map for fiducial points
			HashMap<String, Point> fiducialPoints = new HashMap<String, Point>();
			
			//Get Fiducial C
			fiducialC = getFiducialC(colourWeight);
			if (fiducialC != null) {
				System.out.println("Found Fiducial C");
				fiducialPoints.put("FidC", fiducialC);
			}
			
			//Get Fiducial B
			fiducialB = getFiducialB(colourWeight);
			if (fiducialB != null) {
				System.out.println("Found Fiducial B");
				fiducialPoints.put("FidB", fiducialB);
			}
			
			//Get Fiducial D
			fiducialD = getFiducialD(colourWeight);
			if (fiducialD != null) {
				System.out.println("Found Fiducial D");
				fiducialPoints.put("FidD", fiducialD);
			}
			
			//Get nose points using alternate method - Couldn't get working
			//ArrayList<Point> nosePoints = getNose(colourWeight);
			
			//Get ear
			CascadeClassifier earDetector = new CascadeClassifier("resources/haarcascade_mcs_rightear.xml");
			MatOfRect earDetections = new MatOfRect();
			earDetector.detectMultiScale(image, earDetections);
			int noEars = earDetections.toArray().length;
			System.out.println("Detected " + noEars + " ears.");
			
			if (noEars > 0) {
				
				for (Rect r: earDetections.toArray()) {
					if (earBox == null) {
						earBox = r;
					} else if (r.width > earBox.width) { 
						earBox = r;
					}
				}
				centerEar = new Point(earBox.x + (earBox.width / 2), earBox.y + (earBox.height / 2));
				fiducialPoints.put("TopLeftEar", new Point(earBox.x, earBox.y));
				fiducialPoints.put("TopRightEar", new Point(earBox.x + earBox.width, earBox.y));
				fiducialPoints.put("BottomLeftEar", new Point(earBox.x, earBox.y + earBox.height));
				fiducialPoints.put("BottomRightEar", new Point(earBox.x + earBox.width, earBox.y + earBox.height));
			}
			
			
			//Get eye
			CascadeClassifier eyeDetector = new CascadeClassifier("resources/haarcascade_mcs_righteye.xml");
			MatOfRect eyeDetections = new MatOfRect();
			eyeDetector.detectMultiScale(image, eyeDetections);
			int noEyes = eyeDetections.toArray().length;
			System.out.println("Detected " + noEyes + " eyes.");
			
			if (noEyes > 0) {
				
				//Make sure the eye box is in the right area in relation to the facebox, otherwise it can't be an eye
				Rect upperFaceBox = new Rect(faceBox.x, faceBox.y + (faceBox.height / 4), (faceBox.width / 2), (faceBox.height / 4));
				Core.rectangle(image, new Point(upperFaceBox.x, upperFaceBox.y), new Point(upperFaceBox.x + upperFaceBox.width, upperFaceBox.y + upperFaceBox.height), new Scalar(255, 255, 155));
				for (Rect r: eyeDetections.toArray()) {
					//Core.rectangle(image, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 255));
					if (withinBounds(r, upperFaceBox)) {
						Core.rectangle(image, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(255, 0, 255));
						if (eyeBox == null) {
							eyeBox = r;
						} else if (r.width > eyeBox.width) { 
							eyeBox = r;
						}
					}
				}
				if (eyeBox != null) {
					centerEye = new Point(eyeBox.x + (eyeBox.width / 2), eyeBox.y + (eyeBox.height / 2));
					fiducialPoints.put("CenterEye", centerEye);
				}
				
			}
			
			//Debug points
			for (Point p: points) {
				Core.circle(image, p, 0, new Scalar(255, 0, 0));
			}
			
			//Draw boxes and circles
			int pointWidth = faceBox.width / 35;
			Core.circle(image, centerFace, 3, new Scalar(0, 255, 0));
			Core.rectangle(image, new Point(faceBox.x, faceBox.y), new Point(faceBox.x + faceBox.width, faceBox.y + faceBox.height), new Scalar(0, 255, 0));
			if (fiducialB != null) Core.circle(image, fiducialB, pointWidth, new Scalar(0, 0, 255));
			if (fiducialC != null) Core.circle(image, fiducialC, pointWidth, new Scalar(0, 0, 255));
			if (fiducialD != null) Core.circle(image, fiducialD, pointWidth, new Scalar(0, 0, 255));
			
			if (earBox!= null) Core.rectangle(image, new Point(earBox.x, earBox.y), new Point(earBox.x + earBox.width, earBox.y + earBox.height), new Scalar(0, 255, 0));
			if (centerEar != null) Core.circle(image, centerEar, pointWidth, new Scalar(0, 255, 0));
			
			if (eyeBox!= null) Core.rectangle(image, new Point(eyeBox.x, eyeBox.y), new Point(eyeBox.x + eyeBox.width, eyeBox.y + eyeBox.height), new Scalar(0, 255, 0));
			if (centerEye != null) Core.circle(image, centerEye, pointWidth, new Scalar(0, 255, 0));
			
//			for (Point p: nosePoints) {
//				Core.circle(image, p, pointWidth, new Scalar(0, 255, 0));
//			}
			
			//Write file to facedetects folder
			String filename = imageName.replace("images", "facedetects");
			Highgui.imwrite(filename, image);
			
			return fiducialPoints;
		}
		
		return null;
	}
	
	//Compare the colours of two points
	private boolean compare(double[] newPoint, double[] oldPoint, double weight) {
		
		if (newPoint == null || oldPoint == null) {
			return false;
		}
		
		int difference = 0;
		for (int i = 0; i < oldPoint.length; i++) {
			difference += Math.abs(oldPoint[i] - newPoint[i]);
		}
		
		if (difference / 3 < weight) {
			return true;
		}
		
		return false;
		
	}
	
	//Calculate the weight for comparing skin colour
	private double getSkinWeight(Point start, Rect rectangle) {
		
		//Loop in a spiral round the start point and add them to an ArrayList
		ArrayList<double[]> skinColours = new ArrayList<double[]>();
		boolean spiralBoolean = true;
		Point spiral = start.clone();
		for (int i = 0; i < rectangle.width; i++) {
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
		
		//Calculate the average colour
		double[] sumTotal = {0, 0, 0};
		for (double[] numberArray: skinColours) {
			for (int i = 0; i < numberArray.length; i++) {
				sumTotal[i] += numberArray[i];
			}
		}
		int size = skinColours.size();
		double[] average = {sumTotal[0] / size, sumTotal[1] / size, sumTotal[2] / size};
		
		//Set centerface to be the average
		Core.circle(image, start, 0, new Scalar(average[0], average[1], average[2]));
		
		//Calculate the variance
		double[] variance = {0, 0, 0};
		for (double[] numberArray: skinColours) {
			for (int i = 0; i < numberArray.length; i++) {
				variance[i] += (numberArray[i] - average[i]) * (numberArray[i] - average[i]);
			}
		}
		for (int i = 0; i < variance.length; i++) {
			variance[i] = variance[i] / size;
		}
		
		//Calculate the standard deviation
		double[] standardDeviation = {Math.sqrt(variance[0]), Math.sqrt(variance[1]), Math.sqrt(variance[2])};

		//Get a single number for the weight and return it
		double colourWeight = 0;
		for (int i = 0; i < variance.length; i++) {
			colourWeight += standardDeviation[i];
		}
		colourWeight = colourWeight / variance.length;
		return colourWeight;
	}
	
	//Algorithm for finding Fiducial B
	private Point getFiducialB(double colourWeight) {
		
		//If the algorithm encounters an exception, point can't be found
		try {
			
			//Start from Fiducial C
			Point finder = new Point(fiducialC.x, fiducialC.y);
			
			//Initialise a variable for keeping track of the pixels that denote the edge of the face
			ArrayList<double[]> upperFacePoints = new ArrayList<double[]>();
			
			//Initialise variable for keeping track of direction
			int direction = 0;
			
			//Algorithm that will trace the boundary of the face
			for (int i = 0; i < 10000; i++) {
				if (compare(image.get((int)finder.y, (int)finder.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
					if (direction == 0) {
						direction = 3;
					} else {
						direction--;
					}
					double[] coords = {finder.x, finder.y};
					upperFacePoints.add(coords);
					//points.add(finder.clone());
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
			
			//Initialise variable for a reasonably small interval size
			int intervalSize = faceBox.width / 10;
			
			//Initialise ArrayList for keeping track of regression lines
			ArrayList<SimpleRegression> regressions = new ArrayList<SimpleRegression>();
			
			//Algorithm for calculating the regression lines
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
//				Point start = new Point(array[0][0], sr.predict(array[0][0]));
//				Point end = new Point(array[array.length - 1][0], sr.predict(array[array.length - 1][0]));
//				Core.line(image, start, end, new Scalar(255, 0, 0));
			}
			
			//Using the regression lines to calculate the point of significant angle change
			//and therefore where Fiducial B is
			SimpleRegression lastSr = null;
			boolean found = false;
			finder = null;
			for (SimpleRegression sr: regressions) {
				if (sr.getSlope() > 0 && !found && lastSr != null) {
					found = true;
					RealMatrix coefficients = new Array2DRowRealMatrix(new double[][] { {lastSr.getSlope(), -1}, {sr.getSlope(), -1}}, false);
					DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
					RealVector constants = new ArrayRealVector(new double[] {-lastSr.getIntercept(), -sr.getIntercept()}, false);
					RealVector solution = solver.solve(constants);
					finder = new Point((int)solution.toArray()[0], (int)solution.toArray()[1]);
				}
				lastSr = sr;
			}
			
			return finder;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
	}
	
	//Algorithm for finding Fiducial C
	private Point getFiducialC(double colourWeight) {

		//If the algorithm encounters an exception, point can't be found
		try {
			
			//Start from the center of the face
			Point finder = centerFace.clone();
			
			//Checks to see if the edge of the picture is closer than
			//than the predetermined stopping point
			int endPoint = faceBox.x - (faceBox.width / 4);
			if (centerFace.x < centerFace.x - endPoint) {
				endPoint = 0;
			}
			
			//Start from the center, extend out in a triangle and find the furthest most skin colour point
			for (double i = centerFace.x; i >= endPoint; i--) {
				double yHalfHeight = (centerFace.x - i);
				double topY = centerFace.y - yHalfHeight;
				if (topY < 0) topY = 0;
				double bottomY = centerFace.y + yHalfHeight;
				if (bottomY > image.height() - 1) bottomY = image.height() - 1; 
				for (double j = topY; j <= bottomY; j++) {
					boolean currentPixel = compare(image.get((int)j, (int)i), image.get((int)centerFace.y, (int)centerFace.x), colourWeight);
					boolean rightPixel = compare(image.get((int)j, (int)(i+1)), image.get((int)centerFace.y, (int)centerFace.x), colourWeight);
					if (currentPixel && rightPixel) {
						//points.add(new Point(i, j));
						finder = new Point(i, j);
					}
				}
			}
			
			return finder;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
	}
	
	//Algorithm for finding Fiducial D
	private Point getFiducialD(double colourWeight) {
		
		//If the algorithm encounters an exception, point can't be found
		try {
			
			//Start from Fiducial C
			Point finder = new Point(fiducialC.x, fiducialC.y);
			
			//Initialise a variable for keeping track of the pixels that denote the edge of the face
			ArrayList<double[]> lowerFacePoints = new ArrayList<double[]>();
			
			//Initialise a variable for keeping track of direction
			int direction = 2;

			//Algorithm that will trace the boundary of the face
			for (int i = 0; i < 10000; i++) {
				if (!compare(image.get((int)finder.y, (int)finder.x), image.get((int)centerFace.y, (int)centerFace.x), colourWeight)) {
					if (direction == 0) {
						direction = 3;
					} else {
						direction--;
					}
					double[] coords = {finder.x, finder.y};
					lowerFacePoints.add(coords);
					//points.add(finder.clone());
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
			
			//Initialise variable for a reasonably small interval size
			int intervalSize = faceBox.width / 10;
			
			//Initialise ArrayList for keeping track of regression lines
			ArrayList<SimpleRegression> regressions = new ArrayList<SimpleRegression>();
			
			//Algorithm for calculating the regression lines
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
//				Point start = new Point(array[0][0], sr.predict(array[0][0]));
//				Point end = new Point(array[array.length - 1][0], sr.predict(array[array.length - 1][0]));
//				Core.line(image, start, end, new Scalar(255, 0, 0));
			}

			//Using the regression lines to calculate the point of significant angle change
			//and therefore where Fiducial B is
			SimpleRegression lastSr = null;
			boolean found = false;
			finder = null;
			for (SimpleRegression sr: regressions) {
				if (lastSr != null && lastSr.getSlope() - sr.getSlope() < 0 && !found) {
					found = true;
					RealMatrix coefficients = new Array2DRowRealMatrix(new double[][] { {lastSr.getSlope(), -1}, {sr.getSlope(), -1}}, false);
					DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
					RealVector constants = new ArrayRealVector(new double[] {-lastSr.getIntercept(), -sr.getIntercept()}, false);
					RealVector solution = solver.solve(constants);
					finder = new Point((int)solution.toArray()[0], (int)solution.toArray()[1]);
				}
				lastSr = sr;
			}
			
			return finder;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
	}
	
	//Alternate method for calculating Fiducial B, C and D, but couldn't make it work
	private ArrayList<Point> getNose(double colourWeight) {
		
		ArrayList<Point> borderPoints = new ArrayList<Point>();
		
		Point[][] pointGrid = new Point[image.width()][image.height()];
		int endPoint = faceBox.x - (faceBox.width / 4);
		if (centerFace.x < centerFace.x - endPoint) {
			endPoint = 0;
		}
		for (double i = centerFace.x; i >= endPoint; i--) {
			for (double j = faceBox.y; j <= faceBox.y + faceBox.height; j++) {
				boolean currentPixel = compare(image.get((int)j, (int)i), image.get((int)centerFace.y, (int)centerFace.x), colourWeight);
				if (currentPixel) {
					pointGrid[(int)i][(int)j] = new Point(i, j);
					//points.add(new Point(i, j));
				}
			}
		}
		
		for (int y = faceBox.y; y <= faceBox.y + faceBox.height; y++) {
			int minX = (int)centerFace.x;
			for (int x = (int)centerFace.x; x >= endPoint; x--) {
				if (pointGrid[x][y] != null) {
					minX = x;
				}
			}
			if (pointGrid[minX][y] != null) {
				borderPoints.add(pointGrid[minX][y]);
				//points.add(pointGrid[minX][y]);
			}
		}
		
		double previousX = -1;
		int currentTrend = 0;
		ArrayList<Point> nosePoints = new ArrayList<Point>();
		for (int i = 4; i < borderPoints.size(); i += 5) {
			Point p = borderPoints.get(i);
			points.add(p);
			double averageX = 0;
			for (int j = 4; j >= 0; j--) {
				averageX += borderPoints.get(i - j).x;
			}
			averageX = averageX / 5;
			if (previousX != -1) {
				if (currentTrend < 0) {
					if (averageX > previousX) {
						nosePoints.add(p);
					}
				} else if (currentTrend > 0) {
					if (averageX < previousX) {
						nosePoints.add(p);
					}
				}
				currentTrend = (int) (averageX - previousX);
			}
			previousX = averageX;
		}
		
		return nosePoints;
		
	}
	
	//Method for determing whether a rectangle collides with another
	private boolean withinBounds(Rect inner, Rect outer) {
		
		if (inner.x + inner.width < outer.x) {
			return false;
		}
		
		if (inner.x > outer.x + outer.width) {
			return false;
		}
		
		if (inner.y + inner.height < outer.y) { 
			return false;
		}
		
		if (inner.y > outer.y + outer.height) { 
			return false;
		}
		
		return true;
		
	}
}