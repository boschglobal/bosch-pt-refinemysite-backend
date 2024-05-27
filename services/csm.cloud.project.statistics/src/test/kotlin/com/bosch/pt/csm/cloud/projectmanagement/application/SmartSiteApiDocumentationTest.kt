/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import com.bosch.pt.csm.cloud.projectmanagement.application.config.MessageSourceConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.application.config.WebMvcConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.application.security.WithMockSmartSiteUser
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.authorization.StatisticsAuthorizationComponent
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.MetricsResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.StatisticsListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.StatisticsResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.TimeMetricsListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory.TimeMetricsResourceFactory
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.test.context.ActiveProfiles

@Target(ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
@Inherited
@MustBeDocumented
@WithMockSmartSiteUser
@Import(
    StatisticsAuthorizationComponent::class,
    StatisticsResourceFactory::class,
    StatisticsListResourceFactory::class,
    MetricsResourceFactory::class,
    TimeMetricsListResourceFactory::class,
    TimeMetricsResourceFactory::class,
    StatisticsController::class,
    MessageSourceConfiguration::class,
    WebMvcConfiguration::class,
    ResourceBundleMessageSource::class)
@WebMvcTest
@AutoConfigureRestDocs
@ActiveProfiles("test")
annotation class SmartSiteApiDocumentationTest
