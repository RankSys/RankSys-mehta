/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class MehtaProperties extends java.util.Properties {

    private static final String DEFAULT_FORMAT = "zip";
    private static final String DEFAULT_MAX_LENGTH = "100";
    private static final String DEFAULT_FILTERS = "notInTrain";
    private static final String DEFAULT_IGNORE_SCORES = "false";

    private final Path basePath;

    public MehtaProperties(Path configFile) throws IOException {
        super(System.getProperties());
        load(Files.newInputStream(configFile));
        if (containsKey("basePath")) {
            this.basePath = Paths.get(getProperty("basePath"));
        } else {
            this.basePath = configFile.toAbsolutePath().getParent();
        }

        putIfAbsent("usersFile", basePath.resolve("users.txt").toString());
        putIfAbsent("itemsFile", basePath.resolve("items.txt").toString());
        putIfAbsent("targetUsersFile", basePath.resolve("targetUsers.txt").toString());
        putIfAbsent("trainPreferencesFile", basePath.resolve("train.data").toString());
        putIfAbsent("testPreferencesFile", basePath.resolve("test.data").toString());
        putIfAbsent("totalPreferencesFile", basePath.resolve("total.data").toString());
        putIfAbsent("maxLength", DEFAULT_MAX_LENGTH);
        putIfAbsent("filters", DEFAULT_FILTERS);
        putIfAbsent("format", DEFAULT_FORMAT);
        putIfAbsent("ignoreScores", DEFAULT_IGNORE_SCORES);

        // create missing directories
        getPath("featurePath", "features").toFile().mkdir();
        getPath("modelsPath", "models").toFile().mkdir();
        getPath("recommendationPath", "recommendations").toFile().mkdir();
    }

    public Path getBasePath() {
        return basePath;
    }

    protected Path getPath(String property, Path defaultFile) {
        if (containsKey(property)) {
            return Paths.get(getProperty(property));
        } else {
            return basePath.resolve(defaultFile);
        }
    }

    protected Path getPath(String property, String defaultFile) {
        return getPath(property, Paths.get(defaultFile));
    }

    public Path getFeatureFile(String featureName) {
        return getPath("featurePath", "features")
                .resolve(featureName + ".txt");
    }

    public Path getModelFile(String model) {
        return getPath("modelsPath", "models")
                .resolve(model);
    }

    public Path getRecommendationFile(String recommendationName) {
        return getPath("recommendationPath", "recommendations")
                .resolve(recommendationName + ".rec");
    }

}
