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

package androidx.appcompat

import com.android.tools.lint.detector.api.Constraint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

// Base class for all AppCompat Lint checks that flag usages of methods that should be
// converted to use matching API(s) provided by AppCompat.
@Suppress("UnstableApiUsage")
abstract class BaseMethodDeprecationDetector(
    private val issue: Issue,
    vararg val conditions: DeprecationCondition
) : Detector(), SourceCodeScanner {

    // Collect unique method names from all deprecation conditions defined in this detector.
    private val applicableMethods = conditions.map { it.methodLocation.methodName }.distinct()

    interface Predicate {
        fun matches(context: JavaContext, node: UCallExpression, method: PsiMethod): Boolean
    }

    class SubClassOf(private val superClass: String) : Predicate {
        override fun matches(
            context: JavaContext,
            node: UCallExpression,
            method: PsiMethod
        ): Boolean {
            return context.evaluator.extendsClass(
                (node.receiverType as? PsiClassType)?.resolve(), superClass, false
            )
        }
    }

    class MethodLocation(
        private val className: String,
        val methodName: String,
        private vararg val params: String
    ) : Predicate {
        override fun matches(
            context: JavaContext,
            node: UCallExpression,
            method: PsiMethod
        ): Boolean {
            if (!context.evaluator.isMemberInClass(method, className)) {
                // Method is not in the right class.
                return false
            }
            if (method.name != methodName) {
                // Method name does not match.
                return false
            }
            if (!context.evaluator.methodMatches(method, className, true, *params)) {
                // Method signature does not match.
                return false
            }
            return true
        }
    }

    class DeprecationCondition(
        val methodLocation: MethodLocation,
        val message: String,
        private vararg val predicates: Predicate,
        val constraint: Constraint? = null,
    ) : Predicate {
        override fun matches(
            context: JavaContext,
            node: UCallExpression,
            method: PsiMethod
        ): Boolean {
            if (!methodLocation.matches(context, node, method)) {
                // Method location does not match. The whole condition is not applicable.
                return false
            }

            for (predicate in predicates) {
                if (!predicate.matches(context, node, method)) {
                    // The predicate does not match. The whole condition is not applicable.
                    return false
                }
            }
            return true
        }
    }

    // Mark final so that extending classes are forced to only use the constructor-level
    // configuration APIs
    final override fun getApplicableMethodNames(): List<String> = applicableMethods

    // Mark final so that extending classes are forced to only use the constructor-level
    // configuration APIs.
    final override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        // Find the first condition that matches and report the issue.
        conditions.find { condition ->
            if (condition.matches(context, node, method)) {
                val incident = Incident(context)
                    .issue(issue)
                    .at(node)
                    .message(condition.message)
                condition.constraint?.let { constraint ->
                    context.report(incident, constraint)
                } ?: context.report(incident)

                // We matched and we're not waiting on any constraints.
                if (condition.constraint == null) {
                    return@find true
                }
            }
            return@find false
        }
    }
}
