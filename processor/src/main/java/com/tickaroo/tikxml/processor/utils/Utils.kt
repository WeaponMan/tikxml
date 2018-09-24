/*
 * Copyright (C) 2015 Hannes Dorfmann
 * Copyright (C) 2015 Tickaroo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tickaroo.tikxml.processor.utils

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeSpec
import com.tickaroo.tikxml.annotation.Xml
import com.tickaroo.tikxml.processor.generator.CodeGeneratorHelper
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

fun generateChildBindAnnonymousClass(body: CodeBlock, codeGeneratorHelper: CodeGeneratorHelper) =
        TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(codeGeneratorHelper.childElementBinderType)
                .addMethod(
                        codeGeneratorHelper.fromXmlMethodBuilder()
                                .addCode(body)
                                .build()
                )
                .build()

fun generateCodeBlockIfValueCanBeNull(body: CodeBlock, typeMirror: TypeMirror, resolvedGetter: String): CodeBlock {
    if (typeMirror.isPrimitive())
        return body

    return CodeBlock.builder()
            .beginControlFlow("if ($resolvedGetter != null)")
            .add(body)
            .endControlFlow()
            .build()
}

fun getNameRootNameFromElementOrAnnotation(simpleClassName: String, xmlAnnotation: Xml?): String {
    if (xmlAnnotation != null && xmlAnnotation.name.isNotEmpty()) {
        return xmlAnnotation.name
    }

    return simpleClassName.decapitalize()
}

fun VariableElement.getWriteXmlName(): String {
    return this.asType().getWriteXmlName()
}

fun TypeMirror.getWriteXmlName(): String {
    val element = (this as DeclaredType).asElement() as TypeElement
    val simpleClassName = element.simpleName.toString()
    val xmlAnnotation = element.getAnnotation(Xml::class.java)
    return getNameRootNameFromElementOrAnnotation(simpleClassName, xmlAnnotation)
}