package safemate.service;

import org.springframework.stereotype.Service;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.regression.LinearModel;
import smile.regression.OLS;

@Service
public class AIService {

    private LinearModel olsModel;   // <- ya no es OLS directamente
    private String[] featureNames;

    public void train(double[][] x, double[] y, String[] featureNames) {
        if (featureNames.length != x[0].length) {
            throw new IllegalArgumentException("El número de features no coincide con las columnas de X.");
        }

        this.featureNames = featureNames;

        DataFrame df = DataFrame.of(x, featureNames);
        df = df.merge(DoubleVector.of("target", y));

        Formula formula = Formula.lhs("target");

        // OLS.fit devuelve LinearModel en SMILE 3.x
        this.olsModel = OLS.fit(formula, df);
    }

    public double predict(double[] features) {
        if (olsModel == null) {
            throw new IllegalStateException("El modelo aún no ha sido entrenado.");
        }
        if (features.length != featureNames.length) {
            throw new IllegalArgumentException("El número de features no coincide con el modelo entrenado.");
        }

        // Aquí usamos directamente el array
        return olsModel.predict(features);
    }
}
