/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.cache.DefaultContextCache;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * An {@link DefaultContextCache application context cache} that decreases cache misses by ignoring
 * particular test class annotations when looking up cached application contexts.
 *
 * <p>By default, Spring uses equality of {@link MergedContextConfiguration context configurations}
 * to determine whether an application context can be reused from cache. This includes comparing a
 * test class's annotations. In particular, @{@link DisplayName} and @{@link Nested} will also be
 * part of the comparison so that an application context will not be shared between test classes
 * that, for example, differ only in having a @{@link DisplayName} annotation or not.
 */
public class RelaxedContextCache extends DefaultContextCache {

  // ImportsContextCustomizer is package private so that we cannot access it directly
  public static final String IMPORTS_CONTEXT_CUSTOMIZER_CLASS_NAME =
      "org.springframework.boot.test.context.ImportsContextCustomizer";

  @Override
  public boolean contains(MergedContextConfiguration key) {

    return super.contains(toCacheKey(key));
  }

  @Override
  public ApplicationContext get(MergedContextConfiguration key) {
    return super.get(toCacheKey(key));
  }

  @Override
  public void put(MergedContextConfiguration key, ApplicationContext context) {
    super.put(toCacheKey(key), context);
  }

  @Override
  public void remove(MergedContextConfiguration key, DirtiesContext.HierarchyMode hierarchyMode) {
    super.remove(toCacheKey(key), hierarchyMode);
  }

  MergedContextConfiguration toCacheKey(MergedContextConfiguration cc) {
    return new MergedContextConfiguration(
        cc.getTestClass(),
        cc.getLocations(),
        cc.getClasses(),
        cc.getContextInitializerClasses(),
        cc.getActiveProfiles(),
        cc.getPropertySourceLocations(),
        cc.getPropertySourceProperties(),
        cc.getContextCustomizers().stream()
            .map(this::applyAnnotationFilters)
            .collect(Collectors.toSet()),
        cc.getContextLoader(),
        RelaxedCacheAwareContextLoaderDelegate.getInstance(),
        cc.getParent());
  }

  private ContextCustomizer applyAnnotationFilters(ContextCustomizer contextCustomizer) {
    if (contextCustomizer.getClass().getName().equals(IMPORTS_CONTEXT_CUSTOMIZER_CLASS_NAME)) {
      Object customizerKey = ReflectionTestUtils.getField(contextCustomizer, "key");
      Set<Object> keySet = (Set<Object>) ReflectionTestUtils.getField(customizerKey, "key");
      Set<Object> filteredKeySet =
          keySet.stream()
              .filter(o -> !o.toString().contains(Nested.class.getName()))
              .filter(o -> !o.toString().contains(DisplayName.class.getName()))
              .filter(o -> !o.toString().contains(SuppressWarnings.class.getName()))
              .collect(Collectors.toSet());
      ReflectionTestUtils.setField(customizerKey, "key", filteredKeySet);
    }

    return contextCustomizer;
  }
}
