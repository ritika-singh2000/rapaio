/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
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
 *
 */

package rapaio.experiment.ml.analysis;

import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.VRange;
import rapaio.data.VType;
import rapaio.data.Var;
import rapaio.math.linear.DMatrix;
import rapaio.math.linear.DVector;
import rapaio.math.linear.EigenPair;
import rapaio.math.linear.Linear;
import rapaio.math.linear.dense.SolidDMatrix;
import rapaio.math.linear.dense.SolidDVector;
import rapaio.printer.Printable;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/2/15.
 */
public class PCA implements Printable {

    private static final Logger logger = Logger.getLogger(PCA.class.getName());

    private double tol = 1e-10;
    private int maxRuns = 2_000;
    protected DVector values;
    protected DMatrix eigenVectors;

    protected String[] inputNames;
    protected DVector mean;
    protected DVector sd;

    protected boolean scaling = true;

    public DVector eigenValues() {
        return values;
    }

    public DMatrix eigenVectors() {
        return eigenVectors;
    }

    public PCA withMaxRuns(int maxRuns) {
        this.maxRuns = maxRuns;
        return this;
    }

    public PCA withTol(double tol) {
        this.tol = tol;
        return this;
    }

    public PCA withScaling(boolean scaling) {
        this.scaling = scaling;
        return this;
    }

    public void fit(Frame df) {
        validate(df);

        logger.fine("start pca predict");
        DMatrix x = SolidDMatrix.copy(df);
        if (scaling) {
            logger.fine("compute mean, sd and do scaling");
            mean = SolidDVector.zeros(x.colCount());
            sd = SolidDVector.zeros(x.colCount());
            for (int i = 0; i < x.colCount(); i++) {
                mean.set(i, x.mapCol(i).mean());
                sd.set(i, Math.sqrt(x.mapCol(i).variance()));
            }
            for (int i = 0; i < x.rowCount(); i++) {
                for (int j = 0; j < x.colCount(); j++) {
                    if (sd.get(j) == 0)
                        continue;
                    x.set(i, j, (x.get(i, j) - mean.get(j)) / sd.get(j));
                }
            }
        }

        logger.fine("build scatter");
        DMatrix s = x.scatter();

        logger.fine("compute eigenvalues");
        EigenPair ep = Linear.eigenDecomp(s, maxRuns, tol);
        values = ep.getRV();
        eigenVectors = ep.getRM();

        logger.fine("sort eigen values and vectors");

        Integer[] rows = new Integer[values.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        Arrays.sort(rows, (o1, o2) -> -Double.compare(values.get(o1), values.get(o2)));
        int[] indexes = Arrays.stream(rows).mapToInt(v -> v).toArray();

        values = values.asMatrix().mapRows(indexes).mapCol(0).copy();
        eigenVectors = eigenVectors.mapCols(indexes).copy();
    }

    public Frame predict(Frame df, int k) {
        // TODO check if we have all the initial columns

        DMatrix x = SolidDMatrix.copy(df.mapVars(inputNames));

        if (scaling) {
            for (int i = 0; i < x.rowCount(); i++) {
                for (int j = 0; j < x.colCount(); j++) {
                    if (sd.get(j) != 0)
                        x.set(i, j, (x.get(i, j) - mean.get(j)) / sd.get(j));
                }
            }
        }

        int[] dim = new int[k];
        String[] names = new String[k];
        for (int i = 0; i < dim.length; i++) {
            dim[i] = i;
            names[i] = "pca_" + (i + 1);
        }
        DMatrix result = x.dot(eigenVectors.mapCols(dim));
        Frame rest = df.removeVars(VRange.of(inputNames));
        return rest.varCount() == 0 ?
                SolidFrame.matrix(result, names) :
                SolidFrame.matrix(result, names).bindVars(rest);
    }

    private void validate(Frame df) {
        Set<VType> allowedTypes = new HashSet<>(Arrays.asList(VType.BINARY, VType.INT, VType.DOUBLE));
        df.varStream().forEach(var -> {
            if (!allowedTypes.contains(var.type())) {
                throw new IllegalArgumentException("Var type not allowed. Var name: " + var.name() + ", type: " + var.type().name());
            }
        });

        inputNames = df.varStream().map(Var::name).toArray(String[]::new);
    }

    public String toSummary(Printer printer, POption... options) {
        StringBuilder sb = new StringBuilder();

        sb.append("Eigen values\n");
        sb.append("============\n");
        sb.append(values.toSummary(printer, options)).append("\n");
        sb.append("Eigen vectors\n");
        sb.append("=============\n");
        sb.append(eigenVectors.toSummary(printer, options)).append("\n");

        return sb.toString();
    }
}
