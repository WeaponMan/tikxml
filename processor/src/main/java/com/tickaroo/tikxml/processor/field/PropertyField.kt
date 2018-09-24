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

import com.squareup.javapoet.*
import com.tickaroo.tikxml.processor.generator.CodeGeneratorHelper
import com.tickaroo.tikxml.processor.utils.endXmlElement
import com.tickaroo.tikxml.processor.utils.generateChildBindAnnonymousClass
import com.tickaroo.tikxml.processor.utils.generateCodeBlockIfValueCanBeNull
import com.tickaroo.tikxml.processor.xml.XmlChildElement
import java.util.*
import javax.lang.model.element.VariableElement

/**
 * This class represents a field annotated with [com.tickaroo.tikxml.annotation.PropertyElement]
 * @author Hannes Dorfmann
 */
class PropertyField(
        element: VariableElement,
        name: String,
        val writeAsCData: Boolean = false,
        val converterQualifiedName: String? = null
) : NamedField(element, name), XmlChildElement {

    override val attributes = LinkedHashMap<String, AttributeField>()
    override val childElements = LinkedHashMap<String, XmlChildElement>()

    override fun isXmlElementAccessableFromOutsideTypeAdapter() = true

    override fun generateReadXmlCode(codeGeneratorHelper: CodeGeneratorHelper): TypeSpec {
        return generateChildBindAnnonymousClass(generateReadXmlCodeWithoutMethod(codeGeneratorHelper), codeGeneratorHelper)
    }

    override fun generateWriteXmlCode(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val writeTextContentCodeBlock = codeGeneratorHelper.writeTextContentViaTypeConverterOrPrimitive(
                element,
                accessResolver,
                converterQualifiedName,
                writeAsCData
        )

        val body = CodeBlock.builder()
                .add(codeGeneratorHelper.writeBeginElementAndAttributes(this@PropertyField))
                .add(writeTextContentCodeBlock)
                .endXmlElement()
                .build()

        return generateCodeBlockIfValueCanBeNull(body, element.asType(), accessResolver.resolveGetterForWritingXml())
    }

    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val readTextContentCodeBlock = codeGeneratorHelper.assignViaTypeConverterOrPrimitive(
                element,
                CodeGeneratorHelper.AssignmentType.ELEMENT,
                accessResolver,
                converterQualifiedName
        )
        return CodeBlock.builder()
                .add(codeGeneratorHelper.ignoreAttributes())
                .add(readTextContentCodeBlock)
                .build()
    }
}