package rapaio.data.filter;

import rapaio.data.Frame;
import rapaio.data.VRange;
import rapaio.data.Var;
import rapaio.ml.classifier.ClassifierModel;
import rapaio.ml.classifier.ClassifierResult;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/12/19.
 */
public class FImputeClassifier extends AbstractFFilter {

    public static FImputeClassifier of(ClassifierModel model, VRange inputVars, String targetName) {
        return new FImputeClassifier(model, inputVars, targetName);
    }

    private static final long serialVersionUID = 7428989420235407246L;

    private final ClassifierModel model;
    private final String targetName;

    private FImputeClassifier(ClassifierModel model, VRange inputVars, String targetName) {
        super(inputVars);
        this.model = model;
        this.targetName = targetName;
    }

    @Override
    protected void coreFit(Frame df) {
        var selection = df.mapVars(varNames).stream().filter(s -> !s.isMissing(targetName)).toMappedFrame().copy();
        model.fit(selection, targetName);
    }

    @Override
    public Frame apply(Frame df) {
        var toFill = df.stream().filter(s -> s.isMissing(targetName)).toMappedFrame();
        ClassifierResult result = model.predict(toFill, true, false);
        Var prediction = result.firstClasses();
        for (int i = 0; i < prediction.rowCount(); i++) {
            toFill.setLabel(i, targetName, prediction.getLabel(i));
        }
        return df;
    }

    @Override
    public FImputeClassifier newInstance() {
        return new FImputeClassifier(model.newInstance(), vRange, targetName);
    }
}
