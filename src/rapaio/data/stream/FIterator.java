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

package rapaio.data.stream;

import rapaio.data.Frame;
import rapaio.data.MappedFrame;
import rapaio.data.Mapping;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
@Deprecated
public class FIterator implements Iterator<FSpot>, Serializable {
    private static final String DEFAULT_KEY = "$$$DEFAULT$$$";
    private final Iterator<FSpot> it;
    private final Map<String, List<FSpot>> map = new HashMap<>();
    private FSpot current;

    public FIterator(Iterator<FSpot> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public FSpot next() {
        current = it.next();
        return current;
    }

    @Override
    public void remove() {
        it.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super FSpot> action) {
        it.forEachRemaining(action);
    }

    public void collect() {
        collect(DEFAULT_KEY);
    }

    public void collect(String key) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(current);
    }

    public Frame mappedFrame() {
        return mappedFrame(DEFAULT_KEY);
    }

    public Frame mappedFrame(String key) {
        List<FSpot> spots = map.get(key);
        if (spots.isEmpty()) return null;
        final Mapping mapping = Mapping.newCopyOf(spots.stream().map(FSpot::row).mapToInt(row -> row).toArray());
        final Frame df = spots.stream().findFirst().get().getFrame();
        return MappedFrame.newByRow(df, mapping);
    }
}