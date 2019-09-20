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

package rapaio.ml.eval;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import rapaio.core.*;
import rapaio.data.*;
import rapaio.experiment.ml.eval.*;
import rapaio.ml.regression.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static rapaio.sys.WS.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 8/6/19.
 */
public class RegressionEval {

    public static RegressionEval newInstance() {
        return new RegressionEval();
    }

    Frame df;
    String targetName;
    RMetric metric;
    Map<String, RegressionModel> models = new HashMap<>();
    boolean debug = true;

    private RegressionEval() {
    }

    public RegressionEval withFrame(Frame df) {
        this.df = df;
        return this;
    }

    public Frame getFrame() {
        return df;
    }

    public RegressionEval withTargetName(String targetName) {
        this.targetName = targetName;
        return this;
    }

    public String getTargetName() {
        return targetName;
    }

    public RegressionEval withMetric(RMetric metric) {
        this.metric = metric;
        return this;
    }

    public RMetric getMetric() {
        return metric;
    }

    public RegressionEval withModel(String name, RegressionModel model) {
        models.put(name, model);
        return this;
    }

    public Map<String, RegressionModel> getModels() {
        return models;
    }

    public RegressionEval withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    private void validate() {
        Objects.requireNonNull(df);
        Objects.requireNonNull(targetName);
        Objects.requireNonNull(metric);
        Objects.requireNonNull(models);
        if (models.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one regression model to be tested.");
        }
    }

    private void cvValidate(int folds) {
        validate();
        if (folds <= 1) {
            throw new IllegalArgumentException("Number of folds must be greater than 1.");
        }
    }

    public CVResult cv(int folds) {
        cvValidate(folds);

        if (debug)
            print("\nCrossValidation with " + folds + " folds for models: " + String.join(",", models.keySet()) + "\n");

        List<IntList> strata = buildFolds(df, folds);
        CVResult cvResult = new CVResult(this, folds);

        for (int i = 0; i < folds; i++) {

            // build train and test data for current fold
            Mapping trainMapping = Mapping.empty();
            Mapping testMapping = Mapping.empty();
            for (int j = 0; j < folds; j++) {
                if (j == i) {
                    testMapping.addAll(strata.get(j));
                } else {
                    trainMapping.addAll(strata.get(j));
                }
            }
            Frame train = MappedFrame.byRow(df, trainMapping).copy();
            Frame test = MappedFrame.byRow(df, testMapping).copy();

            // iterate through models

            for (Map.Entry<String, RegressionModel> entry : models.entrySet()) {
                String modelId = entry.getKey();
                RegressionModel model = entry.getValue().newInstance();
                model.fit(train, targetName);
                RegressionResult result = model.predict(test);

                double value = metric.compute(test.rvar(targetName), result.firstPrediction());
                cvResult.putScore(modelId, i, value);

                if (debug)
                    print(String.format("model: %s CV %2d:  score=%.6f, mean=%.6f, se=%.6f\n",
                            modelId,
                            i + 1,
                            cvResult.getScore(modelId, i),
                            cvResult.getMean(modelId),
                            cvResult.getSE(modelId)));
            }
        }
        return cvResult;
    }

    private List<IntList> buildFolds(Frame df, int folds) {
        IntList shuffle = new IntArrayList();
        for (int i = 0; i < df.rowCount(); i++) {
            shuffle.add(i);
        }
        Collections.shuffle(shuffle, RandomSource.getRandom());
        List<IntList> foldMap = new ArrayList<>();
        for (int i = 0; i < folds; i++) {
            foldMap.add(new IntArrayList());
        }
        int currentFold = 0;
        for (int row : shuffle) {
            foldMap.get(currentFold).add(row);
            currentFold++;
            if (currentFold == folds) {
                currentFold = 0;
            }
        }
        return foldMap;
    }
}