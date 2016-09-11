/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.recommender;

import com.google.inject.Inject;
import java.util.function.Supplier;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.novdiv.inverted.neighborhood.InvertedUserNeighborhood;
import org.ranksys.recommenders.nn.user.neighborhood.UserNeighborhood;
import org.ranksys.recommenders.nn.user.neighborhood.UserNeighborhoods;
import org.ranksys.recommenders.nn.user.sim.UserSimilarity;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class UserNeighborhoodFactory implements MehtaFactory<UserNeighborhood<String>> {

    private final FastUserIndex<String> users;
    private final UserSimilarityFactory usf;

    @Inject
    public UserNeighborhoodFactory(FastUserIndex<String> users, UserSimilarityFactory usf) {
        this.users = users;
        this.usf = usf;
    }

    @Override
    public UserNeighborhood<String> create(MehtaParameters params) throws Exception {
        UserNeighborhood<String> userNeighborhood;

        Supplier<UserSimilarity<String>> uss = () -> usf.create(params.subset("sim"));
        boolean cached = params.getBoolean("cached", false);

        switch (params.name()) {
            case "knn":
                userNeighborhood = UserNeighborhoods.topK(uss.get(), params.getInt("k", 10));
                break;
            case "threshold":
                userNeighborhood = UserNeighborhoods.threshold(uss.get(), params.getDouble("t"));
                break;
            case "inverted":
                userNeighborhood = UserNeighborhoods.topK(uss.get(), params.getInt("k", 10));
                userNeighborhood = new InvertedUserNeighborhood<>(userNeighborhood, users::containsUser);
                cached = false;
                break;
            default:
                userNeighborhood = null;
        }

        if (cached) {
            userNeighborhood = UserNeighborhoods.cached(userNeighborhood);
        }

        return userNeighborhood;
    }

}
