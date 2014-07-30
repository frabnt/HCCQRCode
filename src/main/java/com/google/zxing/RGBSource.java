package main.java.com.google.zxing;

public final class RGBSource extends LuminanceSource {
	
	private final byte[][] rgbValues;
	private final int dataWidth;
	private final int dataHeight;
	private final int left;
	private final int top;
	
	//rbg components
    private static final int[] red = {255, 0, 0};
    private static final int[] green = {0, 255, 0};
	private static final int[] black = {0, 0, 0};
	private static final int[] white = {255, 255, 255};
		
	//palette
	private static int[][] palette = new int[4][3];
	
	public RGBSource(int width, int height, int[] pixels) {
	    super(width, height);

	    dataWidth = width;
	    dataHeight = height;
	    left = 0;
	    top = 0;

	    // In order to measure pure decoding speed, we convert the entire image to a greyscale array
	    // up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
	    rgbValues = new byte[3][width * height];
	    
	    double[] distances = new double[4];
	    double minDistFromRed = Double.MAX_VALUE;
	    double minDistFromGreen = Double.MAX_VALUE;
	    double minDistFromBlack = Double.MAX_VALUE;
	    double minDistFromWhite = Double.MAX_VALUE;
	    
	    for (int y = 0; y < height; y++) {
	      int offset = y * width;
	      for (int x = 0; x < width; x++) {
	        int pixel = pixels[offset + x];
	        int r = (pixel >> 16) & 0xff;
	        int g = (pixel >> 8) & 0xff;
	        int b = pixel & 0xff;
	        
	        int[] a = new int[3];
	        a[0] = r;
	        a[1] = g;
	        a[2] = b;
	        
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
	        
	        rgbValues[0][offset + x] = (byte) r;
	        rgbValues[1][offset + x] = (byte) g;
	        rgbValues[2][offset + x] = (byte) b;
	      }
	    }
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
	
	private RGBSource(byte[][] pixels,
            int dataWidth,
            int dataHeight,
            int left,
            int top,
            int width,
            int height) {
		super(width, height);
		if (left + width > dataWidth || top + height > dataHeight) {
			throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
			}
		this.rgbValues = pixels;
		this.dataWidth = dataWidth;
		this.dataHeight = dataHeight;
		this.left = left;
		this.top = top;
    }
	
	@Override
	public byte[] getRow(int y, byte[] row) {
	    if (y < 0 || y >= getHeight()) {
	      throw new IllegalArgumentException("Requested row is outside the image: " + y);
	    }
	    int width = getWidth();
	    if (row == null || row.length < width) {
	      row = new byte[width*3];
	    }
	    int offset = (y + top) * dataWidth + left;
	    System.arraycopy(rgbValues, offset, row, 0, width);
	    return row;
	}
	
	@Override
	public byte[] getMatrix() {
		//TODO
		return null;
	}
	
	@Override
	public byte[][] getByteMatrix() {
		int width = getWidth();
	    int height = getHeight();

	    // If the caller asks for the entire underlying image, save the copy and give them the
	    // original data. The docs specifically warn that result.length must be ignored.
	    if (width == dataWidth && height == dataHeight) {
	      return rgbValues;
	    }
	    
	    int area = width * height;
	    byte[][] matrix = new byte[3][area];
	    int inputOffset = top * dataWidth + left;

	    // If the width matches the full width of the underlying data, perform a single copy.
	    if (width == dataWidth) {
	      System.arraycopy(rgbValues, inputOffset, matrix, 0, area);
	      return matrix;
	    }

	    // Otherwise copy one cropped row at a time.
	    byte[][] rgb = rgbValues;
	    for (int y = 0; y < height; y++) {
	      int outputOffset = y * width;
	      System.arraycopy(rgb, inputOffset, matrix, outputOffset, width);
	      inputOffset += dataWidth;
	    }
	    return matrix;
	}
	
	@Override
	  public boolean isCropSupported() {
	    return true;
	  }

	  @Override
	  public LuminanceSource crop(int left, int top, int width, int height) {
	    return new RGBSource(rgbValues,
	                         dataWidth,
	                         dataHeight,
	                         this.left + left,
	                         this.top + top,
	                         width,
	                         height);
	  }

}
