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

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.util.AnnotationLiteral;

/**
 * This processes all {@link LogAs} annotated methods, and enables an
 * interceptor for them.
 * <p>
 * This is triggered by: META-INF/services/javax.enterprise.inject.spi.Extension
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class MDCExtension implements Extension {

    private static final AnnotationLiteral<MDCInterceptorBinding> MDC_ANNOTATION_BINDING = new AnnotationLiteral<MDCInterceptorBinding>() {
    };

    private final List<String> SETUP_ERRORS = new ArrayList<>();

    /**
     * Process all methods annotated with {@link LogAs} or
     * {@link GenerateTrackingId}
     *
     * @param <T>                  Type definition
     * @param processAnnotatedType the method metadata for the class with the
     *                             annotation
     */
    private <T> void processAnnotatedType(@Observes @WithAnnotations({LogAs.class, GenerateTrackingId.class}) ProcessAnnotatedType<T> processAnnotatedType) {

        processAnnotatedType.configureAnnotatedType()
                .methods()
                .stream()
                .filter(this::isAnyParameterAnnotated)
                .map(m -> m.add(MDC_ANNOTATION_BINDING))
                .map(AnnotatedMethodConfigurator::getAnnotated)
                .map(AnnotatedMethod::getJavaMember)
                .map(MDCInterceptor::wrapMethod)
                .filter(s -> s != null)
                .forEach(SETUP_ERRORS::add);
    }

    private <T> boolean isAnyParameterAnnotated(AnnotatedMethodConfigurator<? super T> m) {
        return m.getAnnotated().getParameters()
                .stream()
                .anyMatch(p ->
                        p.isAnnotationPresent(LogAs.class) ||
                        p.isAnnotationPresent(GenerateTrackingId.class));
    }

    private void validationError(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        SETUP_ERRORS.forEach(message -> afterBeanDiscovery.addDefinitionError(new IllegalStateException(message)));
        SETUP_ERRORS.clear();
    }
}
