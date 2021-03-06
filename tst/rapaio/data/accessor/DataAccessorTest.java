package rapaio.data.accessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.data.VarDouble;
import rapaio.data.VarInt;
import rapaio.data.VarLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 9/18/18.
 */
public class DataAccessorTest {

    private static final double TOL = 1e-20;

    @BeforeEach
    void setUp() {
        RandomSource.setSeed(1234);
    }

    @Test
    void testVarDoubleDataAccessor() {
        VarDouble x = VarDouble.from(100, RandomSource::nextDouble);

        double[] data = x.elements();
        for (int i = 0; i < x.rowCount(); i++) {
            data[i] = 1.;
        }
        x.setElements(data, 10);

        assertEquals(10, x.rowCount());
        for (int i = 0; i < x.rowCount(); i++) {
            assertEquals(1.0, x.getDouble(i), TOL);
        }
    }

    @Test
    void testVarLongDataAccessor() {
        VarLong x = VarLong.from(100, () -> (long) RandomSource.nextDouble() * 100);

        long[] data = x.getArray();
        int rows = x.rowCount();
        for (int i = 0; i < rows; i++) {
            data[i] = 10L;
        }
        x.setArray(data, 10);

        assertEquals(10, x.rowCount());
        for (int i = 0; i < 10; i++) {
            assertEquals(10L, x.getLong(i));
        }
    }

    @Test
    void testVarIntDataAccessor() {
        VarInt x = VarInt.from(100, row -> (int) RandomSource.nextDouble() * 100);

        int[] data = x.elements();
        int rows = x.rowCount();
        for (int i = 0; i < rows; i++) {
            data[i] = 10;
        }
        x.setElements(data, 10);

        assertEquals(10, x.rowCount());
        for (int i = 0; i < 10; i++) {
            assertEquals(10L, x.getInt(i));
        }
    }
}
