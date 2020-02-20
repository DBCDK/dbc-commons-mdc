/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-commons-mdc
 *
 * dbc-commons-mdc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-commons-mdc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.commons.mdc;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static dk.dbc.commons.mdc.MDCInterceptor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class MDCInterceptorTest {

    @Test
    public void testMakeTrackingId() throws Exception {
        System.out.println("testMakeTrackingId");

        Consumer<Object[]> func = makeTrackingIdSetter(2);

        Object[] existing = new Object[] {null, 123, "TRACKING_ID", 321};
        func.accept(existing);
        assertThat(existing[0], nullValue());
        assertThat(existing[1], is(123));
        assertThat(existing[2], is("TRACKING_ID"));
        assertThat(existing[3], is(321));

        Object[] empty = new Object[] {null, 123, "", 321};
        func.accept(empty);
        assertThat(empty[0], nullValue());
        assertThat(empty[1], is(123));
        assertThat(empty[2], not(nullValue()));
        assertThat(empty[2], not(is("")));
        assertThat(empty[3], is(321));

        Object[] blank = new Object[] {null, 123, "     ", 321};
        func.accept(blank);
        assertThat(blank[0], nullValue());
        assertThat(blank[1], is(123));
        assertThat(blank[2], not(nullValue()));
        assertThat(blank[2], not(is("")));
        assertThat(blank[3], is(321));

        Object[] missing = new Object[] {null, 123, null, 321};
        func.accept(missing);
        assertThat(missing[0], nullValue());
        assertThat(missing[1], is(123));
        assertThat(missing[2], not(nullValue()));
        assertThat(missing[2], not(is("")));
        assertThat(missing[3], is(321));
    }

    @Test
    public void testMakeMDCSetterNotNullNotArray() throws Exception {
        System.out.println("testMakeMDCSetterNotNullNotArray");

        Consumer<Object[]> func = makeMDCSetter(2, "abc", false, String.class);
        Object[] existing = new Object[] {null, 123, "TRACKING_ID", 321};
        MDC.clear();
        func.accept(existing);
        assertThat(MDC.get("abc"), is("TRACKING_ID"));

        Object[] missing = new Object[] {null, 123, null, 321};
        MDC.clear();
        func.accept(missing);
        assertThat(MDC.get("abc"), nullValue());
    }

    @Test
    public void testMakeMDCSetterNotNullNotArrayInt() throws Exception {
        System.out.println("testMakeMDCSetterNotNullNotArrayInt");

        Consumer<Object[]> func = makeMDCSetter(2, "abc", false, int.class);
        Object[] existing = new Object[] {null, 123, 456, 321};
        MDC.clear();
        func.accept(existing);
        assertThat(MDC.get("abc"), is("456"));
    }

    @Test
    public void testMakeMDCSetterNullNotArray() throws Exception {
        System.out.println("testMakeMDCSetterNullNotArray");

        Consumer<Object[]> func = makeMDCSetter(2, "abc", true, String.class);
        Object[] missing = new Object[] {null, 123, null, 321};
        MDC.clear();
        func.accept(missing);
        assertThat(MDC.get("abc"), is("null"));
    }

    @Test
    public void testMakeMDCSetterNotNullArray() throws Exception {
        System.out.println("testMakeMDCSetterNotNullArray");

        String[] value = new String[] {"abc", "def"};
        Consumer<Object[]> func = makeMDCSetter(2, "abc", false, value.getClass());
        Object[] existing = new Object[] {null, 123, value, 321};
        MDC.clear();
        func.accept(existing);
        assertThat(MDC.get("abc"), is("[abc, def]"));

        Object[] missing = new Object[] {null, 123, null, 321};
        MDC.clear();
        func.accept(missing);
        assertThat(MDC.get("abc"), nullValue());
    }

    @Test
    public void testMakeMDCSetterNotNullIntArray() throws Exception {
        System.out.println("testMakeMDCSetterNotNullIntArray");

        int[] value = new int[] {1, 2, 3};
        Consumer<Object[]> func = makeMDCSetter(2, "abc", false, value.getClass());
        Object[] existing = new Object[] {null, 123, value, 321};
        MDC.clear();
        func.accept(existing);
        assertThat(MDC.get("abc"), is("[1, 2, 3]"));

        Object[] missing = new Object[] {null, 123, null, 321};
        MDC.clear();
        func.accept(missing);
        assertThat(MDC.get("abc"), nullValue());
    }

    @Test
    public void testMakeMDCSetterNotNullDeepArray() throws Exception {
        System.out.println("testMakeMDCSetterNotNullDeepArray");

        int[][] value = new int[][] {null, new int[] {1, 2, 3}, null};
        Consumer<Object[]> func = makeMDCSetter(2, "abc", false, value.getClass());
        Object[] existing = new Object[] {null, 123, value, 321};
        MDC.clear();
        func.accept(existing);
        assertThat(MDC.get("abc"), is("[null, [1, 2, 3], null]"));

    }
}
