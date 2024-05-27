/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.Credentials.basic
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.PROXY_AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Configuration
class OkHttpConfiguration(
    @Value("\${proxy.data:false}") private val proxyData: Boolean,
    @Value("\${proxy.idp:true}") private val proxyIdp: Boolean
) {

  /** Configure the okhttp client for the connections to the backend. */
  @Bean
  @Primary
  fun okHttpClient(authenticationService: AuthenticationService): OkHttpClient =
      OkHttpClient.Builder()
          .apply {
            readTimeout(500, TimeUnit.SECONDS)
            connectTimeout(500, TimeUnit.SECONDS)
            addProxy(this, proxyData)
            addInterceptor(
                Interceptor { chain: Interceptor.Chain ->
                  chain.proceed(
                      chain
                          .request()
                          .newBuilder()
                          .addHeader(AUTHORIZATION, authenticationService.accessToken)
                          // For profile pictures and various attachments, the Accept header was
                          // somehow set to  list of values starting with application/xml causing
                          // errors during the import. Therefore, we clear the header
                          // and default is to application/json
                          .removeHeader(ACCEPT)
                          .addHeader(ACCEPT, APPLICATION_JSON_VALUE)
                          .build())
                })
          }
          .build()

  /** Configure okhttp client for the connections to the idp. */
  @Bean
  fun idpOkHttpClient(): OkHttpClient =
      OkHttpClient.Builder()
          .apply {
            readTimeout(120, TimeUnit.SECONDS)
            connectTimeout(120, TimeUnit.SECONDS)
            addProxy(this, proxyIdp)
          }
          .build()

  private fun addProxy(builder: OkHttpClient.Builder, proxy: Boolean) {
    val host = System.getProperty("http.proxyHost")
    val port = System.getProperty("http.proxyPort")
    val username = System.getProperty("http.proxyUser")
    val password = System.getProperty("http.proxyPassword")

    if (!proxy) {
      builder.proxy(Proxy.NO_PROXY)
      return
    }

    if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port)) {
      val proxyAddress: SocketAddress = InetSocketAddress(host, port.toInt())
      builder.proxy(Proxy(Proxy.Type.HTTP, proxyAddress))
      if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
        val authenticator = Authenticator { _: Route?, response: Response ->
          response
              .request
              .newBuilder()
              .header(PROXY_AUTHORIZATION, basic(username, password))
              .build()
        }
        builder.proxyAuthenticator(authenticator)
      }
    }
  }
}
