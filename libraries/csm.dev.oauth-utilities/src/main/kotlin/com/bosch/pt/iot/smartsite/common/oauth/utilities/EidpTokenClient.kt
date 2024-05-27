/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.oauth.utilities

import com.google.gson.JsonParser
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.hc.core5.http.HttpStatus.SC_OK
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.net.URIBuilder
import org.jsoup.Connection.Method.GET
import org.jsoup.Connection.Method.POST
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

/**
 * Token client which performs a login with user and password mimicking an actual user doing a
 * browser based login. The login flow is an AuthCode+PKCE OIDC flow which addresses iCPM (the PT
 * KeyCloak) as an Identity Broker. iCPM in turn uses SKID (SingleKeyId) as backing Identity
 * Provider. In short: from an application integration point-of-view iCPM is the IdP. From user
 * perspective SKID is the login provider.
 */
@Suppress("TooManyFunctions")
object EidpTokenClient {

  private val DEV_CONFIG =
      IdpConfig(
          tokenEndpoint =
              "https://p32.authz.bosch.com/auth/realms/central_profile/protocol/openid-connect/token",
          authEndpoint =
              "https://p32.authz.bosch.com/auth/realms/central_profile/protocol/openid-connect/auth",
          loginUrl = "https://stage.singlekey-id.com/auth/api/v1/authentication/login",
          loginBaseUrl = "https://stage.singlekey-id.com",
          redirectUri = "com.bosch.pt.dcs.refinemysite://login",
          clientId = "refinemysite-android-dev",
          clientSecret = "")

  private val PROD_CONFIG =
      IdpConfig(
          tokenEndpoint =
              "https://p36.authz.bosch.com/auth/realms/central_profile/protocol/openid-connect/token",
          authEndpoint =
              "https://p36.authz.bosch.com/auth/realms/central_profile/protocol/openid-connect/auth",
          loginUrl = "https://singlekey-id.com/auth/api/v1/authentication/login",
          loginBaseUrl = "https://singlekey-id.com",
          redirectUri = "com.bosch.pt.dcs.refinemysite://login",
          clientId = "refinemysite-android-prod",
          clientSecret = "")

  private const val MAX_TRIES = 10
  private const val RANDOM_LENGTH = 32
  private const val KEY_CLOAK_AUTH_URL_TEMPLATE =
      "%s?response_type=code" +
          "&client_id=%s" +
          "&redirect_uri=%s" +
          "&scope=openid+profile+email" +
          "&code_challenge=%s" +
          "&code_challenge_method=S256" +
          "&nonce=%s" +
          "&state=%s"

  private val LOGGER = LoggerFactory.getLogger(EidpTokenClient::class.java)

  /** Obtains the access token for user [username] with up to [MAX_TRIES] retries. */
  fun getToken(
      username: String,
      password: String,
      env: Environment,
      captchaByPassConfig: CaptchaByPassConfig = CaptchaByPassConfig("", "")
  ): String {

    for (i in 1..MAX_TRIES) {
      try {
        val captchaBypassKeyProvider = lazy {
          requestOneTimeCaptchaBypassKey(
              getEnvConfig(env).loginBaseUrl, getEnvConfig(env), captchaByPassConfig)
        }

        return loginAndGetAccessToken(
            username, password, getEnvConfig(env), captchaBypassKeyProvider)
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        if (i == MAX_TRIES) {
          LOGGER.warn("Retries exhausted")
          throw e
        } else {
          LOGGER.warn("Retry to login '$username' the $i. time...")
        }
      }
    }
    error("Token couldn't be retrieved in $MAX_TRIES retries")
  }

