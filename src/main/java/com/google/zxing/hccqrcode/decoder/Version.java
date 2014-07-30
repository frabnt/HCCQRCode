/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main.java.com.google.zxing.hccqrcode.decoder;

import main.java.com.google.zxing.FormatException;
import main.java.com.google.zxing.common.BitVectorMatrix;
import main.java.com.google.zxing.hccqrcode.decoder.ErrorCorrectionLevel;

/**
 * See ISO 18004:2006 Annex D
 *
 * @author Sean Owen
 */
public final class Version {

  /**
   * See ISO 18004:2006 Annex D.
   * Element i represents the raw version bits that specify version i + 7
   */
  private static final int[] VERSION_DECODE_INFO = {
      0x07C94, 0x085BC, 0x09A99, 0x0A4D3, 0x0BBF6,
      0x0C762, 0x0D847, 0x0E60D, 0x0F928, 0x10B78,
      0x1145D, 0x12A17, 0x13532, 0x149A6, 0x15683,
      0x168C9, 0x177EC, 0x18EC4, 0x191E1, 0x1AFAB,
      0x1B08E, 0x1CC1A, 0x1D33F, 0x1ED75, 0x1F250,
      0x209D5, 0x216F0, 0x228BA, 0x2379F, 0x24B0B,
      0x2542E, 0x26A64, 0x27541, 0x28C69
  };

  private static final Version[] VERSIONS = buildVersions();

  private final int versionNumber;
  private final int[] alignmentPatternCenters;
  private final ECBlocks[] ecBlocks;
  private final int totalCodewords;

