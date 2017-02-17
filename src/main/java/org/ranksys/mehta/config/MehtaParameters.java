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
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toMap;

/**
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public class MehtaParameters {

    private static final Logger LOG = Logger.getLogger(MehtaParameters.class.getName());

    private final String parent;
    private final String name;
    private final Map<String, String> parameters;

    public MehtaParameters(String parent, String name, Map<String, String> parameters) {
        this.parent = parent;
        this.name = name;
        this.parameters = parameters;
    }

    public Optional<MehtaParameters> subset(String prefix) {
        return get(prefix).map(subName -> {
            Map<String, String> subParams = parameters.keySet().stream()
                    .filter(k -> k.startsWith(prefix + "."))
                    .collect(toMap(k -> k.replaceFirst(prefix + ".", ""), parameters::get));

            return new MehtaParameters(parent + prefix + ".", subName, subParams);
        });
    }

    public String name() {
        return name;
    }

    public Optional<String> get(String key) {
        if (!parameters.containsKey(key)) {
            LOG.log(Level.SEVERE, "Parameter {0}{1} not present", new Object[]{parent, key});
        }
        return Optional.ofNullable(parameters.get(key));
    }

    public String get(String key, String def) {
        if (!parameters.containsKey(key)) {
            LOG.log(Level.WARNING, "Parameter {0}{1} not present, using default {2}", new Object[]{parent, key, def});
        }
        return parameters.getOrDefault(key, def);
    }

    public Optional<Double> getDouble(String key) {
        return get(key).map(Double::parseDouble);
    }

    public double getDouble(String key, double def) {
        return parseDouble(get(key, Double.toString(def)));
    }

    public Optional<Integer> getInt(String key) {
        return get(key).map(Integer::parseInt);
    }

    public int getInt(String key, int def) {
        return parseInt(get(key, Integer.toString(def)));
    }

    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(Boolean::parseBoolean);
    }

    public boolean getBoolean(String key, boolean def) {
        return parseBoolean(get(key, Boolean.toString(def)));
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
        return Objects.equals(this.name, other.name) && Objects.equals(this.parameters, other.parameters);
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
                .forEach(tt -> {
                    if (tt.length == 2) {
                        parameters.put(tt[0], tt[1]);
                    } else {
                        LOG.log(Level.SEVERE, "Malformed parameter {0}", String.join("=", tt));
                    }
                });

        return new MehtaParameters("", name, parameters);
    }
}
