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
import java.nio.file.Files;
import org.ranksys.recommenders.mf.Factorization;
import org.ranksys.recommenders.mf.rec.MFRecommender;
import org.ranksys.recommenders.nn.item.ItemNeighborhoodRecommender;
import org.ranksys.recommenders.nn.item.neighborhood.ItemNeighborhood;
import org.ranksys.recommenders.nn.user.UserNeighborhoodRecommender;
import org.ranksys.recommenders.nn.user.neighborhood.UserNeighborhood;
import org.ranksys.recommenders.fast.FastRecommender;
import org.ranksys.recommenders.basic.PopularityRecommender;
import org.ranksys.recommenders.basic.RandomRecommender;
import org.ranksys.mehta.config.MehtaParameters;
import java.util.Set;
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.formats.factorization.SimpleFMFormat;
import org.ranksys.formats.factorization.SimpleFactorizationFormat;
import org.ranksys.recommenders.fm.PreferenceFM;
import org.ranksys.recommenders.fm.rec.FMRecommender;
import org.ranksys.mehta.config.MehtaProperties;
import org.ranksys.mehta.factories.MehtaFactory;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RecommenderFactory implements MehtaFactory<FastRecommender<String, String>> {

    private final MehtaProperties properties;
    private final FastUserIndex<String> users;
    private final FastItemIndex<String> items;
    private final Provider<FastPreferenceData<String, String>> tpp;
    private final UserNeighborhoodFactory unf;
    private final ItemNeighborhoodFactory inf;
    private final FactorizationSupplierFactory fsf;
    private final FMSupplierFactory fmsf;

    @Inject
    public RecommenderFactory(
            MehtaProperties properties,
            FastUserIndex<String> users, FastItemIndex<String> items,
            @Named("targetUsers") Set<String> targetUsers,
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp,
            UserNeighborhoodFactory unf, ItemNeighborhoodFactory inf,
            FactorizationSupplierFactory fsf, FMSupplierFactory fmsf) {
        this.properties = properties;
        this.users = users;
        this.items = items;
        this.tpp = tpp;
        this.unf = unf;
        this.inf = inf;
        this.fsf = fsf;
        this.fmsf = fmsf;
    }

    @Override
    public FastRecommender<String, String> create(MehtaParameters params) throws Exception {
        FastRecommender<String, String> recommender;

        switch (params.get("recommender")) {
            case "random":
                recommender = new RandomRecommender(users, items);
                break;
            case "pop":
                recommender = new PopularityRecommender<>(tpp.get());
                break;
            case "ub":
                UserNeighborhood<String> un = unf.create(params.subset("neighborhood"));
                recommender = new UserNeighborhoodRecommender<>(tpp.get(), un, params.getInt("q", 1));
                break;
            case "ib":
                ItemNeighborhood<String> in = inf.create(params.subset("neighborhood"));
                recommender = new ItemNeighborhoodRecommender<>(tpp.get(), in, params.getInt("q", 1));
                break;
            case "mf":
                Factorization<String, String> mf = fsf.create(params.subset("mf")).get();
                SimpleFactorizationFormat.get().save(mf, Files.newOutputStream(properties.getModelFile(params.name())));
                recommender = new MFRecommender<>(users, items, mf);
                break;
            case "fm":
                PreferenceFM<String, String> fm = fmsf.create(params.subset("fm")).get();
                SimpleFMFormat.get().save(fm, Files.newOutputStream(properties.getModelFile(params.name())));
                recommender = new FMRecommender<>(fm);
                break;
            default:
                recommender = null;
                break;
        }

        return recommender;
    }

}
