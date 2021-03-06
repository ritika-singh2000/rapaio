package rapaio;

import rapaio.core.RandomSource;
import rapaio.core.distributions.Normal;
import rapaio.data.VarBinary;
import rapaio.data.VarDouble;
import rapaio.data.VarInt;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/25/19.
 */
public final class DataTestingTools {

    public static VarDouble generateRandomDoubleVariable(int len, double nonMissing) {
        Normal normal = Normal.std();
        return VarDouble.from(len, row -> {
            if(RandomSource.nextDouble() < nonMissing) {
                double value = normal.sampleNext();
                return value + Math.signum(value)*2;
            }
            return VarDouble.MISSING_VALUE;
        });
    }

    public static VarInt generateRandomIntVariable(int len, int from, int to, double nonMissing) {
        return VarInt.from(len, row -> RandomSource.nextDouble() < nonMissing ? RandomSource.nextInt(to - from) + from : VarInt.MISSING_VALUE);
    }

    public static VarBinary generateRandomBinaryVariable(int len, double nonMissing) {
        return VarBinary.from(len, row -> RandomSource.nextDouble() < nonMissing ? RandomSource.nextDouble() <= 0.5 : null);
    }
}
