package rapaio.util.function;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/13/19.
 */
@FunctionalInterface
public interface SSupplier<T> extends Supplier<T>, Serializable {
}
