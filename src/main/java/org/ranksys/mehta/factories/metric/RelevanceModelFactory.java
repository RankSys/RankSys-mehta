/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.metric;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.metrics.basic.NDCG;
import org.ranksys.metrics.rel.BackgroundBinaryRelevanceModel;
import org.ranksys.metrics.rel.BinaryRelevanceModel;
import org.ranksys.metrics.rel.NoRelevanceModel;
import org.ranksys.metrics.rel.RelevanceModel;
import org.ranksys.novdiv.intentaware.metrics.ERRIA;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RelevanceModelFactory implements MehtaFactory<RelevanceModel<String, String>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public RelevanceModelFactory(
            @Named("testPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public Optional<RelevanceModel<String, String>> create(MehtaParameters params) {
        
        Supplier<Double> l = () -> params.getDouble("threshold", 1.0);

        Optional<RelevanceModel<String, String>> relModel;
        switch (params.name()) {
            case "bin":
                relModel = Optional.of(new BinaryRelevanceModel<>(true, tpp.get(), l.get()));
                break;
            case "none":
                relModel = Optional.of(new NoRelevanceModel<>());
                break;
            case "bck":
                relModel = params.getDouble("background").map(b -> new BackgroundBinaryRelevanceModel<>(true, tpp.get(), l.get(), b));
                break;
            case "ndcg":
                relModel = Optional.of(new NDCG.NDCGRelevanceModel<>(true, tpp.get(), l.get()));
                break;
            case "err":
                relModel = Optional.of(new ERRIA.ERRRelevanceModel<>(true, tpp.get(), l.get()));
                break;
            default:
                relModel = Optional.empty();
                break;
        }

        relModel.ifPresent(RelevanceModel::initialize);

        return relModel;
    }
}
