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
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.mf.Factorization;
import org.ranksys.recommenders.mf.Factorizer;
import org.ranksys.recommenders.mf.als.HKVFactorizer;
import org.ranksys.recommenders.mf.als.PZTFactorizer;
import org.ranksys.recommenders.mf.plsa.PLSAFactorizer;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FactorizationFactory implements MehtaFactory<Factorization<String, String>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public FactorizationFactory(
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public Optional<Factorization<String, String>> create(MehtaParameters params) {

        Supplier<Double> reg = () -> params.getDouble("reg", 1.0);
        Supplier<Double> alpha = () -> params.getDouble("alpha", 1.0);

        Optional<Factorizer<String, String>> factorizer;
        switch (params.name()) {
            case "hkv":
                double a1 = alpha.get();
                factorizer = Optional.of(new HKVFactorizer<>(reg.get(), x -> 1 + a1 * x, params.getInt("numIter", 20)));
                break;
            case "pzt":
                double a2 = alpha.get();
                factorizer = Optional.of(new PZTFactorizer<>(reg.get(), x -> 1 + a2 * x, params.getInt("numIter", 20)));
                break;
            case "plsa":
                factorizer = Optional.of(new PLSAFactorizer<>(params.getInt("numIter", 100)));
                break;
            default:
                factorizer = Optional.empty();
                break;
        }

        return factorizer.map(f -> f.factorize(params.getInt("k", 50), tpp.get()));
    }

}
