/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.recommender;

import com.google.inject.Inject;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.novdiv.inverted.neighborhood.InvertedUserNeighborhood;
import org.ranksys.recommenders.nn.user.neighborhood.UserNeighborhood;
import org.ranksys.recommenders.nn.user.neighborhood.UserNeighborhoods;
import org.ranksys.recommenders.nn.user.sim.UserSimilarity;

import java.util.Optional;
import java.util.function.Supplier;

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
    public Optional<UserNeighborhood<String>> create(MehtaParameters params) {

        Supplier<Optional<UserSimilarity<String>>> uss = () -> params.subset("sim").flatMap(usf::create);

        Optional<UserNeighborhood<String>> userNeighborhood;
        switch (params.name()) {
            case "knn":
                userNeighborhood = uss.get().map(us -> UserNeighborhoods.topK(us, params.getInt("k", 100)));
                break;
            case "threshold":
                userNeighborhood = uss.get().flatMap(us -> params.getDouble("t").map(t -> UserNeighborhoods.threshold(us, t)));
                break;
            case "inverted":
                userNeighborhood = uss.get()
                        .map(us -> UserNeighborhoods.topK(us, params.getInt("k", 100)))
                        .map(un -> new InvertedUserNeighborhood<>(un, users::containsUser));
                break;
            default:
                userNeighborhood = Optional.empty();
        }

        boolean cached = params.getBoolean("cached", false) && !params.name().equals("inverted");

        return userNeighborhood.map(un -> cached ? UserNeighborhoods.cached(un) : un);
    }

}
