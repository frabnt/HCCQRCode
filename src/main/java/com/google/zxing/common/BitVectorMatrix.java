package main.java.com.google.zxing.common;

public class BitVectorMatrix {
	
	private final int width;
	private final int height;
    private final int rowSize;
	private final int[] bits1;
	private final int[] bits2;
	
	public BitVectorMatrix(int width, int height) {
	    if (width < 1 || height < 1) {
	      throw new IllegalArgumentException("Both dimensions must be greater than 0");
	    }
	    
	    this.width = width;
	    this.height = height;
	    this.rowSize = (width + 31) / 32;

	    bits1 = new int[rowSize * height];
	    bits2 = new int[rowSize * height];
	}
	
	// A helper to construct a square matrix.
	public BitVectorMatrix(int dimension) {
	    this(dimension, dimension);
	}
	
	public boolean[] get(int x, int y) {
	   int offset = y * rowSize + (x / 32);
	   boolean[] result = new boolean[2];
	   result[0] = ((bits1[offset] >>> (x & 0x1f)) & 1) != 0;
	   result[1] = ((bits2[offset] >>> (x & 0x1f)) & 1) != 0;
	   return result;
	}
	
	/**
	* <p>Flips the given bit.</p>
	*
	* @param x The horizontal component (i.e. which column)
	* @param y The vertical component (i.e. which row)
	*/
	public void flip(int x, int y) {
	    int offset = y * rowSize + (x / 32);
	    bits1[offset] ^= 1 << (x & 0x1f);
	    bits2[offset] ^= 1 << (x & 0x1f);
	}
	
	/**
	   * <p>Sets a square region of the bit matrix to true.</p>
	   *
	   * @param left The horizontal position to begin at (inclusive)
	   * @param top The vertical position to begin at (inclusive)
	   * @param width The width of the region
	   * @param height The height of the region
	   */
	  public void setRegion(int left, int top, int width, int height, int destination) {
	    if (top < 0 || left < 0) {
	      throw new IllegalArgumentException("Left and top must be nonnegative");
	    }
	    if (height < 1 || width < 1) {
	      throw new IllegalArgumentException("Height and width must be at least 1");
	    }
	    int right = left + width;
	    int bottom = top + height;
	    if (bottom > this.height || right > this.width) {
	      throw new IllegalArgumentException("The region must fit inside the matrix");
	    }
	    for (int y = top; y < bottom; y++) {
	      int offset = y * rowSize;
	      for (int x = left; x < right; x++) {
	    	  if(destination == 0) {
	    		  bits1[offset + (x / 32)] |= 1 << (x & 0x1f);
	    	  }
	    	  else 
	    		  bits2[offset + (x / 32)] |= 1 << (x & 0x1f);
	      }
	    }
	  }
	  
	  public void set(int x, int y, int destination) {
		    int offset = y * rowSize + (x / 32);
		    if (destination == 0) {
		    	bits1[offset] |= 1 << (x & 0x1f);
		    }
		    else {
		    	bits2[offset] |= 1 << (x & 0x1f);
		    }
		    
	  }
	  
	  /**
	   * This is useful in detecting a corner of a 'pure' barcode.
	   *
	   * @return {@code x,y} coordinate of top-left-most 1 bit, or null if it is all white
	   */
	  public int[] getTopLeftOnBit() {
	    int bitsOffset = 0;
	    while (bitsOffset < bits1.length && bits1[bitsOffset] == 0) {
	      bitsOffset++;
	    }
	    if (bitsOffset == bits1.length) {
	      return null;
	    }
	    int y = bitsOffset / rowSize;
	    int x = (bitsOffset % rowSize) * 32;

	    int theBits = bits1[bitsOffset];
	    int bit = 0;
	    while ((theBits << (31-bit)) == 0) {
	      bit++;
	    }
	    x += bit;
	    return new int[] {x, y};
	  }

	  public int[] getBottomRightOnBit() {
	    int bitsOffset = bits1.length - 1;
	    while (bitsOffset >= 0 && bits1[bitsOffset] == 0) {
	      bitsOffset--;
	    }
	    if (bitsOffset < 0) {
	      return null;
	    }

	    int y = bitsOffset / rowSize;
	    int x = (bitsOffset % rowSize) * 32;

	    int theBits = bits1[bitsOffset];
	    int bit = 31;
	    while ((theBits >>> bit) == 0) {
	      bit--;
	    }
	    x += bit;

	    return new int[] {x, y};
	  }
	  
	  public int getWidth() {
		    return width;
	  }
	  
	  public int getHeight() {
		  return this.height;
	  }
	  
	  @Override
	  public String toString() {
	    StringBuilder result = new StringBuilder(height * (width + 1));
	    for (int y = 0; y < height; y++) {
	      for (int x = 0; x < width; x++) {
	    	  boolean[] temp = this.get(x, y);
			  String nero = "b";
			  String white = "w";
			  String red = "r";
			  String green = "g";
			  if (temp[0] && temp[1]) {
				  result.append(nero+";");
			  }
			  else if (temp[0] && !temp[1]) {
				  result.append(red+";");
			  }
			  else if (!temp[0] && temp[1]) {
				  result.append(green+";");
			  }
			  else 
				  result.append(white+";");
	        
	      }
	      result.append('\n');
	    }
	    return result.toString();
	  }

}
