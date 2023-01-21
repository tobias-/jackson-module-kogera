package com.fasterxml.jackson.module.kotlin

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedField
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.jsontype.NamedType
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.setterSignature
import kotlinx.metadata.jvm.signature
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Method

internal class KotlinAnnotationIntrospector(
    private val context: Module.SetupContext,
    private val nullToEmptyCollection: Boolean,
    private val nullToEmptyMap: Boolean,
    private val cache: ReflectionCache
) : NopAnnotationIntrospector() {

    // TODO: implement nullIsSameAsDefault flag, which represents when TRUE that if something has a default value,
    //       it can be passed a null to default it
    //       this likely impacts this class to be accurate about what COULD be considered required

    override fun hasRequiredMarker(m: AnnotatedMember): Boolean? = try {
        when {
            nullToEmptyCollection && m.type.isCollectionLikeType -> false
            nullToEmptyMap && m.type.isMapLikeType -> false
            else -> cache.getKmClass(m.member.declaringClass)?.let {
                when (m) {
                    is AnnotatedField -> m.hasRequiredMarker(it)
                    is AnnotatedMethod -> m.getRequiredMarkerFromCorrespondingAccessor(it)
                    is AnnotatedParameter -> m.hasRequiredMarker(it)
                    else -> null
                }
            }
        }
    } catch (ex: UnsupportedOperationException) {
        null
    }

    /**
     * Subclasses can be detected automatically for sealed classes, since all possible subclasses are known
     * at compile-time to Kotlin. This makes [com.fasterxml.jackson.annotation.JsonSubTypes] redundant.
     */
    override fun findSubtypes(a: Annotated): List<NamedType>? = cache.getKmClass(a.rawType)?.let { kmClass ->
        kmClass.sealedSubclasses.map { NamedType(it.reconstructClass()) }.ifEmpty { null }
    }

    private fun AnnotatedField.hasRequiredMarker(kmClass: KmClass): Boolean? {
        val member = annotated

        val byAnnotation = member.isRequiredByAnnotation()
        val fieldSignature = member.toSignature()
        val byNullability = kmClass.properties
            .find { it.fieldSignature == fieldSignature }
            ?.let { !it.returnType.isNullable() }

        return requiredAnnotationOrNullability(byAnnotation, byNullability)
    }

    private fun AccessibleObject.isRequiredByAnnotation(): Boolean? = annotations
        .filterIsInstance<JsonProperty>()
        .firstOrNull()
        ?.required

    private fun requiredAnnotationOrNullability(byAnnotation: Boolean?, byNullability: Boolean?): Boolean? = when {
        byAnnotation != null && byNullability != null -> byAnnotation || byNullability
        byNullability != null -> byNullability
        else -> byAnnotation
    }

    private fun KmProperty.isRequiredByNullability(): Boolean = !this.returnType.isNullable()

    private fun AnnotatedMethod.getRequiredMarkerFromCorrespondingAccessor(kmClass: KmClass): Boolean? {
        val memberSignature = member.toSignature()
        kmClass.properties.forEach { kmProperty ->
            if (kmProperty.getterSignature == memberSignature || kmProperty.setterSignature == memberSignature) {
                val byAnnotation = this.member.isRequiredByAnnotation()
                val byNullability = kmProperty.isRequiredByNullability()
                return requiredAnnotationOrNullability(byAnnotation, byNullability)
            }
        }
        return null
    }

    private fun AnnotatedParameter.hasRequiredMarker(kmClass: KmClass): Boolean? {
        val byAnnotation = this.getAnnotation(JsonProperty::class.java)?.required
        val byNullability: Boolean? = when (val member = member) {
            is Constructor<*> -> {
                val paramDef = kmClass.findKmConstructor(member)
                    ?.let { it.valueParameters[index] }
                    ?: return null

                paramDef to member.parameterTypes[index]
            }
            is Method -> {
                val signature = member.toSignature()
                val paramDef = kmClass.functions.find { it.signature == signature }
                    ?.let { it.valueParameters[index] }
                    ?: return null

                paramDef to member.parameterTypes[index]
            }
            else -> null
        }?.let { (paramDef, paramType) ->
            val isPrimitive = paramType.isPrimitive
            val isOptional = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(paramDef.flags)
            val isMarkedNullable = paramDef.type.isNullable()

            !isMarkedNullable && !isOptional &&
                !(isPrimitive && !context.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES))
        }

        return requiredAnnotationOrNullability(byAnnotation, byNullability)
    }
}
