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

package rapaio.experiment;

import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.Var;
import rapaio.data.VarNominal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides filter for frames.
 * <p>
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
@Deprecated
public final class ExperimentalTools implements Serializable {

    private static final long serialVersionUID = 3213473753038938378L;

    private ExperimentalTools() {
    }


    /**
     * Consolidates all the common nominal columns from all frames given as parameter.
     *
     * @param source list of source frames
     * @return list of frames with common nominal columns consolidated
     */
    public static List<Frame> consolidate(List<Frame> source) {

        // predict reunion of labels for all columns
        HashMap<String, ArrayList<String>> dicts = new HashMap<>();
        for (int i = 0; i < source.size(); i++) {
            for (Frame frame : source) {
                for (String colName : frame.varNames()) {
                    if (!frame.rvar(colName).type().isNominal()) {
                        continue;
                    }
                    if (!dicts.containsKey(colName)) {
                        dicts.put(colName, new ArrayList<>());
                    }
                    dicts.get(colName).addAll(frame.rvar(colName).levels());
                }
            }
        }

        // rebuild each frame according with the new consolidated data
        List<Frame> dest = new ArrayList<>();
        for (Frame frame : source) {
            Var[] vars = new Var[frame.varCount()];
            for (int i = 0; i < frame.varCount(); i++) {
                Var v = frame.rvar(i);
                String colName = frame.varNames()[i];
                if (!v.type().isNominal()) {
                    vars[i] = v;
                } else {
                    vars[i] = VarNominal.empty(v.rowCount(), dicts.get(colName)).withName(colName);
                    for (int k = 0; k < vars[i].rowCount(); k++) {
                        vars[i].setLabel(k, v.getLabel(k));
                    }
                }
            }
            dest.add(SolidFrame.byVars(frame.rowCount(), vars));
        }

        return dest;
    }
}
