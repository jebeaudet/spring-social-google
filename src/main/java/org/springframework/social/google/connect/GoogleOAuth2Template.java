/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.google.connect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Google-specific extension to OAuth2Template.
 * @author Gabriel Axel
 */
public class GoogleOAuth2Template extends OAuth2Template
{
    private ClientHttpRequestFactory clientHttpRequestFactory;

    public GoogleOAuth2Template(String clientId, String clientSecret, ClientHttpRequestFactory clientHttpRequestFactory)
    {
        super(clientId,
              clientSecret,
              "https://accounts.google.com/o/oauth2/v2/auth",
              "https://www.googleapis.com/oauth2/v4/token");
        this.clientHttpRequestFactory = clientHttpRequestFactory;
        setUseParametersForClientAuthentication(true);
    }

    @Override
    @SuppressWarnings({ "rawtypes" })
    protected AccessGrant postForAccessGrant(String accessTokenUrl, MultiValueMap<String, String> parameters)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);
        ResponseEntity<Map> responseEntity = getRestTemplate().exchange(accessTokenUrl,
                                                                        HttpMethod.POST,
                                                                        requestEntity,
                                                                        Map.class);
        Map<String, Object> responseMap = responseEntity.getBody();
        return extractAccessGrant(responseMap);
    }

    @Override
    public RestTemplate createRestTemplate()
    {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        List<HttpMessageConverter<?>> converters = new ArrayList<>(2);
        converters.add(new FormHttpMessageConverter());
        converters.add(new FormMapHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

    private AccessGrant extractAccessGrant(Map<String, Object> result)
    {
        String accessToken = (String) result.get("access_token");
        String scope = (String) result.get("scope");
        String refreshToken = (String) result.get("refresh_token");

        // result.get("expires_in") may be an Integer, so cast it to Number first.
        Number expiresInNumber = (Number) result.get("expires_in");
        Long expiresIn = (expiresInNumber == null) ? null : expiresInNumber.longValue();

        return createAccessGrant(accessToken, scope, refreshToken, expiresIn, result);
    }

}
