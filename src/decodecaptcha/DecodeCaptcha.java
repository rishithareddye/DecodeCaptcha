
package decodecaptcha;

/**
 *
 * @author Sai Harin Purumandla
 */
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;


public class DecodeCaptcha {
	
	private static BufferedImage[] templates = new BufferedImage[35]; // template size set to 35 (upper case alphabets + numbers)
	private static boolean debug = false;
	private static boolean show = true;
	private static boolean alpha = false; // flag to know that captcha contains alphabets or not
	public static void main(String[] args) throws Exception {
		System.out.println("Does the captcha contain alphabets (yes or no) :");
	Scanner sc=new Scanner(System.in);
	String in=sc.nextLine(); // taking imput (yes or no)
	if(in.toLowerCase().equals("yes") || in.toLowerCase().equals("y")) // checking if the input is yes
	{
		alpha=true; // if input is yes alpha flag is set to true
		for (int i=0; i<35; i++) { // for each possible character
			templates[i] = ImageIO.read(new File("template" + i+ ".gif")); // taking all the templates along with alphabets into consideration
		}

	}
	else
	{
		alpha=false;// if input is not yes alpha flag is set to true
		for (int i=0; i<10; i++) { // for each possible character
			templates[i] = ImageIO.read(new File("template" + i+ ".gif"));// taking all the templates along with out alphabets into consideration
		}
	}
		int numTests = 100; // total number of trials
		
		// read in the templates for each character
		for (int i=0; i<35; i++) { // for each possible character
			templates[i] = ImageIO.read(new File("template" + i+ ".gif"));
		}

		int correct = 0;
		for (int i=0; i<numTests; i++) {
			
			// generate a random string
			String rightAnswer = randomString(6,alpha);
			
			// make a captcha from it
			BufferedImage captchaImage = generateCaptcha(rightAnswer);
			
			if (show) {
				ImageIO.write(captchaImage, "jpeg", new File("see.jpg"));
				show = false;
			}
			
			// guess the captcha
			
			String guess = guessCaptcha(captchaImage);
			// see if we got it right
			if (guess.equals(rightAnswer)) correct++;
		}

		
		System.out.println("accuracy: " + (correct / (double) numTests));
	}
	
	
	public static String guessCaptcha(BufferedImage testImage) throws Exception {
		
		String guess = "";
		
		// convert the image from a jpeg to a gif
		BufferedImage testImageGif = jpegToGif(testImage);

		// cut the CAPTCHA image into subimages that hopefully contain one
		// character each
		// note: hardcoding the number of characters is not very robust
		//System.out.println(testImageGif.getWidth()+"-<>");
		int slice=numSlices(testImageGif.getWidth(),testImageGif.getHeight());
		if(slice==0)
		{
			System.out.println("Sorry This captcha is not compatible with thid Decoder");
			System.exit(0);
		}
		BufferedImage[] subImages = cutUpImage(testImageGif, slice); 
		
		// for each of these subimages, clean it up and then try to guess
		// the character
		int i=0;
		for (BufferedImage subImage: subImages) {
			++i;
			normalizeColor(subImage);
			filterNoise(subImage);
			String bestGuess = guessString(makeGuess(subImage));
			guess += "" + bestGuess;
			
		}
		
		return guess;
	}

	private static BufferedImage jpegToGif(BufferedImage image) {
		
		// this is pretty kludgy... we're just going to write the jpeg
		// out as a gif and read it back in
		
		BufferedImage gifImage = image;
		
		try {
			ImageIO.write(image, "gif", new File("temp.gif"));
			gifImage = ImageIO.read(new File("temp.gif"));
		} catch (Exception e) {e.printStackTrace();}
		
		return gifImage;
	}


	private static BufferedImage[] cutUpImage(BufferedImage image, int numSlices) {
		BufferedImage[] subimages = new BufferedImage[numSlices];

		int origHeight = image.getHeight();
		int origWidth = image.getWidth();
		int width = origWidth / numSlices;

		for (int i=0; i<numSlices; i++) {
			subimages[i] = image.getSubimage(i*width, 0, width, origHeight);
		}

		return subimages;
	}

