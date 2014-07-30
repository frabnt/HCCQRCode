package main.java.com.google.zxing.hccqrcode;

import java.util.List;
import java.util.Map;

import main.java.com.google.zxing.BarcodeFormat;
import main.java.com.google.zxing.MyBinaryBitmap;
import main.java.com.google.zxing.ChecksumException;
import main.java.com.google.zxing.DecodeHintType;
import main.java.com.google.zxing.FormatException;
import main.java.com.google.zxing.NotFoundException;
import main.java.com.google.zxing.MyReader;
import main.java.com.google.zxing.Result;
import main.java.com.google.zxing.ResultMetadataType;
import main.java.com.google.zxing.ResultPoint;
import main.java.com.google.zxing.common.BitMatrix;
import main.java.com.google.zxing.common.BitVectorMatrix;
import main.java.com.google.zxing.common.DecoderResult;
import main.java.com.google.zxing.common.MyDetectorResult;
import main.java.com.google.zxing.hccqrcode.decoder.Decoder;
import main.java.com.google.zxing.qrcode.decoder.QRCodeDecoderMetaData;
import main.java.com.google.zxing.qrcode.detector.MyDetector;

public class HCCQRcodeReader implements MyReader {
	
	  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

	  private final Decoder decoder = new Decoder();

	  protected final Decoder getDecoder() {
	    return decoder;
	  }
	  
	  /**
	   * Locates and decodes a QR code in an image.
	   *
	   * @return a String representing the content encoded by the QR code
	   * @throws NotFoundException if a QR code cannot be found
	   * @throws FormatException if a QR code cannot be decoded
	   * @throws ChecksumException if error correction fails
	   */
	  @Override
	  public Result decode(MyBinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
	    return decode(image, null);
	  }

	  @Override
	  public final Result decode(MyBinaryBitmap image, Map<DecodeHintType,?> hints)
	      throws NotFoundException, ChecksumException, FormatException {
		//System.out.println("provaDecode");
	    DecoderResult decoderResult;
	    ResultPoint[] points;
	    if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
	      BitVectorMatrix bits = extractPureBits(image.getBlackMatrix());
	      
	      decoderResult = decoder.decode(bits, hints);
	      points = NO_POINTS;
	    } else {
	      MyDetectorResult detectorResult = new MyDetector(image.getBlackMatrix()).detect(hints);
	      //System.out.println(image.getBlackMatrix().toString());
	      decoderResult = decoder.decode(detectorResult.getBits(), hints);
	      points = detectorResult.getPoints();
	    }

