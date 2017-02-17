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
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.core.Recommendation;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.mehta.config.MehtaModule;
import org.ranksys.mehta.config.MehtaParameters;
import org.ranksys.mehta.config.MehtaProperties;
import org.ranksys.mehta.factories.metric.RecommendationMetricFactory;
import org.ranksys.metrics.RecommendationMetric;
import org.ranksys.metrics.basic.AverageRecommendationMetric;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.inject.name.Names.named;
import static java.util.stream.Collectors.toList;
import static org.jooq.lambda.tuple.Tuple.tuple;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class Metrics {

    private static final Logger LOG = Logger.getLogger(Metrics.class.getName());

    public static void main(String[] args) throws Exception {
        Path config = Paths.get(args[0]);
        Path metricsFile = Paths.get(args[1]);
        InputStream in = args[2].equals("-") ? System.in : new FileInputStream(args[2]);

        new Metrics(config, metricsFile, in).execute();
    }

    protected final MehtaProperties properties;
    protected final Injector injector;
    protected final Path metricsFile;
    protected final InputStream in;

    public Metrics(Path config, Path metricsFile, InputStream in) throws IOException {
        this.properties = new MehtaProperties(config);
        this.injector = Guice.createInjector(new MehtaModule(properties));
        this.metricsFile = metricsFile;
        this.in = in;

        properties.put("ignoreScores", true);
    }

    public void execute() throws Exception {

        Collection<Tuple2<String, RecommendationMetric<String, String>>> recommendationMetrics = getRecommendationMetrics();

        MehtaParameters.read(in).forEach(Unchecked.consumer(params -> {
            String recName = params.name();

            LOG.log(Level.INFO, "evaluating {0}", recName);

            List<Recommendation<String, String>> recommendations = readRecommendations(recName);
            if (recommendations == null) {
                LOG.log(Level.WARNING, "{0} does not exists", recName);
                return;
            }

            evaluateRecommendationMetrics(recommendationMetrics, recName, recommendations);
        }));
    }

    private Collection<Tuple2<String, RecommendationMetric<String, String>>> getRecommendationMetrics() throws IOException {
        RecommendationMetricFactory rmf = injector.getInstance(RecommendationMetricFactory.class);

        return MehtaParameters.read(Files.newInputStream(metricsFile))
                .map(params -> {
                    String name = params.name();
                    Optional<RecommendationMetric<String, String>> metric = rmf.create(params);
                    if (!metric.isPresent()) {
                        LOG.log(Level.SEVERE, "Could not parse metric {0}", name);
                    }
                    return tuple(name, metric);
                })
                .filter(t -> t.v2.isPresent())
                .map(t -> tuple(t.v1, t.v2.get()))
                .collect(toList());
    }

    private List<Recommendation<String, String>> readRecommendations(String recName) throws IOException {
        RecommendationFormat<String, String> format = injector.getInstance(Key.get(new TypeLiteral<RecommendationFormat<String, String>>() {}));

        Path recPath = properties.getRecommendationFile(recName);
        if (!Files.exists(recPath)) {
            return null;
        }

        return format.getReader(recPath).readAll()
                .collect(toList());
    }

    private void evaluateRecommendationMetrics(
            Collection<Tuple2<String, RecommendationMetric<String, String>>> recommendationMetrics,
            String recName, List<Recommendation<String, String>> recommendations) {
        int numUsers = injector.getInstance(Key.get(new TypeLiteral<Set<String>>() {}, named("targetUsers"))).size();

        recommendationMetrics.forEach(Unchecked.consumer(t -> {
            String metricName = t.v1;
            RecommendationMetric<String, String> metric = t.v2;

            double avg = recommendations.parallelStream()
                    .collect(
                            () -> new AverageRecommendationMetric<>(metric, numUsers),
                            (m, r) -> println(recName, metricName, r.getUser(), Double.toString(m.addAndEvaluate(r))),
                            AverageRecommendationMetric::combine
                    )
                    .evaluate();

            println(recName, metricName, "", Double.toString(avg));
        }));
    }

    private static synchronized void println(String... fields) {
        System.out.println(String.join("\t", fields));
    }

}
