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

package rapaio.ml.classifier;

import rapaio.data.Frame;
import rapaio.data.Nominal;
import rapaio.data.SolidFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class CPrediction {

    private final int rows;
    private final boolean withClasses;
    private final boolean withDensities;
    private final List<String> targetVars = new ArrayList<>();
    private final Map<String, String[]> dictionaries = new HashMap<>();
    private final Map<String, Nominal> classes = new HashMap<>();
    private final Map<String, Frame> densities = new HashMap<>();

    // builder

    public static CPrediction newEmpty(int rows, boolean withClasses, boolean withDensities) {
        return new CPrediction(rows, withClasses, withDensities);
    }

    // private constructor

    private CPrediction(int rows, boolean withClasses, boolean withDensities) {
        this.rows = rows;
        this.withClasses = withClasses;
        this.withDensities = withDensities;
    }

    public void addTarget(String target, String[] dictionary) {
        targetVars.add(target);
        dictionaries.put(target, dictionary);
        if (withClasses) {
            classes.put(target, Nominal.newEmpty(rows, dictionary));
        }
        if (withDensities) {
            densities.put(target, SolidFrame.newMatrix(rows, dictionary));
        }
    }

    public int getRows() {
        return rows;
    }

    public boolean isWithClasses() {
        return withClasses;
    }

    public boolean isWithDensities() {
        return withDensities;
    }

    /**
     * Returns target variables built at learning time
     *
     * @return target variable names
     */
    public String[] targetVars() {
        return targetVars.toArray(new String[targetVars.size()]);
    }

    /**
     * Returns first target variable built at learning time
     *
     * @return target variable names
     */
    public String firstTargetVar() {
        return targetVars.get(0);
    }

    /**
     * Returns dictionaries used at learning times for target variables
     *
     * @return map with target variable names as key and dictionaries as variables
     */
    public Map<String, String[]> dictionaries() {
        return dictionaries;
    }

    /**
     * Returns dictionary used at learning times for first target variables
     *
     * @return map with target variable names as key and dictionaries as variables
     */
    public String[] firstDictionary() {
        return dictionaries.get(firstTargetVar());
    }

    /**
     * Returns dictionary used at learning time for the given target variable
     */
    public String[] dictionary(String targetVar) {
        return dictionaries.get(targetVar);
    }

    /**
     * Returns predicted target classes for each target variable name
     *
     * @return map with nominal variables as predicted classes
     */
    public Map<String, Nominal> classes() {
        return classes;
    }

    /**
     * Returns predicted target classes for first target variable name
     *
     * @return nominal variable with predicted classes
     */
    public Nominal firstClasses() {
        return classes.get(firstTargetVar());
    }

    /**
     * Returns predicted target classes for given target variable name
     *
     * @param targetVar given target variable name
     * @return nominal variable with predicted classes
     */
    public Nominal classes(String targetVar) {
        return classes.get(targetVar);
    }

    /**
     * Returns predicted class densities frame if is computed,
     * otherwise returns null.
     *
     * @return predicted class densities (frame with one
     * column for each target class, including missing value)
     */
    public Map<String, Frame> densities() {
        return densities;
    }

    /**
     * Returns predicted class density for the first target variable if is computed,
     * otherwise returns null.
     *
     * @return predicted class densities (frame with one
     * column for each target class, including missing value)
     */
    public Frame firstDensity() {
        return densities().get(firstTargetVar());
    }

    /**
     * Returns predicted class density for the given variable if densities are computed,
     * otherwise returns null.
     *
     * @param targetVar given target variable name
     * @return map of frames for each target class, with frames having variables for each
     * classification label
     */
    public Frame density(final String targetVar) {
        return densities.get(targetVar);
    }
}
