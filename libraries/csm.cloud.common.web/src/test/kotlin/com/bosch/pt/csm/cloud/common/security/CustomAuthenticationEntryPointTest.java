/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security;

import static com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN;
import static com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes.INSUFFICIENT_SCOPE;
import static org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes.INVALID_REQUEST;
import static org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes.INVALID_TOKEN;

import com.bosch.pt.csm.cloud.common.facade.rest.ErrorResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

/** Unit test for {@link CustomAuthenticationEntryPoint}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Verify correct error translation in OAuth2 filter")
class CustomAuthenticationEntryPointTest {

  private static final String TRACE_ID_STRING = "0";

  @Mock private MessageSource messageSource;

  @Mock private Environment environment;

  private CustomAuthenticationEntryPoint cut;

  @BeforeEach
  void init() {
    when(messageSource.getMessage(anyString(), any(), any())).then(returnsFirstArg());
    cut = new CustomAuthenticationEntryPoint(messageSource, environment);
  }

  @Test
  void commenceWhenNoBearerTokenErrorThenStatus401AndAuthHeader() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    cut.commence(request, response, new BadCredentialsException("test"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_NO_TOKEN_PROVIDED));
  }

  @Test
  void commenceWhenNoBearerTokenErrorAndRealmSetThenStatus401AndAuthHeaderWithRealm()
      throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    cut.setRealmName("test");
    cut.commence(request, response, new BadCredentialsException("test"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer realm=\"test\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_NO_TOKEN_PROVIDED));
  }

  @Test
  void commenceWhenInvalidRequestErrorThenStatus400AndHeaderWithError() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error = new BearerTokenError(INVALID_REQUEST, BAD_REQUEST, null, null);

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo("Bearer error=\"invalid_request\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInvalidRequestErrorThenStatus400AndHeaderWithErrorDetails() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error =
        new BearerTokenError(INVALID_REQUEST, BAD_REQUEST, "The access token expired", null, null);

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo(
            "Bearer error=\"invalid_request\", error_description=\"The access token expired\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInvalidRequestErrorThenStatus400AndHeaderWithErrorUri() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error =
        new BearerTokenError(INVALID_REQUEST, BAD_REQUEST, null, "https://example.com", null);

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo("Bearer error=\"invalid_request\", error_uri=\"https://example.com\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInvalidTokenErrorThenStatus401AndHeaderWithError() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error = new BearerTokenError(INVALID_TOKEN, UNAUTHORIZED, null, null);

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"invalid_token\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInsufficientScopeErrorThenStatus403AndHeaderWithError() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error = new BearerTokenError(INSUFFICIENT_SCOPE, FORBIDDEN, null, null);

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo("Bearer error=\"insufficient_scope\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInsufficientScopeErrorThenStatus403AndHeaderWithErrorAndScope()
      throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error =
        new BearerTokenError(INSUFFICIENT_SCOPE, FORBIDDEN, null, null, "test.read test.write");

    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo("Bearer error=\"insufficient_scope\", scope=\"test.read test.write\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  @Test
  void commenceWhenInsufficientScopeAndRealmSetThenStatus403AndHeaderWithErrorAndAllDetails()
      throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    BearerTokenError error =
        new BearerTokenError(
            INSUFFICIENT_SCOPE,
            FORBIDDEN,
            "Insufficient scope",
            "https://example.com",
            "test.read test.write");

    cut.setRealmName("test");
    cut.commence(request, response, new OAuth2AuthenticationException(error));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo(
            "Bearer realm=\"test\", error=\"insufficient_scope\", error_description=\"Insufficient scope\", "
                + "error_uri=\"https://example.com\", scope=\"test.read test.write\"");
    assertThat(response.getContentAsString())
        .isEqualTo(createJsonErrorBody(SERVER_ERROR_INVALID_TOKEN));
  }

  private String createJsonErrorBody(String message) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    ErrorResource errorResource = new ErrorResource(message, TRACE_ID_STRING);
    return objectMapper.writeValueAsString(errorResource);
  }
}
