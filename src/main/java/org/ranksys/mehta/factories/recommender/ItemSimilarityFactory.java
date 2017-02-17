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
import org.ranksys.recommenders.nn.item.sim.ItemSimilarities;
import org.ranksys.recommenders.nn.item.sim.ItemSimilarity;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class ItemSimilarityFactory implements MehtaFactory<ItemSimilarity<String>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public ItemSimilarityFactory(
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public Optional<ItemSimilarity<String>> create(MehtaParameters params) {

        Supplier<Boolean> dense = () -> params.getBoolean("dense", true);

        switch (params.name()) {
            case "cosine":
            case "set-cosine":
                return Optional.of(ItemSimilarities.setCosine(tpp.get(), params.getDouble("alpha", 0.5), dense.get()));
            case "vec-cosine":
                return Optional.of(ItemSimilarities.vectorCosine(tpp.get(), dense.get()));
            case "jaccard":
            case "set-jaccard":
                return Optional.of(ItemSimilarities.setJaccard(tpp.get(), dense.get()));
            case "vec-jaccard":
                return Optional.of(ItemSimilarities.vectorJaccard(tpp.get(), dense.get()));
            default:
                return Optional.empty();
        }
    }

}
