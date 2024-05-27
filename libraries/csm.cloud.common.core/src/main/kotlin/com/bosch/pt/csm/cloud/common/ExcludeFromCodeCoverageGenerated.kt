/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common

/**
 * This annotation can be used to exclude classes or functions from test coverage.
 *
 * Use [ExcludeFromCodeCoverageGenerated] only in java classes. In Kotlin classes the more specific
 * [ExcludeFromCodeCoverage] type alias should be used.
 *
 * Jacoco requires an annotation with the keyword "Generated" visible in the byte-code to exclude
 * the specific code from coverage.
 */
@Retention(AnnotationRetention.BINARY) annotation class ExcludeFromCodeCoverageGenerated

/**
 * This annotation can be used to exclude classes or functions from test coverage.
 *
 * In java classes the type alias is not visible, therefore the [ExcludeFromCodeCoverageGenerated]
 * annotation should be used.
 */
typealias ExcludeFromCodeCoverage = ExcludeFromCodeCoverageGenerated
