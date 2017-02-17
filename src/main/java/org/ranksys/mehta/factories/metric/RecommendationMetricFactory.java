/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.metric;

import com.google.inject.Inject;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.metrics.RecommendationMetric;
import org.ranksys.metrics.basic.*;
import org.ranksys.metrics.rel.IdealRelevanceModel;
import org.ranksys.metrics.rel.RelevanceModel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RecommendationMetricFactory implements MehtaFactory<RecommendationMetric<String, String>> {

    private final RelevanceModelFactory rmf;

    @Inject
    public RecommendationMetricFactory(RelevanceModelFactory rmf) {
        this.rmf = rmf;
    }

    @Override
    public Optional<RecommendationMetric<String, String>> create(MehtaParameters params) {

        Supplier<Integer> cutoff = () -> params.getInt("cutoff", 10);
        Supplier<Optional<RelevanceModel<String, String>>> rms = () -> params.subset("rel").flatMap(rmf::create);

        return params.get("metric").flatMap(metricName -> {
            switch (metricName) {
                case "prec":
                    return rms.get().map(rm -> new Precision<>(cutoff.get(), rm));
                case "recall":
                    return rms.get().map(rm -> new Recall<>(cutoff.get(), (IdealRelevanceModel<String, String>) rm));
                case "fscore":
                    return rms.get().map(rm -> new FScore<>(cutoff.get(), (IdealRelevanceModel<String, String>) rm));
                case "hitrate":
                case "onecall":
                    return rms.get().map(rm -> new KCall<>(cutoff.get(), 1, rm));
                case "rr":
                    return rms.get().map(rm -> new ReciprocalRank<>(cutoff.get(), rm));
                case "ndcg":
                    return rms.get().map(rm -> new NDCG<>(cutoff.get(), (NDCG.NDCGRelevanceModel<String, String>) rm));
                case "numq":
                    return Optional.of(new Recommendability<>());
                case "numret":
                    return Optional.of(new NumRetrieved<>());
                default:
                    return Optional.empty();
            }
        });
    }
}
