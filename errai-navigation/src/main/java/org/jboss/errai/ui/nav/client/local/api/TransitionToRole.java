/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.nav.client.local.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import org.jboss.errai.ui.nav.client.local.UniquePageRole;

/**
 * A qualifier for {@link Anchor} elements linking to Errai Navigation {@link Page Pages} by {@link UniquePageRole}. An
 * injected anchor with this qualifier has an {@link EventListener} registered for "click" events that navigates to the
 * Errai Navigation page with the unique page role specified by the qualifier {@link #value()}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Documented
@Qualifier
@Retention(RUNTIME)
@Target({ PARAMETER, FIELD })
public @interface TransitionToRole {

  /**
   * The class of an Errai Navigation {@link UniquePageRole}.
   */
  @Nonbinding Class<? extends UniquePageRole> value();

}
