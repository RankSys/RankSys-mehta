/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories.metric;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.factories.MehtaFactory;
import org.ranksys.metrics.basic.NDCG;
import org.ranksys.metrics.rel.BackgroundBinaryRelevanceModel;
import org.ranksys.metrics.rel.BinaryRelevanceModel;
import org.ranksys.metrics.rel.NoRelevanceModel;
import org.ranksys.metrics.rel.RelevanceModel;
import org.ranksys.novdiv.intentaware.metrics.ERRIA;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class RelevanceModelFactory implements MehtaFactory<RelevanceModel<String, String>> {

    private final Provider<FastPreferenceData<String, String>> tpp;

    @Inject
    public RelevanceModelFactory(
            @Named("testPreferences") Provider<FastPreferenceData<String, String>> tpp) {
        this.tpp = tpp;
    }

    @Override
    public RelevanceModel<String, String> create(MehtaParameters params) {
        
        RelevanceModel<String, String> relModel;
        
        double l = params.getDouble("threshold", 1.0);

        switch (params.name()) {
            case "bin":
                relModel = new BinaryRelevanceModel<>(true, tpp.get(), l);
                break;
            case "none":
                relModel = new NoRelevanceModel<>();
                break;
            case "bck":
                double background = params.getDouble("background");
                relModel = new BackgroundBinaryRelevanceModel<>(true, tpp.get(), l, background);
                break;
            case "ndcg":
                relModel = new NDCG.NDCGRelevanceModel<>(true, tpp.get(), l);
                break;
            case "err":
                relModel = new ERRIA.ERRRelevanceModel<>(true, tpp.get(), l);
                break;
            default:
                relModel = null;
                break;
        }
        if (relModel != null) {
            relModel.initialize();
        }

        return relModel;
    }
}
