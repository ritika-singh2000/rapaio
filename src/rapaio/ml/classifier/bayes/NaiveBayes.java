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

package rapaio.ml.classifier.bayes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import rapaio.core.tools.DensityVector;
import rapaio.data.Frame;
import rapaio.data.VRange;
import rapaio.data.VType;
import rapaio.data.Var;
import rapaio.ml.classifier.AbstractClassifierModel;
import rapaio.ml.classifier.ClassifierResult;
import rapaio.ml.classifier.bayes.nb.Estimator;
import rapaio.ml.classifier.bayes.nb.Prior;
import rapaio.ml.classifier.bayes.nb.PriorMLE;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.ListParam;
import rapaio.ml.common.ValueParam;
import rapaio.printer.Printable;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mixed Naive Bayes Classifier.
 * <p>
 *
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NaiveBayes extends AbstractClassifierModel<NaiveBayes, ClassifierResult> implements Printable {

    public static NaiveBayes newModel() {
        return new NaiveBayes();
    }

    private static final long serialVersionUID = -7602854063045679683L;

    // algorithm parameters

    public final ValueParam<Prior, NaiveBayes> prior = new ValueParam<>(this, new PriorMLE(),
            "prior",
            "Prior maximum likelihood estimator used");

    public final ListParam<Estimator, NaiveBayes> estimators = new ListParam<>(this, List.of(),
            "estimators",
            "Naive base estimators",
            (existentValues, newValues) -> {
                Set<String> varNames = new HashSet<>();
                for (Estimator e : existentValues) {
                    varNames.addAll(e.getTestNames());
                }
                for (Estimator e : newValues) {
                    for (String testVarName : e.getTestNames()) {
                        if (varNames.contains(testVarName)) {
                            return false;
                        }
                    }
                    varNames.addAll(e.getTestNames());
                }
                return true;
            });

    @Override
    public NaiveBayes newInstance() {
        return new NaiveBayes().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "NaiveBayes";
    }

    @Override
    public String fullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(name()).append("{prior=").append(prior.get().fittedName()).append(",");
        sb.append("estimators=[")
                .append(estimators.get().stream().map(Estimator::fittedName).collect(Collectors.joining(",")))
                .append("]}");
        return sb.toString();
    }

    @Override
    public Capabilities capabilities() {
        return Capabilities.builder()
                .minInputCount(0)
                .maxInputCount(1_000_000)
                .inputTypes(Arrays.asList(VType.NOMINAL, VType.DOUBLE, VType.INT, VType.BINARY))
                .minTargetCount(1)
                .maxTargetCount(1)
                .targetType(VType.NOMINAL)
                .targetType(VType.BINARY)
                .allowMissingTargetValues(false)
                .allowMissingInputValues(true)
                .build();
    }

    @Override
    protected FitSetup prepareFit(Frame df, Var weights, String... targetVars) {
        List<String> targets = VRange.of(targetVars).parseVarNames(df);
        this.targetNames = targets.toArray(new String[0]);
        this.targetTypes = targets.stream().map(name -> df.rvar(name).type()).toArray(VType[]::new);
        this.targetLevels = new HashMap<>();
        this.targetLevels.put(firstTargetName(), df.rvar(firstTargetName()).levels());

        HashSet<String> targetSet = new HashSet<>(targets);
        HashSet<String> allVarsSet = new HashSet<>(Arrays.asList(df.varNames()));

        String[] inputs = estimators.get().stream().flatMap(e -> e.getTestNames().stream()).toArray(String[]::new);
        for (String inputVar : inputs) {
            if (targetSet.contains(inputVar)) {
                throw new IllegalStateException("Input variable: " + inputVar + " is also a target variable.");
            }
            if (!allVarsSet.contains(inputVar)) {
                throw new IllegalStateException("Input variable: " + inputVar + " is not contained in training data frame.");
            }
        }
        this.inputNames = inputs;
        this.inputTypes = Arrays.stream(inputNames).map(name -> df.rvar(name).type()).toArray(VType[]::new);

        capabilities().checkAtLearnPhase(df, weights, targetVars);
        return FitSetup.valueOf(df, weights, targetVars);
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {

        // build priors

        prior.get().fitPriors(df, weights, firstTargetName());

        // build conditional probabilities

        for (Estimator estimator : estimators.get()) {
            boolean fitted = estimator.fit(df, weights, firstTargetName());
            if (!fitted) {
                String message = "Estimator: " + estimator.fittedName() + " cannot be fitted.";
                throw new IllegalStateException(message);
            }
        }
        return true;
    }

    @Override
    protected ClassifierResult corePredict(Frame df, final boolean withClasses, final boolean withDensities) {

        ClassifierResult pred = ClassifierResult.build(this, df, withClasses, withDensities);
        IntStream.range(0, df.rowCount()).parallel().forEach(
                i -> {
                    DensityVector<String> dv = DensityVector.emptyByLabels(false, firstTargetLevels());
                    for (int j = 1; j < firstTargetLevels().size(); j++) {
                        double sumLog = Math.log(prior.get().computePrior(firstTargetLevel(j)));

                        for (Estimator estimator : estimators.get()) {
                            sumLog += Math.log(estimator.predict(df, i, firstTargetLevel(j)));
                        }
                        dv.increment(firstTargetLevel(j), Math.exp(sumLog));
                    }
                    dv.normalize();

                    if (withClasses) {
                        pred.firstClasses().setLabel(i, dv.findBestLabel());
                    }
                    if (withDensities) {
                        for (int j = 1; j < firstTargetLevels().size(); j++) {
                            pred.firstDensity().setDouble(i, j, dv.get(firstTargetLevel(j)));
                        }
                    }
                });
        return pred;
    }

    @Override
    public String toSummary(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(name()).append(" model\n");
        sb.append("================\n\n");

        sb.append(capabilitiesSummary());

        if (!hasLearned()) {
            sb.append("Model not fitted.\n\n");
            sb.append("Prior: ").append(prior.get().name()).append("\n");
            sb.append("Estimators: \n");
            for (Estimator estimator : estimators.get()) {
                sb.append("\t- ").append(estimator.fittedName()).append("\n");
            }
        } else {
            sb.append("Model is fitted.\n\n");

            sb.append(inputVarsSummary(printer, options));
            sb.append(targetVarsSummary());

            sb.append("Prior: ").append(prior.get().fittedName()).append("\n");
            sb.append("Estimators: \n");
            for (Estimator estimator : estimators.get()) {
                sb.append("\t- ").append(estimator.fittedName()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toContent(Printer printer, POption<?>... options) {
        return toSummary(printer, options);
    }

    @Override
    public String toFullContent(Printer printer, POption<?>... options) {
        return toSummary(printer, options);
    }

    @Override
    public String toString() {
        return fullName();
    }
}
