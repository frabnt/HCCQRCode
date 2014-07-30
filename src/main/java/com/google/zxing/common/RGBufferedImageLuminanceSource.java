package main.java.com.google.zxing.common;

import java.lang.Object;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


//import javase.BufferedImageLuminanceSource;
import main.java.com.google.zxing.LuminanceSource;

public final class RGBufferedImageLuminanceSource extends LuminanceSource {
	
	

	
	private static final double MINUS_45_IN_RADIANS = -0.7853981633974483; // Math.toRadians(-45.0)

	private final BufferedImage image;
	private final int left;
	private final int top;
	
	//rbg components
	private static final int[] red = {255, 0, 0};
	private static final int[] green = {0, 255, 0};
	private static final int[] black = {0, 0, 0};
	private static final int[] white = {255, 255, 255};
	
	//palette
	private static int[][] palette = new int[4][3];

	public RGBufferedImageLuminanceSource(BufferedImage image) {
	    this(image, 0, 0, image.getWidth(), image.getHeight());
	}
	
	public BufferedImage getImage(){
		  return this.image;
    }
	
    public RGBufferedImageLuminanceSource(BufferedImage image, int left, int top, int width, int height) {
	    super(width, height);

	    if (image.getType() == BufferedImage.TYPE_INT_RGB) {
	      this.image = image;
	    } else {
	      int sourceWidth = image.getWidth();
	      int sourceHeight = image.getHeight();
	      if (left + width > sourceWidth || top + height > sourceHeight) {
	        throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
	      }

	      this.image = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);

	      WritableRaster raster = this.image.getRaster();
	      int[] buffer = new int[width*3];
	      
	      double[] distances = new double[4];
	      double minDistFromRed = Double.MAX_VALUE;
	      double minDistFromGreen = Double.MAX_VALUE;
	      double minDistFromBlack = Double.MAX_VALUE;
	      double minDistFromWhite = Double.MAX_VALUE;
	      
	      for (int y = top; y < top + height; y++) {
	        image.getRGB(left, y, width, 1, buffer, 0, sourceWidth);
	        for (int x = 0; x < width; x++) {
	        	
	        	int pixel = buffer[x];
	        	int[] a = new int[3];
	        	
	        	a[0] = ((pixel >> 16) & 0xFF); //red component
	        	a[1] = ((pixel >> 8) & 0xFF); //green component
	        	a[2] = ((pixel >> 0) & 0xFF); //blue component
	        	
	        	
	        	
	        	distances = euclideanDistance(a);
	        	
	        	if (minDistFromRed > distances[0]) {
	        		minDistFromRed = distances[0];
	        		palette[0] = a;
	        	}
	        	if (minDistFromGreen > distances[1]) {
	        		minDistFromGreen = distances[1];
	        		palette[1] = a;
	        	}
	        	if (minDistFromBlack > distances[2]) {
	        		minDistFromBlack = distances[2];
	        		palette[2] = a;
	        	}
	        	if (minDistFromWhite > distances[3]) {
	        		minDistFromWhite = distances[3];
	        		palette[3] = a;
	        	}
	        	
	        	//System.out.println("temp = ("+(a[0] & 0xFF)+","+(a[1] & 0xFF)+","+(a[2] & 0xFF)+")");
	        	raster.setPixel(x, y, a);  	
	        }
	      }
	      
      }
	  this.left = left;
	  this.top = top;
    }
    
    public double[] euclideanDistance (int[] rgbValues) {
    	double[] dist = new double[4];
    	
    	double a1 = (rgbValues[0]-red[0]);
    	double a2 = (rgbValues[1]-red[1]);
    	double a3 = (rgbValues[2]-red[2]);
    	
    	double redSum = a1*a1 + a2*a2 + a3*a3;
		//double distFromRed = Math.sqrt(redSum);
		dist[0] = redSum;
		
		double b1 = (rgbValues[0]-green[0]);
    	double b2 = (rgbValues[1]-green[1]);
    	double b3 = (rgbValues[2]-green[2]);
    	
		
		double greenSum = b1*b1 + b2*b2 +b3*b3;
		//double distFromGreen = Math.sqrt(greenSum);
		dist[1] = greenSum;
		
		double c1 = (rgbValues[0]-black[0]);
    	double c2 = (rgbValues[1]-black[1]);
    	double c3 = (rgbValues[2]-black[2]);
		
		double blackSum = c1*c1 + c2*c2 + c3*c3;
		//double distFromBlack = Math.sqrt(blackSum);
		dist[2] = blackSum;
		
		double d1 = (rgbValues[0]-white[0]);
    	double d2 = (rgbValues[1]-white[1]);
    	double d3 = (rgbValues[2]-white[2]);
		
		double whiteSum = d1*d1 + d2*d2 + d3*d3;
		//double distFromWhite = Math.sqrt(whiteSum);
		dist[3] = whiteSum;
		
		return dist;
    }
    
    @Override
    public int[][] getPalette() {
    	return palette;
    }
    
    @Override
    public byte[] getRow(int y, byte[] row) {
      if (y < 0 || y >= getHeight()) {
        throw new IllegalArgumentException("Requested row is outside the image: " + y);
      }
      int width = getWidth();
      System.out.println("width="+width+"; row.length="+row.length);
      if (row == null || row.length < width*3) {
        row = new byte[width*3];
      }
      int index = 0;
      int[] temp = new int[3];
      for (int i = 0; i < width; i++) {
    	  image.getRaster().getPixel(i, y, temp);
    	  for (int j = 0; j < 3; j++) {
    		  row[index] = (byte) temp[j];
    		  index++;
    	  }
      }
      //image.getRaster().getDataElements(left, top + y, width, 1, row);
      return row;
    }
    
    @Override
    public byte[][] getByteMatrix() {
      int width = getWidth();
      int height = getHeight();
      int area = width * height;
      byte[][] matrix = new byte[3][area];
      // The underlying raster of image consists of area bytes with three RGB values for each pixel
      //image.getRaster().getDataElements(left, top, width, height, matrix);
      
      int[] temp = new int[3];
      for (int y = top; y < top + height; y++) {
    	  for (int x = 0; x < width; x++) {
    		  image.getRaster().getPixel(x, y, temp);
    		  matrix[0][x+y*width] = (byte)temp[0];
    		  matrix[1][x+y*width] = (byte)temp[1];
    		  matrix[2][x+y*width] = (byte)temp[2];
    	  }
      }
      return matrix;
    }
    
    @Override
    public byte[] getMatrix() {
    	int width = getWidth();
        int height = getHeight();
        int area = width * height;
        byte[] matrix = new byte[area*3];
        // The underlying raster of image consists of area bytes with three RGB values for each pixel
        //image.getRaster().getDataElements(left, top, width, height, matrix);
        image.getRaster().getDataElements(left, top, width, height, matrix);
        return matrix;
    }
    
    @Override
    public boolean isCropSupported() {
      return true;
    }
    
    @Override
    public LuminanceSource crop(int left, int top, int width, int height) {
      return new RGBufferedImageLuminanceSource(image, this.left + left, this.top + top, width, height);
    }

    /**
     * This is always true, since the image is a gray-scale image.
     *
     * @return true
     */
    @Override
    public boolean isRotateSupported() {
      return true;
    }
    
    @Override
    public LuminanceSource rotateCounterClockwise() {
      int sourceWidth = image.getWidth();
      int sourceHeight = image.getHeight();

      // Rotate 90 degrees counterclockwise.
      AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);

      // Note width/height are flipped since we are rotating 90 degrees.
      BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, BufferedImage.TYPE_INT_RGB);

      // Draw the original image into rotated, via transformation
      Graphics2D g = rotatedImage.createGraphics();
      g.drawImage(image, transform, null);
      g.dispose();

      // Maintain the cropped region, but rotate it too.
      int width = getWidth();
      return new RGBufferedImageLuminanceSource(rotatedImage, top, sourceWidth - (left + width), getHeight(), width);
    }
    
    @Override
    public LuminanceSource rotateCounterClockwise45() {
      int width = getWidth();
      int height = getHeight();

      int oldCenterX = left + width / 2;
      int oldCenterY = top + height / 2;

      // Rotate 45 degrees counterclockwise.
      AffineTransform transform = AffineTransform.getRotateInstance(MINUS_45_IN_RADIANS, oldCenterX, oldCenterY);

      int sourceDimension = Math.max(image.getWidth(), image.getHeight());
      BufferedImage rotatedImage = new BufferedImage(sourceDimension, sourceDimension, BufferedImage.TYPE_INT_RGB);

      // Draw the original image into rotated, via transformation
      Graphics2D g = rotatedImage.createGraphics();
      g.drawImage(image, transform, null);
      g.dispose();

      int halfDimension = Math.max(width, height) / 2;
      int newLeft = Math.max(0, oldCenterX - halfDimension);
      int newTop = Math.max(0, oldCenterY - halfDimension);
      int newRight = Math.min(sourceDimension - 1, oldCenterX + halfDimension);
      int newBottom = Math.min(sourceDimension - 1, oldCenterY + halfDimension);

      return new RGBufferedImageLuminanceSource(rotatedImage, newLeft, newTop, newRight - newLeft, newBottom - newTop);
    }
}
