package test;

import java.io.File;
import java.io.FileInputStream;

import javax.imageio.ImageIO;

import main.java.com.google.zxing.BinaryBitmap;
import main.java.com.google.zxing.MyBinaryBitmap;
import main.java.com.google.zxing.Result;
import javase.BufferedImageLuminanceSource;
import main.java.com.google.zxing.common.BitVectorMatrix;
import main.java.com.google.zxing.common.HybridBinarizer;
import main.java.com.google.zxing.common.SimpleHybridBinarizer;
import main.java.com.google.zxing.hccqrcode.HCCQRcodeReader;
import main.java.com.google.zxing.qrcode.QRCodeReader;

public class MainHCCQRCodeReader {
	
	public static void main (String args[]) throws Exception {
		Result result = null;
		MyBinaryBitmap binaryBitmap;
		
		String path = "C:/Users/utente/Desktop/test2.jpg";
		
		try {
			BufferedImageLuminanceSource image = new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(path)));
			ImageIO.write(image.getImage(), "png", new File("C:/Users/utente/Desktop/greyScaleImage.png"));
			/*byte[] row = new byte[image.getWidth()];
			image.getRow(335, row);
			for (int i = 0; i < row.length; i++) {
				System.out.println("row-element ("+i+") = "+row[i]);
			}*/
			SimpleHybridBinarizer hybrid = new SimpleHybridBinarizer(image);
			
			
			binaryBitmap = new MyBinaryBitmap(hybrid);
			HCCQRcodeReader reader = new HCCQRcodeReader();
	
			result = reader.decode(binaryBitmap);
			System.out.println("QRCode: " + result.getText());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
    //standard QRCode	
	/*public static void main (String args[]) throws Exception {
		Result result = null;
		BinaryBitmap binaryBitmap;
		
		String path = "C:/Users/utente/Desktop/QR-code-scanned-BW.png";
		
		try {
			BufferedImageLuminanceSource image = new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(path)));
			ImageIO.write(image.getImage(), "png", new File("C:/Users/utente/Desktop/greyScaleImage.png"));
			HybridBinarizer hybrid = new HybridBinarizer(image);
			binaryBitmap = new BinaryBitmap(hybrid);
			QRCodeReader reader = new QRCodeReader();
			result = reader.decode(binaryBitmap);
			System.out.println("QRCode: " + result.getText());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}*/

}
