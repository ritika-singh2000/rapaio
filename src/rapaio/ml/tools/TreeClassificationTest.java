/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package rapaio.ml.tools;

import rapaio.core.RandomSource;
import rapaio.data.Frame;
import rapaio.data.RowComparators;
import rapaio.data.Vector;
import rapaio.data.Vectors;
import rapaio.data.filters.BaseFilters;

import java.util.List;

/**
 * @author <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
public class TreeClassificationTest {

    public static enum Method {
        ENTROPY {
            @Override
            double compute(DensityTable dt) {
                return dt.getSplitEntropy(false);
            }

            @Override
            int compare(double previous, double current) {
                if (previous == current) return 0;
                return (previous < current) ? -1 : 1;
            }
        },
        INFO_GAIN {
            @Override
            double compute(DensityTable dt) {
                return dt.getInfoGain(false);
            }

            @Override
            int compare(double previous, double current) {
                if (previous == current) return 0;
                return previous < current ? 1 : -1;
            }
        },
        GAIN_RATIO {
            @Override
            double compute(DensityTable dt) {
                return dt.getGainRatio();
            }

            @Override
            int compare(double previous, double current) {
                if (previous == current) return 0;
                return previous < current ? 1 : -1;
            }
        },
        GINI {
            @Override
            double compute(DensityTable dt) {
                throw new IllegalArgumentException("Not yet implemented");
            }

            @Override
            int compare(double previous, double current) {
                if (previous == current) return 0;
                return previous < current ? 1 : -1;
            }
        };

        abstract double compute(DensityTable dt);

        /**
         * comparison for given criteria
         * Possible return values are:
         * <ul>
         * <li>1 if we found an improvement</li>
         * <li>0 if is the same as previous</li>
         * <li>-1 if there are no improvements</li>
         * </ul>
         *
         * @param previous previous criteria value
         * @param current  current criteria value
         * @return an integer which tells if there is an improvement or not
         */
        abstract int compare(double previous, double current);
    }

    private final Method method;
    private final int minCount;
    private String testName = null;
    private double bestValue = Double.NEGATIVE_INFINITY;
    private String binarySplitLabel;
    private double binarySplitValue;

    public Method getMethod() {
        return method;
    }

    public String getTestName() {
        return testName;
    }

    public double getBestValue() {
        return bestValue;
    }

    public String getBinarySplitLabel() {
        return binarySplitLabel;
    }

    public double getBinarySplitValue() {
        return binarySplitValue;
    }

    public TreeClassificationTest(Method method, int minCount) {
        this.method = method;
        this.minCount = minCount;
    }

    public void binaryNominalTest(Frame df, String testColName, String targetColName, List<Double> weights, String testLabel) {
        Vector test = df.getCol(testColName);
        Vector target = df.getCol(targetColName);
        DensityTable dt = new DensityTable(test, target, weights, testLabel);
        double value = method.compute(dt);
        int comp = method.compare(bestValue, value);
        if (comp < 0) return;
        if (comp == 0 && RandomSource.nextDouble() > 0.5) return;
        bestValue = value;
        testName = testColName;
        binarySplitLabel = testLabel;
        binarySplitValue = Double.NaN;
    }

    public void binaryNumericTest(Frame df, String testColName, String targetColName, List<Double> weights) {
        Vector test = df.getCol(testColName);
        Vector target = df.getCol(targetColName);

        DensityTable dt = new DensityTable(DensityTable.NUMERIC_DEFAULT_LABELS, target.getDictionary());
        int misCount = 0;
        for (int i = 0; i < df.rowCount(); i++) {
            int row = (test.isMissing(i)) ? 0 : 2;
            if (test.isMissing(i)) misCount++;
            dt.update(row, target.getIndex(i), weights.get(i));
        }

        Vector sort = BaseFilters.sort(
                Vectors.newSeq(df.rowCount()),
                RowComparators.numericComparator(test, true));

        for (int i = 0; i < df.rowCount(); i++) {
            int row = sort.getIndex(i);

            if (test.isMissing(row)) continue;

            dt.update(2, target.getIndex(row), -weights.get(row));
            dt.update(1, target.getIndex(row), +weights.get(row));

            if (i >= misCount + minCount &&
                    i < df.rowCount() - 1 - minCount &&
                    test.getValue(sort.getIndex(i)) < test.getValue(sort.getIndex(i + 1))) {

                double value = method.compute(dt);
                int comp = method.compare(bestValue, value);
                if (comp < 0) continue;
                if (comp == 0 && RandomSource.nextDouble() > 0.5) continue;
                bestValue = value;
                testName = testColName;
                binarySplitLabel = null;
                binarySplitValue = test.getValue(row);

//                System.out.println(String.format("best:%.3f, test:%s, splitVal:%.3f", bestValue, testName, binarySplitValue));
            }
        }

    }
}