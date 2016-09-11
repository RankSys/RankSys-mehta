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
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.novdiv.inverted.neighborhood.InvertedItemNeighborhood;
import org.ranksys.recommenders.nn.item.neighborhood.ItemNeighborhood;
import org.ranksys.recommenders.nn.item.neighborhood.ItemNeighborhoods;
import org.ranksys.recommenders.nn.item.sim.ItemSimilarity;

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
    public ItemNeighborhood<String> create(MehtaParameters params) throws Exception {
        ItemNeighborhood<String> itemNeighborhood;

        Supplier<ItemSimilarity<String>> iss = () -> isf.create(params.subset("sim"));
        boolean cached = params.getBoolean("cached", true);

        switch (params.name()) {
            case "knn":
                itemNeighborhood = ItemNeighborhoods.topK(iss.get(), params.getInt("k", 10));
                break;
            case "threshold":
                itemNeighborhood = ItemNeighborhoods.threshold(iss.get(), params.getDouble("t"));
                break;
            case "inverted":
                itemNeighborhood = ItemNeighborhoods.topK(iss.get(), params.getInt("k", 10));
                itemNeighborhood = new InvertedItemNeighborhood<>(itemNeighborhood);
                cached = false;
                break;
            default:
                itemNeighborhood = null;
        }

        if (cached) {
            itemNeighborhood = ItemNeighborhoods.cached(itemNeighborhood);
        }

        return itemNeighborhood;
    }

}
