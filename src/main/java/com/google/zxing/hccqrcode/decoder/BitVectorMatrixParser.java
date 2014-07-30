package main.java.com.google.zxing.hccqrcode.decoder;

import main.java.com.google.zxing.FormatException;
import main.java.com.google.zxing.common.BitVectorMatrix;

public class BitVectorMatrixParser {
	
	  private final BitVectorMatrix bitVectorMatrix;
	  private Version parsedVersion;
	  private FormatInformation parsedFormatInfo;
	  private boolean mirror;

	  /**
	   * @param bitMatrix {@link BitMatrix} to parse
	   * @throws FormatException if dimension is not >= 21 and 1 mod 4
	   */
	  BitVectorMatrixParser(BitVectorMatrix bitVectorMatrix) throws FormatException {
	    int dimension = bitVectorMatrix.getHeight();
	    if (dimension < 21 || (dimension & 0x03) != 1) {
	      throw FormatException.getFormatInstance();
	    }
	    this.bitVectorMatrix = bitVectorMatrix;
	  }

	  /**
	   * <p>Reads format information from one of its two locations within the QR Code.</p>
	   *
	   * @return {@link FormatInformation} encapsulating the QR Code's format info
	   * @throws FormatException if both format information locations cannot be parsed as
	   * the valid encoding of format information
	   */
	  FormatInformation readFormatInformation() throws FormatException {

	    if (parsedFormatInfo != null) {
	      return parsedFormatInfo;
	    }

	    // Read top-left format info bits
	    int formatInfoBits1 = 0;
	    for (int i = 0; i < 6; i++) {
	      formatInfoBits1 = copyBit(i, 8, formatInfoBits1);
	    }
	    // .. and skip a bit in the timing pattern ...
	    formatInfoBits1 = copyBit(7, 8, formatInfoBits1);
	    formatInfoBits1 = copyBit(8, 8, formatInfoBits1);
	    formatInfoBits1 = copyBit(8, 7, formatInfoBits1);
	    // .. and skip a bit in the timing pattern ...
	    for (int j = 5; j >= 0; j--) {
	      formatInfoBits1 = copyBit(8, j, formatInfoBits1);
	    }

	    // Read the top-right/bottom-left pattern too
	    int dimension = bitVectorMatrix.getHeight();
	    int formatInfoBits2 = 0;
	    int jMin = dimension - 7;
	    for (int j = dimension - 1; j >= jMin; j--) {
	      formatInfoBits2 = copyBit(8, j, formatInfoBits2);
	    }
	    for (int i = dimension - 8; i < dimension; i++) {
	      formatInfoBits2 = copyBit(i, 8, formatInfoBits2);
	    }

	    parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits1, formatInfoBits2);
	    if (parsedFormatInfo != null) {
	      return parsedFormatInfo;
	    }
	    throw FormatException.getFormatInstance();
	  }

	  /**
	   * <p>Reads version information from one of its two locations within the QR Code.</p>
	   *
	   * @return {@link Version} encapsulating the QR Code's version
	   * @throws FormatException if both version information locations cannot be parsed as
	   * the valid encoding of version information
	   */
	  Version readVersion() throws FormatException {

	    if (parsedVersion != null) {
	      return parsedVersion;
	    }

	    int dimension = bitVectorMatrix.getHeight();

	    int provisionalVersion = (dimension - 17) / 4;
	    if (provisionalVersion <= 6) {
	      return Version.getVersionForNumber(provisionalVersion);
	    }

	    // Read top-right version info: 3 wide by 6 tall
	    int versionBits = 0;
	    int ijMin = dimension - 11;
	    for (int j = 5; j >= 0; j--) {
	      for (int i = dimension - 9; i >= ijMin; i--) {
	        versionBits = copyBit(i, j, versionBits);
	      }
	    }

	    Version theParsedVersion = Version.decodeVersionInformation(versionBits);
	    if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
	      parsedVersion = theParsedVersion;
	      return theParsedVersion;
	    }

	    // Hmm, failed. Try bottom left: 6 wide by 3 tall
	    versionBits = 0;
	    for (int i = 5; i >= 0; i--) {
	      for (int j = dimension - 9; j >= ijMin; j--) {
	        versionBits = copyBit(i, j, versionBits);
	      }
	    }

	    theParsedVersion = Version.decodeVersionInformation(versionBits);
	    if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
	      parsedVersion = theParsedVersion;
	      return theParsedVersion;
	    }
	    throw FormatException.getFormatInstance();
	  }

	  private int copyBit(int i, int j, int versionBits) {
	    boolean bit = mirror ? bitVectorMatrix.get(j, i)[0] : bitVectorMatrix.get(i, j)[0];
	    return bit ? (versionBits << 1) | 0x1 : versionBits << 1;
	  }

	  /**
	   * <p>Reads the bits in the {@link BitMatrix} representing the finder pattern in the
	   * correct order in order to reconstruct the codewords bytes contained within the
	   * QR Code.</p>
	   *
	   * @return bytes encoded within the QR Code
	   * @throws FormatException if the exact number of bytes expected is not read
	   */
	  byte[] readCodewords() throws FormatException {

	    FormatInformation formatInfo = readFormatInformation();
	    Version version = readVersion();

	    // Get the data mask for the format used in this QR Code. This will exclude
	    // some bits from reading as we wind through the bit matrix.
	    DataMask dataMask = DataMask.forReference(formatInfo.getDataMask());
	    int dimension = bitVectorMatrix.getHeight();
	    //System.out.println(bitVectorMatrix.toString());
	    dataMask.unmaskBitMatrix(bitVectorMatrix, dimension);
	    //System.out.println(bitVectorMatrix.toString());
	    //System.out.println("dimension= "+dimension);

	    BitVectorMatrix functionPattern = version.buildFunctionPattern();
	    //System.out.println("dim FP="+functionPattern.getHeight());
        
	    boolean readingUp = true;
	    int totalCodewords = version.getTotalCodewords();
	    byte[] result = new byte[totalCodewords];
	    int resultOffset = 0;
	    int currentByte = 0;
	    int bitsRead = 0;
	    // Read columns in pairs, from right to left
	    for (int j = dimension - 1; j > 0; j -= 2) {
	      if (j == 6) {
	        // Skip whole column with vertical alignment pattern;
	        // saves time and makes the other code proceed more cleanly
	        j--;
	      }
	      // Read alternatingly from bottom to top then top to bottom
	      for (int count = 0; count < dimension; count++) {
	        int i = readingUp ? dimension - 1 - count : count;
	        for (int col = 0; col < 2; col++) {
	          // Ignore bits covered by the function pattern
	          if (!functionPattern.get(j - col, i)[0] && !functionPattern.get(j - col, i)[1]) {
	            // Read two bits
	        	bitsRead = bitsRead + 2;
	        	boolean temp1 = bitVectorMatrix.get(j - col, i)[0];
	            boolean temp2 = bitVectorMatrix.get(j - col, i)[1];
	            
	            currentByte <<= 1;
	            if (temp1) {
	              currentByte |= 1;
	            }
	            
	            currentByte <<= 1;
	            if (temp2) {
	            	currentByte |= 1;
	            }
	            
	            //System.out.println("("+temp1+","+temp2+")"+"; position: ("+(j-col)+","+i+")");
	            // If we've made a whole byte, save it off
	            if (bitsRead == 8 && resultOffset < totalCodewords) {
	              result[resultOffset++] = (byte) currentByte;  
	              //System.out.println("currentByte="+currentByte+"; resultOffset="+resultOffset);
	              bitsRead = 0;
	              currentByte = 0;
	            }
	          }
	          /*else
	        	  System.out.println("function pattern bits, position: "+(j-col)+","+i);*/
	        }
	      }
	      readingUp ^= true; // readingUp = !readingUp; // switch directions
	    }
	    if (resultOffset != version.getTotalCodewords()) {
	    	System.out.println("resultOffset: "+resultOffset+"; totalCodeWords: "+version.getTotalCodewords());
	    	System.out.println("format exception in BitVectorMatrixParser-readCodeWords");
	      throw FormatException.getFormatInstance();
	    }
	    return result;
	  }

	  /**
	   * Revert the mask removal done while reading the code words. The bit matrix should revert to its original state.
	   */
	  void remask() {
	    if (parsedFormatInfo == null) {
	      return; // We have no format information, and have no data mask
	    }
	    DataMask dataMask = DataMask.forReference(parsedFormatInfo.getDataMask());
	    int dimension = bitVectorMatrix.getHeight();
	    dataMask.unmaskBitMatrix(bitVectorMatrix, dimension);
	  }

	  /**
	   * Prepare the parser for a mirrored operation.
	   * This flag has effect only on the {@link #readFormatInformation()} and the
	   * {@link #readVersion()}. Before proceeding with {@link #readCodewords()} the
	   * {@link #mirror()} method should be called.
	   * 
	   * @param mirror Whether to read version and format information mirrored.
	   */
	  void setMirror(boolean mirror) {
	    parsedVersion = null;
	    parsedFormatInfo = null;
	    this.mirror = mirror;
	  }

	  /** Mirror the bit matrix in order to attempt a second reading. */
	  void mirror() {
	    for (int x = 0; x < bitVectorMatrix.getWidth(); x++) {
	      for (int y = x + 1; y < bitVectorMatrix.getHeight(); y++) {
	        if (bitVectorMatrix.get(x, y) != bitVectorMatrix.get(y, x)) {
	          bitVectorMatrix.flip(y, x);
	          bitVectorMatrix.flip(x, y);          
	        }
	      }
	    }
	  }

}
