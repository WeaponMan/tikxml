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

package com.tickaroo.tikxml.processor.field

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.tickaroo.tikxml.processor.generator.CodeGeneratorHelper
import com.tickaroo.tikxml.processor.utils.generateChildBindAnnonymousClass
import com.tickaroo.tikxml.processor.utils.generateCodeBlockIfValueCanBeNull
import com.tickaroo.tikxml.processor.utils.getWriteXmlName
import com.tickaroo.tikxml.processor.xml.XmlChildElement
import java.util.*
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/**
 * Represents a Field with [com.tickaroo.tikxml.annotation.Element] annotation
 * @author Hannes Dorfmann
 */
open class ElementField(element: VariableElement, name: String) : NamedField(element, name), XmlChildElement {

    override val attributes = LinkedHashMap<String, AttributeField>()
    override val childElements = LinkedHashMap<String, XmlChildElement>()

    override fun isXmlElementAccessableFromOutsideTypeAdapter() = false

    override fun generateReadXmlCode(codeGeneratorHelper: CodeGeneratorHelper, isNested: Boolean): TypeSpec {
        return generateChildBindAnnonymousClass(generateReadXmlCodeWithoutMethod(codeGeneratorHelper, isNested), codeGeneratorHelper)
    }

    override fun generateWriteXmlCode(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val typeMirror = element.asType()
        val className = ClassName.get(typeMirror)
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val writerVarName = CodeGeneratorHelper.writerParam
        val resolvedGetter = accessResolver.resolveGetterForWritingXml()
        val defaultName = element.getWriteXmlName()
        val overrideName = if (defaultName == name) null else name

        val body = CodeBlock.builder()
                .addStatement("$configVarName.getTypeAdapter(\$T.class).toXml($writerVarName, $configVarName, $resolvedGetter, \$S)", className, overrideName)
                .build();

        return generateCodeBlockIfValueCanBeNull(body, typeMirror, resolvedGetter)
    }

    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper, isNested: Boolean): CodeBlock {
        val className = ClassName.get(element.asType())
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val readerVarName = CodeGeneratorHelper.readerParam
        val valueFromAdapter = "(\$T) $configVarName.getTypeAdapter(\$T.class).fromXml($readerVarName, $configVarName)"
        return accessResolver.resolveAssignment(valueFromAdapter, className, className)
    }
}

class ListElementField(
        element: VariableElement,
        name: String,
        private val genericListType: TypeMirror
) : ElementField(element, name) {

    companion object {
        const val sizeVarName = "listSize"
        const val listVarName = "list"
        const val itemVarName = "item"
    }

    override fun generateWriteXmlCode(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val typeMirror = element.asType()
        val listType = ParameterizedTypeName.get(typeMirror)
        val itemClassName = ClassName.get(genericListType)
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val writerVarName = CodeGeneratorHelper.writerParam
        val resolvedGetter = accessResolver.resolveGetterForWritingXml()
        val defaultName = element.getWriteXmlName()
        val overrideName = if (defaultName == name) null else name

        val body = CodeBlock.builder()
                .addStatement("\$T $listVarName = $resolvedGetter", listType)
                .beginControlFlow("for (int i = 0, $sizeVarName = $listVarName.size(); i < $sizeVarName; i++)")
                .addStatement("\$T $itemVarName = $listVarName.get(i)", itemClassName)
                .addStatement("$configVarName.getTypeAdapter(\$T.class).toXml($writerVarName, $configVarName, $itemVarName, \$S)", itemClassName, overrideName)
                .endControlFlow()
                .build()

        return generateCodeBlockIfValueCanBeNull(body, typeMirror, resolvedGetter)
    }

    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper, isNested: Boolean): CodeBlock {
        val className = ClassName.get(genericListType)
        val arrayListType = ParameterizedTypeName.get(ClassName.get(ArrayList::class.java), className)
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val readerVarName = CodeGeneratorHelper.readerParam
        val valueFromAdapter = "(\$T) $configVarName.getTypeAdapter(\$T.class).fromXml($readerVarName, $configVarName)"
        val resolvedGetter = accessResolver.resolveGetterForReadingXml()
        return CodeBlock.builder()
                .beginControlFlow("if ($resolvedGetter == null)")
                .add(accessResolver.resolveAssignment("(\$T) new \$T()", arrayListType, arrayListType)) // TODO remove this
                .endControlFlow()
                .addStatement("$resolvedGetter.add($valueFromAdapter)", className, className)
                .build()
    }
}