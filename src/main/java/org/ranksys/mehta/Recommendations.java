/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.jooq.lambda.Unchecked;
import org.ranksys.evaluation.runner.RecommenderRunner;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.RecommendationFormat.Writer;
import org.ranksys.mehta.config.MehtaModule;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.config.MehtaProperties;
import org.ranksys.mehta.factories.recommender.RecommenderFactory;
import org.ranksys.recommenders.fast.FastRecommender;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class Recommendations {

    private static final Logger LOG = Logger.getLogger(Recommendations.class.getName());

    public static void main(String[] args) throws Exception {
        Path config = Paths.get(args[0]);
        InputStream in = args[1].equals("-") ? System.in : new FileInputStream(args[1]);

        new Recommendations(config, in).execute();
    }

    protected final InputStream in;
    protected final MehtaProperties properties;
    protected final Injector injector;

    public Recommendations(Path config, InputStream in) throws IOException {
        this.properties = new MehtaProperties(config);
        this.injector = Guice.createInjector(new MehtaModule(properties));
        this.in = in;
    }

    public void execute() {
        RecommenderFactory rf = injector.getInstance(RecommenderFactory.class);
        RecommendationFormat<String, String> format = injector.getInstance(Key.get(new TypeLiteral<RecommendationFormat<String, String>>() {}));
        RecommenderRunner<String, String> rr = injector.getInstance(Key.get(new TypeLiteral<RecommenderRunner<String, String>>() {}));

        MehtaParameters.read(in).forEach(Unchecked.consumer(params -> {
            String recName = params.name();
            Path recommendationFile = properties.getRecommendationFile(recName);

            if (Files.exists(recommendationFile) && !recName.startsWith("test")) {
                LOG.log(Level.INFO, "{0} already exists", recName);
                return;
            }

            Optional<FastRecommender<String, String>> recommender = rf.create(params);
            if (!recommender.isPresent()) {
                LOG.log(Level.WARNING, "{0} not recognized", recName);
                return;
            }

            LOG.log(Level.INFO, "{0} in process", recName);

            try (Writer<String, String> writer = format.getWriter(recommendationFile)) {
                rr.run(recommender.get(), writer);
            }

            LOG.log(Level.INFO, "{0} completed", recName);
        }));
    }

}
