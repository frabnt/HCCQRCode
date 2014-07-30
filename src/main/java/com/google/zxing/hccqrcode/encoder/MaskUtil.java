/*
 * Copyright 2008 ZXing authors
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

package main.java.com.google.zxing.hccqrcode.encoder;

/**
 * @author Francesco Benedetto
 * 
 */

//High Capacity Colored QR code (HCCQRCode) is considered as a binary matrix composed simply of dark
//(the darkest color: black and blue) and light (the lightest color: white and yellow)

final class MaskUtil {

  // Penalty weights from section 6.8.2.1
  private static final int N1 = 3;
  private static final int N2 = 3;
  private static final int N3 = 40;
  private static final int N4 = 10;

  private MaskUtil() {
    // do nothing
  }

  /**
   * Apply mask penalty rule 1 and return the penalty. Find repetitive cells with the same color and
   * give penalty to them. Example: 00000 or 11111.
   */
  static int applyMaskPenaltyRule1(ByteVectorMatrix matrix) {
    return applyMaskPenaltyRule1Internal(matrix, true) + applyMaskPenaltyRule1Internal(matrix, false);
  }

  /**
   * Apply mask penalty rule 2 and return the penalty. Find 2x2 blocks with the same color and give
   * penalty to them. This is actually equivalent to the spec's rule, which is to find MxN blocks and give a
   * penalty proportional to (M-1)x(N-1), because this is the number of 2x2 blocks inside such a block.
   */
  static int applyMaskPenaltyRule2(ByteVectorMatrix matrix) {
    int penalty = 0;
    byte[][][] array = matrix.getArray();
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    for (int y = 0; y < height - 1; y++) {
      for (int x = 0; x < width - 1; x++) {
        byte[] value = array[y][x];
        //coding used: black->(1,1) blue->(1,0) yellow->(0,1) white->(0,0)
        //first bit of array is set to 1 if module is dark, otherwise it's set to 0
        int value1 = value[0];
        if (value1 == array[y][x + 1][0] && value1 == array[y + 1][x][0] && value1 == array[y + 1][x + 1][0]) {
          penalty++;
        }
      }
    }
    return N2 * penalty;
  }

  /**
   * Apply mask penalty rule 3 and return the penalty. Find consecutive runs of 1:1:3:1:1:4
   * starting with black, or 4:1:1:3:1:1 starting with white, and give penalty to them.  If we
   * find patterns like 000010111010000, we give penalty once.
   */
  static int applyMaskPenaltyRule3(ByteVectorMatrix matrix) {
    int numPenalties = 0;
    byte[][][] array = matrix.getArray();
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        byte[][] arrayY = array[y];  // We can at least optimize this access
        if (x + 6 < width &&
            arrayY[x][0] == 1 &&
            arrayY[x +  1][0] == 0 &&
            arrayY[x +  2][0] == 1 &&
            arrayY[x +  3][0] == 1 &&
            arrayY[x +  4][0] == 1 &&
            arrayY[x +  5][0] == 0 &&
            arrayY[x +  6][0] == 1 &&
            (isWhiteHorizontal(arrayY, x - 4, x) || isWhiteHorizontal(arrayY, x + 7, x + 11))) {
          numPenalties++;
        }
        if (y + 6 < height &&
            array[y][x][0] == 1  &&
            array[y +  1][x][0] == 0  &&
            array[y +  2][x][0] == 1  &&
            array[y +  3][x][0] == 1  &&
            array[y +  4][x][0] == 1  &&
            array[y +  5][x][0] == 0  &&
            array[y +  6][x][0] == 1 &&
            (isWhiteVertical(array, x, y - 4, y) || isWhiteVertical(array, x, y + 7, y + 11))) {
          numPenalties++;
        }
      }
    }
    return numPenalties * N3;
  }
  
  //to check if a row's elements from "from" to "to" are all white or yellow (light colors)
  private static boolean isWhiteHorizontal(byte[][] rowArray, int from, int to) {
    for (int i = from; i < to; i++) {
      if (i >= 0 && i < rowArray.length && rowArray[i][0] == 1) {
        return false;
      }
    }
    return true;
  }
  
  //to check if a colomun's elements from "from" to "to" are all white or yellow (light colors)"
  private static boolean isWhiteVertical(byte[][][] array, int col, int from, int to) {
    for (int i = from; i < to; i++) {
      if (i >= 0 && i < array.length && array[i][col][0] == 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Apply mask penalty rule 4 and return the penalty. Calculate the ratio of dark cells and give
   * penalty if the ratio is far from 50%. It gives 10 penalty for 5% distance.
   */
  static int applyMaskPenaltyRule4(ByteVectorMatrix matrix) {
    int numDarkCells = 0;
    byte[][][] array = matrix.getArray();
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    for (int y = 0; y < height; y++) {
      byte[][] arrayY = array[y];
      for (int x = 0; x < width; x++) {
        if (arrayY[x][0] == 1) {
          numDarkCells++;
        }
      }
    }
    int numTotalCells = matrix.getHeight() * matrix.getWidth();
    int fivePercentVariances = Math.abs(numDarkCells * 2 - numTotalCells) * 10 / numTotalCells;
    return fivePercentVariances * N4;
  }

  /**
   * Return the mask bit for "getMaskPattern" at "x" and "y". See 8.8 of JISX0510:2004 for mask
   * pattern conditions.
   */
  static boolean getDataMaskBit(int maskPattern, int x, int y) {
    int intermediate;
    int temp;
    switch (maskPattern) {
      case 0:
        intermediate = (y + x) & 0x1;
        break;
      case 1:
        intermediate = y & 0x1;
        break;
      case 2:
        intermediate = x % 3;
        break;
      case 3:
        intermediate = (y + x) % 3;
        break;
      case 4:
        intermediate = ((y / 2) + (x / 3)) & 0x1;
        break;
      case 5:
        temp = y * x;
        intermediate = (temp & 0x1) + (temp % 3);
        break;
      case 6:
        temp = y * x;
        intermediate = ((temp & 0x1) + (temp % 3)) & 0x1;
        break;
      case 7:
        temp = y * x;
        intermediate = ((temp % 3) + ((y + x) & 0x1)) & 0x1;
        break;
      default:
        throw new IllegalArgumentException("Invalid mask pattern: " + maskPattern);
    }
    return intermediate == 0;
  }

  /**
   * Helper function for applyMaskPenaltyRule1. We need this for doing this calculation in both
   * vertical and horizontal orders respectively.
   */
  private static int applyMaskPenaltyRule1Internal(ByteVectorMatrix matrix, boolean isHorizontal) {
    int penalty = 0;
    int iLimit = isHorizontal ? matrix.getHeight() : matrix.getWidth();
    int jLimit = isHorizontal ? matrix.getWidth() : matrix.getHeight();
    byte[][][] array = matrix.getArray();
    for (int i = 0; i < iLimit; i++) {
      int numSameBitCells = 0;
      int prevBit = -1;
      for (int j = 0; j < jLimit; j++) {
        int bit = isHorizontal ? array[i][j][0] : array[j][i][0];
        if (bit == prevBit) {
          numSameBitCells++;
        } else {
          if (numSameBitCells >= 5) {
            penalty += N1 + (numSameBitCells - 5);
          }
          numSameBitCells = 1;  // Include the cell itself.
          prevBit = bit;
        }
      }
      if (numSameBitCells >= 5) {
        penalty += N1 + (numSameBitCells - 5);
      }
    }
    return penalty;
  }

}
