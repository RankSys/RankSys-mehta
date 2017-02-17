/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.recommender;

import com.google.inject.Inject;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.novdiv.inverted.neighborhood.InvertedItemNeighborhood;
import org.ranksys.recommenders.nn.item.neighborhood.ItemNeighborhood;
import org.ranksys.recommenders.nn.item.neighborhood.ItemNeighborhoods;
import org.ranksys.recommenders.nn.item.sim.ItemSimilarity;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class ItemNeighborhoodFactory implements MehtaFactory<ItemNeighborhood<String>> {

    private final ItemSimilarityFactory isf;

    @Inject
    public ItemNeighborhoodFactory(ItemSimilarityFactory isf) {
        this.isf = isf;
    }

    @Override
    public Optional<ItemNeighborhood<String>> create(MehtaParameters params) {

        Supplier<Optional<ItemSimilarity<String>>> iss = () -> params.subset("sim").flatMap(isf::create);

        Optional<ItemNeighborhood<String>> itemNeighborhood;
        switch (params.name()) {
            case "knn":
                itemNeighborhood = iss.get().map(is -> ItemNeighborhoods.topK(is, params.getInt("k", 10)));
                break;
            case "threshold":
                itemNeighborhood = iss.get().flatMap(is -> params.getDouble("t").map(t -> ItemNeighborhoods.threshold(is, t)));
                break;
            case "inverted":
                itemNeighborhood = iss.get()
                        .map(is -> ItemNeighborhoods.topK(is, params.getInt("k", 10)))
                        .map(InvertedItemNeighborhood::new);
                break;
            default:
                itemNeighborhood = Optional.empty();
        }

        boolean cached = params.getBoolean("cached", false) && !params.name().equals("inverted");

        return itemNeighborhood.map(in -> cached ? ItemNeighborhoods.cached(in) : in);
    }

}
