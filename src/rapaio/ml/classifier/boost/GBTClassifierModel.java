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

package rapaio.ml.classifier.boost;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rapaio.data.Frame;
import rapaio.data.VType;
import rapaio.data.Var;
import rapaio.data.sample.RowSampler;
import rapaio.math.linear.DM;
import rapaio.math.linear.DV;
import rapaio.math.linear.dense.DMStripe;
import rapaio.ml.classifier.AbstractClassifierModel;
import rapaio.ml.classifier.ClassifierResult;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.ValueParam;
import rapaio.ml.loss.KDevianceLoss;
import rapaio.ml.loss.L2Loss;
import rapaio.ml.regression.RegressionModel;
import rapaio.ml.regression.tree.RTree;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 12/12/14.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GBTClassifierModel extends AbstractClassifierModel<GBTClassifierModel, ClassifierResult> {

    public static GBTClassifierModel newModel() {
        return new GBTClassifierModel();
    }

    private static final long serialVersionUID = -2979235364091072967L;

    public final ValueParam<Double, GBTClassifierModel> shrinkage = new ValueParam<>(this, 1.0,
            "shrinkage",
            "Shrinkage factor",
            Double::isFinite);

    public final ValueParam<Boolean, GBTClassifierModel> debug = new ValueParam<>(this, false,
            "debug",
            "debug");

    public final ValueParam<RTree, GBTClassifierModel> model = new ValueParam<>(this,
            RTree.newCART().maxDepth.set(2).minCount.set(5).loss.set(new L2Loss()),
            "model",
            "Model");

    // learning artifacts

    private int K;
    private DM f;
    private DM p;
    private DM residual;

    @Getter
    private List<List<RTree>> trees;

    @Override
    public GBTClassifierModel newInstance() {
        return new GBTClassifierModel().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "GBTClassifier";
    }

    @Override
    public Capabilities capabilities() {
        return Capabilities.builder()
                .minInputCount(1).maxInputCount(1_000_000)
                .inputTypes(Arrays.asList(VType.BINARY, VType.INT, VType.NOMINAL, VType.DOUBLE))
                .allowMissingInputValues(true)
                .minTargetCount(1).maxTargetCount(1)
                .targetType(VType.NOMINAL)
                .allowMissingTargetValues(false)
                .build();
    }

    @Override
    public boolean coreFit(Frame df, Var weights) {

        // algorithm described by ESTL pag. 387

        K = firstTargetLevels().size() - 1;
        f = DMStripe.empty(K, df.rowCount());
        p = rapaio.math.linear.dense.DMStripe.empty(K, df.rowCount());
        residual = rapaio.math.linear.dense.DMStripe.empty(K, df.rowCount());

        trees = IntStream.range(0, K).mapToObj(i -> new ArrayList<RTree>()).collect(Collectors.toList());

        // build individual regression targets for each class

        final DMStripe yk = rapaio.math.linear.dense.DMStripe.fill(K, df.rowCount(), 0.0);
        for (int i = 0; i < df.rowCount(); i++) {
            yk.set(df.getInt(i, firstTargetName()) - 1, i, 1);
        }

        for (int m = 0; m < runs.get(); m++) {
            buildAdditionalTree(df, weights, yk);
            if (runningHook.get() != null) {
                runningHook.get().accept(this, m);
            }
        }
        return true;
    }

    private void buildAdditionalTree(Frame df, Var w, DM yk) {

        // a) Set p_k(x)

        DV max = f.t().amax(1);

        for (int i = 0; i < df.rowCount(); i++) {
            double sum = 0;
            for (int k = 0; k < K; k++) {
                sum += Math.exp(f.get(k, i) - max.get(i));
            }
            for (int k = 0; k < K; k++) {
                p.set(k, i, Math.exp(f.get(k, i) - max.get(i)) / sum);
            }
        }
        residual = yk.copy().sub(p);

        // b)

        Frame x = df.removeVars(targetNames);
        RowSampler.Sample sample = rowSampler.get().nextSample(x, w);

        for (int k = 0; k < K; k++) {

            Var residual_k = residual.mapRow(k).asVarDouble().mapRows(sample.getMapping()).withName("##tt##");

            var tree = model.get().newInstance();
            tree.fit(sample.getDf().bindVars(residual_k), sample.getWeights(), "##tt##");
            tree.boostUpdate(df, yk.mapRow(k).asVarDouble(), p.mapRow(k).asVarDouble(), new KDevianceLoss(K));

            trees.get(k).add(tree);

            var prediction = tree.predict(df, false).firstPrediction();
            for (int i = 0; i < df.rowCount(); i++) {
                f.inc(k, i, shrinkage.get() * prediction.getDouble(i));
            }
        }
    }

    @Override
    public ClassifierResult corePredict(Frame df, boolean withClasses, boolean withDistributions) {
        ClassifierResult cr = ClassifierResult.build(this, df, withClasses, withDistributions);

        DM p_f = rapaio.math.linear.dense.DMStripe.empty(K, df.rowCount());

        for (int k = 0; k < K; k++) {
            for (RegressionModel tree : trees.get(k)) {
                var rr = tree.predict(df, false).firstPrediction();
                for (int i = 0; i < df.rowCount(); i++) {
                    p_f.set(k, i, p_f.get(k, i) + shrinkage.get() * rr.getDouble(i));
                }
            }
        }

        // make probabilities

        DV max = p_f.t().amax(1);

        for (int i = 0; i < df.rowCount(); i++) {
            double t = 0.0;
            for (int k = 0; k < K; k++) {
                t += Math.exp(p_f.get(k, i) - max.get(i));
            }
            if (t != 0) {
                for (int k = 0; k < K; k++) {
                    cr.firstDensity().setDouble(i, k + 1, Math.exp(p_f.get(k, i) - max.get(i)) / t);
                }
            }
        }

        for (int i = 0; i < df.rowCount(); i++) {
            int maxIndex = 0;
            double maxValue = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < K; k++) {
                if (cr.firstDensity().getDouble(i, k + 1) > maxValue) {
                    maxValue = cr.firstDensity().getDouble(i, k + 1);
                    maxIndex = k + 1;
                }
            }
            cr.firstClasses().setInt(i, maxIndex);
        }
        return cr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName()).append("; fitted=").append(hasLearned());
        if (hasLearned()) {
            sb.append(", fitted trees=").append(trees.get(0).size());
        }
        return sb.toString();
    }

    @Override
    public String toSummary(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n > ").append(fullName()).append("\n");
        if (hasLearned()) {
            sb.append("weak learners built: ").append(trees.get(0).size()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toContent(POption<?>... options) {
        return toSummary();
    }

    @Override
    public String toFullContent(POption<?>... options) {
        return toSummary();
    }
}

