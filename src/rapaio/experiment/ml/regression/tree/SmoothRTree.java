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

package rapaio.experiment.ml.regression.tree;

import rapaio.data.*;
import rapaio.experiment.ml.regression.boost.gbt.*;
import rapaio.ml.loss.*;
import rapaio.experiment.ml.regression.tree.srt.*;
import rapaio.ml.common.*;
import rapaio.ml.regression.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 6/19/19.
 */
public class SmoothRTree extends AbstractRegressionModel<SmoothRTree, RegressionResult<SmoothRTree>>
        implements GBTRtree<SmoothRTree, RegressionResult<SmoothRTree>> {

    private static final long serialVersionUID = 5062591010395009141L;

    private int minCount = 5;
    private double minScore = 1e-320;
    private double minWeight = 1e-10;
    private int maxDepth = 3;
    private VarSelector varSelector = VarSelector.all();
    private SmoothRFunction smoothRFunction = FixedScaleSmoothSplineRFunction.fromScales(1, 5, 0.1, new double[]{0.001, 0.01, 0.1});
    private RegressionLoss loss = new L2RegressionLoss();

    private SmoothRTreeNode root;

    @Override
    public String name() {
        return null;
    }

    @Override
    public String fullName() {
        return null;
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .withAllowMissingInputValues(false)
                .withAllowMissingTargetValues(false)
                .withInputCount(1, Integer.MAX_VALUE)
                .withInputTypes(VType.DOUBLE, VType.INT, VType.BINARY, VType.LONG)
                .withTargetCount(1, 1)
                .withTargetTypes(VType.DOUBLE);
    }

    public int getMinCount() {
        return minCount;
    }

    public SmoothRTree withMinCount(int minCount) {
        this.minCount = minCount;
        return this;
    }

    public double getMinScore() {
        return minScore;
    }

    public SmoothRTree withMinScore(double minScore) {
        this.minScore = minScore;
        return this;
    }

    public double getMinWeight() {
        return minWeight;
    }

    public SmoothRTree withMinWeight(double minWeight) {
        this.minWeight = minWeight;
        return this;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public SmoothRTree withMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public VarSelector getVarSelector() {
        return varSelector;
    }

    public SmoothRTree withVarSelector(VarSelector varSelector) {
        this.varSelector = varSelector;
        return this;
    }

    public SmoothRFunction getSmoothRFunction() {
        return smoothRFunction;
    }

    public SmoothRTree withSmoothRFunction(SmoothRFunction smoothRFunction) {
        this.smoothRFunction = smoothRFunction;
        return this;
    }

    public RegressionLoss getLoss() {
        return loss;
    }

    public SmoothRTree withLoss(RegressionLoss loss) {
        this.loss = loss;
        return this;
    }

    @Override
    public SmoothRTree newInstance() {
        return newInstanceDecoration(new SmoothRTree())
                .withMinCount(getMinCount())
                .withMinScore(getMinScore())
                .withMinWeight(getMinWeight())
                .withMaxDepth(getMaxDepth())
                .withVarSelector(getVarSelector())
                .withSmoothRFunction(getSmoothRFunction())
                .withLoss(getLoss());
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        this.root = new SmoothRTreeNode(null);
        root.coreFit(this, df, weights);
        return true;
    }

    @Override
    protected RegressionResult<SmoothRTree> corePredict(Frame df, boolean withResiduals) {
        RegressionResult<SmoothRTree> prediction = RegressionResult.build(this, df, withResiduals);
        for (int i = 0; i < df.rowCount(); i++) {
            prediction.firstPrediction().setDouble(i, root.predict(df, i, this, 1.0));
        }
        prediction.buildComplete();
        return prediction;
    }

    @Override
    public String summary() {
        return null;
    }

    @Override
    public String content() {
        StringBuilder sb = new StringBuilder();
        nodeContent(sb, root, 0);
        return sb.toString();
    }

    private void nodeContent(StringBuilder sb, SmoothRTreeNode node, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("\t");
        }
        sb.append("model: ").append(node.getFunction().toString()).append(";");
        sb.append("\n");
        if (!node.isLeaf()) {
            nodeContent(sb, node.getLeftNode(), level + 1);
            nodeContent(sb, node.getRightNode(), level + 1);
        }
    }

    @Override
    public String fullContent() {
        return null;
    }

    @Override
    public void boostUpdate(Frame x, Var y, Var fx, GBTRegressionLoss lossFunction) {

    }
}