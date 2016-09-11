/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.metric;

import com.google.inject.Inject;
import java.util.function.Supplier;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.metrics.RecommendationMetric;
import org.ranksys.metrics.basic.FScore;
import org.ranksys.metrics.basic.KCall;
import org.ranksys.metrics.basic.NDCG;
import org.ranksys.metrics.basic.NumRetrieved;
import org.ranksys.metrics.basic.Precision;
import org.ranksys.metrics.basic.Recall;
import org.ranksys.metrics.basic.ReciprocalRank;
import org.ranksys.metrics.basic.Recommendability;
import org.ranksys.metrics.rel.IdealRelevanceModel;
import org.ranksys.metrics.rel.RelevanceModel;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RecommendationMetricFactory implements MehtaFactory<RecommendationMetric<String, String>> {

    private final RelevanceModelFactory rmf;

    @Inject
    public RecommendationMetricFactory(RelevanceModelFactory rmf) {
        this.rmf = rmf;
    }

    @Override
    public RecommendationMetric<String, String> create(MehtaParameters params) {
        RecommendationMetric<String, String> metric;

        int cutoff = params.getInt("cutoff", 10);

        Supplier<RelevanceModel<String, String>> rms = () -> rmf.create(params.subset("rel"));

        switch (params.get("metric")) {
            case "prec":
                metric = new Precision<>(cutoff, rms.get());
                break;
            case "recall":
                metric = new Recall<>(cutoff, (IdealRelevanceModel<String, String>) rms.get());
                break;
            case "fscore":
                metric = new FScore<>(cutoff, (IdealRelevanceModel<String, String>) rms.get());
                break;
            case "hitrate":
            case "onecall":
                metric = new KCall<>(cutoff, 1, rms.get());
                break;
            case "rr":
                metric = new ReciprocalRank<>(cutoff, rms.get());
                break;
            case "ndcg":
                metric = new NDCG<>(cutoff, (NDCG.NDCGRelevanceModel<String, String>) rms.get());
                break;
            case "numq":
                metric = new Recommendability<>();
                break;
            case "numret":
                metric = new NumRetrieved<>();
                break;
            default:
                metric = null;
                break;
        }

        return metric;
    }
}
