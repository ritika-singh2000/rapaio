/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
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

package rapaio.data.filter;

import rapaio.data.Var;
import rapaio.data.stream.VSpot;

import java.util.function.Function;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 12/4/14.
 */
@Deprecated
public class VFAbstractUpdateValue extends VFAbstract {

    private final Function<VSpot, Double> f;

    public VFAbstractUpdateValue(Function<VSpot, Double> f) {
        this.f = f;
    }

    @Override
    public void fit(Var... vars) {
        checkSingleVar(vars);
    }

    @Override
    public Var apply(Var... vars) {
        checkSingleVar(vars);
        vars[0].spotStream().forEach(s -> s.setValue(f.apply(s)));
        return vars[0];
    }
}
