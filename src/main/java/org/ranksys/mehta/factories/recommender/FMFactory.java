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
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.fm.PreferenceFM;
import org.ranksys.recommenders.fm.learner.BPRLearner;
import org.ranksys.recommenders.fm.learner.PreferenceFMLearner;
import org.ranksys.recommenders.fm.learner.RMSELearner;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FMFactory implements MehtaFactory<PreferenceFM<String, String>> {

    private final FastUserIndex<String> users;
    private final FastItemIndex<String> items;
    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public FMFactory(
            FastUserIndex<String> users, FastItemIndex<String> items,
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.users = users;
        this.items = items;
        this.tpp = tpp;
    }

    @Override
    public Optional<PreferenceFM<String, String>> create(MehtaParameters params) {

        Supplier<Double> learnRate = () -> params.getDouble("learnRate", 0.01);
        Supplier<Integer> numIter = () -> params.getInt("numIter", 50);
        Supplier<Double> regB = () -> params.getDouble("regB", 0.01);
        Supplier<Double> regW = () -> params.getDouble("regW", 0.01);
        Supplier<Double> regM = () -> params.getDouble("regM", 0.01);

        Optional<PreferenceFMLearner<String, String>> learner;
        switch (params.name()) {
            case "rmse":
                double negativeProp = params.getDouble("negativeProp", 2.0);
                learner = Optional.of(new RMSELearner<>(learnRate.get(), numIter.get(), regB.get(), regW.get(), regM.get(), negativeProp, users, items));
                break;
            case "bpr":
                learner = Optional.of(new BPRLearner<>(learnRate.get(), numIter.get(), regW.get(), regM.get(), users, items));
                break;
            default:
                learner = Optional.empty();
        }

        return learner.map(l -> l.learn(tpp.get(), params.getInt("k", 100), params.getDouble("sdev", 0.1)));
    }

}
