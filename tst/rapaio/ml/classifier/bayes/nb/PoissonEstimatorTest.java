package rapaio.ml.classifier.bayes.nb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.core.distributions.Poisson;
import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.VRange;
import rapaio.data.VarDouble;
import rapaio.data.VarInt;
import rapaio.data.VarNominal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 3/9/20.
 */
public class PoissonEstimatorTest {

    @BeforeEach
    void beforeEach() {
        RandomSource.setSeed(123);
    }

    @Test
    void testBuilders() {
        var estimator1 = PoissonEstimator.forName("x");
        assertNotNull(estimator1);
        assertEquals("x", estimator1.getTestName());
        assertEquals("Poisson{test=x}", estimator1.name());
        assertEquals("Poisson{test=x, values=[]}", estimator1.fittedName());

        var estimator2 = estimator1.newInstance();
        assertEquals(estimator1.fittedName(), estimator2.fittedName());

        SolidFrame df = SolidFrame.byVars(
                VarInt.seq(0, 10).withName("x"),
                VarInt.seq(0, 10).withName("y"),
                VarInt.seq(0, 10).withName("z")
        );
        var list1 = PoissonEstimator.forRange(df, VRange.all());
        var list2 = PoissonEstimator.forNames("x", "y", "z");

        for (int i = 0; i < 3; i++) {
            assertEquals(list1.get(i).fittedName(), list2.get(i).fittedName());
        }
    }

    @Test
    void testFitPredict() {
        Poisson poisson = Poisson.of(10);
        VarDouble sample = poisson.sample(10_000).withName("x");
        Frame df = SolidFrame.byVars(sample, VarNominal.from(10_000, row -> row % 2 == 0 ? "a" : "b").withName("y"));

        var estim = PoissonEstimator.forName("x");
        estim.fit(df, VarDouble.fill(df.rowCount(), 1), "y");

        assertEquals("Poisson{test=x, values=[{level:a,lambda:9.989},{level:b,lambda:10.006}]}", estim.fittedName());

        var lambdaMap = estim.getLambdaMap();
        assertEquals(2, lambdaMap.size());
        assertTrue(lambdaMap.containsKey("a"));
        assertTrue(lambdaMap.containsKey("b"));

        assertTrue(Math.abs(lambdaMap.get("a").getLambda() - 10) <= 1);
        assertTrue(Math.abs(lambdaMap.get("b").getLambda() - 10) <= 1);

        Frame test = SolidFrame.byVars(VarDouble.copy(1, 2, 3).withName("x"));
        assertEquals(lambdaMap.get("a").pdf(1), estim.predict(test, 0, "a"));
        assertEquals(lambdaMap.get("b").pdf(3), estim.predict(test, 2, "b"));
        assertEquals(1e-100, estim.predict(test, 0, "c"));
    }

    @Test
    void testInvalidVauluesOnFit() {
        VarDouble x = VarDouble.copy(0, -1, 100).withName("x");
        VarNominal y = VarNominal.copy("a", "a", "b").withName("y");
        Frame df = SolidFrame.byVars(x, y);

        assertFalse(PoissonEstimator.forName("x").fit(df, VarDouble.fill(3, 1), "y"));
    }
}
