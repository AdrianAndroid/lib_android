/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.hilt.ext

import com.google.auto.common.MoreElements
import javax.lang.model.element.Element
import kotlin.reflect.KClass

fun Element.hasAnnotation(clazz: KClass<out Annotation>) =
    MoreElements.isAnnotationPresent(this, clazz.java)

fun Element.hasAnnotation(qName: String) = annotationMirrors.any {
    MoreElements.asType(it.annotationType.asElement()).qualifiedName.contentEquals(qName)
}
