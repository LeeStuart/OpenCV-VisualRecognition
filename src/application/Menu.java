package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/*
 * Menu class, used so that user can easily select images for
 * comparison
 */
public class Menu {
	
	//ArrayList to store all image file names in folder
	private ArrayList<String> images;
	
	//Constructor method, initialises images ArrayList
	public Menu() {
		
		images = new ArrayList<String>();
		
	}
	
	//Method used to select 2 images for comparison
	public String[] load() {
		
		//Variables need to accept console input
		InputStreamReader cin = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(cin);
		
		//Print out text to make it look neater
		System.out.println("Side-Profile Visual Recognition");
		System.out.println("*******************************");
		System.out.println();
		
		//Load images and display options
		images = loadImages();
		for (int i = 0; i < images.size(); i++) {
			System.out.println(i + ":> " + images.get(i).replace("images\\", ""));
		}
		
		//Instructions, and setting default value in case something goes wrong
		System.out.println("Select 2 images to compare, or type 't' to identify all possible images");
		String userInput = "1 2";
		
		//Try reading line from input
		try {
			userInput = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Check if user wishes to identify all possible images
		if (userInput.equals("t")) {
			return null;
		}
		
		//Final data splitting to put into correct format.
		String[] inputs = userInput.split(" ");
		inputs[0] = images.get(Integer.parseInt(inputs[0]));
		inputs[1] = images.get(Integer.parseInt(inputs[1]));
		
		return inputs;
		
	}
	
	//Class to read all files in images folder, and return them in ArrayList
	public ArrayList<String> loadImages() {
		
		//Initialise ArrayList to store file names
		ArrayList<String> allimages = new ArrayList<String>();
		
		//Initialise variable for the images folder and get it's children
		File folder = new File("images");
		File[] fileList = folder.listFiles();
		
		//For each element in fileList, if element is a file, add it to the ArrayList 
		for (File f : fileList) {
			if (f.isFile()) {
				allimages.add(f.getPath());
			}
		}
		
		return allimages;
		
	}

}