  private Version(int versionNumber,
                  int[] alignmentPatternCenters,
                  ECBlocks... ecBlocks) {
    this.versionNumber = versionNumber;
    this.alignmentPatternCenters = alignmentPatternCenters;
    this.ecBlocks = ecBlocks;
    int total = 0;
    int ecCodewords = ecBlocks[0].getECCodewordsPerBlock();
    ECB[] ecbArray = ecBlocks[0].getECBlocks();
    for (ECB ecBlock : ecbArray) {
    	//System.out.println("getDataCodewords="+ecBlock.getDataCodewords()+"; ecCodewords="+ecCodewords);
      total += ecBlock.getCount() * (ecBlock.getDataCodewords() + ecCodewords);
      //System.out.println("total="+total);
    }
    this.totalCodewords = total; //each module is able to store exactly BpM bits,
                                   //so each old totalCodeWords values have to be multiplied for 2 (a BpM)
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public int[] getAlignmentPatternCenters() {
    return alignmentPatternCenters;
  }

  public int getTotalCodewords() {
    return totalCodewords;
  }

  public int getDimensionForVersion() {
    return 17 + 4 * versionNumber;
  }

  public ECBlocks getECBlocksForLevel(ErrorCorrectionLevel ecLevel) {
    return ecBlocks[ecLevel.ordinal()];
  }

  /**
   * <p>Deduces version information purely from QR Code dimensions.</p>
   *
   * @param dimension dimension in modules
   * @return Version for a QR Code of that dimension
   * @throws FormatException if dimension is not 1 mod 4
   */
  public static Version getProvisionalVersionForDimension(int dimension) throws FormatException {
    if (dimension % 4 != 1) {
      throw FormatException.getFormatInstance();
    }
    try {
      return getVersionForNumber((dimension - 17) / 4);
    } catch (IllegalArgumentException ignored) {
      throw FormatException.getFormatInstance();
    }
  }

  public static Version getVersionForNumber(int versionNumber) {
    if (versionNumber < 1 || versionNumber > 40) {
      throw new IllegalArgumentException();
    }
    return VERSIONS[versionNumber - 1];
  }

  static Version decodeVersionInformation(int versionBits) {
    int bestDifference = Integer.MAX_VALUE;
    int bestVersion = 0;
    for (int i = 0; i < VERSION_DECODE_INFO.length; i++) {
      int targetVersion = VERSION_DECODE_INFO[i];
      // Do the version info bits match exactly? done.
      if (targetVersion == versionBits) {
        return getVersionForNumber(i + 7);
      }
      // Otherwise see if this is the closest to a real version info bit string
      // we have seen so far
      int bitsDifference = FormatInformation.numBitsDiffering(versionBits, targetVersion);
      if (bitsDifference < bestDifference) {
        bestVersion = i + 7;
        bestDifference = bitsDifference;
      }
    }
    // We can tolerate up to 3 bits of error since no two version info codewords will
    // differ in less than 8 bits.
    if (bestDifference <= 3) {
      return getVersionForNumber(bestVersion);
    }
    // If we didn't find a close enough match, fail
    return null;
  }

  /**
   * See ISO 18004:2006 Annex E
   */
  BitVectorMatrix buildFunctionPattern() {
    int dimension = getDimensionForVersion();
    BitVectorMatrix bitVectorMatrix = new BitVectorMatrix(dimension);

    // Top left finder pattern + separator + format
    bitVectorMatrix.setRegion(0, 0, 9, 9, 0);
    bitVectorMatrix.setRegion(0, 0, 9, 9, 1);
    // Top right finder pattern + separator + format
    bitVectorMatrix.setRegion(dimension - 8, 0, 8, 9, 0);
    bitVectorMatrix.setRegion(dimension - 8, 0, 8, 9, 1);
    // Bottom left finder pattern + separator + format
    bitVectorMatrix.setRegion(0, dimension - 8, 9, 8, 0);
    bitVectorMatrix.setRegion(0, dimension - 8, 9, 8, 1);

    // Alignment patterns
    int max = alignmentPatternCenters.length;
    for (int x = 0; x < max; x++) {
      int i = alignmentPatternCenters[x] - 2;
      for (int y = 0; y < max; y++) {
        if ((x == 0 && (y == 0 || y == max - 1)) || (x == max - 1 && y == 0)) {
          // No alignment patterns near the three finder paterns
          continue;
        }
        bitVectorMatrix.setRegion(alignmentPatternCenters[y] - 2, i, 5, 5, 0);
        bitVectorMatrix.setRegion(alignmentPatternCenters[y] - 2, i, 5, 5, 1);
      }
    }

    // Vertical timing pattern
    bitVectorMatrix.setRegion(6, 9, 1, dimension - 17, 0);
    bitVectorMatrix.setRegion(6, 9, 1, dimension - 17, 1);
    // Horizontal timing pattern
    bitVectorMatrix.setRegion(9, 6, dimension - 17, 1, 0);
    bitVectorMatrix.setRegion(9, 6, dimension - 17, 1, 1);

    if (versionNumber > 6) {
      // Version info, top right
      bitVectorMatrix.setRegion(dimension - 11, 0, 3, 6, 0);
      bitVectorMatrix.setRegion(dimension - 11, 0, 3, 6, 1);
      // Version info, bottom left
      bitVectorMatrix.setRegion(0, dimension - 11, 6, 3, 0);
      bitVectorMatrix.setRegion(0, dimension - 11, 6, 3, 1);
    }

    return bitVectorMatrix;
  }

  /**
   * <p>Encapsulates a set of error-correction blocks in one symbol version. Most versions will
   * use blocks of differing sizes within one version, so, this encapsulates the parameters for
   * each set of blocks. It also holds the number of error-correction codewords per block since it
   * will be the same across all blocks within one version.</p>
   */
  public static final class ECBlocks {
    private final int ecCodewordsPerBlock;
    private final ECB[] ecBlocks;

    ECBlocks(int ecCodewordsPerBlock, ECB... ecBlocks) {
      this.ecCodewordsPerBlock = ecCodewordsPerBlock;
      this.ecBlocks = ecBlocks;
    }

    public int getECCodewordsPerBlock() {
      return ecCodewordsPerBlock;
    }

    public int getNumBlocks() {
      int total = 0;
      for (ECB ecBlock : ecBlocks) {
        total += ecBlock.getCount();
      }
      return total;
    }

    public int getTotalECCodewords() {
      return ecCodewordsPerBlock * getNumBlocks();
    }

    public ECB[] getECBlocks() {
      return ecBlocks;
    }
  }

  /**
   * <p>Encapsualtes the parameters for one error-correction block in one symbol version.
   * This includes the number of data codewords, and the number of times a block with these
   * parameters is used consecutively in the QR code version's format.</p>
   */
  public static final class ECB {
    private final int count;
    private final int dataCodewords;

    ECB(int count, int dataCodewords) {
      this.count = count;
      this.dataCodewords = dataCodewords;
    }

    public int getCount() {
      return count;
    }

    public int getDataCodewords() {
      return dataCodewords;
    }
  }

  @Override
  public String toString() {
    return String.valueOf(versionNumber);
  }

  /**
   * See ISO 18004:2006 6.5.1 Table 9
   */
  private static Version[] buildVersions() {
    return new Version[]{
        new Version(1, new int[]{},
            new ECBlocks(7*2, new ECB(1, 19*2)),
            new ECBlocks(10*2, new ECB(1, 16*2)),
            new ECBlocks(13*2, new ECB(1, 13*2)),
            new ECBlocks(17*2, new ECB(1, 9*2))),
        new Version(2, new int[]{6, 18},
            new ECBlocks(10*2, new ECB(1, 34*2)),
            new ECBlocks(16*2, new ECB(1, 28*2)),
            new ECBlocks(22*2, new ECB(1, 22*2)),
            new ECBlocks(28*2, new ECB(1, 16*2))),
        new Version(3, new int[]{6, 22},
            new ECBlocks(15*2, new ECB(1, 55*2)),
            new ECBlocks(26*2, new ECB(1, 44*2)),
            new ECBlocks(18*2, new ECB(2, 17*2)),
            new ECBlocks(22*2, new ECB(2, 13*2))),
        new Version(4, new int[]{6, 26},
            new ECBlocks(20*2, new ECB(1, 80*2)),
            new ECBlocks(18*2, new ECB(2, 32*2)),
            new ECBlocks(26*2, new ECB(2, 24*2)),
            new ECBlocks(16*2, new ECB(4, 9*2))),
        new Version(5, new int[]{6, 30},
            new ECBlocks(26*2, new ECB(1, 108*2)),
            new ECBlocks(24*2, new ECB(2, 43*2)),
            new ECBlocks(18*2, new ECB(2, 31),
                new ECB(2, 31)),
            new ECBlocks(22*2, new ECB(2, 23),
                new ECB(2, 23))),
        new Version(6, new int[]{6, 34},
            new ECBlocks(18*2, new ECB(2, 68*2)),
            new ECBlocks(16*2, new ECB(4, 27*2)),
            new ECBlocks(24*2, new ECB(4, 19*2)),
            new ECBlocks(28*2, new ECB(4, 15*2))),
        new Version(7, new int[]{6, 22, 38},
            new ECBlocks(20*2, new ECB(2, 78*2)),
            new ECBlocks(18*2, new ECB(4, 31*2)),
            new ECBlocks(18*2, new ECB(4, 29),
                new ECB(2, 15*2)),
            new ECBlocks(26*2, new ECB(3, 13*2),
                new ECB(2, 27))),
        new Version(8, new int[]{6, 24, 42},
            new ECBlocks(24*2, new ECB(2, 97*2)),
            new ECBlocks(22*2, new ECB(2, 77),
                new ECB(2, 77)),
            new ECBlocks(22*2, new ECB(2, 18*2),
                new ECB(4, 37)),
            new ECBlocks(26*2, new ECB(2, 14*2),
                new ECB(4, 29))),
        new Version(9, new int[]{6, 26, 46},
            new ECBlocks(30*2, new ECB(2, 116*2)),
            new ECBlocks(22*2, new ECB(1, 36*2),
                new ECB(4, 73)),
            new ECBlocks(20*2, new ECB(4, 33),
                new ECB(4, 33)),
            new ECBlocks(24*2, new ECB(4, 25),
                new ECB(4, 25))),
        new Version(10, new int[]{6, 28, 50},
            new ECBlocks(18*2, new ECB(2, 137),
                new ECB(2, 137)),
            new ECBlocks(26*2, new ECB(3, 86),
                new ECB(2, 87)),
            new ECBlocks(24*2, new ECB(4, 38),
                new ECB(4, 39)),
            new ECBlocks(28*2, new ECB(4, 15*2),
                new ECB(4, 31))),
        new Version(11, new int[]{6, 30, 54},
            new ECBlocks(20*2, new ECB(4, 81*2)),
            new ECBlocks(30*2, new ECB(2, 101),
                new ECB(3, 51*2)),
            new ECBlocks(28*2, new ECB(4, 45),
                new ECB(4, 45)),
            new ECBlocks(24*2, new ECB(6, 25),
                new ECB(5, 13*2))),
        new Version(12, new int[]{6, 32, 58},
            new ECBlocks(24*2, new ECB(2, 185),
                new ECB(2, 185)),
            new ECBlocks(22*2, new ECB(4, 36*2),
                new ECB(4, 73)),
            new ECBlocks(26*2, new ECB(8, 41),
                new ECB(2, 21*2)),
            new ECBlocks(28*2, new ECB(3, 14*2),
                new ECB(8, 29))),
        new Version(13, new int[]{6, 34, 62},
            new ECBlocks(26*2, new ECB(4, 107*2)),
            new ECBlocks(22*2, new ECB(7, 37*2),
                new ECB(2, 75)),
            new ECBlocks(24*2, new ECB(4, 20*2),
                new ECB(8, 41)),
            new ECBlocks(22*2, new ECB(8, 11*2),
                new ECB(8, 23))),
        new Version(14, new int[]{6, 26, 46, 66},
            new ECBlocks(30*2, new ECB(2, 115*2),
                new ECB(2, 231)),
            new ECBlocks(24*2, new ECB(8, 81),
                new ECB(1, 41*2)),
            new ECBlocks(20*2, new ECB(6, 16*2),
                new ECB(10, 33)),
            new ECBlocks(24*2, new ECB(6, 12*2),
                new ECB(10, 25))),
        new Version(15, new int[]{6, 26, 48, 70},
            new ECBlocks(22*2, new ECB(4, 87*2),
                new ECB(2, 87*2)),
            new ECBlocks(24*2, new ECB(2, 41*2),
                new ECB(8, 83)),
            new ECBlocks(30*2, new ECB(5, 49),
                new ECB(7, 49)),
            new ECBlocks(24*2, new ECB(6, 12*2),
                new ECB(12, 25))),
        new Version(16, new int[]{6, 26, 50, 74},
            new ECBlocks(24*2, new ECB(4, 98*2),
                new ECB(2, 98*2)),
            new ECBlocks(28*2, new ECB(6, 45*2),
                new ECB(4, 91)),
            new ECBlocks(24*2, new ECB(15, 19*2),
                new ECB(2, 39)),
            new ECBlocks(30*2, new ECB(8, 31),
                new ECB(8, 32))),
        new Version(17, new int[]{6, 30, 54, 78},
            new ECBlocks(28*2, new ECB(2, 107*2),
                new ECB(4, 107*2)),
            new ECBlocks(28*2, new ECB(8, 91),
                new ECB(3, 92)),
            new ECBlocks(28*2, new ECB(12, 45),
                new ECB(4, 23*2)),
            new ECBlocks(28*2, new ECB(14, 29),
                new ECB(5, 15*2))),
        new Version(18, new int[]{6, 30, 56, 82},
            new ECBlocks(30*2, new ECB(4, 120*2),
                new ECB(2, 120*2)),
            new ECBlocks(26*2, new ECB(7, 43*2),
                new ECB(6, 87)),
            new ECBlocks(28*2, new ECB(17, 22*2),
                new ECB(1, 22*2)),
            new ECBlocks(28*2, new ECB(6, 29),
                new ECB(15, 15*2))),
        new Version(19, new int[]{6, 30, 58, 86},
            new ECBlocks(28*2, new ECB(6, 227),
                new ECB(1, 228)),
            new ECBlocks(26*2, new ECB(6, 89),
                new ECB(8, 45*2)),
            new ECBlocks(26*2, new ECB(13, 21*2),
                new ECB(8, 43)),
            new ECBlocks(26*2, new ECB(18, 27),
                new ECB(7, 14*2))),
        new Version(20, new int[]{6, 34, 62, 90},
            new ECBlocks(28*2, new ECB(6, 215),
                new ECB(2, 216)),
            new ECBlocks(26*2, new ECB(6, 83),
                new ECB(10, 42*2)),
            new ECBlocks(30*2, new ECB(10, 24*2),
                new ECB(10, 49)),
            new ECBlocks(28*2, new ECB(5, 15*2),
                new ECB(20, 31))),
        new Version(21, new int[]{6, 28, 50, 72, 94},
            new ECBlocks(28*2, new ECB(4, 116*2),
                new ECB(4, 116*2)),
            new ECBlocks(26*2, new ECB(8, 83),
            	new ECB(9, 84)),
            new ECBlocks(28*2, new ECB(19, 22*2),
                new ECB(4, 45)),
            new ECBlocks(30*2, new ECB(21, 16*2),
                new ECB(4, 33))),
        new Version(22, new int[]{6, 26, 50, 74, 98},
            new ECBlocks(28*2, new ECB(4, 223),
                new ECB(5, 112*2)),
            new ECBlocks(28*2, new ECB(17, 46*2)),
            new ECBlocks(30*2, new ECB(14, 49),
                new ECB(9, 25*2)),
            new ECBlocks(24*2, new ECB(34, 13*2))),
        new Version(23, new int[]{6, 30, 54, 78, 102},
            new ECBlocks(30*2, new ECB(8, 243),
                new ECB(1, 122*2)),
            new ECBlocks(28*2, new ECB(8, 95),
                new ECB(10, 48*2)),
            new ECBlocks(30*2, new ECB(22, 49),
                new ECB(3, 25*2)),
            new ECBlocks(30*2, new ECB(2, 15*2),
                new ECB(28, 31))),
        new Version(24, new int[]{6, 28, 54, 80, 106},
            new ECBlocks(30*2, new ECB(2, 117*2),
                new ECB(8, 235)),
            new ECBlocks(28*2, new ECB(12, 91),
                new ECB(8, 46*2)),
            new ECBlocks(30*2, new ECB(22, 49),
                new ECB(5, 25*2)),
            new ECBlocks(30*2, new ECB(28, 16*2),
                new ECB(4, 33))),
        new Version(25, new int[]{6, 32, 58, 84, 110},
            new ECBlocks(26*2, new ECB(4, 106*2),
                new ECB(8, 213)),
            new ECBlocks(28*2, new ECB(16, 95),
                new ECB(5, 48*2)),
            new ECBlocks(30*2, new ECB(14, 49),
                new ECB(15, 25*2)),
            new ECBlocks(30*2, new ECB(9, 15*2),
                new ECB(26, 31))),
        new Version(26, new int[]{6, 30, 58, 86, 114},
            new ECBlocks(28*2, new ECB(8, 114*2),
                new ECB(4, 229)),
            new ECBlocks(28*2, new ECB(15, 46*2),
                new ECB(8, 93)),
            new ECBlocks(28*2, new ECB(22, 22*2),
                new ECB(12, 45)),
            new ECBlocks(30*2, new ECB(29, 16*2),
                new ECB(8, 33))),
        new Version(27, new int[]{6, 34, 62, 90, 118},
            new ECBlocks(30*2, new ECB(5, 122*2),
                new ECB(7, 245)),
            new ECBlocks(28*2, new ECB(20, 45*2),
                new ECB(5, 91)),
            new ECBlocks(30*2, new ECB(17, 47),
                new ECB(17, 24*2)),
            new ECBlocks(30*2, new ECB(25, 31),
                new ECB(15, 16*2))),
        new Version(28, new int[]{6, 26, 50, 74, 98, 122},
            new ECBlocks(30*2, new ECB(6, 235),
                new ECB(7, 118*2)),
            new ECBlocks(28*2, new ECB(6, 91),
                new ECB(20, 46*2)),
            new ECBlocks(30*2, new ECB(8, 49),
                new ECB(27, 25*2)),
            new ECBlocks(30*2, new ECB(22, 31),
                new ECB(20, 16*2))),
        new Version(29, new int[]{6, 30, 54, 78, 102, 126},
            new ECBlocks(30*2, new ECB(7, 116*2),
                new ECB(7, 116*2)),
            new ECBlocks(28*2, new ECB(21, 45*2),
                new ECB(7, 45*2)),
            new ECBlocks(30*2, new ECB(16, 47),
                new ECB(22, 24*2)),
            new ECBlocks(30*2, new ECB(7, 15*2),
                new ECB(38, 31))),
        new Version(30, new int[]{6, 26, 52, 78, 104, 130},
            new ECBlocks(30*2, new ECB(5, 231),
                new ECB(10, 231)),
            new ECBlocks(28*2, new ECB(14, 47*2),
                new ECB(15, 95)),
            new ECBlocks(30*2, new ECB(35, 49),
                new ECB(5, 25*2)),
            new ECBlocks(30*2, new ECB(3, 15*2),
                new ECB(45, 31))),
        new Version(31, new int[]{6, 30, 56, 82, 108, 134},
            new ECBlocks(30*2, new ECB(10, 115*2),
                new ECB(6, 231)),
            new ECBlocks(28*2, new ECB(4, 93),
                new ECB(27, 47*2)),
            new ECBlocks(30*2, new ECB(41, 24*2),
                new ECB(2, 49)),
            new ECBlocks(30*2, new ECB(46, 31),
                new ECB(5, 16*2))),
        new Version(32, new int[]{6, 34, 60, 86, 112, 138},
            new ECBlocks(30*2, new ECB(17, 115*2)),
            new ECBlocks(28*2, new ECB(20, 93),
                new ECB(13, 47*2)),
            new ECBlocks(30*2, new ECB(20, 49),
                new ECB(25, 25*2)),
            new ECBlocks(30*2, new ECB(38, 31),
                new ECB(16, 16*2))),
        new Version(33, new int[]{6, 30, 58, 86, 114, 142},
            new ECBlocks(30*2, new ECB(17, 229),
                new ECB(1, 229)),
            new ECBlocks(28*2, new ECB(13, 46*2),
                new ECB(22, 93)),
            new ECBlocks(30*2, new ECB(30, 24*2),
                new ECB(18, 49)),
            new ECBlocks(30*2, new ECB(42, 31),
                new ECB(15, 16*2))),
        new Version(34, new int[]{6, 34, 62, 90, 118, 146},
            new ECBlocks(30*2, new ECB(7, 115*2),
                new ECB(12, 231)),
            new ECBlocks(28*2, new ECB(28, 93),
                new ECB(9, 47*2)),
            new ECBlocks(30*2, new ECB(37, 24*2),
                new ECB(14, 49)),
            new ECBlocks(30*2, new ECB(58, 16*2),
                new ECB(2, 33))),
        new Version(35, new int[]{6, 30, 54, 78, 102, 126, 150},
            new ECBlocks(30*2, new ECB(5, 121*2),
                new ECB(14, 243)),
            new ECBlocks(28*2, new ECB(24, 95),
                new ECB(14, 48*2)),
            new ECBlocks(30*2, new ECB(25, 24*2),
                new ECB(28, 49)),
            new ECBlocks(30*2, new ECB(44, 31),
                new ECB(19, 16*2))),
        new Version(36, new int[]{6, 24, 50, 76, 102, 128, 154},
            new ECBlocks(30*2, new ECB(12, 243),
                new ECB(8, 122*2)),
            new ECBlocks(28*2, new ECB(12, 95),
                new ECB(28, 48*2)),
            new ECBlocks(30*2, new ECB(36, 24*2),
                new ECB(20, 49)),
            new ECBlocks(30*2, new ECB(4, 31),
                new ECB(62, 16*2))),
        new Version(37, new int[]{6, 28, 54, 80, 106, 132, 158},
            new ECBlocks(30*2, new ECB(13, 122*2),
                new ECB(8, 245)),
            new ECBlocks(28*2, new ECB(15, 46*2),
                new ECB(28, 93)),
            new ECBlocks(30*2, new ECB(39, 24*2),
                new ECB(20, 49)),
            new ECBlocks(30*2, new ECB(48, 31),
                new ECB(22, 16*2))),
        new Version(38, new int[]{6, 32, 58, 84, 110, 136, 162},
            new ECBlocks(30*2, new ECB(8, 245),
                new ECB(14, 123*2)),
            new ECBlocks(28*2, new ECB(26, 93),
                new ECB(19, 47*2)),
            new ECBlocks(30*2, new ECB(34, 24*2),
                new ECB(28, 49)),
            new ECBlocks(30*2, new ECB(10, 15*2),
                new ECB(64, 31))),
        new Version(39, new int[]{6, 26, 54, 82, 110, 138, 166},
            new ECBlocks(30*2, new ECB(16, 117*2),
                new ECB(8, 235)),
            new ECBlocks(28*2, new ECB(33, 47*2),
                new ECB(14, 95)),
            new ECBlocks(30*2, new ECB(21, 24*2),
                new ECB(44, 49)),
            new ECBlocks(30*2, new ECB(20, 31),
                new ECB(57, 16*2))),
        new Version(40, new int[]{6, 30, 58, 86, 114, 142, 170},
            new ECBlocks(30*2, new ECB(13, 118*2),
                new ECB(12, 237)),
            new ECBlocks(28*2, new ECB(36, 95),
                new ECB(13, 48*2)),
            new ECBlocks(30*2, new ECB(34, 49),
                new ECB(34, 49)),
            new ECBlocks(30*2, new ECB(40, 31),
                new ECB(41, 16*2)))
    };
  }

}
