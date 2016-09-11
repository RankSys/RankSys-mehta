/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.core.index.fast.FastItemIndex;
import org.ranksys.core.index.fast.FastUserIndex;
import org.ranksys.core.index.fast.SimpleFastItemIndex;
import org.ranksys.core.index.fast.SimpleFastUserIndex;
import org.ranksys.core.preference.fast.FastPreferenceData;
import org.ranksys.core.preference.fast.SimpleFastPreferenceData;
import org.ranksys.evaluation.runner.RecommenderRunner;
import org.ranksys.evaluation.runner.fast.FastFilterRecommenderRunner;
import org.ranksys.evaluation.runner.fast.FastFilters;
import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import static org.ranksys.formats.parsing.Parsers.sp;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.MahoutRecommendationFormat;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;
import org.ranksys.formats.rec.ZipRecommendationFormat;
import org.ranksys.mehta.factories.FilterFactory;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class MehtaModule extends AbstractModule {

    private final MehtaProperties properties;

    public MehtaModule(MehtaProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);
        bind(MehtaProperties.class).toInstance(properties);
    }

    @Provides
    @Singleton
    public FastUserIndex<String> getUsers(@Named("usersFile") String usersFile) throws IOException {
        return SimpleFastUserIndex.load(UsersReader.read(usersFile, sp));
    }

    @Provides
    @Singleton
    public FastItemIndex<String> getItems(@Named("itemsFile") String itemsFile) throws IOException {
        return SimpleFastItemIndex.load(ItemsReader.read(itemsFile, sp));
    }

    @Provides
    @Singleton
    @Named("trainPreferences")
    public FastPreferenceData<String, String> getTrainPreferences(
            @Named("trainPreferencesFile") String trainPreferencesFile,
            FastUserIndex<String> users, FastItemIndex<String> items) throws IOException {
        return getPreferences(trainPreferencesFile, users, items);
    }

    @Provides
    @Singleton
    @Named("testPreferences")
    public FastPreferenceData<String, String> getTestPreferences(
            @Named("testPreferencesFile") String testPreferencesFile,
            FastUserIndex<String> users, FastItemIndex<String> items) throws IOException {
        return getPreferences(testPreferencesFile, users, items);
    }

    @Provides
    @Singleton
    @Named("totalPreferences")
    public FastPreferenceData<String, String> getTotalPreferences(
            @Named("totalPreferencesFile") String totalPreferencesFile,
            FastUserIndex<String> users, FastItemIndex<String> items) throws IOException {
        return getPreferences(totalPreferencesFile, users, items);
    }

    @Provides
    @Singleton
    @Named("targetUsers")
    public Set<String> getTargetUsers(
            @Named("targetUsersFile") String targetUsersFile,
            @Named("testPreferences") Provider<FastPreferenceData<String, String>> testPreferencesProvider) throws IOException {
        if (Files.exists(Paths.get(targetUsersFile))) {
            return Files.lines(Paths.get(targetUsersFile))
                    .map(sp)
                    .collect(toSet());
        } else {
            return testPreferencesProvider.get().getUsersWithPreferences()
                    .collect(toSet());
        }
    }

    @Provides
    public RecommenderRunner<String, String> getRecommenderRunner(
            FastUserIndex<String> users,
            FastItemIndex<String> items,
            @Named("targetUsers") Set<String> targetUsers,
            Function<String, IntPredicate> filters,
            @Named("maxLength") int maxLength) {
        return new FastFilterRecommenderRunner<>(users, items, targetUsers.stream(),
                filters, maxLength);
    }

    @Provides
    public Function<String, IntPredicate> getFilters(
            @Named("filters") String filters, FilterFactory filterFactory) {
        return Stream.of(filters.split(","))
                .map(Unchecked.function(filterName -> filterFactory.create(filterName)))
                .reduce(FastFilters::and)
                .get();
    }

    @Provides
    public RecommendationFormat<String, String> getRecommendationFormat(
            @Named("format") String formatName,
            @Named("ignoreScores") boolean ignoreScores) {
        RecommendationFormat<String, String> format;

        switch (formatName) {
            case "mahout":
                format = new MahoutRecommendationFormat<>(sp, sp);
                break;
            case "zip":
                format = new ZipRecommendationFormat<>(sp, sp, ignoreScores);
                break;
            case "simple":
                format = new SimpleRecommendationFormat<>(sp, sp);
                break;
            case "trec":
                format = new TRECRecommendationFormat<>(sp, sp);
                break;
            default:
                format = null;
        }

        return format;
    }

    public FastPreferenceData<String, String> getPreferences(
            String preferencesFile,
            FastUserIndex<String> users, FastItemIndex<String> items) throws IOException {
        Stream<Tuple3<String, String, Double>> tuples = SimpleRatingPreferencesReader.get()
                .read(preferencesFile, sp, sp);

        return SimpleFastPreferenceData.load(tuples, users, items);
    }

}
