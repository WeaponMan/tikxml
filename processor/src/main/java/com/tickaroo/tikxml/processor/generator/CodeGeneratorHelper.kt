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

package com.tickaroo.tikxml.processor.generator

import com.squareup.javapoet.*
import com.sun.tools.javac.jvm.Code
import com.tickaroo.tikxml.TikXmlConfig
import com.tickaroo.tikxml.TypeConverterNotFoundException
import com.tickaroo.tikxml.XmlReader
import com.tickaroo.tikxml.annotation.ElementNameMatcher
import com.tickaroo.tikxml.processor.ProcessingException
import com.tickaroo.tikxml.processor.field.ListElementField
import com.tickaroo.tikxml.processor.field.PolymorphicSubstitutionField
import com.tickaroo.tikxml.processor.field.PolymorphicSubstitutionListField
import com.tickaroo.tikxml.processor.field.PolymorphicTypeElementNameMatcher
import com.tickaroo.tikxml.processor.field.access.FieldAccessResolver
import com.tickaroo.tikxml.processor.utils.*
import com.tickaroo.tikxml.processor.xml.PlaceholderXmlElement
import com.tickaroo.tikxml.processor.xml.XmlChildElement
import com.tickaroo.tikxml.processor.xml.XmlElement
import com.tickaroo.tikxml.typeadapter.AttributeBinder
import com.tickaroo.tikxml.typeadapter.ChildElementBinder
import com.tickaroo.tikxml.typeadapter.NestedChildElementBinder
import java.io.IOException
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 *
 * @author Hannes Dorfmann
 */
