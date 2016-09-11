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
import java.util.function.Supplier;
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.fm.PreferenceFM;
import org.ranksys.recommenders.fm.learner.BPRLearner;
import org.ranksys.recommenders.fm.learner.PreferenceFMLearner;
import org.ranksys.recommenders.fm.learner.RMSELearner;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FMSupplierFactory implements MehtaFactory<Supplier<PreferenceFM<String, String>>> {

    private final FastUserIndex<String> users;
    private final FastItemIndex<String> items;
    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public FMSupplierFactory(
            FastUserIndex<String> users, FastItemIndex<String> items,
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.users = users;
        this.items = items;
        this.tpp = tpp;
    }

    @Override
    public Supplier<PreferenceFM<String, String>> create(MehtaParameters params) throws Exception {
        PreferenceFMLearner<String, String> learner;

        double learnRate = params.getDouble("learnRate", 0.01);
        int numIter = params.getInt("numIter", 50);
        double regB = params.getDouble("regB", 0.01);
        double regW = params.getDouble("regW", 0.01);
        double regM = params.getDouble("regM", 0.01);
        double negativeProp = params.getDouble("negativeProp", 2.0);
        int K = params.getInt("k", 100);
        double sdev = params.getDouble("sdev", 0.1);

        switch (params.name()) {
            case "rmse":
                learner = new RMSELearner<>(learnRate, numIter, regB, regW, regM, negativeProp, users, items);
                break;
            case "bpr":
                learner = new BPRLearner<>(learnRate, numIter, regW, regM, users, items);
                break;
            default:
                learner = null;
        }

        if (learner == null) {
            return () -> null;
        } else {
            return () -> learner.learn(tpp.get(), K, sdev);
        }
    }

}
