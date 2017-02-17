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
import org.jooq.lambda.Unchecked;
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.formats.factorization.SimpleFMFormat;
import org.ranksys.formats.factorization.SimpleFactorizationFormat;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.config.MehtaProperties;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.recommenders.basic.PopularityRecommender;
import org.ranksys.recommenders.basic.RandomRecommender;
import org.ranksys.recommenders.fast.FastRecommender;
import org.ranksys.recommenders.fm.PreferenceFM;
import org.ranksys.recommenders.fm.rec.FMRecommender;
import org.ranksys.recommenders.mf.Factorization;
import org.ranksys.recommenders.mf.rec.MFRecommender;
import org.ranksys.recommenders.nn.item.ItemNeighborhoodRecommender;
import org.ranksys.recommenders.nn.user.UserNeighborhoodRecommender;

import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RecommenderFactory implements MehtaFactory<FastRecommender<String, String>> {

    private static final Logger LOG = Logger.getLogger(RecommenderFactory.class.getName());

    private final MehtaProperties properties;
    private final FastUserIndex<String> users;
    private final FastItemIndex<String> items;
    private final Provider<FastPreferenceData<String, String>> tpp;
    private final UserNeighborhoodFactory unf;
    private final ItemNeighborhoodFactory inf;
    private final FactorizationFactory ff;
    private final FMFactory fmf;

    @Inject
    public RecommenderFactory(
            MehtaProperties properties,
            FastUserIndex<String> users, FastItemIndex<String> items,
            @Named("targetUsers") Set<String> targetUsers,
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp,
            UserNeighborhoodFactory unf, ItemNeighborhoodFactory inf,
            FactorizationFactory ff, FMFactory fmf) {
        this.properties = properties;
        this.users = users;
        this.items = items;
        this.tpp = tpp;
        this.unf = unf;
        this.inf = inf;
        this.ff = ff;
        this.fmf = fmf;
    }

    @Override
    public Optional<FastRecommender<String, String>> create(MehtaParameters params) {
        switch (params.get("recommender", "")) {
            case "random":
                return Optional.of(new RandomRecommender<>(users, items));
            case "pop":
                return Optional.of(new PopularityRecommender<>(tpp.get()));
            case "ub":
                return params.subset("neighborhood").flatMap(unf::create)
                        .map(un -> new UserNeighborhoodRecommender<>(tpp.get(), un, params.getInt("q", 1)));
            case "ib":
                return params.subset("neighborhood").flatMap(inf::create)
                        .map(in -> new ItemNeighborhoodRecommender<>(tpp.get(), in, params.getInt("q", 1)));
            case "mf":
                Optional<Factorization<String, String>> mfo = params.subset("mf").flatMap(ff::create);
                mfo.ifPresent(Unchecked.consumer(mf -> SimpleFactorizationFormat.get().save(mf, Files.newOutputStream(properties.getModelFile(params.name())))));
                return mfo.map(mf -> new MFRecommender<>(users, items, mf));
            case "fm":
                Optional<PreferenceFM<String, String>> fmo = params.subset("fm").flatMap(fmf::create);
                fmo.ifPresent(Unchecked.consumer(fm -> SimpleFMFormat.get().save(fm, Files.newOutputStream(properties.getModelFile(params.name())))));
                return fmo.map(FMRecommender::new);
            default:
                return Optional.empty();
        }
    }

}
