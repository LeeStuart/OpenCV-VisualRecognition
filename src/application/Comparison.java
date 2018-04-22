package application;

import java.util.HashMap;

import org.opencv.core.Point;

/*
 * Class for comparison algorithm
 */
public class Comparison {
	
	//Variables for the fiducial points of both images
	private HashMap<String, Point> image1;
	private HashMap<String, Point> image2;
	
	//Constructor method
	public Comparison(HashMap<String, Point> image1, HashMap<String, Point> image2) {
		
		this.image1 = image1;
		this.image2 = image2;
		
	}
	
	//Main run method
	public void run() {
		
		//Comparing Ratio of Fiducial C to Fiducial B and Fiducial C to Fiducial D
		System.out.println("Fid C, Fid B, Fid D");
		compareRatio("FidC", "FidB", "FidD");
		
		System.out.println("Top Left Ear, Top Right Ear, Bottom Left Ear");
		compareRatio("TopLeftEar", "TopRightEar", "BottomLeftEar");
		
		System.out.println("Center Eye, Top Left Ear, Fid C");
		compareRatio("CenterEye", "TopLeftEar", "FidC");
		
	}
	
	//Calcualtes the ratio between the distance from middle to point A and middle to Point B
	private double compareRatio(String middlePoint, String pointA, String pointB) {
		
		Point M1 = image1.get(middlePoint);
		Point M2 = image2.get(middlePoint);
		Point A1 = image1.get(pointA);
		Point A2 = image2.get(pointA);
		Point B1 = image1.get(pointB);
		Point B2 = image2.get(pointB);
		if (M1 != null && M2 != null && A1 != null && A2 != null && B1 != null && B2 != null) {
			double distanceMA1 = calculateDistance(M1, A1);
			double distanceMB1 = calculateDistance(M1, B1);
			System.out.println("Image 1: " + distanceMA1 + ", " + distanceMB1);
			double distanceMA2 = calculateDistance(M2, A2);
			double distanceMB2 = calculateDistance(M2, B2);
			System.out.println("Image 2: " + distanceMA2 + ", " + distanceMB2);
			double ratio1 = distanceMA1 / distanceMB1;
			double ratio2 = distanceMA2 / distanceMB2;
			System.out.println("Ratio 1: " + ratio1);
			System.out.println("Ratio 2: " + ratio2);
			double difference = Math.abs(ratio1 - ratio2);
			System.out.println("Difference: " + difference);
			return difference;
		}
		
		return -1;
	}
	
	//Calculates the distance between two points
	private double calculateDistance(Point a, Point b) {
		
		double yDistance = b.y - a.y;
		double xDistance = b.x - a.x;
		
		double yDistanceSquared = yDistance * yDistance;
		double xDistanceSquared = xDistance * xDistance;
		
		double distanceSquared = yDistanceSquared + xDistanceSquared;
		
		double distance = Math.sqrt(distanceSquared);
		
		return distance;
		
	}

}
