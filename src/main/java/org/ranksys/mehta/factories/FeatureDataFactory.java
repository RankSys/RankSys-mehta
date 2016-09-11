/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.ranksys.core.feature.fast.FastFeatureData;
import org.ranksys.core.feature.fast.SimpleFastFeatureData;
import org.ranksys.core.index.fast.FastFeatureIndex;
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.SimpleFastFeatureIndex;
import static org.ranksys.core.util.FastStringSplitter.split;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import static org.ranksys.formats.parsing.Parsers.sp;
import org.ranksys.mehta.config.MehtaProperties;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class FeatureDataFactory {

    private final MehtaProperties properties;
    private final FastItemIndex<String> items;

    @Inject
    public FeatureDataFactory(
            MehtaProperties properties, 
            FastItemIndex<String> items) {
        this.properties = properties;
        this.items = items;
    }

    public FastFeatureData<String, String, Double> create(String featureName) throws IOException {
        Path featureDataPath = properties.getFeatureFile(featureName);
        FastFeatureIndex<String> feats = SimpleFastFeatureIndex.load(Files.lines(featureDataPath)
                .map(line -> sp.parse(split(line, '\t')[1]))
                .distinct()
                .sorted());

        InputStream is = Files.newInputStream(featureDataPath);

        return SimpleFastFeatureData.load(SimpleFeaturesReader.get().read(is, sp, sp), items, feats);
    }

}