  private fun loginAndGetAccessToken(
      username: String,
      password: String,
      config: IdpConfig,
      captchaBypassKeyProvider: Lazy<String>
  ): String {

    val codeVerifier = generateCodeVerifier()

    // Call IdP server with client id and redirect uri
    val authorizationInitUrl = assembleAuthInitUrl(config, codeVerifier)
    LOGGER.debug("Requesting auth init $authorizationInitUrl")

    // collect the cookies
    // Includes cookies [.AspNetCore.Antiforgery.085ONM3l57w, AUTH_SESSION_ID_LEGACY, KC_RESTART,
    // AUTH_SESSION_ID] (latter 3 belong to KeyCloak aka iCPM, others belong to SKID)
    val cookies = mutableMapOf<String, String>()
    val authorizationInitResponse = followRedirects(authorizationInitUrl, cookies)
    LOGGER.debug("Redirected auth init to {}", authorizationInitResponse.url())

    // Add cookie [XSRF-TOKEN]
    cookies.putAll(getXsrfTokenAsCookie(cookies, config))
    LOGGER.debug("Cookies are now {}", cookies.keys)

    // Perform the actual login flow for human users
    val requestVerificationToken = extractVerificationToken(authorizationInitResponse)
    LOGGER.debug("Submitting username to {} ...", authorizationInitResponse.url())

    // Submit username for the login flow
    val usernameSubmissionResponse =
        submitUsernameOrSolveCaptcha(
            authorizationInitResponse = authorizationInitResponse,
            cookies = cookies,
            username = username,
            config = config,
            requestVerificationToken = requestVerificationToken,
            captchaBypassKeyProvider = captchaBypassKeyProvider)

    // get password form to extract verification token
    val passwordFormUrl =
        URL(URL(config.loginBaseUrl), usernameSubmissionResponse.header("Location")!!)
    LOGGER.debug("Requesting password form to {} ...", passwordFormUrl)

    val passwordFormResponse =
        getPasswordFormResponse(
            passwordFormUrl = passwordFormUrl, cookies, captchaBypassKeyProvider)

    // submit password form
    LOGGER.debug("Submitting password to {} ...", passwordFormUrl)
    val verificationTokenFromPasswordForm = extractVerificationToken(passwordFormResponse)

    val passwordSubmissionResponse =
        submitPasswordOrSolveCaptcha(
            passwordFormUrl = passwordFormUrl.toString(),
            cookies = cookies,
            password = password,
            config = config,
            verificationTokenFromPasswordForm = verificationTokenFromPasswordForm,
            captchaBypassKeyProvider = captchaBypassKeyProvider)

    // follow all redirects until we are redirected to our own redirect URI
    // (which cannot be called over the network)
    var redirectUrl =
        getLocationFromHeader(passwordSubmissionResponse, passwordFormUrl.toString()).toString()

    while (!redirectUrl.startsWith(config.redirectUri)) {
      redirectUrl = followRedirects(cookies, redirectUrl)
    }

    // Extract the auth code from the pseudo URL query parameter
    val authCode = extractQueryParameter(URI.create(redirectUrl), "code")

    // Call the token resource from Key Cloak
    val tokenResource = getTokenResource(config, authCode, codeVerifier, cookies)

    // extract and return the access token
    return JsonParser.parseString(tokenResource).asJsonObject.get("access_token").asString
  }

  private fun getPasswordFormResponse(
      passwordFormUrl: URL,
      cookies: MutableMap<String, String>,
      captchaBypassKeyProvider: Lazy<String>
  ): Response =
      Jsoup.connect(passwordFormUrl.toString())
          .cookies(cookies)
          .header("Accept", "*/*")
          .header("X-CAPTCHA", captchaBypassKeyProvider.value)
          .followRedirects(false)
          .execute()

  private fun submitUsernameOrSolveCaptcha(
      authorizationInitResponse: Response,
      cookies: MutableMap<String, String>,
      username: String,
      config: IdpConfig,
      requestVerificationToken: String,
      captchaBypassKeyProvider: Lazy<String>
  ): Response {
    val usernameSubmissionResponse =
        submitUsername(authorizationInitResponse, cookies, username, requestVerificationToken)

    LOGGER.debug("Additional cookies set: {}", usernameSubmissionResponse.cookies().keys)
    cookies.putAll(usernameSubmissionResponse.cookies())

    if (usernameSubmissionResponse.headers("Location").any { it.contains("captcha") }) {
      val location = getLocationFromHeader(usernameSubmissionResponse, config.loginBaseUrl)
      bypassCaptchaChallenge(location, cookies, captchaBypassKeyProvider.value)

      throw RestartLoginNeededAfterCaptchaException()
    }
    return usernameSubmissionResponse
  }

  private fun submitUsername(
      authorizationInitResponse: Response,
      cookies: MutableMap<String, String>,
      username: String,
      requestVerificationToken: String
  ) =
      Jsoup.connect(authorizationInitResponse.url().toString())
          .cookies(cookies)
          .method(POST)
          .header("Accept", "*/*")
          .data("UserIdentifierInput.EmailInput.StringValue", username)
          .data("__RequestVerificationToken", requestVerificationToken)
          .followRedirects(false)
          .execute()
          .also { logResponse(it) }

  private fun submitPasswordOrSolveCaptcha(
      passwordFormUrl: String,
      cookies: MutableMap<String, String>,
      password: String,
      config: IdpConfig,
      verificationTokenFromPasswordForm: String,
      captchaBypassKeyProvider: Lazy<String>
  ): Response {
    val passwordSubmissionResponse =
        submitPassword(
            passwordFormUrl,
            cookies,
            captchaBypassKeyProvider,
            password,
            verificationTokenFromPasswordForm)

    LOGGER.debug("Additional cookies set: {}", passwordSubmissionResponse.cookies().keys)
    cookies.putAll(passwordSubmissionResponse.cookies())

    if (passwordSubmissionResponse.headers("Location").any { it.contains("captcha") }) {
      val location = getLocationFromHeader(passwordSubmissionResponse, config.loginBaseUrl)
      bypassCaptchaChallenge(location, cookies, captchaBypassKeyProvider.value)

      throw RestartLoginNeededAfterCaptchaException()
    }

    return passwordSubmissionResponse
  }

