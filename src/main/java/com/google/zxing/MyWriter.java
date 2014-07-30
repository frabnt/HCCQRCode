package main.java.com.google.zxing;

import java.util.Map;

import main.java.com.google.zxing.common.BitVectorMatrix;

public interface MyWriter {
	
	BitVectorMatrix encode(String contents, BarcodeFormat format, int width, int height)
		      throws WriterException;

		  /**
		   * @param contents The contents to encode in the barcode
		   * @param format The barcode format to generate
		   * @param width The preferred width in pixels
		   * @param height The preferred height in pixels
		   * @param hints Additional parameters to supply to the encoder
		   * @return {@link BitMatrix} representing encoded barcode image
		   * @throws WriterException if contents cannot be encoded legally in a format
		   */
		  BitVectorMatrix encode(String contents,
		                   BarcodeFormat format,
		                   int width,
		                   int height,
		                   Map<EncodeHintType,?> hints)
		      throws WriterException;

}


