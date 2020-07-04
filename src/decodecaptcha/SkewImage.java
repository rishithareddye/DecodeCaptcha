/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decodecaptcha;

/**
 *
 * @author lingg
 */
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class SkewImage {

    private static final int MAX_LETTER_COUNT = 6;
    private static final int LETTER_WIDTH = 33;
    private static final int IMAGE_HEIGHT = 40;
    private static final double SKEW = 0.0;
    private static final int DRAW_LINES = 2;
    private static final int DRAW_BOXES = 2;
    private static final int MAX_X = LETTER_WIDTH * MAX_LETTER_COUNT;
    private static final int MAX_Y = IMAGE_HEIGHT;
	/*
	*SkewImage.java function can create a perfect number based captcha but not alphanumeric hence I used a function isInteger 
	*/
    public static boolean isInteger(String s) { // takes the string and checks if the string is integers (used in below code)
        boolean isValidInteger = false;
        try
        {
           Integer.parseInt(s);
   
           // s is a valid integer
   
           isValidInteger = true;
        }
        catch (NumberFormatException ex)
        {
           // s is not an integer
        }
   
        return isValidInteger;
     }
  
    private static final Color [] RANDOM_BG_COLORS = {
        Color.RED, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK};

    private static final Color [] RANDOM_FG_COLORS = {Color.BLACK, Color.BLUE, Color.DARK_GRAY};

    
    public BufferedImage skewImage(String securityChars) {
        BufferedImage outImage = new BufferedImage(MAX_X, MAX_Y,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, MAX_X, MAX_Y);
        for (int i = 0; i < DRAW_BOXES; i++) {
            paindBoxes(g2d);
        }

        Font font = new Font("dialog", 1, 33);
        g2d.setFont(font);

        AffineTransform affineTransform = new AffineTransform();
        for (int i = 0; i < MAX_LETTER_COUNT; i++) {
            double angle = 0;
            if (Math.random() * 2 > 1) {
                angle = Math.random() * SKEW;
            } else {
                angle = Math.random() * -SKEW;
            }
            affineTransform.rotate(angle, (LETTER_WIDTH * i) + (LETTER_WIDTH / 2), MAX_Y / 2);
            g2d.setTransform(affineTransform);
            
            setRandomFont(g2d);
            setRandomFGColor(g2d);
            String image=securityChars.substring(i, i + 1);//store the substring in a variable
            int width=LETTER_WIDTH;
            g2d.drawString(image,
//                    (i * LETTER_WIDTH) + 3, 28 + (int) (Math.random() * 6));
            		(i * width) + 3, 31 + (int) (Math.random() * 0));

            affineTransform.rotate(-angle, (LETTER_WIDTH * i) + (LETTER_WIDTH / 2), MAX_Y / 2);
        }

        for (int i = 0; i < DRAW_LINES; i ++) {
            g2d.setXORMode(Color.RED);
            setRandomBGColor(g2d);
            g2d.setStroke(new BasicStroke(4));
            int y1 = (int) (Math.random() * MAX_Y);
            int y2 = (int) (Math.random() * MAX_Y);
            g2d.drawLine(0, y1, MAX_X, y2);
        }

        return outImage;
    }

    private void paindBoxes(Graphics2D g2d) {
        int colorId = (int) (Math.random() * RANDOM_BG_COLORS.length);
        g2d.setColor(RANDOM_BG_COLORS[colorId]);
        g2d.fillRect(getRandomX(), getRandomY(),
                getRandomX(), getRandomY());
    }

    private int getRandomX() {
        return (int) (Math.random() * MAX_X);
    }

    private int getRandomY() {
        return (int) (Math.random() * MAX_Y);
    }
    
    private void setRandomFont(Graphics2D g2d) {
        Font font = new Font("dialog", 1, 33);
        g2d.setFont(font);
    }
    
    private void setRandomFGColor(Graphics2D g2d) {
        int colorId = (int) (Math.random() * RANDOM_FG_COLORS.length);
        g2d.setColor(RANDOM_FG_COLORS[colorId]);
    }

    private void setRandomBGColor(Graphics2D g2d) {
        int colorId = (int) (Math.random() * RANDOM_BG_COLORS.length);
        g2d.setColor(RANDOM_BG_COLORS[colorId]);
    }
}
