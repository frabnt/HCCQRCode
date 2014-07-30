package test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import main.java.com.google.zxing.BarcodeFormat;
import main.java.com.google.zxing.EncodeHintType;
import main.java.com.google.zxing.common.BitVectorMatrix;
import main.java.com.google.zxing.hccqrcode.HCCQRcodeWriter;
import main.java.com.google.zxing.hccqrcode.decoder.ErrorCorrectionLevel;

public class MainHCCQRcodeGenerator {
	
public static void main (String agrs[]) {
		
		String myCodeText = "www.$%&#*§+546fenqrgfeiorgherioghrioefjoerinveirnverfsjdcomaoeicmoqermcermcrioecmkdlfmmekfmek9656e+46t4r6h4t84hty54h65456v65a1cer65456re465v4d654fver654v165er65g4re6554ytg65v16er54v5er45g4re561dcsewcwqfewfwefewfwef454651we1cer1g544f5e46r5www.$%&#*§+546fenqrgfeiooferhufheruiheiurhveruihvruiehhveurhveruifo3hrfh64er4fer64gver4654vdfv4dfv54v6erfwedfewfefwefwefw464oehhviue646454654vfdsvfdvdfvdfvdfv46546vfdvvvvvf87878erfd7vdf54v6dfvf6d4oerhugshvfhvisv4er+4v6f4dvfvdvdfv6546898799er7fved8v4ve8f4ve9r8v48df4vfevf4d464vfwww.$%&#*§+546fenqrgfeiorgherioghrioefjoerinveirnverfsjdcomaoeicmoqermcermcrioecmkdlfmmekfmek9656e+46t4r6h4t84hty54h65456v65a1cer65456re465v4d654fver654v165er65g4re6554ytg65v16er54v5er45g4re561dcsewcwqfewfwefewfwef454651we1cer1g544f5e46r5www.$%&#*§+546fenqrg" ;
		myCodeText += "veishvrueiafheiawhfweiuhfuiahsdcbjasbckjdbcsnnferonvraidnfskj565dc4s5ccdsc4968w4e6c";
		
		String prova = "ebfewauibfaisdbcasbdsjkvnkdsv46df45v46df4vfdfffffffdb3uierfrbneffjrennren8785rfrfreiofreihfreiowhveroicnednkcjncd564we5csdcds54csd65weacsd65a46a5c4d5s6edwncqrieoc489e4cdsvtrg45fncreuibceiwbe4dcs654c4sdcdcasddscsdcsanceoinacu465d4sdcsaccdscs5454cdcsacweuewaerucieroc5ecd5mweoiancndasjkcasdj456ds4csdcdsacasdcasnvowsdicusb<jckcjasihcshauchu4564sc5acs65ac4ascnduesahicuias<dchsdui<chisduchsdi6d8s4c5zndosnuicsndkjcsdc56snweicnuiweBCUISDCSKJCKJSDCNncwesdkjcnakjdncsdkjncdkjc65455464csodicskjcnxcjznferuiairuaebchjsdafbdfhjbvhdfjabfkjvbdfjbvkjadfvbkjdfbvdkfjvbdfkjbndwe9uCNSDIBJBDSncwiuebsdibcsdjbjcbjsdbcdskjbc 45645cdskncndkjcsd6 cdskcuiwaesbdbcshjbcsdj<bcksbckjsdbcjdksbkj465465dsmcsdckjcnkjsdbckjsdbcsdkjcbdskjcbdjskcbdjs46545sd4c";
		String a = "Hello World! Good morning!";
		
		String filePath = "C:/Users/utente/Desktop/testQRCode.png";
		int size = 200;
		String fileType = "png";
		File myFile = new File(filePath);
		
		try {
			Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
			hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
			HCCQRcodeWriter qrCodeWriter = new HCCQRcodeWriter();
			BitVectorMatrix bitVectorMatrix = qrCodeWriter.encode(a, BarcodeFormat.QR_CODE, size, size, hintMap);
			int width = bitVectorMatrix.getWidth();
			BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
			image.createGraphics();
			//rosso viene interpretato come colore scuro
			Color red = new Color (255, 0, 0);
			Color green = new Color (0, 255, 0);
			
			Graphics2D graphics = (Graphics2D) image.getGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, width);
			
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < width; j++) {
					boolean[] test = bitVectorMatrix.get(i, j);
					if(test[0] && test[1]) {
						graphics.setColor(Color.BLACK);
						graphics.fillRect(i, j, 1, 1);
					}
					else if (test[0] && !test[1]) {
						graphics.setColor(red);
						graphics.fillRect(i, j, 1, 1);
					}
					else if (!test[0] && test[1]) {
						graphics.setColor(green);
						graphics.fillRect(i, j, 1, 1);
					}
				}
			}
			
		    //System.out.println(bitMatrix.toString());
			
			ImageIO.write(image, fileType, myFile);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("You have succesfully created QR code");
		
	}


}