  private fun submitPassword(
      passwordFormUrl: String,
      cookies: MutableMap<String, String>,
      captchaBypassKeyProvider: Lazy<String>,
      password: String,
      verificationTokenFromPasswordForm: String
  ): Response =
      Jsoup.connect(passwordFormUrl)
          .followRedirects(false)
          .cookies(cookies)
          .header("Accept", "*/*")
          .header("X-CAPTCHA", captchaBypassKeyProvider.value)
          .method(POST)
          .data("Password", password)
          .data("RememberMe", "false")
          .data("__RequestVerificationToken", verificationTokenFromPasswordForm)
          .execute()

  private fun bypassCaptchaChallenge(
      location: URL,
      cookies: MutableMap<String, String>,
      captchaBypassKey: String
  ): Response {
    LOGGER.debug(
        "We hit a captcha challenge.  Submitting one-time captcha bypass key at {} ...", location)
    val captchaSubmissionFormResponse =
        Jsoup.connect(location.toString()).cookies(cookies).execute().also { logResponse(it) }

    val captchaRequestVerificationToken = extractVerificationToken(captchaSubmissionFormResponse)

    return Jsoup.connect(location.toString())
        .cookies(cookies)
        .method(POST)
        .headers(
            mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json"))
        .data("__RequestVerificationToken", captchaRequestVerificationToken)
        .data("CaptchaValue", captchaBypassKey)
        .followRedirects(false)
        .execute()
        .also { logResponse(it) }
        .also {
          cookies.putAll(it.cookies())
          LOGGER.debug("Additional cookies set: {}", it.cookies().keys)
        }
  }

  private fun requestOneTimeCaptchaBypassKey(
      loginBaseUrl: String,
      config: IdpConfig,
      captchaByPassConfig: CaptchaByPassConfig
  ): String {
    if (captchaByPassConfig.clientId.isBlank()) return "n/a"
    val clientAccessToken: String =
        Jsoup.connect(URL(URL(loginBaseUrl), "/auth/connect/token").toString())
            .method(POST)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .ignoreContentType(true)
            .data(
                mapOf(
                    "grant_type" to "client_credentials",
                    "client_id" to captchaByPassConfig.clientId,
                    "client_secret" to captchaByPassConfig.clientSecret,
                    "scope" to "create_captcha_bypass_key"))
            .execute()
            .also { logResponse(it) }
            .body()
            .let { JsonParser.parseString(it).asJsonObject.get("access_token").asString }
            .also { LOGGER.debug("Obtained client access token for captcha bypass: {}", it) }

    val generateCaptchaBypassKeyResponse =
        Jsoup.connect(URL(URL(config.loginBaseUrl), "/auth/api/v1/captcha/key").toString())
            .method(POST)
            .header("Authorization", "Bearer $clientAccessToken")
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .execute()

    when (val statusCode = generateCaptchaBypassKeyResponse.statusCode()) {
      201 -> {
        LOGGER.debug(
            "$statusCode: You have created the Captcha bypass key) " +
                " expires at ${LocalDateTime.now().plusMinutes(10)}")
      }
      401 -> {
        error("$statusCode: The access token does not have \"client_id\" claim")
      }
      403 -> {
        error(
            "$statusCode: The access token does not have the required scope \"create_captcha_bypass_key\"")
      }
      404 -> {
        error("$statusCode: The feature is not enabled")
      }
      429 -> {
        error(
            "$statusCode: Too many requests have been made to create" +
                " one-time captcha bypass keys. Please review your limits")
      }
    }

    return generateCaptchaBypassKeyResponse
        .also { logResponse(it) }
        .body()
        .let { JsonParser.parseString(it).asJsonObject.get("key").asString }
        .also { LOGGER.debug("Obtained one-time captcha bypass key: {}", it) }
  }

  private fun logResponse(it: Response) {
    LOGGER.debug("{} - {}, Headers: {}", it.statusCode(), it.statusMessage(), it.headers())
  }

  private fun extractVerificationToken(authorizationInitResponse: Response): String =
      authorizationInitResponse
          .parse()
          .selectFirst("input[name='__RequestVerificationToken']")!!
          .`val`()

  private fun followRedirects(
      urlAsString: String,
      cookies: MutableMap<String, String>,
      maxDepth: Int = 10,
  ): Response {
    val response =
        Jsoup.connect(urlAsString)
            .followRedirects(false)
            .cookies(cookies)
            .header("Accept", "*/*")
            .execute()
    LOGGER.debug(
        "{} - {}, Headers: {}", response.statusCode(), response.statusMessage(), response.headers())
    cookies.putAll(response.cookies())
    if (maxDepth == 1) return response
    return when (response.statusCode()) {
      302,
      303,
      307 -> {
        val location = getLocationFromHeader(response, urlAsString)
        LOGGER.debug("Redirecting as original request to {} ...", location)
        followRedirects(location.toString(), cookies, maxDepth - 1)
      }
      else -> response
    }
  }

  private fun getLocationFromHeader(response: Response, baseUrlAsString: String) =
      response.header("location")!!.let {
        if (!URI(it).isAbsolute) {
          URL(URL(baseUrlAsString), it)
        } else URL(it)
      }

  private fun generateCodeChallenge(codeVerifier: String): String {
    val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
    val messageDigest =
        MessageDigest.getInstance("SHA-256").let {
          it.update(bytes, 0, bytes.size)
          it.digest()
        }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(messageDigest)
  }

  private fun generateCodeVerifier(): String {
    val secureRandom = SecureRandom()
    val codeVerifier = ByteArray(RANDOM_LENGTH)
    secureRandom.nextBytes(codeVerifier)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
  }

  private fun assembleAuthInitUrl(config: IdpConfig, codeVerifier: String): String =
      String.format(
          KEY_CLOAK_AUTH_URL_TEMPLATE,
          config.authEndpoint,
          config.clientId,
          config.redirectUri,
          generateCodeChallenge(codeVerifier),
          randomAlphanumeric(RANDOM_LENGTH),
          randomAlphanumeric(RANDOM_LENGTH))

  private fun getEnvConfig(env: Environment): IdpConfig =
      when (env) {
        Environment.DEV -> DEV_CONFIG
        Environment.PROD -> PROD_CONFIG
      }

  private fun followRedirects(cookies: Map<String, String>, url: String): String {
    LOGGER.debug("GETing $url")
    val response =
        Jsoup.connect(url).cookies(cookies).method(GET).followRedirects(false).execute().also {
          logResponse(it)
        }
    val locationHeader = response.header("location")

    when (response.statusCode()) {
      200 -> {
        if (locationHeader == null) {
          error("Location header was null. There is no URL available for redirection")
        }
      }
    }
    return locationHeader!!
  }

  private fun extractQueryParameter(uri: URI, queryParameterName: String) =
      URIBuilder(uri, UTF_8)
          .queryParams
          .filter { param: NameValuePair -> queryParameterName == param.name }
          .map { it.value }
          .firstOrNull()
          ?: throw IllegalArgumentException(
              "Query parameter $queryParameterName couldn't be extracted")

  private fun getXsrfTokenAsCookie(cookies: Map<String, String>, config: IdpConfig) =
      Jsoup.connect(config.loginBaseUrl)
          .cookies(cookies)
          .method(GET)
          .execute()
          .apply { require(statusCode() == SC_OK) { "Unable to obtain XSRF-TOKEN Cookie" } }
          .cookies()

  private fun getTokenResource(
      config: IdpConfig,
      authCode: String,
      codeVerifier: String,
      cookies: MutableMap<String, String>
  ): String {
    // For some reason KC required basic auth even for public clients even with empty password
    val basicAuth =
        Base64.getEncoder()
            .encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())

    // Retrieve the tokens (access, identity, refresh)
    val response =
        Jsoup.connect(config.tokenEndpoint)
            .method(POST)
            .cookies(cookies)
            .data("grant_type", "authorization_code")
            .data("code", authCode)
            .data("redirect_uri", config.redirectUri)
            .data("code_verifier", codeVerifier)
            .header("Accept", "application/json")
            .header("Authorization", "Basic $basicAuth")
            .followRedirects(false)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .execute()
    LOGGER.debug(
        "{} - {}, Headers: {}", response.statusCode(), response.statusMessage(), response.headers())
    require(response.statusCode() == SC_OK) {
      "Failed to obtain token resource with status ${response.statusCode()} from Key Cloak."
    }
    return response.body()
  }

  class RestartLoginNeededAfterCaptchaException : RuntimeException()

  data class IdpConfig(
      /** The IdP's token endpoint */
      val tokenEndpoint: String,
      /** The IdP's authorization endpoint */
      val authEndpoint: String,
      /** The login URL called by the SKID login site as XHR call. */
      val loginUrl: String,
      /** We use the icon URL to extract the XSRF token */
      val loginBaseUrl: String,
      /** The redirect URI configured with the IdP. */
      val redirectUri: String,
      /** The client id on the IdP. */
      val clientId: String,
      /** The client secret which can be empty string for public clients. */
      val clientSecret: String
  )

  data class CaptchaByPassConfig(
      /** Need this to directly connect to SKID and request a one-time captcha bypass key */
      val clientId: String,
      val clientSecret: String
  )
}
