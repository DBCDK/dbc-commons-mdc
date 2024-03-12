/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of mdc
 *
 * mdc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mdc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.commons.mdc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Across bean boundary interceptor annotation
 * <p>
 * This allows for logging parameters as MDC values
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Inherited
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(forRemoval = true, since = "2024 - use artifact: dk.dbc:dbc-commons-payara-helpers")
public @interface LogAs {

    /**
     * Name of MDC field
     *
     * @return name of MDC field
     */
    String value();

    /**
     * Also include field if it is null
     *
     * @return always include field
     */
    boolean includeNull() default false;
}
