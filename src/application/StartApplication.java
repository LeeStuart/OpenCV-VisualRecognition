package application;

import java.util.ArrayList;
import java.util.HashMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

/*
 * Main class to start application. Will load up the DetectFace 
 * and Comparison classes and run them based on user input
 * received from the Menu class.
 */
public class StartApplication {
	
	public static void main(String args[]) {
		
		//Required line for OpenCV to run.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//Load menu and get selected images
		Menu menu = new Menu();
		String[] images = menu.load();
		
		//If images is null, then user selected to identify 
		//all faces instead of run the comparison algorithm
		if (images != null) {
			
			//Hash Maps to store Fiducial Points of images
			HashMap<String, Point> image1;
			HashMap<String, Point> image2;
			
			//Read images from file into array
			Mat[] imageMats = new Mat[images.length];
			for (int i = 0; i < images.length; i++) {
				imageMats[i] = Highgui.imread(images[i]);
			}
			
			//Run the DetectFace class on both images
			DetectFace df = new DetectFace();
			image1 = df.run(imageMats[0], images[0]);
			df = new DetectFace();
			image2 = df.run(imageMats[1], images[1]);
			
			//Run Comparison class
			System.out.println("Compare");
			Comparison c = new Comparison(image1, image2);
			c.run();
			
		} else {
			
			//Get filenames for all images in images folder
			ArrayList<String> allimages = menu.loadImages();
			
			//Loop through each file and run the DetectFace class on it
			for (String s: allimages) {
				DetectFace df = new DetectFace();
				Mat image = Highgui.imread(s);
				System.out.println(s);
				df.run(image, s);
			}
			
		}
		
	}

}
