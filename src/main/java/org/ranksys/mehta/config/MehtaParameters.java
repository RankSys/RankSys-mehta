/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toMap;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class MehtaParameters {

    private final String name;
    private final Map<String, String> parameters;

    public MehtaParameters(String name, Map<String, String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public MehtaParameters subset(String prefix) {
        String subName = parameters.get(prefix);
        Map<String, String> subParams = parameters.keySet().stream()
                .filter(k -> k.startsWith(prefix + "."))
                .collect(toMap(k -> k.replaceFirst(prefix + ".", ""), k -> parameters.get(k)));

        return new MehtaParameters(subName, subParams);
    }

    public String name() {
        return name;
    }

    public String get(String key) {
        return parameters.get(key);
    }

    public String get(String key, String def) {
        return parameters.getOrDefault(key, def);
    }

    public double getDouble(String key) {
        return parseDouble(parameters.get(key));
    }

    public double getDouble(String key, double def) {
        return parseDouble(parameters.getOrDefault(key, Double.toString(def)));
    }

    public int getInt(String key) {
        return parseInt(parameters.get(key));
    }

    public int getInt(String key, int def) {
        return parseInt(parameters.getOrDefault(key, Integer.toString(def)));
    }

    public boolean getBoolean(String key) {
        return parseBoolean(parameters.get(key));
    }

    public boolean getBoolean(String key, boolean def) {
        return parseBoolean(parameters.getOrDefault(key, Boolean.toString(def)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.parameters);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MehtaParameters other = (MehtaParameters) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.parameters, other.parameters);
    }

    private static final Predicate<String> IS_EMPTY = String::isEmpty;
    private static final Predicate<String> IS_COMMENT_LINE = line -> line.startsWith("#");
    private static final Supplier<Predicate<String>> IS_COMMENT_BLOCK_SUPPLIER = () -> {
        boolean[] commentedSection = new boolean[]{false};
        return line -> {
            if (line.startsWith("==")) {
                commentedSection[0] = !commentedSection[0];
                return true;
            }
            return commentedSection[0];
        };
    };

    public static Stream<MehtaParameters> read(final InputStream in) {
        Predicate<String> isCommentBlock = IS_COMMENT_BLOCK_SUPPLIER.get();
        return new BufferedReader(new InputStreamReader(in)).lines().sequential()
                .filter(IS_EMPTY.negate())
                .filter(IS_COMMENT_LINE.negate())
                .filter(isCommentBlock.negate())
                .map(MehtaParameters::parse);
    }

    public static MehtaParameters parse(String line) {
        String[] tokens = line.split("\\s+");

        String name = tokens[0];
        Map<String, String> parameters = new HashMap<>();
        Stream.of(tokens).skip(1)
                .map(t -> t.split("="))
                .forEach(tt -> parameters.put(tt[0], tt[1]));

        return new MehtaParameters(name, parameters);
    }
}
