/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.evaluation.runner.fast.FastFilters;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FilterFactory {

    private final Provider<FastPreferenceData<String, String>> tpp;
    private final FeatureDataFactory fdf;

    @Inject
    public FilterFactory(
            @Named("trainPreferences") Provider<FastPreferenceData<String, String>> tpp,
            FeatureDataFactory fdf) {
        this.tpp = tpp;
        this.fdf = fdf;
    }

    public Function<String, IntPredicate> create(String filterParams) throws IOException {
        String[] params = filterParams.split("_");
        Function<String, IntPredicate> filter;
        switch (params[0]) {
            case "all":
                filter = FastFilters.all();
                break;
            case "notInTrain":
                filter = FastFilters.notInTrain(tpp.get());
                break;
            case "withFeatures":
                String featureName = params[1];
                filter = FastFilters.withFeatures(fdf.create(featureName));
                break;
            default:
                filter = null;
                break;
        }

        return filter;
    }

}
