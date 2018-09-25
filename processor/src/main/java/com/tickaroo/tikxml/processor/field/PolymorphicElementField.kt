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
import com.tickaroo.tikxml.processor.ProcessingException
import com.tickaroo.tikxml.processor.field.access.FieldAccessResolver
import com.tickaroo.tikxml.processor.generator.CodeGeneratorHelper
import java.util.*
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/**
 * Represents a Field with [com.tickaroo.tikxml.annotation.Element] annotation
 * @author Hannes Dorfmann
 */
open class PolymorphicElementField(
        element: VariableElement,
        name: String,
        val typeElementNameMatcher: List<PolymorphicTypeElementNameMatcher>
) : ElementField(
        element,
        name
) {

    val substitutions = ArrayList<PolymorphicSubstitutionField>()

    override fun generateReadXmlCode(codeGeneratorHelper: CodeGeneratorHelper): TypeSpec {
        throw ProcessingException(element, "Oops, en error has occurred while generating reading xml code for $this." +
                " Please fill an issue at https://github.com/Tickaroo/tikxml/issues")
    }

    override fun generateWriteXmlCode(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        throw ProcessingException(element, "Oops, en error has occurred while generating writing xml code for $this." +
                " Please fill an issue at https://github.com/Tickaroo/tikxml/issues")
    }

    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        throw ProcessingException(element, "Oops, en error has occurred while generating reading xml code for $this. " +
                "Please fill an issue at https://github.com/Tickaroo/tikxml/issues")
    }
}

class PolymorphicListElementField(
        element: VariableElement,
        name: String,
        typeElementNameMatcher: List<PolymorphicTypeElementNameMatcher>,
        val genericListTypeMirror: TypeMirror
) : PolymorphicElementField(
        element,
        name,
        typeElementNameMatcher
)

/**
 * This kind of element will be used to replace a [PolymorphicElementField]
 */
open class PolymorphicSubstitutionField(
        element: VariableElement,
        override val typeMirror: TypeMirror,
        override var accessResolver: FieldAccessResolver,
        name: String,
        val originalElementTypeMirror: TypeMirror
) : ElementField(
        element,
        name
) {
    override fun isXmlElementAccessableFromOutsideTypeAdapter(): Boolean = false

    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val className = ClassName.get(typeMirror)
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val readerVarName = CodeGeneratorHelper.readerParam
        val valueFromAdapter = "(\$T) $configVarName.getTypeAdapter(\$T.class).fromXml($readerVarName, $configVarName)"
        return accessResolver.resolveAssignment(valueFromAdapter, className, className)
    }
}

/**
 * This kind of element will be used to replace a [PolymorphicElementField] but for List elements
 */
class PolymorphicSubstitutionListField(
        element: VariableElement,
        typeMirror: TypeMirror,
        accessResolver: FieldAccessResolver,
        name: String,
        private val genericListType: TypeMirror
) : PolymorphicSubstitutionField(
        element,
        typeMirror,
        accessResolver,
        name,
        element.asType()
) {
    override fun generateReadXmlCodeWithoutMethod(codeGeneratorHelper: CodeGeneratorHelper): CodeBlock {
        val className = ClassName.get(typeMirror)
        val arrayListType = ParameterizedTypeName.get(ClassName.get(ArrayList::class.java), ClassName.get(genericListType))
        val configVarName = CodeGeneratorHelper.tikConfigParam
        val readerVarName = CodeGeneratorHelper.readerParam
        val valueFromAdapter = "(\$T) $configVarName.getTypeAdapter(\$T.class).fromXml($readerVarName, $configVarName)"
        val resolvedGetter = accessResolver.resolveGetterForReadingXml()
        val uniqueItemVar = codeGeneratorHelper.uniqueVariableName("element")
        return CodeBlock.builder()
                .beginControlFlow("if ($resolvedGetter == null)")
                .add(accessResolver.resolveAssignment("new \$T()", arrayListType)) // TODO remove this
                .endControlFlow()
                .addStatement("\$T $uniqueItemVar = $valueFromAdapter", className, className, className)
                .addStatement("$resolvedGetter.add($uniqueItemVar)")
                .build()
    }
}

data class PolymorphicTypeElementNameMatcher(val xmlElementName: String, val type: TypeMirror)