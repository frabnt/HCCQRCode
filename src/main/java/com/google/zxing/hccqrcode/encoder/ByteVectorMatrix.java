package main.java.com.google.zxing.hccqrcode.encoder;

/**
 * The original code was a matrix of ints[], but since it only ever gets assigned
 * (-1,-1), (0,0), (1,0), (0,1) and (1,1), I'm going to use less memory and go with bytes[].
 * 
 * @author Francesco Benedetto
 *
 */

public final class ByteVectorMatrix {
	
	//instance and static fields
	private final byte[][][] bytes; //3D array of bytes
	private final int width;
	private final int height;
	
	
	public ByteVectorMatrix (int width, int height) {
		bytes = new byte[height][width][2];
		this.width = width;
		this.height = height;
	}
	
	public int getWidth () {
		return this.width;
	}
	
	public int getHeight () {
		return this.height;
	}
	
	public byte[] get (int x, int y) {
		return bytes[y][x];
	}
	
	//return a 3D array of bytes rappresenting the HCCQRCode
	public byte[][][] getArray () {
		return this.bytes;
	}
	
	//set a couple of booleans in a matrix cell: true means 1 and false means 0
	public void set (int x, int y, boolean value1, boolean value2) {
		byte[] bits = new byte[2];
		bits[0]=0;
		bits[1]=0;
		bytes[y][x] = bits;
		if (value1 && value2) {
			bits[0]=1;
			bits[1]=1;
			bytes[y][x] = bits;
		}
		else if (value1 && !value2) {
			bits[0] = 1;
			bits[1] = 0;
			bytes[y][x] = bits;
		}
		else if (!value1 && value2) {
			bits[0] = 0;
			bits[1] = 1;
			bytes[y][x] = bits;
		}
	}
	
	//set a couple of ints in a matrix cell: before to do it, they have to be converted in bytes
	public void set (int x, int y, int bit1, int bit2) {
		byte[] bits = new byte[2];
		bits[0] = (byte)bit1;
		bits[1] = (byte)bit2;
		bytes[y][x] = bits;
	}
	
	/*public void set (int x, int y, byte bit1, byte bit2) {
		byte[] bits = new byte[2];
		bits[0] = bit1;
		bits[1] = bit2;
		bytes[y][x] = bits;
	}*/
	
	public void set (int x, int y, int[] bits) {
		byte[] convert = new byte[2];
		convert[0] = (byte)bits[0];
		convert[1] = (byte)bits[1];
		bytes[y][x] = convert;
	}
	
	public void clear (byte value) {
		byte[] reset = new byte[2];
		reset[0] = value;
		reset[1] = value;
		
		for (int i = 0; i < this.height; i++) {
			for (int j = 0; j < this.width; j++) {
				bytes[i][j] = reset;
			}
		}
	}
	
	public String toString() {
	    StringBuilder result = new StringBuilder(2 * width * height + 2);
	    for (int y = 0; y < height; ++y) {
	      for (int x = 0; x < width; ++x) {
	    	  byte[] res = bytes[y][x];
	    			  if( (int)res[0] == 0 && (int)res[1] == 0)
	    				  result.append(" (0,0) ");
	    			  else if ((int)res[0] == 0 && (int)res[1] == 1)
	    				  result.append(" (0,1) ");
	    			  else if ((int)res[0] == 1 && (int)res[1] == 0)
	    				  result.append(" (1,0) ");
	    		      else if ((int)res[0] == 1 && (int)res[1] == 1)
	    		    	  result.append(" (1,1) ");
	    		      else
	    		    	  result.append("    ");
	      }
	      result.append("\n");
	    }
	    return result.toString();
	}
}
