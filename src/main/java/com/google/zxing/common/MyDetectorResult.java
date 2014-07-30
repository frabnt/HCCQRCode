package main.java.com.google.zxing.common;

import main.java.com.google.zxing.ResultPoint;

public class MyDetectorResult {
	

	  private final BitVectorMatrix bits;
	  private final ResultPoint[] points;

	  public MyDetectorResult(BitVectorMatrix bits, ResultPoint[] points) {
	    this.bits = bits;
	    this.points = points;
	  }

	  public final BitVectorMatrix getBits() {
	    return bits;
	  }

	  public final ResultPoint[] getPoints() {
	    return points;
	  }

}
