package rapaio.ml.classifier.bayes.nb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.VType;
import rapaio.data.VarBinary;
import rapaio.data.VarDouble;
import rapaio.data.VarNominal;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/5/20.
 */
public class KernelEstimatorTest {

    private static final double TOL = 1e-12;
    private Frame df;

    @BeforeEach
    void beforeEach() {
        VarNominal target = VarNominal.copy("a", "a", "a", "a", "a", "b", "b", "b", "b", "b").withName("t");
        VarDouble x = VarDouble.copy(10, 11, 12, 11, 13, 2, 3, 4, 3, 2).withName("x");
        VarDouble y = VarDouble.copy(9, 10, 11, 12, 11, 13, 2, 3, 4, 3).withName("y");
        df = SolidFrame.byVars(x, y, target);
    }

    @Test
    void testNaming() {
        KernelEstimator estim = KernelEstimator.forName("x");
        assertEquals("Kernel{test=x}", estim.name());

        assertEquals("Kernel{test=x, kdes=[]}", estim.fittedName());
    }

    @Test
    void testNewInstance() {
        KernelEstimator e = KernelEstimator.forName("x");
        e.fit(df, VarDouble.fill(df.rowCount(), 1), "t");

        assertEquals("Kernel{test=x, kdes=[a:{kfun=KFuncGaussian,bw=0.8759585},b:{kfun=KFuncGaussian,bw=0.6427778}]}", e.fittedName());

        Estimator copy = e.newInstance();
        assertNotNull(copy);
        assertEquals("Kernel{test=x, kdes=[]}", copy.fittedName());
    }

    @Test
    void testBuilders() {
        VarDouble d1 = VarDouble.empty().withName("d1");
        VarDouble d2 = VarDouble.empty().withName("d2");

        VarBinary b1 = VarBinary.empty().withName("b1");
        VarBinary b2 = VarBinary.empty().withName("b2");

        VarNominal n1 = VarNominal.empty(0).withName("n1");
        VarNominal n2 = VarNominal.empty(0).withName("n2");


        assertEquals(Arrays.asList("d1", "d2"), KernelEstimator.forType(SolidFrame.byVars(d1, d2, b1, b2, n1, n2), VType.DOUBLE)
                .stream().flatMap(v -> v.getTestNames().stream()).collect(Collectors.toList()));
        assertEquals(Arrays.asList("d1", "d2"), KernelEstimator.forType(SolidFrame.byVars(d1, d2, b1, b2, n1, n2), VType.DOUBLE)
                .stream().flatMap(v -> v.getTestNames().stream()).collect(Collectors.toList()));

        assertEquals("n1", KernelEstimator.forName("n1").getTestNames().get(0));
    }

    @Test
    void testFit() {
        KernelEstimator estimator1 = KernelEstimator.forName("x");
        estimator1.fit(df, VarDouble.fill(df.rowCount(), 1), "t");

        assertEquals(Collections.singletonList("x"), estimator1.getTestNames());
        assertEquals("Kernel{test=x, kdes=[a:{kfun=KFuncGaussian,bw=0.8759585},b:{kfun=KFuncGaussian,bw=0.6427778}]}", estimator1.fittedName());

        VarDouble pred1 = VarDouble.from(df.rowCount(), row -> estimator1.predict(df, row, "a"));
        assertTrue(pred1.getDouble(0) < pred1.getDouble(1));
        assertEquals(pred1.getDouble(1), pred1.getDouble(3), TOL);


        KernelEstimator estimator2 = KernelEstimator.forName("y");
        estimator2.fit(df, VarDouble.fill(df.rowCount(), 1), "t");

        assertEquals(Collections.singletonList("y"), estimator2.getTestNames());
        assertEquals("Kernel{test=y, kdes=[a:{kfun=KFuncGaussian,bw=0.8759585},b:{kfun=KFuncGaussian,bw=3.4784743}]}", estimator2.fittedName());

        assertEquals(Double.NaN, estimator2.predict(df, 0, "inexistent"), TOL);
    }

}
