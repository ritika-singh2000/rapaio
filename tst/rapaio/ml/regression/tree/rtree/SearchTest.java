package rapaio.ml.regression.tree.rtree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.filter.VRefSort;
import rapaio.datasets.Datasets;
import rapaio.ml.regression.tree.RTree;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 7/20/19.
 */
public class SearchTest {

    private static final String TARGET = "humidity";
    private static final String NUM_TEST = "temp";
    private static final String NOM_TEST = "outlook";
    private Frame df;
    private Var w;
    private RTree tree;

    @BeforeEach
    void setUp() {
        df = Datasets.loadPlay();
        w = VarDouble.fill(df.rowCount(), 1);
        tree = RTree.newDecisionStump();
    }

    @Test
    void ignoreTest() {

        Search m = Search.Ignore;
        Optional<Candidate> cs = m.computeCandidate(tree, df, w, NOM_TEST, TARGET);
        assertFalse(cs.isPresent());
    }

    @Test
    void nominalFullTest() {

        Search m = Search.NominalFull;
        Optional<Candidate> cs = m.computeCandidate(tree, df, w, NOM_TEST, TARGET);

        assertTrue(cs.isPresent());

        Candidate c = cs.get();
        assertEquals(NOM_TEST, c.getTestName());

        assertEquals(3, c.getGroupNames().size());
        assertEquals(3, c.getGroupPredicates().size());

        assertEquals("outlook = 'sunny'", c.getGroupNames().get(0));
        assertEquals("outlook = 'overcast'", c.getGroupNames().get(1));
        assertEquals("outlook = 'rain'", c.getGroupNames().get(2));

        assertEquals(4.432653061224499, c.getScore(), 1e-20);
    }

    @Test
    void nominalFullTestFailed() {
        Optional<Candidate> cs = Search.NominalFull.computeCandidate(tree, df.mapRows(1), w.mapRows(1), NOM_TEST, TARGET);
        assertFalse(cs.isPresent());
    }

    @Test
    void nominalBinaryTest() {
        Optional<Candidate> cs = Search.NominalBinary.computeCandidate(tree, df, w, NOM_TEST, TARGET);
        assertTrue(cs.isPresent());
        assertEquals("Candidate{score=4.318367346938771, testName='outlook', groupNames=[outlook = 'overcast', outlook != 'overcast']}",
                cs.get().toString());
    }

    @Test
    void numericBinaryTest() {
        Var target = df.rvar(TARGET).fapply(new VRefSort(df.rvar(NUM_TEST).refComparator()));
        Var test = df.rvar(NUM_TEST).fapply(new VRefSort(df.rvar(NUM_TEST).refComparator()));
        Var weights = w.fapply(new VRefSort(df.rvar(NUM_TEST).refComparator()));

        Optional<Candidate> c = Search.NumericBinary.computeCandidate(tree, df, w, NUM_TEST, TARGET);

        assertTrue(c.isPresent());
        assertEquals(32.657653061224515, c.get().getScore(), 1e-12);
        assertEquals("temp", c.get().getTestName());
        assertEquals(2, c.get().getGroupNames().size());
        assertEquals("temp <= 69.5", c.get().getGroupNames().get(0));
        assertEquals("temp > 69.5", c.get().getGroupNames().get(1));
    }

}
