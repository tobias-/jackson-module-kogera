package com.fasterxml.jackson.module.kotlin._ported.test

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestPropertyRequiredness {

    private data class TestParamClass(val foo: String = "bar")

    @Suppress("UNUSED_PARAMETER")
    private class TestClass {
        fun setA(value: Int) {}
        fun setB(value: Int = 5) {}
        fun setC(value: Int?) {}
        fun setD(value: Int? = 5) {}

        fun getE(): Int = 5
        fun getF(): Int? = 5

        val g: Int = 5
        val h: Int? = 5

        fun setI(value: TestParamClass) {}
        fun setJ(value: TestParamClass = TestParamClass()) {}
        fun setK(value: TestParamClass?) {}
        fun setL(value: TestParamClass? = TestParamClass()) {}
    }

    @Test
    fun shouldHandleFalseFailOnNullForPrimitives() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        val testClass = TestClass::class.java
        "a".isOptionalForDeserializationOf(testClass, mapper)
        "b".isOptionalForDeserializationOf(testClass, mapper)
        "c".isOptionalForDeserializationOf(testClass, mapper)
        "d".isOptionalForDeserializationOf(testClass, mapper)

        "e".isOptionalForSerializationOf(testClass, mapper)
        "f".isOptionalForSerializationOf(testClass, mapper)

        "g".isRequiredForDeserializationOf(testClass, mapper)
        "g".isRequiredForSerializationOf(testClass, mapper)

        "h".isOptionalForSerializationOf(testClass, mapper)
        "h".isOptionalForDeserializationOf(testClass, mapper)

        "i".isOptionalForDeserializationOf(testClass, mapper)
        "j".isOptionalForDeserializationOf(testClass, mapper)
        "k".isOptionalForDeserializationOf(testClass, mapper)
        "l".isOptionalForDeserializationOf(testClass, mapper)
    }

    @Test fun shouldHandleTrueFailOnNullForPrimitives() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        val testClass = TestClass::class.java
        "a".isOptionalForDeserializationOf(testClass, mapper)
        "b".isOptionalForDeserializationOf(testClass, mapper)
        "c".isOptionalForDeserializationOf(testClass, mapper)
        "d".isOptionalForDeserializationOf(testClass, mapper)

        "g".isRequiredForDeserializationOf(testClass, mapper)

        "h".isOptionalForDeserializationOf(testClass, mapper)

        "i".isOptionalForDeserializationOf(testClass, mapper)
        "j".isOptionalForDeserializationOf(testClass, mapper)
        "k".isOptionalForDeserializationOf(testClass, mapper)
        "l".isOptionalForDeserializationOf(testClass, mapper)
    }

    // ---

    private data class TestDataClass(
        val a: Int,
        val b: Int?,
        val c: Int = 5,
        val d: Int? = 5,
        val e: TestParamClass,
        val f: TestParamClass?,
        val g: TestParamClass = TestParamClass(),
        val h: TestParamClass? = TestParamClass(),
        // TODO: either error in test case with this not being on the property getter,
        //       or error in introspection not seeing this on the constructor parameter
        @JsonProperty("x", required = true) val x: Int?,
        @get:JsonProperty("z", required = true) val z: Int
    )

    @Test fun shouldHandleFalseFailOnNullForPrimitivesForDataClasses() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        val testClass = TestDataClass::class.java

        "a".isOptionalForDeserializationOf(testClass, mapper)
        "a".isRequiredForSerializationOf(testClass, mapper)

        "b".isOptionalForDeserializationOf(testClass, mapper)
        "b".isOptionalForSerializationOf(testClass, mapper)

        "c".isOptionalForDeserializationOf(testClass, mapper)
        "c".isRequiredForSerializationOf(testClass, mapper)

        "d".isOptionalForDeserializationOf(testClass, mapper)
        "d".isOptionalForSerializationOf(testClass, mapper)

        "e".isRequiredForDeserializationOf(testClass, mapper)
        "e".isRequiredForSerializationOf(testClass, mapper)

        "f".isOptionalForSerializationOf(testClass, mapper)
        "f".isOptionalForDeserializationOf(testClass, mapper)

        "g".isOptionalForDeserializationOf(testClass, mapper)
        "g".isRequiredForSerializationOf(testClass, mapper)

        "h".isOptionalForSerializationOf(testClass, mapper)
        "h".isOptionalForDeserializationOf(testClass, mapper)

        "x".isRequiredForDeserializationOf(testClass, mapper)
        "x".isOptionalForSerializationOf(testClass, mapper)

        "z".isRequiredForDeserializationOf(testClass, mapper)
        "z".isRequiredForSerializationOf(testClass, mapper)
    }

    @Test fun shouldHandleTrueFailOnNullForPrimitivesForDataClasses() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        val testClass = TestDataClass::class.java

        "a".isRequiredForDeserializationOf(testClass, mapper)
        "a".isRequiredForSerializationOf(testClass, mapper)

        "b".isOptionalForDeserializationOf(testClass, mapper)
        "b".isOptionalForSerializationOf(testClass, mapper)

        "c".isOptionalForDeserializationOf(testClass, mapper)
        "c".isRequiredForSerializationOf(testClass, mapper)

        "d".isOptionalForDeserializationOf(testClass, mapper)
        "d".isOptionalForSerializationOf(testClass, mapper)

        "e".isRequiredForDeserializationOf(testClass, mapper)
        "e".isRequiredForSerializationOf(testClass, mapper)

        "f".isOptionalForSerializationOf(testClass, mapper)
        "f".isOptionalForDeserializationOf(testClass, mapper)

        "g".isOptionalForDeserializationOf(testClass, mapper)
        "g".isRequiredForSerializationOf(testClass, mapper)

        "h".isOptionalForSerializationOf(testClass, mapper)
        "h".isOptionalForDeserializationOf(testClass, mapper)

        "x".isRequiredForDeserializationOf(testClass, mapper)
        "x".isOptionalForSerializationOf(testClass, mapper)

        "z".isRequiredForDeserializationOf(testClass, mapper)
        "z".isRequiredForSerializationOf(testClass, mapper)
    }

    private fun String.isRequiredForSerializationOf(type: Class<*>, mapper: ObjectMapper) {
        assertTrue(
            introspectSerialization(type, mapper).isRequired(this),
            "Property $this should be required for serialization!"
        )
    }

    private fun String.isRequiredForDeserializationOf(type: Class<*>, mapper: ObjectMapper) {
        assertTrue(
            introspectDeserialization(type, mapper).isRequired(this),
            "Property $this should be required for deserialization!"
        )
    }

    private fun String.isOptionalForSerializationOf(type: Class<*>, mapper: ObjectMapper) {
        assertFalse(
            introspectSerialization(type, mapper).isRequired(this),
            "Property $this should be optional for serialization!"
        )
    }

    private fun String.isOptionalForDeserializationOf(type: Class<*>, mapper: ObjectMapper) {
        assertFalse(
            introspectDeserialization(type, mapper).isRequired(this),
            "Property $this should be optional for deserialization of ${type.simpleName}!"
        )
    }

    private fun introspectSerialization(type: Class<*>, mapper: ObjectMapper): BeanDescription =
        mapper.serializationConfig.introspect(mapper.serializationConfig.constructType(type))

    private fun introspectDeserialization(type: Class<*>, mapper: ObjectMapper): BeanDescription =
        mapper.deserializationConfig.introspect(mapper.deserializationConfig.constructType(type))

    private fun BeanDescription.isRequired(propertyName: String): Boolean =
        this.findProperties().find { it.name == propertyName }?.isRequired ?: false
}
