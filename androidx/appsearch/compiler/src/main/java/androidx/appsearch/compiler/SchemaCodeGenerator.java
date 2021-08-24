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

package androidx.appsearch.compiler;

import androidx.annotation.NonNull;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/** Generates java code for an {@link androidx.appsearch.app.AppSearchSchema}. */
class SchemaCodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final AppSearchDocumentModel mModel;

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull AppSearchDocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        new SchemaCodeGenerator(env, model).generate(classBuilder);
    }

    private SchemaCodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model) {
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;
    }

    private void generate(@NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        classBuilder.addField(
                FieldSpec.builder(String.class, "SCHEMA_TYPE")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", mModel.getSchemaName())
                        .build());

        classBuilder.addMethod(
                MethodSpec.methodBuilder("getSchemaType")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(mHelper.mStringType))
                        .addAnnotation(Override.class)
                        .addStatement("return SCHEMA_TYPE")
                        .build());

        classBuilder.addMethod(
                MethodSpec.methodBuilder("getSchema")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(mHelper.getAppSearchClass("AppSearchSchema"))
                        .addAnnotation(Override.class)
                        .addException(mHelper.getAppSearchExceptionClass())
                        .addStatement("return $L", createSchemaInitializer())
                        .build());
    }

    private CodeBlock createSchemaInitializer() throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T(SCHEMA_TYPE)", mHelper.getAppSearchClass("AppSearchSchema", "Builder"))
                .indent();
        for (VariableElement property : mModel.getPropertyFields().values()) {
            codeBlock.add("\n.addProperty($L)", createPropertySchema(property));
        }
        codeBlock.add("\n.build()").unindent();
        return codeBlock.build();
    }

    private CodeBlock createPropertySchema(@NonNull VariableElement property)
            throws ProcessingException {
        AnnotationMirror annotation =
                mHelper.getAnnotation(property, IntrospectionHelper.PROPERTY_CLASS);
        Map<String, Object> params = mHelper.getAnnotationParams(annotation);

        // Start the builder for that property
        String propertyName = mModel.getPropertyName(property);
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T($S)",
                        mHelper.getAppSearchClass("AppSearchSchema", "PropertyConfig", "Builder"),
                        propertyName)
                .indent();

        // Find the property type
        Types typeUtil = mEnv.getTypeUtils();
        TypeMirror propertyType;
        boolean repeated = false;
        boolean isPropertyDocument = false;
        if (property.asType().getKind() == TypeKind.ERROR) {
            throw new ProcessingException("Property type unknown to java compiler", property);
        } else if (typeUtil.isAssignable(
                typeUtil.erasure(property.asType()), mHelper.mCollectionType)) {
            List<? extends TypeMirror> genericTypes =
                    ((DeclaredType) property.asType()).getTypeArguments();
            if (genericTypes.isEmpty()) {
                throw new ProcessingException(
                        "Property is repeated but has no generic type", property);
            }
            propertyType = genericTypes.get(0);
            repeated = true;
        } else if (property.asType().getKind() == TypeKind.ARRAY
                // Byte arrays have a native representation in Icing, so they are not considered a
                // "repeated" type
                && !typeUtil.isSameType(property.asType(), mHelper.mBytePrimitiveArrayType)
                && !typeUtil.isSameType(property.asType(), mHelper.mByteBoxArrayType)) {
            propertyType = ((ArrayType) property.asType()).getComponentType();
            repeated = true;

        } else {
            propertyType = property.asType();
        }
        ClassName propertyTypeEnum;
        if (typeUtil.isSameType(propertyType, mHelper.mStringType)) {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_STRING");
        } else if (typeUtil.isSameType(propertyType, mHelper.mIntegerBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntPrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mLongBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mLongPrimitiveType)) {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_INT64");
        } else if (typeUtil.isSameType(propertyType, mHelper.mFloatBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatPrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mDoublePrimitiveType)) {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_DOUBLE");
        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mBooleanPrimitiveType)) {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_BOOLEAN");
        } else if (typeUtil.isSameType(propertyType, mHelper.mBytePrimitiveArrayType)
                || typeUtil.isSameType(propertyType, mHelper.mByteBoxArrayType)) {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_BYTES");
        } else {
            propertyTypeEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "DATA_TYPE_DOCUMENT");
            isPropertyDocument = true;
        }
        codeBlock.add("\n.setDataType($T)", propertyTypeEnum);

        if (isPropertyDocument) {
            codeBlock.add("\n.setSchemaType($T.getInstance()"
                    + ".getOrCreateFactory($T.class).getSchemaType())",
                    mHelper.getAppSearchClass("DataClassFactoryRegistry"), propertyType);
        }
        // Find property cardinality
        ClassName cardinalityEnum;
        if (repeated) {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_REPEATED");
        } else if (Boolean.parseBoolean(params.get("required").toString())) {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_REQUIRED");
        } else {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_OPTIONAL");
        }
        codeBlock.add("\n.setCardinality($T)", cardinalityEnum);

        // Find tokenizer type
        int tokenizerType = Integer.parseInt(params.get("tokenizerType").toString());
        if (Integer.parseInt(params.get("indexingType").toString()) == 0) {
            //TODO(b/171857731) remove this hack after apply to Icing lib's change.
            tokenizerType = 0;
        }
        ClassName tokenizerEnum;
        if (tokenizerType == 0 || isPropertyDocument) {  // TOKENIZER_TYPE_NONE
            //It is only valid for tokenizer_type to be 'NONE' if the data type is
            // {@link PropertyConfig#DATA_TYPE_DOCUMENT}.
            tokenizerEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "TOKENIZER_TYPE_NONE");
        } else if (tokenizerType == 1) {  // TOKENIZER_TYPE_PLAIN
            tokenizerEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "TOKENIZER_TYPE_PLAIN");
        } else {
            throw new ProcessingException("Unknown tokenizer type " + tokenizerType, property);
        }
        codeBlock.add("\n.setTokenizerType($T)", tokenizerEnum);

        // Find indexing type
        int indexingType = Integer.parseInt(params.get("indexingType").toString());
        ClassName indexingEnum;
        if (indexingType == 0) {  // INDEXING_TYPE_NONE
            indexingEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_NONE");
        } else if (indexingType == 1) {  // INDEXING_TYPE_EXACT_TERMS
            indexingEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_EXACT_TERMS");
        } else if (indexingType == 2) {  // INDEXING_TYPE_PREFIXES
            indexingEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_PREFIXES");
        } else {
            throw new ProcessingException("Unknown indexing type " + indexingType, property);
        }
        codeBlock.add("\n.setIndexingType($T)", indexingEnum);

        // Done!
        codeBlock.add("\n.build()");
        codeBlock.unindent();
        return codeBlock.build();
    }
}
