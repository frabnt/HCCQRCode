package main.java.com.google.zxing.common;

import main.java.com.google.zxing.LuminanceSource;
import main.java.com.google.zxing.NotFoundException;
import main.java.com.google.zxing.SimpleBinarizer;

public final class RGBHybridBinarizer extends SimpleGlobalHistogramBinarizer {
	
	private static final int BLOCK_SIZE_POWER = 3;
	private static final int BLOCK_SIZE = 1 << BLOCK_SIZE_POWER; // ...0100...00
    private static final int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;   // ...0011...11
    private static final int MINIMUM_DIMENSION = BLOCK_SIZE * 5;
	//private static final int MIN_DYNAMIC_RANGE = 24;
	//rbg components
	/*private static final int[] red = {255, 0, 0};
	private static final int[] green = {0, 255, 0};
	private static final int[] black = {0, 0, 0};
	private static final int[] white = {255, 255, 255};*/
	
	private BitVectorMatrix matrix;
	
	public RGBHybridBinarizer(LuminanceSource source) {
	    super(source);
	}
	  
	public BitVectorMatrix getMatrix () {
		  return matrix;
    }
	
	@Override
	  public SimpleBinarizer createBinarizer(LuminanceSource source) {
	    return new SimpleHybridBinarizer(source);
	}
	
	@Override
	  public BitVectorMatrix getBlackMatrix() throws NotFoundException {
	    if (matrix != null) {
	      return matrix;
	    }
	    LuminanceSource source = getLuminanceSource();
	    int[][] palette = source.getPalette();
	    int width = source.getWidth();
	    int height = source.getHeight();
	    if (width >= MINIMUM_DIMENSION && height >= MINIMUM_DIMENSION) {
	      byte[][] rgbValues = source.getByteMatrix();
	      int subWidth = width >> BLOCK_SIZE_POWER;
	      if ((width & BLOCK_SIZE_MASK) != 0) {
	        subWidth++;
	      }
	      int subHeight = height >> BLOCK_SIZE_POWER;
	      if ((height & BLOCK_SIZE_MASK) != 0) {
	        subHeight++;
	      }

	      BitVectorMatrix newMatrix = new BitVectorMatrix(width, height);
	      
	      calculateThresholdForBlock(rgbValues, subWidth, subHeight, width, height, newMatrix, palette);
	      matrix = newMatrix;
	      //System.out.println("matrix-toString\n"+matrix.toString());
	    } else {
	      // If the image is too small, fall back to the global histogram approach.
	        matrix = super.getBlackMatrix();
	    	System.out.println("Image is too small (see SimpleHybridBinarizer)");
	    }
	    return matrix;
	}
	
	private static void calculateThresholdForBlock(byte[][] rgbValues,
            int subWidth,
            int subHeight,
            int width,
            int height,
            BitVectorMatrix matrix, int[][] palette) {
		for (int y = 0; y < subHeight; y++) {
			int yoffset = y << BLOCK_SIZE_POWER;
			int maxYOffset = height - BLOCK_SIZE;
			if (yoffset > maxYOffset) {
				yoffset = maxYOffset;
				}
			for (int x = 0; x < subWidth; x++) {
				int xoffset = x << BLOCK_SIZE_POWER;
				int maxXOffset = width - BLOCK_SIZE;
				if (xoffset > maxXOffset) {
					xoffset = maxXOffset;
					}
				thresholdBlock(rgbValues, xoffset, yoffset, width, matrix, palette);
			}
		 }
	}
	
	private static void thresholdBlock(byte[][] rgbValues,
            int xoffset,
            int yoffset,
            int stride,
            BitVectorMatrix matrix, int[][] palette) {
		
		for (int y = 0, offset = yoffset * stride + xoffset; y < BLOCK_SIZE; y++, offset += stride) {
			for (int x = 0; x < BLOCK_SIZE; x++) {
				int[] temp = {(rgbValues[0][offset + x] & 0xFF), (rgbValues[1][offset + x] & 0xFF), (rgbValues[2][offset + x] & 0xFF)};
				//System.out.println("temp-0="+temp[0]+"; temp-1="+temp[1]+"; temp-2="+temp[2]);
				
				
				/*double redSum = Math.pow((temp[0]-red[0]), 2) + Math.pow((temp[1]-red[1]), 2) + Math.pow((temp[2]-red[2]), 2);
				double distFromRed = Math.sqrt(redSum);
				
				double greenSum = Math.pow((temp[0]-green[0]), 2) + Math.pow((temp[1]-green[1]), 2) + Math.pow((temp[2]-green[2]), 2);
				double distFromGreen = Math.sqrt(greenSum);
				
				double blackSum = Math.pow((temp[0]-black[0]), 2) + Math.pow((temp[1]-black[1]), 2) + Math.pow((temp[2]-black[2]), 2);
				double distFromBlack = Math.sqrt(blackSum);
				
				double whiteSum = Math.pow((temp[0]-white[0]), 2) + Math.pow((temp[1]-white[1]), 2) + Math.pow((temp[2]-white[2]), 2);
				double distFromWhite = Math.sqrt(whiteSum);
				*/
				
				double a1 = (temp[0]-palette[0][0]);
		    	double a2 = (temp[1]-palette[0][1]);
		    	double a3 = (temp[2]-palette[0][2]);
				
				double redSum = a1*a1 + a2*a2 + a3*a3;
				//double distFromRed = Math.sqrt(redSum);
				
				a1 = (temp[0]-palette[1][0]);
		    	a2 = (temp[1]-palette[1][1]);
		    	a3 = (temp[2]-palette[1][2]);
				
				double greenSum = a1*a1 + a2*a2 + a3*a3;
				//double distFromGreen = Math.sqrt(greenSum);
				
				a1 = (temp[0]-palette[2][0]);
		    	a2 = (temp[1]-palette[2][1]);
		    	a3 = (temp[2]-palette[2][2]);
				
				double blackSum = a1*a1 + a2*a2 + a3*a3;
				//double distFromBlack = Math.sqrt(blackSum);
				
				a1 = (temp[0]-palette[3][0]);
		    	a2 = (temp[1]-palette[3][1]);
		    	a3 = (temp[2]-palette[3][2]);
				
				double whiteSum = a1*a1 + a2*a2 + a3*a3;
				//double distFromWhite = Math.sqrt(whiteSum);
				
				double min = redSum;
				//System.out.println("dist-red = "+distFromRed);
				if (min > greenSum) {
					min = greenSum;
					//System.out.println("dist-green = "+distFromGreen);
				}
				if (min > blackSum) {
					//System.out.println("dist-black = "+distFromBlack);
					min = blackSum;
				}
				if (min > whiteSum) {
					min = whiteSum;
					//System.out.println("dist-white = "+distFromWhite);
				}
				//System.out.println("min="+min);
				
				
				
				if (min == blackSum) {
					  //System.out.println("black");
			          matrix.set(xoffset + x, yoffset + y, 0);
			          matrix.set(xoffset + x, yoffset + y, 1);
			    }
			    else if (min == redSum) {
			    	    //System.out.println("red");
			        	matrix.set(xoffset + x, yoffset + y, 0);
			    }
			    else if (min == greenSum) {
			    	    //System.out.println("green");
			        	matrix.set(xoffset + x, yoffset + y, 1);
			    }
			}
		}
		//matrix.toString();
	}
	
}
