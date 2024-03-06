/* SHOGun, https://terrestris.github.io/shogun/
 *
 * Copyright © 2020-present terrestris GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.terrestris.shogun.lib.config;

import de.terrestris.shogun.lib.annotation.JsonSuperType;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.function.Predicate;

@Configuration
@EnableAutoConfiguration
public abstract class SwaggerConfig {

    protected String title = "SHOGun REST API";
    protected String description = "This is the REST API description of SHOGun";
    protected String version = "1.0.0";
    protected String termsOfServiceUrl = "https://www.terrestris.de/en/impressum/";
    protected Contact contact = new Contact("terrestris GmbH & Co. KG", "www.terrestris.de", "info@terrestris.de");
    protected String license = "Apache License, Version 2.0";
    protected String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt";

    @Bean
    public Docket api() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo())
            .securityContexts(Collections.singletonList(actuatorSecurityContext()))
            .securitySchemes(Collections.singletonList(basicAuthScheme()));

        directModelSubsitutions(docket);

        return docket;
    }

    private SecurityContext actuatorSecurityContext() {
        return SecurityContext.builder()
            .securityReferences(Collections.singletonList(basicAuthReference()))
            .forPaths(setSecurityContextPaths()::test)
            .build();
    }

    private SecurityReference basicAuthReference() {
        return new SecurityReference("basicAuth", new AuthorizationScope[0]);
    }

    private SecurityScheme basicAuthScheme() {
        return new BasicAuth("basicAuth");
    }

    protected void directModelSubsitutions(Docket docket) {
        var reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forJavaClassPath())
            .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));

        for (var cl : reflections.getTypesAnnotatedWith(JsonSuperType.class)) {
            var annotation = cl.getAnnotation(JsonSuperType.class);
            var superType = annotation.type();

            if (!annotation.override() && !superType.isInterface()) {
                throw new IllegalStateException("The super type " + superType.getName() + " is not an interface. " +
                    "Set override to true if this is intended.");
            }

            docket.directModelSubstitute(superType, cl);
        }
    }

    protected ApiInfo apiInfo() {
        return new ApiInfo(
            title,
            description,
            version,
            termsOfServiceUrl,
            contact,
            license,
            licenseUrl,
            Collections.emptyList()
        );
    }

    /**
     * Define the project specific paths that require BasicAuth authentication.
     *
     * Possible return values could be:
     * <ul>
     *     <li>{@link PathSelectors#none()}</li>
     *     <li>{@link PathSelectors#any()}</li>
     *     <li>{@link PathSelectors#regex(String)}</li>
     *     <li>{@link PathSelectors#ant(String)}</li>
     * </ul>
     *
     *  Some examples for specifying a custom list of endpoints:
     *  <pre>
     *  PathSelectors.ant("/files/**");
     *  </pre>
     *
     *  <pre>
     *  Predicates.or(
     *      PathSelectors.ant("/files/**"),
     *      PathSelectors.ant("/applications/**")
     *  );
     *  </pre>
     *
     *  The latter requires {@code com.google.guava} on the classpath.
     *
     * @return The predicate that defines the secured paths.
     */
    protected abstract Predicate<String> setSecurityContextPaths();
}
