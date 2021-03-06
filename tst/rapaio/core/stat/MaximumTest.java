package rapaio.core.stat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.data.VarDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/9/18.
 */
public class MaximumTest {

    private static final double TOL = 1e-20;

    @BeforeEach
    void beforeEach() {
        RandomSource.setSeed(123);
    }

    @Test
    void testDouble() {
        VarDouble x = VarDouble.from(100, row -> row % 7 == 0 ? Double.NaN : RandomSource.nextDouble());
        double max = 0.0;
        for (int i = 0; i < x.rowCount(); i++) {
            if (x.isMissing(i)) {
                continue;
            }
            max = Math.max(max, x.getDouble(i));
        }
        Maximum maximum = Maximum.of(x);
        assertEquals(max, maximum.value(), TOL);

        assertEquals("> maximum[?]\n" +
                "total rows: 100 (complete: 85, missing: 15)\n" +
                "maximum: 0.9908989\n", maximum.toSummary());
    }
}
