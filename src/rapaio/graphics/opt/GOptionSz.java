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

package rapaio.graphics.opt;

import rapaio.data.NumericVar;
import rapaio.data.Var;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 9/14/17.
 */
public class GOptionSz implements GOption<Var> {

    private static final long serialVersionUID = 6568267641815981670L;
    private final Var size;

    public GOptionSz(Var size) {
        this.size = size;
    }

    public GOptionSz(double size) {
        this.size = NumericVar.scalar(size);
    }

    @Override
    public void bind(GOpts opts) {
        opts.setSz(this);
    }

    @Override
    public Var apply(GOpts opts) {
        return size;
    }
}