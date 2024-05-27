package com.bosch.pt.csm.cloud.event.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange

@Configuration
class RestConfig {

  @Bean fun webClient(): WebClient = WebClient.builder().filter(addAuthorizationToken()).build()

  private fun addAuthorizationToken(): ExchangeFilterFunction =
      ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        ReactiveRequestContextHolder.getRequest().flatMap { r: ServerWebExchange ->
          val clientRequest =
              ClientRequest.from(request)
                  .headers { headers: HttpHeaders ->
                    headers[AUTHORIZATION] = r.request.headers.getFirst(AUTHORIZATION)
                  }
                  .build()

          next.exchange(clientRequest)
        }
      }
}
