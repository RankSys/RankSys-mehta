/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.recommender;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import static java.lang.Double.NaN;
import java.util.function.Supplier;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.mf.Factorization;
import org.ranksys.recommenders.mf.Factorizer;
import org.ranksys.recommenders.mf.als.HKVFactorizer;
import org.ranksys.recommenders.mf.als.PZTFactorizer;
import org.ranksys.recommenders.mf.plsa.PLSAFactorizer;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FactorizationSupplierFactory implements MehtaFactory<Supplier<Factorization<String, String>>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public FactorizationSupplierFactory(
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public Supplier<Factorization<String, String>> create(MehtaParameters params) throws Exception {
        Factorizer<String, String> factorizer;

        double reg = params.getDouble("reg", NaN);
        double alpha = params.getDouble("alpha", NaN);
        int numIter = params.getInt("numIter");
        int K = params.getInt("k", 50);

        switch (params.name()) {
            case "hkv":
                factorizer = new HKVFactorizer<>(reg, x -> 1 + alpha * x, numIter);
                break;
            case "pzt":
                factorizer = new PZTFactorizer<>(reg, x -> 1 + alpha * x, numIter);
                break;
            case "plsa":
                factorizer = new PLSAFactorizer<>(numIter);
                break;
            default:
                factorizer = null;
                break;
        }

        if (factorizer == null) {
            return () -> null;
        } else {
            return () -> factorizer.factorize(K, tpp.get());
        }
    }

}
