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

package rapaio.ml.regression.linear;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.filter.FIntercept;
import rapaio.math.linear.DM;
import rapaio.math.linear.decomposition.QRDecomposition;
import rapaio.math.linear.dense.DMStripe;
import rapaio.ml.regression.linear.impl.BaseLinearRegressionModel;

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LinearRegressionModel extends BaseLinearRegressionModel<LinearRegressionModel> {

    /**
     * Builds a linear regression model with intercept.
     *
     * @return new instance of linear regression model
     */
    public static LinearRegressionModel newModel() {
        return new LinearRegressionModel();
    }

    private static final long serialVersionUID = 8595413796946622895L;

    @Override
    public LinearRegressionModel newInstance() {
        return new LinearRegressionModel().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "LinearRegression";
    }

    @Override
    protected FitSetup prepareFit(Frame df, Var weights, String... targetVarNames) {
        // add intercept variable
        Frame transformed = intercept.get() ? FIntercept.filter().apply(df) : df;

        // collect standard information
        return super.prepareFit(transformed, weights, targetVarNames);
    }


    @Override
    protected boolean coreFit(Frame df, Var weights) {
        DM X = DMStripe.copy(df.mapVars(inputNames()));
        DM Y = rapaio.math.linear.dense.DMStripe.copy(df.mapVars(targetNames()));
        beta = QRDecomposition.from(X).solve(Y);
        return true;
    }
}
