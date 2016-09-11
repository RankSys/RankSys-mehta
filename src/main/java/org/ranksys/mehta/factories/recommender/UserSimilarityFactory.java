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
import org.ranksys.recommenders.nn.user.sim.UserSimilarity;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.nn.user.sim.UserSimilarities;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class UserSimilarityFactory implements MehtaFactory<UserSimilarity<String>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public UserSimilarityFactory(
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public UserSimilarity<String> create(MehtaParameters params) {
        UserSimilarity<String> userSimilarity;

        double alpha = params.getDouble("alpha", 0.5);
        boolean dense = params.getBoolean("dense", true);

        switch (params.name()) {
            case "cosine":
            case "set-cosine":
                userSimilarity = UserSimilarities.setCosine(tpp.get(), alpha, dense);
                break;
            case "vec-cosine":
                userSimilarity = UserSimilarities.vectorCosine(tpp.get(), dense);
                break;
            case "jaccard":
            case "set-jaccard":
                userSimilarity = UserSimilarities.setJaccard(tpp.get(), dense);
                break;
            case "vec-jaccard":
                userSimilarity = UserSimilarities.vectorJaccard(tpp.get(), dense);
                break;
            default:
                userSimilarity = null;
                break;
        }
        
        return userSimilarity;
    }

}
