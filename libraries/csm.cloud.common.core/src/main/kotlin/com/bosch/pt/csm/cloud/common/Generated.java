/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation MUST ONLY be used to exclude classes or functions from test coverage.
 *
 * <p>Jacoco requires an annotation @Generated visible during runtime
 *
 * @deprecated The annotation name is misleading and therefore this annotation should no longer be
 *     used. In java classes use {@link ExcludeFromCodeCoverageGenerated} and in kotlin classes
 *     {@code ExcludeFromCodeCoverage} (in the same package).
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Generated {}
