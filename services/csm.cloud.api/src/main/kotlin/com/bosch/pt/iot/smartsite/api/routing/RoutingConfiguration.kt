/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing

import com.bosch.pt.iot.smartsite.api.security.config.FallbackTokenRelayGatewayFilterConfiguration.FallbackTokenRelayGatewayFilterFactory
import io.netty.handler.codec.http.HttpHeaderNames.COOKIE
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.BooleanSpec
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
class RoutingConfiguration(
    @Value("\${company.company.service.url}") private val companyUrl: String,
    @Value("\${event.service.url}") private val eventUrl: String,
    @Value("\${featuretoggle.service.url}") private val featureToggleUrl: String,
    @Value("\${job.service.url}") private val jobsUrl: String,
    @Value("\${project.project.service.url}") private val projectUrl: String,
    @Value("\${project.activity.service.url}") private val projectActivityUrl: String,
    @Value("\${project.api.timeseries.service.url}") private val projectApiTimeseriesUrl: String,
    @Value("\${project.news.service.url}") private val projectNewsUrl: String,
    @Value("\${project.notifications.service.url}") private val projectNotificationsUrl: String,
    @Value("\${project.statistics.service.url}") private val projectStatisticsUrl: String,
    @Value("\${user.user.service.url}") private val userUrl: String,

    // default value empty ""
    @Value("\${reset.service.url:}") private val resetUrl: String,
    @Value("\${route.prefix:}") private val routePrefix: String,
    @Value("\${route.prefix.timeline:/timeline}") private val routePrefixTimeline: String,
    @Value("\${route.prefix.internal:/internal}") private val routePrefixInternal: String,

    // Filter to replace session cookie with bearer token for backend services
    private val tokenRelayGatewayFilterFactory: FallbackTokenRelayGatewayFilterFactory,
    private val logger: Logger
) {

  @Order(1)
  @Bean
  fun addStandardRoutes(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator =
      routeLocatorBuilder
          .routes()
          .also { logger.info("add standard routes") }
          .apply {
            timelineRoute("", projectApiTimeseriesUrl)
            timelineRoute("/", projectApiTimeseriesUrl)
            timelineRoute("/users", projectApiTimeseriesUrl)
            timelineRoute("/companies", projectApiTimeseriesUrl)
            timelineRoute("/projects/rfvs", projectApiTimeseriesUrl)
            timelineRoute("/projects/tasks/schedules/daycards", projectApiTimeseriesUrl)
            timelineRoute("/projects/tasks/constraints", projectApiTimeseriesUrl)
            timelineRoute("/projects/constraints", projectApiTimeseriesUrl)
            timelineRoute("/projects/tasks/topics", projectApiTimeseriesUrl)
            timelineRoute("/projects/tasks", projectApiTimeseriesUrl)
            timelineRoute("/projects/workareas", projectApiTimeseriesUrl)
            timelineRoute("/projects/participants", projectApiTimeseriesUrl)
            timelineRoute("/projects/crafts", projectApiTimeseriesUrl)
            timelineRoute("/projects/relations", projectApiTimeseriesUrl)
            timelineRoute("/projects/milestones", projectApiTimeseriesUrl)
            timelineRoute("/projects", projectApiTimeseriesUrl)
            timelineRoute("/projects/workdays", projectApiTimeseriesUrl)
            timelineRoute("/translations/**", projectApiTimeseriesUrl)

            securedRoute("/announcements/**", userUrl, versioned = true)
            securedRoute("/crafts/**", userUrl, versioned = true)
            securedRoute("/documents/**", userUrl, versioned = true)
            securedRoute("/users/**", userUrl, versioned = true)
            securedRoute("/companies/**", companyUrl, versioned = true)
            securedRoute("/projects/metrics/**", projectStatisticsUrl, versioned = true)
            securedRoute("/projects/{id}/metrics/**", projectStatisticsUrl, versioned = true)
            securedRoute("/projects/tasks/news/**", projectNewsUrl, versioned = true)
            securedRoute("/projects/{id}/news/**", projectNewsUrl, versioned = true)
            securedRoute("/projects/tasks/{id}/news/**", projectNewsUrl, versioned = true)
            securedRoute("/projects/tasks/activities/**", projectActivityUrl, versioned = true)
            securedRoute("/projects/tasks/{id}/activities/**", projectActivityUrl, versioned = true)
            securedRoute("/projects/notifications/**", projectNotificationsUrl, versioned = true)
            securedRoute(
                "/projects/{id}/notifications/**", projectNotificationsUrl, versioned = true)
            securedRoute("/projects/{id}/import/**", projectUrl, versioned = true)
            securedRoute("/projects/**", projectUrl, versioned = true)
            securedRoute("/events/**", eventUrl, versioned = true)
            securedRoute("/jobs/**", jobsUrl, versioned = true)
            securedRoute("/features/**", featureToggleUrl, versioned = true)

            internalRoute("/announcements/**", userUrl, versioned = true)
            internalRoute("/crafts/**", userUrl, versioned = true)
            internalRoute("/documents/**", userUrl, versioned = true)
            internalRoute("/users/**", userUrl, versioned = true)
            internalRoute("/companies/**", companyUrl, versioned = true)
            internalRoute("/projects/metrics/**", projectStatisticsUrl, versioned = true)
            internalRoute("/projects/{id}/metrics/**", projectStatisticsUrl, versioned = true)
            internalRoute("/projects/tasks/news/**", projectNewsUrl, versioned = true)
            internalRoute("/projects/{id}/news/**", projectNewsUrl, versioned = true)
            internalRoute("/projects/tasks/{id}/news/**", projectNewsUrl, versioned = true)
            internalRoute("/projects/tasks/activities/**", projectActivityUrl, versioned = true)
            internalRoute(
                "/projects/tasks/{id}/activities/**", projectActivityUrl, versioned = true)
            internalRoute("/projects/notifications/**", projectNotificationsUrl, versioned = true)
            internalRoute(
                "/projects/{id}/notifications/**", projectNotificationsUrl, versioned = true)
            internalRoute("/projects/{id}/import/**", projectUrl, versioned = true)
            internalRoute("/projects/**", projectUrl, versioned = true)
            internalRoute("/events/**", eventUrl, versioned = true)
            internalRoute("/jobs/**", jobsUrl, versioned = true)
            internalRoute("/features/**", featureToggleUrl, versioned = true)

            securedRoute("/graphql/**", projectApiTimeseriesUrl)
            publicRoute("/graphiql/**", projectApiTimeseriesUrl)
          }
          .build()

  @Order(2)
  @Profile("docs")
  @Bean
  fun addDocumentationRoutes(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator =
      routeLocatorBuilder
          .routes()
          .also { logger.info("add documentation routes") }
          .apply {
            internalRoute("/docs/events/**", eventUrl, prefixed = true)
            internalRoute("/docs/features/**", featureToggleUrl, prefixed = true)
            internalRoute("/docs/users/**", userUrl, prefixed = true)
            internalRoute("/docs/companies/**", companyUrl, prefixed = true)
            internalRoute("/docs/projects/activities/**", projectActivityUrl, prefixed = true)
            internalRoute("/docs/projects/news/**", projectNewsUrl, prefixed = true)
            internalRoute(
                "/docs/projects/notifications/**", projectNotificationsUrl, prefixed = true)
            internalRoute("/docs/projects/statistics/**", projectStatisticsUrl, prefixed = true)
            internalRoute("/docs/projects/**", projectUrl, prefixed = true)
            internalRoute("/docs/jobs/**", jobsUrl, prefixed = true)
          }
          .build()

  @Profile("!production")
  @Order(3)
  @Bean
  fun addSwaggerRoutes(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator =
      routeLocatorBuilder
          .routes()
          .also { logger.info("add swagger routes") }
          .apply {
            publicRoute("/swagger/activities/**", projectActivityUrl)
            publicRoute("/swagger/companies/**", companyUrl)
            publicRoute("/swagger/features/**", featureToggleUrl)
            publicRoute("/swagger/jobs/**", jobsUrl)
            publicRoute("/swagger/news/**", projectNewsUrl)
            publicRoute("/swagger/notifications/**", projectNotificationsUrl)
            publicRoute("/swagger/projects/**", projectUrl)
            publicRoute("/swagger/statistics/**", projectStatisticsUrl)
            publicRoute("/swagger/timeline/**", projectApiTimeseriesUrl)
            publicRoute("/swagger/users/**", userUrl)
          }
          .build()

  @Profile("production")
  @Order(3)
  @Bean
  fun addSwaggerRoutesProduction(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator =
      routeLocatorBuilder
          .routes()
          .also { logger.info("add swagger routes") }
          .apply { publicRoute("/swagger/timeline/**", projectApiTimeseriesUrl) }
          .build()

  @Order(4)
  @Profile("db-resettable")
  @Bean
  fun addResetRoutes(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator =
      routeLocatorBuilder
          .routes()
          .also { logger.info("add reset routes") }
          .apply {
            internalRoute("/import/**", resetUrl, prefixed = true)
            internalRoute("/reset/**", resetUrl, prefixed = true)
          }
          .build()

  @Bean
  fun internalApiStaticResourceLocator(): RouterFunction<ServerResponse> =
      resources("/internal/**", ClassPathResource("static/"))

  @Bean
  fun apiStaticResourceLocator(): RouterFunction<ServerResponse> =
      resources("/api/**", ClassPathResource("static/"))

  fun RouteLocatorBuilder.Builder.timelineRoute(path: String, uri: String) {
    route(path) { it.versionedPath(path, routePrefixTimeline).authToken().uri(uri) }
  }

  fun RouteLocatorBuilder.Builder.securedRoute(
      path: String,
      uri: String,
      versioned: Boolean = false,
  ) {
    if (versioned) {
      route(path) { it.versionedPath(path, routePrefix).authToken().uri(uri) }
    } else {
      route(path) { it.path(path).authToken().uri(uri) }
    }
  }

  fun RouteLocatorBuilder.Builder.internalRoute(
      path: String,
      uri: String,
      prefixed: Boolean = false,
      versioned: Boolean = false,
  ) {
    if (prefixed) {
      route(path) { it.prefixedPath(path, routePrefixInternal).authToken().uri(uri) }
    } else if (versioned) {
      route(path) { it.versionedPath(path, routePrefixInternal).authToken().uri(uri) }
    } else {
      route(path) { it.path(path).authToken().uri(uri) }
    }
  }

  fun RouteLocatorBuilder.Builder.publicRoute(path: String, uri: String) {
    route(path) { it.path(path).uri(uri) }
  }

  private fun PredicateSpec.versionedPath(path: String, prefix: String): BooleanSpec =
      prefixedPath("/{version:v[1-9][0-9]*}$path", prefix)

  private fun PredicateSpec.prefixedPath(path: String, prefix: String): BooleanSpec =
      this.path(prefix + path).apply {
        filters { it.rewritePath("$prefix(?<path>.*)", "\${path}") }
      }

  private fun BooleanSpec.authToken(): BooleanSpec =
      this.apply {
        filters {
          it
              // invoke service to replace session cookie with bearer token
              .filter(tokenRelayGatewayFilterFactory.apply())
              // remove cookie to not forward it to a backend service
              .removeRequestHeader(COOKIE.toString())
        }
      }
}
