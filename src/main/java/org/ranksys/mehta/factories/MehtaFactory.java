/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ranksys.mehta.factories;

import org.ranksys.mehta.config.MehtaParameters;

import java.util.Optional;

/**
 *
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 */
public interface MehtaFactory<T> {

    public Optional<T> create(MehtaParameters params);
}