	private static void normalizeColor(BufferedImage image) {
		
		// count the number of pixels of each color in the image
		HashMap<Integer, Integer> counts = colorHistogram(image);
		Integer[] a=sortmap(counts); // sorting the map
		Integer minFreq = 1000;
		for (Integer i: counts.keySet()) {
			if (counts.get(i) < minFreq) {
				minFreq = counts.get(i);
			}
		}
		/*
		*
		*Main logic to normalise the code
		* Assumption: all the colors which start from edges are the noise to the captcha or background.
		*/
		ArrayList<Integer> topValues = new ArrayList<>();
		for (Integer i: counts.keySet()) {
			topValues.add(i); // adding all the colors into the the array list topValues without any condition
		}
		Integer[] out=findEdgecolors(image); // findEdgecolors function returns the array of RGB values of colors which are at the edges of the picture
		for(int i=0;i<out.length;i++)
		{
			if(out[i]!=null)
				topValues.remove(out[i]); // remove the colours from topValues list if the color exist in the array returned by the findEdgecolors funciton (removing the colors which start from edges)
		}
		/*
		*Now topvalues consists of colors which are not in the edges of the clipped image
		*/
		int white_rgb = Color.YELLOW.getRGB();
		int black_rgb = Color.BLACK.getRGB();

		for (int x=0; x<image.getWidth(); x++) {
			for (int y=0; y<image.getHeight(); y++) {
				int pixelVal = image.getRGB(x, y);

				if (!topValues.contains(pixelVal)) {
					image.setRGB(x, y, white_rgb); //replacing the colors in topvalue with black
				} else {
					image.setRGB(x, y, black_rgb); // rest is colored with yellow (background)
				}
			}
		}
		
		if (debug) {
			try {
				ImageIO.write(image, "gif", new File("colorNormalized.gif"));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	private static void filterNoise(BufferedImage image) {
		
		// try to clean up the image by removing stray marks
		for (int x=0; x<image.getWidth(); x++) {
			for (int y=0; y<image.getHeight(); y++) {
				
				int pixelVal = image.getRGB(x, y);

				// check how many pixels in a 2 x 2 rectangle with this point 
				// in the center have the same color as this point; if not 
				// many, flip this pixel's color
				int startX = Math.max(x-2, 0);
				int startY = Math.max(y-2, 0);
				int endX = Math.min(x+2, image.getWidth()-1);
				int endY = Math.min(y+2, image.getHeight()-1);

				int matchCount = 0;
				int totalCount = 0;
				for (int i=startX; i<=endX; i++) {
					for (int j=startY; j<=endY; j++) {
						if (image.getRGB(i,j) == pixelVal) {
							matchCount++;
						}
						totalCount++;
					}
				}

				if ((matchCount / (double) totalCount) < .2) {
					if (pixelVal == Color.YELLOW.getRGB()) {
						image.setRGB(x, y, Color.BLACK.getRGB());
					} else {
						image.setRGB(x, y, Color.YELLOW.getRGB());
					}
				}
			}
		}
		
		if (debug) {
			try {
				ImageIO.write(image, "gif", new File("noiseFiltered.gif"));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	public static int makeGuess(BufferedImage subImage) {
		// check the degree of overlap between each character template and the image
		double bestOverlap = -1;
		int bestGuess = -1;

		for (int i=0; i<templates.length; i++) { // for each possible character

			int totalCount = 0;
			int matchCount = 0;

			for (int x=0; x<subImage.getWidth(); x++) {
				for (int y=0; y<subImage.getHeight(); y++) {

					int pixelVal = subImage.getRGB(x, y);

					if (!isBlack(pixelVal)) continue;
					if (isBlack(templates[i].getRGB(x, y))) matchCount++;
					totalCount++;
				}
			}

			if (debug) 
				System.out.println(i + ": matched " + matchCount + " / " + totalCount);

			double overlap = matchCount / (double) totalCount;
			if (overlap > bestOverlap) {
				bestOverlap = overlap;
				bestGuess = i;
			}
		}

		return bestGuess;
	}


	private static HashMap<Integer, Integer> colorHistogram(BufferedImage image) {
		HashMap<Integer, Integer> counts = new HashMap<>();

		for (int x=0; x<image.getWidth(); x++) {
			for (int y=0; y<image.getHeight(); y++) {
				int pixelVal = image.getRGB(x, y);

				if (!counts.containsKey(pixelVal)) {
					counts.put(pixelVal, 1);
				} else {
					counts.put(pixelVal, counts.get(pixelVal)+1);
				}
			}
		}

		if (debug) {
			for (Integer i: counts.keySet()) {
				System.out.println(i + ": " + counts.get(i));
			}
		}
		//System.out.println(image.getWidth()+"---"+image.getHeight());

		return counts;
	}


	private static boolean isBlack(int value) {
		return (16777216 - Math.abs(value)) / (double) 16777216 < .05;
	}	
	private static BufferedImage generateCaptcha(String answer) {
        SkewImage skewImage = new SkewImage();
        return skewImage.skewImage(answer);
	}
	/* 
	* added functions start from here
	*/
	public static int numSlices(int width,int height) // calculate the number of slices based on the skewimage.java class
	{
		int slice=0;
		if((height%40<2|| height%40>38) && (width%33<2 || width%33>31)){ // if height is 40 or a a near divisior of 40 and if width is 33 or near divisor of 33 
			slice=(int) (width/33); // slices =width/33
		}
		return (int)slice;
	}
		
	private static Integer[] sortmap(HashMap<Integer, Integer> hs) // takes color histogram as input and return the sorted colors values in an Integer array 
	{
		SortedSet<Integer> values = new TreeSet<Integer>(hs.values()); //convert to tressset (sorted list)
    
	return (Integer[]) values.toArray(new Integer[values.size()]); // convert tree to Integer array 
	}
	/*
	*findEdgecolors function is used to find the color values which are at the borders of the image
	*/
	public static Integer[] findEdgecolors(BufferedImage image)
	{
		// try to clean up the image by removing stray marks
				Integer[] colors=new Integer[10]; 
				int[] width={0,image.getWidth()-1}; // horizontal comparision array
				int[] heigth={0,image.getHeight()-1}; // vertical comparision array
				int i=0;
				for(int x=0;x<=1;x++){ 
					for (int y=0; y<image.getHeight(); y++) {
						
						int pixelVal = image.getRGB(width[x], y);//getting the edge color in RGB value
						if(!Arrays.asList(colors).contains(pixelVal)) // checking if the color already exists if yes do nothing if no add the color to array
						{
							colors[i]=pixelVal;
							i++;
						}
					}
				}
				 i=0;
					for(int x=0;x<image.getWidth();x++){
						for (int y=0; y<=1; y++) {
							
							int pixelVal = image.getRGB(x, width[y]);//getting the edge color in RGB value
							if(!Arrays.asList(colors).contains(pixelVal))// checking if the color already exists if yes do nothing if no add the color to array
							{
								colors[i]=pixelVal;
								i++;
							}
						}
					}
				
		return colors;
	}
	public static String randomString(int length,boolean flag) { // returns a random string to generate a captacha
		String alpha="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String num="0123456789";
		char[] characterSet;
		if(flag) // flag alpha is set both alphabets and numbers are considered.
		characterSet = (num+alpha).toCharArray();
		else // flag alpha is not set only numbers are considered.
		characterSet = (num).toCharArray();
	    Random random = new SecureRandom();
	    char[] result = new char[length];
	    for (int i = 0; i < result.length; i++) {
	        // picks a random index out of character set > random character
	        int randomCharIndex = random.nextInt(characterSet.length);
	        result[i] = characterSet[randomCharIndex]; //generates ramdomnumber
	    }
	    return new String(result);
	}
	// makeGuess function only generates integers and gueses integers. hence to know the exact string makeGuess returned guessString is used
		public static String guessString(int length) { // takes the integer output of makeGuess and returns the string it guessed
		char[] characterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	   if(length<0)
		   length=0;
	    return new String(""+characterSet[length]);
	}
}

