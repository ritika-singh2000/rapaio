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

package rapaio.ml.regression.tree;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import rapaio.core.stat.*;
import rapaio.data.*;
import rapaio.experiment.ml.regression.boost.gbt.*;
import rapaio.experiment.ml.regression.loss.*;
import rapaio.experiment.ml.regression.tree.*;
import rapaio.ml.common.*;
import rapaio.ml.common.predicate.*;
import rapaio.ml.regression.*;
import rapaio.ml.regression.tree.rtree.*;
import rapaio.printer.*;
import rapaio.printer.format.*;
import rapaio.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rapaio.printer.format.Format.*;

/**
 * Implements a regression decision tree.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a> on 11/24/14.
 */
public class RTree extends AbstractRegressionModel<RTree, RegressionResult<RTree>>
        implements GBTRtree<RTree, RegressionResult<RTree>>, Printable {

    private static final long serialVersionUID = -2748764643670512376L;

    public static RTree newDecisionStump() {
        return new RTree()
                .withMaxDepth(2)
                .withTest(VType.DOUBLE, RTreeTest.NumericBinary)
                .withTest(VType.INT, RTreeTest.NumericBinary)
                .withTest(VType.BINARY, RTreeTest.NumericBinary)
                .withTest(VType.NOMINAL, RTreeTest.NominalBinary)
                .withSplitter(RTreeSplitter.REMAINS_TO_MAJORITY)
                .withPurityFunction(RTreePurityFunction.WEIGHTED_VAR_GAIN);
    }

    public static RTree newC45() {
        return new RTree()
                .withMaxDepth(Integer.MAX_VALUE)
                .withTest(VType.DOUBLE, RTreeTest.NumericBinary)
                .withTest(VType.INT, RTreeTest.NumericBinary)
                .withTest(VType.BINARY, RTreeTest.NumericBinary)
                .withTest(VType.NOMINAL, RTreeTest.NominalFull)
                .withSplitter(RTreeSplitter.REMAINS_TO_RANDOM)
                .withPurityFunction(RTreePurityFunction.WEIGHTED_VAR_GAIN)
                .withMinCount(2);
    }

    public static RTree newCART() {
        return new RTree()
                .withMaxDepth(Integer.MAX_VALUE)
                .withTest(VType.DOUBLE, RTreeTest.NumericBinary)
                .withTest(VType.INT, RTreeTest.NumericBinary)
                .withTest(VType.BINARY, RTreeTest.NumericBinary)
                .withTest(VType.NOMINAL, RTreeTest.NominalBinary)
                .withSplitter(RTreeSplitter.REMAINS_TO_RANDOM)
                .withPurityFunction(RTreePurityFunction.WEIGHTED_VAR_GAIN)
                .withMinCount(1);
    }

    private static final Map<VType, RTreeTest> DEFAULT_TEST_MAP;

    static {
        DEFAULT_TEST_MAP = new HashMap<>();
        DEFAULT_TEST_MAP.put(VType.DOUBLE, RTreeTest.NumericBinary);
        DEFAULT_TEST_MAP.put(VType.INT, RTreeTest.NumericBinary);
        DEFAULT_TEST_MAP.put(VType.LONG, RTreeTest.NumericBinary);
        DEFAULT_TEST_MAP.put(VType.BINARY, RTreeTest.NumericBinary);
        DEFAULT_TEST_MAP.put(VType.NOMINAL, RTreeTest.NominalFull);
        DEFAULT_TEST_MAP.put(VType.TEXT, RTreeTest.Ignore);
    }

    private int minCount = 1;
    private int maxDepth = Integer.MAX_VALUE;
    private int maxSize = Integer.MAX_VALUE;
    private double minScore = 1e-320;

    private SortedMap<VType, RTreeTest> testMap = new TreeMap<>(DEFAULT_TEST_MAP);

    private RTreePurityFunction function = RTreePurityFunction.WEIGHTED_VAR_GAIN;
    private RTreeSplitter splitter = RTreeSplitter.REMAINS_IGNORED;
    private RTreePredictor predictor = RTreePredictor.STANDARD;
    private VarSelector varSelector = VarSelector.all();
    private RegressionLoss regressionLoss = new L2RegressionLoss();

    // tree root node

    private RTreeNode root;

    private RTree() {
    }

    @Override
    public RTree newInstance() {
        RTree newInstance = newInstanceDecoration(new RTree())
                .withMinCount(minCount)
                .withMinScore(minScore)
                .withMaxDepth(maxDepth)
                .withMaxSize(maxSize)
                .withPurityFunction(function)
                .withSplitter(splitter)
                .withPredictor(predictor)
                .withVarSelector(varSelector)
                .withRegressionLoss(regressionLoss);
        newInstance.testMap = new TreeMap<>(this.testMap);
        return newInstance;
    }

    @Override
    public String name() {
        return "RTree";
    }

    @Override
    public String fullName() {
        StringBuilder sb = new StringBuilder();
        sb.append("TreeClassifier {");
        sb.append("  minCount=").append(minCount).append(",\n");
        sb.append("  minScore=").append(Format.floatFlex(minScore)).append(",\n");
        sb.append("  maxDepth=").append(maxDepth).append(",\n");
        sb.append("  maxSize=").append(maxSize).append(",\n");
        for (Map.Entry<VType, RTreeTest> e : testMap.entrySet()) {
            sb.append("  test[").append(e.getKey().code()).append("]=")
                    .append(e.getValue().name()).append(",\n");
        }
        sb.append("  regressionLoss=").append(regressionLoss.name()).append("\n");
        sb.append("  purityFunction=").append(function.name()).append(",\n");
        sb.append("  splitter=").append(splitter.name()).append(",\n");
        sb.append("  predictor=").append(predictor.name()).append("\n");
        sb.append("  varSelector=").append(varSelector.name()).append(",\n");
        sb.append("  runs=").append(runs).append(",\n");
        sb.append("  poolSize=").append(poolSize).append(",\n");
        sb.append("  sampler=").append(sampler.name()).append(",\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .withInputCount(1, 1_000_000)
                .withTargetCount(1, 1)
                .withInputTypes(VType.BINARY, VType.INT, VType.DOUBLE, VType.NOMINAL)
                .withTargetTypes(VType.DOUBLE)
                .withAllowMissingInputValues(true)
                .withAllowMissingTargetValues(false);
    }

    public int minCount() {
        return minCount;
    }

    public RTree withMinCount(int minCount) {
        this.minCount = minCount;
        return this;
    }

    public double minScore() {
        return minScore;
    }

    public RTree withMinScore(double minScore) {
        this.minScore = minScore;
        return this;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public RTree withMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public int maxSize() {
        return maxSize;
    }

    public RTree withMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public Map<VType, RTreeTest> testMap() {
        return testMap;
    }

    public RTree withTests(Map<VType, RTreeTest> testMap) {
        this.testMap = new TreeMap<>(testMap);
        return this;
    }

    public RTree withTest(VType vType, RTreeTest test) {
        this.testMap.put(vType, test);
        return this;
    }

    public RegressionLoss regressionLoss() {
        return regressionLoss;
    }

    public RTree withRegressionLoss(RegressionLoss regressionLoss) {
        this.regressionLoss = regressionLoss;
        return this;
    }

    public RTreeSplitter splitter() {
        return splitter;
    }

    public RTree withSplitter(RTreeSplitter splitter) {
        this.splitter = splitter;
        return this;
    }

    public RTreePredictor predictor() {
        return predictor;
    }

    public RTree withPredictor(RTreePredictor predictor) {
        this.predictor = predictor;
        return this;
    }

    public RTreePurityFunction purityFunction() {
        return function;
    }

    public RTree withPurityFunction(RTreePurityFunction function) {
        this.function = function;
        return this;
    }

    public VarSelector varSelector() {
        return varSelector;
    }

    public RTree withVarSelector(VarSelector varSelector) {
        this.varSelector = varSelector;
        return this;
    }

    public RTreeNode root() {
        return root;
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {

        capabilities().checkAtLearnPhase(df, weights, targetNames);

        int id = 1;

        Int2ObjectOpenHashMap<Frame> frameMap = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<Var> weightsMap = new Int2ObjectOpenHashMap<>();

        this.varSelector.withVarNames(inputNames());
        root = new RTreeNode(id++, null, "root", (row, frame) -> true, 1);

        // prepare data for root
        frameMap.put(root.id(), df);
        weightsMap.put(root.id(), weights);

        // make queue and initialize it

        Queue<RTreeNode> queue = new ConcurrentLinkedQueue<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            RTreeNode last = queue.poll();
            int lastId = last.id();
            Frame lastDf = frameMap.get(lastId);
            Var lastWeights = weightsMap.get(lastId);
            learnNode(last, lastDf, lastWeights);

            if (last.isLeaf()) {
                continue;
            }
            // now that we have a best candidate,do the effective split

            List<RowPredicate> predicates = last.bestCandidate().getGroupPredicates();
            List<Mapping> mappings = splitter.performSplitMapping(lastDf, lastWeights, predicates);

            for (int i = 0; i < predicates.size(); i++) {
                RowPredicate predicate = predicates.get(i);
                RTreeNode child = new RTreeNode(id++, last, predicate.toString(), predicate, last.depth() + 1);
                last.children().add(child);

                frameMap.put(child.id(), lastDf.mapRows(mappings.get(i)));
                weightsMap.put(child.id(), lastWeights.mapRows(mappings.get(i)).copy());

                queue.add(child);
            }

            frameMap.remove(last.id());
            weightsMap.remove(last.id());
        }
        return true;
    }

    private void learnNode(RTreeNode node, Frame df, Var weights) {

        node.setLeaf(true);
        node.setValue(regressionLoss.computeConstantWeightedMinimum(df, firstTargetName(), weights));
        node.setWeight(Sum.of(weights).value());

        if (node.weight() == 0) {
            node.setValue(node.getParent() != null ? node.getParent().value() : Double.NaN);
            node.setWeight(node.getParent() != null ? node.getParent().value() : Double.NaN);
            return;
        }
        if (df.rowCount() <= minCount() || node.depth() >= (maxDepth == -1 ? Integer.MAX_VALUE : maxDepth)) {
            return;
        }

        Stream<String> stream = Arrays.stream(varSelector.nextVarNames());
        if (runs > 1) {
            stream = stream.parallel();
        }

        List<RTreeCandidate> candidates = stream
                .map(testCol -> testMap.get(df.type(testCol))
                        .computeCandidate(this, df, weights, testCol, firstTargetName(), purityFunction())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        RTreeCandidate bestCandidate = null;
        for (RTreeCandidate candidate : candidates) {
            if (bestCandidate == null || candidate.getScore() >= bestCandidate.getScore()) {
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null
                || bestCandidate.getGroupNames().isEmpty()
                || bestCandidate.getScore() <= minScore) {
            return;
        }
        node.setBestCandidate(bestCandidate);
        node.setLeaf(false);
    }

    @Override
    protected RegressionResult<RTree> corePredict(Frame df, boolean withResiduals) {
        RegressionResult<RTree> pred = RegressionResult.build(this, df, withResiduals);

        for (int i = 0; i < df.rowCount(); i++) {
            DoublePair result = predictor.predict(i, df, root);
            pred.prediction(firstTargetName()).setDouble(i, result._1);
        }
        pred.buildComplete();
        return pred;
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n > ").append(fullName());
        sb.append("\n model fitted: ").append(hasLearned).append("\n");

        if (!hasLearned) {
            return sb.toString();
        }

        sb.append("\n");
        sb.append("description:\n");
        sb.append("split, mean (total weight) [* if is leaf]\n\n");

        buildSummary(sb, root, 0);
        return sb.toString();
    }

    @Override
    public String content() {
        return summary();
    }

    @Override
    public String fullContent() {
        return summary();
    }

    private void buildSummary(StringBuilder sb, RTreeNode node, int level) {
        sb.append("|");
        for (int i = 0; i < level; i++) {
            sb.append("   |");
        }
        sb.append(node.groupName()).append("  ");

        sb.append(floatFlex(node.value()));
        sb.append(" (").append(floatFlex(node.weight())).append(") ");
        if (node.isLeaf()) sb.append(" *");
        sb.append("\n");

//        children

        if (!node.isLeaf()) {
            node.children().forEach(child -> buildSummary(sb, child, level + 1));
        }
    }

    @Deprecated
    public void boostUpdate(Frame x, Var y, Var fx, GBTRegressionLoss lossFunction) {
        root.boostUpdate(x, y, fx, lossFunction, splitter);
    }
}
