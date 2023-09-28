/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.efs.sdk.organizationmanager.apidoc;

import com.efs.sdk.organizationmanager.security.oauth.OAuthConfiguration;
import com.efs.sdk.organizationmanager.security.oauth.OAuthConfigurationHelper;
import io.micrometer.core.instrument.util.IOUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;

/**
 * SpringDoc configuration for Swagger API-documentation
 *
 * @author e:fs TechHub GmbH
 */
@Configuration
@EnableWebMvc
public class SpringDocConfig {

    private static final String SECURITY_REFERENCE = "spring_oauth";

    /**
     * Configuration of the api-title.
     */
    @Value("${apidoc.title}")
    private String title;

    private final OAuthConfiguration oAuthConfig;

    public SpringDocConfig(OAuthConfigurationHelper oAuthConfigurationHelper) {
        this.oAuthConfig = oAuthConfigurationHelper.getOpenidConfigProperty();
    }

    /**
     * Provides and configures the Docket that provides the API documentation.
     *
     * @return The Docket bean.
     */
    @Bean
    public OpenAPI springOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .components(new Components().addSecuritySchemes(SECURITY_REFERENCE, securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_REFERENCE));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .scheme("bearer")
                .bearerFormat("jwt")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow().authorizationUrl(oAuthConfig.authorizationEndpoint())
                        .tokenUrl(getTokenEndpoint()).scopes(new Scopes().addString("profile",
                                "Request profile"))));
    }

    /**
     * <b>HACK</b> in case of retrieving token-endpoint via cluster-internal-domain, token-endpoint is generated with this internal domain. SpringDoc is not
     * able to resolve this information for frontend-flow.
     * <br>
     * Therefore we replace the domain with the issuer-url, which is generated based on the frontend-url.
     *
     * @return The token-endpoint
     */
    private String getTokenEndpoint() {
        String tokenEndpoint = oAuthConfig.tokenEndpoint();
        String issuer = oAuthConfig.issuer();
        int strOffset = tokenEndpoint.indexOf("/protocol");
        String tokenIssuer = tokenEndpoint.substring(0, strOffset);
        return tokenEndpoint.replace(tokenIssuer, issuer);
    }

    /**
     * Builds the API information included in Swagger UI.
     *
     * @return The api information.
     */
    private Info buildInfo() {
        return new Info().title(title)
                .description(
                        IOUtils.toString(getClass().getResourceAsStream("/description.md"), StandardCharsets.UTF_8))
                .version("1.*").termsOfService("TERMS OF SERVICE URL")
                .contact(new Contact().name("Superb Data Kraken").url("https://efs.ai").email("sdk@efs-techhub.com"))
                .license(new License().name("Apache License 2.0").url("license-url"));
    }

}