	    // If the code was mirrored: swap the bottom-left and the top-right points.
	    if (decoderResult.getOther() instanceof QRCodeDecoderMetaData) {
	      ((QRCodeDecoderMetaData) decoderResult.getOther()).applyMirroredCorrection(points);
	    }

	    Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
	    List<byte[]> byteSegments = decoderResult.getByteSegments();
	    if (byteSegments != null) {
	      result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
	    }
	    String ecLevel = decoderResult.getECLevel();
	    if (ecLevel != null) {
	      result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
	    }
	    if (decoderResult.hasStructuredAppend()) {
	      result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE,
	                         decoderResult.getStructuredAppendSequenceNumber());
	      result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_PARITY,
	                         decoderResult.getStructuredAppendParity());
	    }
	    return result;
	  }

	  @Override
	  public void reset() {
	    // do nothing
	  }
	  
	  /**
	   * This method detects a code in a "pure" image -- that is, pure monochrome image
	   * which contains only an unrotated, unskewed, image of a code, with some white border
	   * around it. This is a specialized method that works exceptionally fast in this special
	   * case.
	   *
	   * @see com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(BitMatrix)
	   */
	  private static BitVectorMatrix extractPureBits(BitVectorMatrix image) throws NotFoundException {

	    int[] leftTopBlack = image.getTopLeftOnBit();
	    int[] rightBottomBlack = image.getBottomRightOnBit();
	    if (leftTopBlack == null || rightBottomBlack == null) {
	      throw NotFoundException.getNotFoundInstance();
	    }

	    float moduleSize = moduleSize(leftTopBlack, image);

	    int top = leftTopBlack[1];
	    int bottom = rightBottomBlack[1];
	    int left = leftTopBlack[0];
	    int right = rightBottomBlack[0];
	    
	    // Sanity check!
	    if (left >= right || top >= bottom) {
	      throw NotFoundException.getNotFoundInstance();
	    }

	    if (bottom - top != right - left) {
	      // Special case, where bottom-right module wasn't black so we found something else in the last row
	      // Assume it's a square, so use height as the width
	      right = left + (bottom - top);
	    }

	    int matrixWidth = Math.round((right - left + 1) / moduleSize);
	    int matrixHeight = Math.round((bottom - top + 1) / moduleSize);
	    if (matrixWidth <= 0 || matrixHeight <= 0) {
	      throw NotFoundException.getNotFoundInstance();
	    }
	    if (matrixHeight != matrixWidth) {
	      // Only possibly decode square regions
	      throw NotFoundException.getNotFoundInstance();
	    }

	    // Push in the "border" by half the module width so that we start
	    // sampling in the middle of the module. Just in case the image is a
	    // little off, this will help recover.
	    int nudge = (int) (moduleSize / 2.0f);
	    top += nudge;
	    left += nudge;
	    
	    // But careful that this does not sample off the edge
	    int nudgedTooFarRight = left + (int) ((matrixWidth - 1) * moduleSize) - (right - 1);
	    if (nudgedTooFarRight > 0) {
	      if (nudgedTooFarRight > nudge) {
	        // Neither way fits; abort
	        throw NotFoundException.getNotFoundInstance();
	      }
	      left -= nudgedTooFarRight;
	    }
	    int nudgedTooFarDown = top + (int) ((matrixHeight - 1) * moduleSize) - (bottom - 1);
	    if (nudgedTooFarDown > 0) {
	      if (nudgedTooFarDown > nudge) {
	        // Neither way fits; abort
	        throw NotFoundException.getNotFoundInstance();
	      }
	      top -= nudgedTooFarDown;
	    }

	    // Now just read off the bits
	    BitVectorMatrix bits = new BitVectorMatrix(matrixWidth, matrixHeight);
	    for (int y = 0; y < matrixHeight; y++) {
	      int iOffset = top + (int) (y * moduleSize);
	      for (int x = 0; x < matrixWidth; x++) {
	        if (image.get(left + (int) (x * moduleSize), iOffset)[0] && image.get(left + (int) (x * moduleSize), iOffset)[1]) {
	          bits.set(x, y, 0);
	          bits.set(x, y, 1);
	        }
	        else if (image.get(left + (int) (x * moduleSize), iOffset)[0] && !image.get(left + (int) (x * moduleSize), iOffset)[1])
	        	bits.set(x, y, 0);
	        else  if (!image.get(left + (int) (x * moduleSize), iOffset)[0] && image.get(left + (int) (x * moduleSize), iOffset)[1])
	        	bits.set(x, y, 1);
	      }
	    }
	    return bits;
	  }
	  
	  private static float moduleSize(int[] leftTopBlack, BitVectorMatrix image) throws NotFoundException {
		    int height = image.getHeight();
		    int width = image.getWidth();
		    int x = leftTopBlack[0];
		    int y = leftTopBlack[1];
		    boolean inBlack = true;
		    int transitions = 0;
		    while (x < width && y < height) {
		      if (inBlack != image.get(x, y)[0]) {
		        if (++transitions == 5) {
		          break;
		        }
		        inBlack = !inBlack;
		      }
		      x++;
		      y++;
		    }
		    if (x == width || y == height) {
		      throw NotFoundException.getNotFoundInstance();
		    }
		    return (x - leftTopBlack[0]) / 7.0f;
	  }


}
