package test;
import java.io.File;
import java.io.FileInputStream;

import javax.imageio.ImageIO;

import main.java.com.google.zxing.MyBinaryBitmap;
import main.java.com.google.zxing.Result;
import main.java.com.google.zxing.common.RGBHybridBinarizer;
import main.java.com.google.zxing.common.RGBufferedImageLuminanceSource;
import main.java.com.google.zxing.hccqrcode.HCCQRcodeReader;

public class RGBTest {

	public static void main(String[] args) {
		
		String path = "C:/Users/utente/Desktop/prova2.jpg";
		Result result = null;
		MyBinaryBitmap binaryBitmap;
		
		try {
			RGBufferedImageLuminanceSource image = new RGBufferedImageLuminanceSource(ImageIO.read(new FileInputStream(path)));
			ImageIO.write(image.getImage(), "png", new File("C:/Users/utente/Desktop/rgbImage.png"));
			/*byte[] row = new byte[image.getWidth()*3];
			image.getRow(385, row);
			for (int i = 0; i < row.length; i++) {
				System.out.println("row-element ("+i+") = "+row[i]);
			}*/
			
			int[][] palette = image.getPalette();
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 3; j++) {
					System.out.print(palette[i][j]+" ");
				}
				System.out.println();
			}
			
			RGBHybridBinarizer rgb = new RGBHybridBinarizer(image);
			
			binaryBitmap = new MyBinaryBitmap(rgb);
			HCCQRcodeReader reader = new HCCQRcodeReader();
	
			result = reader.decode(binaryBitmap);
			System.out.println("QRCode: " + result.getText());
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		

	}

}