class CodeGeneratorHelper(
        val customTypeConverterManager: CustomTypeConverterManager,
        val typeConvertersForPrimitives: Set<String>,
        val valueType: ClassName,
        val elementUtils: Elements,
        val typeUtils: Types
) {

    // Constants
    companion object PARAMS {
        const val namespaceDefinitionPrefix = "xmlns"
        const val valueParam = "value"
        const val tikConfigParam = "config"
        const val tikConfigMethodExceptionOnUnreadXml = "exceptionOnUnreadXml"
        const val raiseExceptionOnUnReadVar = "exceptionOnUnreadXml"
        const val textContentParam = "textContent"
        const val readerParam = "reader"
        const val writerParam = "writer"
        const val attributeBindersParam = "attributeBinders"
        const val childElementBindersParam = "childElementBinders"

        val booleanTypes = mapOf<String, String>(
                "java.lang.Boolean" to "java.lang.Boolean",
                "boolean" to "boolean",
                "kotlin.Boolean" to "java.lang.Boolean",
                Boolean::class.java.canonicalName to "java.lang.Boolean"
        )

        val doubleTypes = mapOf<String, String>(
                "java.lang.Double" to "java.lang.Double",
                "double" to "double",
                "kotlin.Double" to "java.lang.Double",
                Double::class.java.canonicalName to "java.lang.Double"
        )

        val integerTypes = mapOf<String, String>(
                "java.lang.Integer" to "java.lang.Integer",
                "int" to "int",
                "kotlin.Int" to "java.lang.Integer",
                Integer::class.java.canonicalName to "java.lang.Integer"
        )

        val stringTypes = Collections.singletonMap(
                String::class.java.canonicalName, "java.lang.String"
        )

        val longTypes = mapOf<String, String>(
                "java.lang.Long" to "java.lang.Long",
                "long" to "long",
                "kotlin.Long" to "java.lang.Long",
                Long::class.java.canonicalName to "java.lang.Long"
        )

        fun tryGeneratePrimitiveConverter(
                typesMap: Map<String, String>,
                typeConvertersForPrimitives: Set<String>,
                codeWriterFormat: String
        ): String? {
            return typesMap.asSequence().filter {
                typeConvertersForPrimitives.contains(it.key)
            }.map {
                it.value
            }.firstOrNull()?.let { className ->
                codeWriterFormat.format(className)
            }
        }

        fun surroundWithTryCatch(resolvedCodeBlock: CodeBlock): CodeBlock =
                CodeBlock.builder()
                        .beginControlFlow("try")
                        .add(resolvedCodeBlock)
                        .nextControlFlow("catch(\$T e)", ClassName.get(TypeConverterNotFoundException::class.java))
                        .addStatement("throw e")
                        .nextControlFlow("catch(\$T e)", ClassName.get(Exception::class.java))
                        .addStatement("throw new \$T(e)", ClassName.get(IOException::class.java))
                        .endControlFlow()
                        .build()

        fun surroundWithTryCatch(
                elementNotPrimitive: Boolean,
                resolvedGetter: String,
                writeStatement: String
        ): CodeBlock {
            val writeCodeBlock = CodeBlock.builder()
                    .addStatement(writeStatement)
                    .build()

            val tryCatchCodeBlock = surroundWithTryCatch(writeCodeBlock)
            if (elementNotPrimitive) {
                // Only write values if they are not null, otherwise don't write values as xml
                return CodeBlock.builder()
                        .beginControlFlow("if ($resolvedGetter != null)")
                        .add(tryCatchCodeBlock)
                        .endControlFlow()
                        .build()
            }

            return tryCatchCodeBlock
        }

        fun writeValueWithoutConverter(
                elementNotPrimitive: Boolean,
                resolvedGetter: String,
                xmlWriterMethod: String,
                attributeName: String? = null
        ): CodeBlock {
            val builder = CodeBlock.builder()

            if (elementNotPrimitive) {
                // Only write values if they are not null, otherwise don't write values as xml
                builder.beginControlFlow("if ($resolvedGetter != null)")
            }

            if (attributeName != null) {
                builder.addStatement("$writerParam.$xmlWriterMethod(\"$attributeName\", $resolvedGetter)")
            } else {
                // For text content support
                builder.addStatement("$writerParam.$xmlWriterMethod($resolvedGetter)")
            }

            if (elementNotPrimitive) {
                // Only write values if they are not null, otherwise don't write values as xml
                builder.endControlFlow()
            }
            return builder.build()
        }
    }


    /**
     * Get the parameterized (generics) type name for [AttributeBinder]
     */
    val attributeBinderType = ParameterizedTypeName.get(ClassName.get(AttributeBinder::class.java), valueType)

    /**
     * Get the parameterized (generics) type name for [ChildElementBinder]
     */
    val childElementBinderType = ParameterizedTypeName.get(ClassName.get(ChildElementBinder::class.java), valueType)

    /**
     * Get the parameterized (generics) type name for [NestedChildElementBinder]
     */
    val nestedChildElementBinderType = ParameterizedTypeName.get(ClassName.get(NestedChildElementBinder::class.java), valueType)

    /**
     */
    private var temporaryVaribaleCounter = 0

    /**
     * Sometime during codegenetation we need unique variable names (i.e. for temporary variables).
     * This function generates one for you by simply adding a unique number
     * (a counter that will be incremented everytime you call this method) at the end of the original
     * variable name
     */
    fun uniqueVariableName(originalVariableName: String) = originalVariableName + (temporaryVaribaleCounter++)

    /**
     * Generate the attribute binders
     */
    fun generateAttributeBinders(currentElement: XmlElement): CodeBlock {

        // TODO optimize it for one single attribute

        val builder = CodeBlock.builder()
        for ((xmlElementName, attributeField) in currentElement.attributes) {

            val fromXmlMethodBuilder = fromXmlMethodBuilder()
            fromXmlMethodBuilder.addCode(assignViaTypeConverterOrPrimitive(attributeField.element, AssignmentType.ATTRIBUTE, attributeField.accessResolver, attributeField.converterQualifiedName))


            val anonymousAttributeBinder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.get(AttributeBinder::class.java), valueType))
                    .addMethod(fromXmlMethodBuilder.build())
                    .build()


            builder.addStatement("$attributeBindersParam.put(\$S, \$L)", xmlElementName, anonymousAttributeBinder)
        }

        return builder.build()
    }

    fun generateElementReadFlowControl(element: XmlElement, targetClassToParseInto: ClassName): CodeBlock {
        val notNestedElements = element.childElements.filter { it.value !is PlaceholderXmlElement }.map {
            it.key to it.value.generateReadXmlCodeWithoutMethod(this, false)
        }
        val generateChildBinder = element.childElements.size != notNestedElements.size
        val typeName = ParameterizedTypeName.get(ClassName.get(ChildElementBinder::class.java), targetClassToParseInto)

        return CodeBlock.builder().apply {
            addStatement("\$L.beginElement()", readerParam)
            addStatement("String elementName = \$L.nextElementName()", readerParam)
            when (notNestedElements.size) {
                0 -> {
                    if (generateChildBinder) {
                        add(generateChildElementBinding(typeName))
                    } else {
                        add(generateCheckForNotMappedElements())
                    }
                }
                1 -> {
                    val (attrName, assignCode) = notNestedElements.first()
                    beginControlFlow("if(elementName.equals(\$S))", attrName)
                    add(assignCode)
                    addStatement("\$L.endElement()", readerParam)
                    nextControlFlow("else")
                    if (generateChildBinder) {
                        add(generateChildElementBinding(typeName))
                    } else {
                        add(generateCheckForNotMappedElements())
                    }
                    endControlFlow()
                }
                else -> {
                    beginControlFlow("switch(elementName)")

                    notNestedElements.forEach {
                        val (attrName, assignCode) = it
                        add("case \$S:\n", attrName)
                        indent()
                        add(assignCode)
                        addStatement("\$L.endElement()", readerParam)
                        addStatement("break")
                        unindent()
                    }

                    add("default:\n")
                    indent()
                    if (generateChildBinder) {
                        add(generateChildElementBinding(typeName))
                    } else {
                        add(generateCheckForNotMappedElements())
                    }
                    addStatement("break")
                    unindent()
                    endControlFlow()
                }
            }
        }.build()
    }


    /**
     * Generate code for reading xml parameters
     */
    fun generateAttributesReadFlowControl(element: XmlElement): CodeBlock {
        val attributes = element.attributes.map {
            it.key to assignViaTypeConverterOrPrimitive(
                    it.value.element, AssignmentType.ATTRIBUTE,
                    it.value.accessResolver,
                    it.value.converterQualifiedName
            )
        }

        return CodeBlock.builder().apply {
            beginControlFlow("while(\$L.hasAttribute())", readerParam)
            addStatement("String attributeName = \$L.nextAttributeName()", readerParam)
            when (attributes.size) {
                0 -> {
                    add(generateCheckForNotMappedAttributes())
                }
                1 -> {
                    val (attrName, assignCode) = attributes.first()
                    beginControlFlow("if(attributeName.equals(\$S))", attrName)
                    add(assignCode)
                    nextControlFlow("else")
                    add(generateCheckForNotMappedAttributes())
                    endControlFlow()
                }
                else -> {
                    beginControlFlow("switch(attributeName)")

                    attributes.forEach {
                        val (attrName, assignCode) = it
                        add("case \$S:\n", attrName)
                        indent()
                        add(assignCode)
                        addStatement("break")
                        unindent()
                    }

                    add("default:\n")
                    indent()
                    add(generateCheckForNotMappedAttributes())
                    addStatement("break")
                    unindent()
                    endControlFlow() // end switch
                }
            }
            endControlFlow() // end while hasAttribute()
        }.build()
    }

    fun generateChildElementBinding(typeName: ParameterizedTypeName) = CodeBlock.builder()
            .addStatement("\$T childElementBinder = \$L.get(elementName)", typeName, childElementBindersParam)
            .beginControlFlow("if (childElementBinder != null)")
            .addStatement("childElementBinder.fromXml(\$L, \$L, \$L)", readerParam, tikConfigParam, valueParam)
            .addStatement("\$L.endElement()", readerParam)
            .nextControlFlow("else")
            .add(generateCheckForNotMappedElements())
            .endControlFlow()
            .build()

    fun generateCheckForNotMappedAttributes(): CodeBlock = CodeBlock.builder()
            .beginControlFlow(
                    "if (\$L && !attributeName.startsWith(\$S))",
                    raiseExceptionOnUnReadVar,
                    namespaceDefinitionPrefix
            )
            .addStatement(
                    "throw new \$T(\$S+attributeName+\$S+\$L.getPath()+\$S)", IOException::class.java,
                    "Could not map the xml attribute with the name '",
                    "' at path ",
                    readerParam,
                    " to java class. Have you annotated such a field in your java class to map this xml attribute? " +
                            "Otherwise you can turn this error message off with " +
                            "TikXml.Builder().exceptionOnUnreadXml(false).build()."
            )
            .endControlFlow() // End if
            .addStatement("\$L.skipAttributeValue()", readerParam)
            .build()


    fun generateCheckForNotMappedElements(): CodeBlock = CodeBlock.builder()
            .beginControlFlow("if (\$L)", raiseExceptionOnUnReadVar)
            .addStatement("throw new \$T(\$S + \$L + \$S + \$L.getPath()+\$S)", IOException::class.java,
                    "Could not map the xml element with the tag name <", "elementName", "> at path '",
                    readerParam,
                    "' to java class. Have you annotated such a field in your java class to map this xml attribute?" +
                            " Otherwise you can turn this error message off with " +
                            "TikXml.Builder().exceptionOnUnreadXml(false).build().")
            .endControlFlow() // End if
            .addStatement("\$L.skipRemainingElement()", readerParam)
            .build()

    /**
     * get the assignment statement for reading attributes
     */
    fun assignViaTypeConverterOrPrimitive(
            element: Element,
            assignmentType: AssignmentType,
            accessResolver: FieldAccessResolver,
            customTypeConverterQualifiedClassName: String?
    ): CodeBlock {
        val type = element.asType()
        val xmlReaderMethodPrefix = assignmentType.xmlReaderMethodPrefix()
        val codeWriterFormat = "$tikConfigParam.getTypeConverter(%s.class).read($readerParam.$xmlReaderMethodPrefix())"

        var resolveMethodName = ""
        val assignmentStatement = when {
            customTypeConverterQualifiedClassName != null -> {
                val fieldName = customTypeConverterManager.getFieldNameForConverter(customTypeConverterQualifiedClassName)
                "$fieldName.read($readerParam.$xmlReaderMethodPrefix())"
            }
            type.isString() -> {
                tryGeneratePrimitiveConverter(stringTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isBoolean() -> {
                resolveMethodName = "AsBoolean"
                tryGeneratePrimitiveConverter(booleanTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isDouble() -> {
                resolveMethodName = "AsDouble"
                tryGeneratePrimitiveConverter(doubleTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isInt() -> {
                resolveMethodName = "AsInt"
                tryGeneratePrimitiveConverter(integerTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isLong() -> {
                resolveMethodName = "AsLong"
                tryGeneratePrimitiveConverter(longTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            else -> {
                codeWriterFormat.format(type.toString())
            }
        }

        return assignmentStatement?.let { surroundWithTryCatch(accessResolver.resolveAssignment(it)) }
                ?: accessResolver.resolveAssignment("$readerParam.$xmlReaderMethodPrefix$resolveMethodName()")
    }

    /**
     * Generate a [NestedChildElementBinder] and recursively calls [com.tickaroo.tikxml.processor.xml.XmlChildElement] to generate its code
     */
    fun generateNestedChildElementBinder(element: XmlElement): TypeSpec {

        val initializerBuilder = CodeBlock.builder()
        if (element.hasAttributes()) {
            val attributeMapType = ParameterizedTypeName.get(ClassName.get(HashMap::class.java), ClassName.get(String::class.java), attributeBinderType)
            initializerBuilder.addStatement("$attributeBindersParam = new \$T()", attributeMapType);
            initializerBuilder.add(generateAttributeBinders(element))
        }

        if (element.hasChildElements()) {
            val childBinderTypeMap = ParameterizedTypeName.get(ClassName.get(HashMap::class.java), ClassName.get(String::class.java), childElementBinderType)
            initializerBuilder.addStatement("$childElementBindersParam = new \$T()", childBinderTypeMap);
            for ((xmlName, xmlElement) in element.childElements) {
                initializerBuilder.addStatement("${CodeGeneratorHelper.childElementBindersParam}.put(\$S, \$L)", xmlName, xmlElement.generateReadXmlCode(this, true))
            }
        }


        // TODO text content?
        return TypeSpec.anonymousClassBuilder("false")
                .addSuperinterface(nestedChildElementBinderType)
                .addInitializerBlock(initializerBuilder.build())
                .build()
    }

    fun ignoreAttributes(isNested: Boolean) = CodeBlock.builder()
            .beginControlFlow("while(\$L.hasAttribute())", readerParam)
            .addStatement("String attributeName = \$L.nextAttributeName()", readerParam)
            .apply {
                if (isNested) {
                    beginControlFlow("if (\$L.exceptionOnUnreadXml() && !attributeName.startsWith(\$S))", tikConfigParam, "xmlns")
                } else {
                    beginControlFlow("if (\$L && !attributeName.startsWith(\$S))", raiseExceptionOnUnReadVar, "xmlns")
                }
            }
            .addStatement("throw new \$T(\"Unread attribute '\"+ attributeName +\"' at path \"+ $readerParam.getPath())", ClassName.get(IOException::class.java))
            .endControlFlow()
            .addStatement("\$L.skipAttributeValue()", readerParam)
            .endControlFlow()
            .build()

    fun fromXmlMethodBuilder() = MethodSpec.methodBuilder("fromXml")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(XmlReader::class.java, readerParam)
            .addParameter(TikXmlConfig::class.java, tikConfigParam)
            .addParameter(valueType, valueParam)
            .addException(IOException::class.java)

    /**
     * Generates the code to write attributes as xml
     */
    fun writeAttributesAsXml(currentElement: XmlElement): CodeBlock {

        val builder = CodeBlock.builder()
        for ((_, attributeField) in currentElement.attributes) {
            builder.add(writeAttributeViaTypeConverterOrPrimitive(attributeField.name, attributeField.element, attributeField.accessResolver, attributeField.converterQualifiedName))
        }

        return builder.build()
    }

    /**
     * write the value of an attribute or
     */
    fun writeAttributeViaTypeConverterOrPrimitive(
            attributeName: String,
            element: Element,
            accessResolver: FieldAccessResolver,
            customTypeConverterQualifiedClassName: String?
    ): CodeBlock {
        val type = element.asType()
        val elementNotPrimitive = !type.isPrimitive()
        val xmlWriterMethod = "attribute"
        val resolvedGetter = accessResolver.resolveGetterForWritingXml()
        val codeWriterFormat = "$writerParam.$xmlWriterMethod(\"$attributeName\", $tikConfigParam.getTypeConverter(%s.class).write($resolvedGetter))"

        val writeStatement = when {
            customTypeConverterQualifiedClassName != null -> {
                val fieldName = customTypeConverterManager.getFieldNameForConverter(customTypeConverterQualifiedClassName)
                "$writerParam.$xmlWriterMethod(\"$attributeName\", $fieldName.write($resolvedGetter))"
            }
            type.isString() -> {
                tryGeneratePrimitiveConverter(stringTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isBoolean() -> {
                tryGeneratePrimitiveConverter(booleanTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isDouble() -> {
                tryGeneratePrimitiveConverter(doubleTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isInt() -> {
                tryGeneratePrimitiveConverter(integerTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isLong() -> {
                tryGeneratePrimitiveConverter(longTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            else -> {
                codeWriterFormat.format(type.toString())
            }
        }

        return writeStatement?.let { surroundWithTryCatch(elementNotPrimitive, resolvedGetter, it) }
                ?: writeValueWithoutConverter(elementNotPrimitive, resolvedGetter, xmlWriterMethod, attributeName)
    }

    /**
     * Writes the typical <foo attr="1" other="asd" opeining xml stuff
     */
    fun writeBeginElementAndAttributes(childElement: XmlChildElement) =
            CodeBlock.builder()
                    .add(writeBeginElement(childElement.name))
                    .apply { add(writeAttributesAsXml(childElement)) }
                    .build()

    /**
     * writes the typical <foo  xml opening stuff
     */
    fun writeBeginElement(elementName: String) =
            CodeBlock.builder().addStatement("$writerParam.beginElement(\$S)", elementName).build()

    /**
     * Writes the code to generate xml by generating to the corresponding type adapter depending on the type of the element
     */
    fun writeResolvePolymorphismAndDelegteToTypeAdpters(
            variableName: String,
            typeElementNameMatcher: List<PolymorphicTypeElementNameMatcher>
    ) = CodeBlock.builder().apply {
        // Cannot be done with instanceof because then the inheritance hierarchy matters and so matters the order of the if checks
        val orderByInheritanceHierarchy = orderByInheritanceHierarchy(typeElementNameMatcher, elementUtils, typeUtils)
        if (orderByInheritanceHierarchy.size != typeElementNameMatcher.size) {
            throw ProcessingException(null,
                    "Oops: an unexpected exception has occurred while determining " +
                            "the correct order for inheritance hierarchy.\n " +
                            "Please file an issue at https://github.com/Tickaroo/tikxml/issues\n" +
                            "Some debug information:\n" +
                            "\tordered hierarchy elements: ${orderByInheritanceHierarchy.size}\n " +
                            "\tTypeElementMatcher size ${typeElementNameMatcher.size}\n " +
                            "\tordered hierarchy list: $orderByInheritanceHierarchy\n" +
                            "\tTypeElementMatcher list $typeElementNameMatcher"
            )
        }
        orderByInheritanceHierarchy.forEachIndexed { i, nameMatcher ->
            val className = ClassName.get(nameMatcher.type)
            if (i == 0) {
                beginControlFlow("if ($variableName instanceof \$T)", className)
            } else {
                nextControlFlow("else if ($variableName instanceof \$T)", className)
            }
            val defaultName = nameMatcher.type.getWriteXmlName()
            val overrideName = if (defaultName == nameMatcher.xmlElementName) null else nameMatcher.xmlElementName
            addStatement(
                    "$tikConfigParam.getTypeAdapter(\$T.class).toXml($writerParam, $tikConfigParam, (\$T) $variableName, \$S)",
                    className,
                    className,
                    overrideName
            )
        }


        if (typeElementNameMatcher.isNotEmpty()) {
            nextControlFlow("else")
            addStatement(
                    "throw new \$T(\$S + $variableName + \$S)",
                    ClassName.get(IOException::class.java),
                    "Don't know how to write the element of type ",
                    " as XML. Most likely you have forgotten to register for this type " +
                            "with @${ElementNameMatcher::class.simpleName} when resolving polymorphism."
            )
            endControlFlow()
        }
    }.build()

    /**
     * Writes the text content via type adapter. This is used i.e. for property fields and or textContent fields
     */
    //TODO: almost same as writeAttributeViaTypeConverterOrPrimitive function (xmlWriterMethod is not same, codeWriterFormat is not same) but is doable to merge it into one
    fun writeTextContentViaTypeConverterOrPrimitive(
            element: Element,
            accessResolver: FieldAccessResolver,
            customTypeConverterQualifiedClassName: String?,
            asCData: Boolean
    ): CodeBlock {
        val type = element.asType()
        val elementNotPrimitive = !type.isPrimitive()
        val xmlWriterMethod = if (asCData && type.isString()) "textContentAsCData" else "textContent"
        val resolvedGetter = accessResolver.resolveGetterForWritingXml()
        val codeWriterFormat = "$writerParam.$xmlWriterMethod($tikConfigParam.getTypeConverter(%s.class).write($resolvedGetter))"

        val writeStatement = when {
            customTypeConverterQualifiedClassName != null -> {
                val fieldName = customTypeConverterManager.getFieldNameForConverter(customTypeConverterQualifiedClassName)
                "$writerParam.$xmlWriterMethod($fieldName.write($resolvedGetter))"
            }
            type.isString() -> {
                tryGeneratePrimitiveConverter(stringTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isBoolean() -> {
                tryGeneratePrimitiveConverter(booleanTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isDouble() -> {
                tryGeneratePrimitiveConverter(doubleTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isInt() -> {
                tryGeneratePrimitiveConverter(integerTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            type.isLong() -> {
                tryGeneratePrimitiveConverter(longTypes, typeConvertersForPrimitives, codeWriterFormat)
            }
            else -> {
                codeWriterFormat.format(type.toString())
            }
        }

        return writeStatement?.let { surroundWithTryCatch(elementNotPrimitive, resolvedGetter, it) }
                ?: writeValueWithoutConverter(elementNotPrimitive, resolvedGetter, xmlWriterMethod)
    }

    /**
     * Generates the code tat is able to resolve polymorphism for lists, polymorphic elements or by simply forwarding code generation to the child.
     */
    fun writeChildrenByResolvingPolymorphismElementsOrFieldsOrDelegateToChildCodeGenerator(xmlElement: XmlElement): CodeBlock {
        val sizeVarName = ListElementField.sizeVarName
        val itemVarName = ListElementField.itemVarName
        val listVarName = ListElementField.listVarName

        return CodeBlock.builder().apply {
            xmlElement.childElements.values.groupBy { it.element }.forEach {
                val first = it.value.first()
                when (first) {
                    is PolymorphicSubstitutionListField -> {
                        // Resolve polymorphism on list items
                        val listType = ClassName.get(first.originalElementTypeMirror)
                        val resolvedGetter = first.accessResolver.resolveGetterForWritingXml()

                        val elementTypeMatchers: List<PolymorphicTypeElementNameMatcher> = it.value.map {
                            val i = it as PolymorphicSubstitutionListField
                            PolymorphicTypeElementNameMatcher(i.name, i.typeMirror)
                        }

                        beginControlFlow("if ($resolvedGetter != null)")
                        addStatement("\$T $listVarName = $resolvedGetter", listType)
                        beginControlFlow("for (int i =0, $sizeVarName = $listVarName.size(); i<$sizeVarName; i++)")
                        addStatement("\$T $itemVarName = $listVarName.get(i)", ClassName.get(Object::class.java))
                        add(writeResolvePolymorphismAndDelegteToTypeAdpters(itemVarName, elementTypeMatchers)) // does the if instance of checks
                        endControlFlow() // end for loop
                        endControlFlow() // end != null check

                    }
                    is PolymorphicSubstitutionField -> {
                        val resolvedGetter = first.accessResolver.resolveGetterForWritingXml()
                        // Resolve polymorphism for fields
                        val elementTypeMatchers: List<PolymorphicTypeElementNameMatcher> = it.value.map {
                            val i = it as PolymorphicSubstitutionField
                            PolymorphicTypeElementNameMatcher(i.name, i.typeMirror)
                        }
                        beginControlFlow("if ($resolvedGetter != null)")
                        addStatement("\$T element = $resolvedGetter", ClassName.get(first.originalElementTypeMirror))  // does the if instance of checks
                        add(writeResolvePolymorphismAndDelegteToTypeAdpters("element", elementTypeMatchers))
                        endControlFlow() // end != null check

                    }
                    else -> it.value.forEach { add(it.generateWriteXmlCode(this@CodeGeneratorHelper)) }
                }
            }
        }.build()
    }

    /**
     * Used to specify whether we are going to assign an xml attribute or an xml element text content
     */
    enum class AssignmentType {
        ATTRIBUTE,
        ELEMENT;

        fun xmlReaderMethodPrefix() = when (this) {
            ATTRIBUTE -> "nextAttributeValue"
            ELEMENT -> "nextTextContent"
        }
    }
}