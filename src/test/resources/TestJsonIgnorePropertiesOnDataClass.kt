package io.github.thewisenerd.linters.sidekt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SampleWithAnnotation(
    val firstVariable: String,
    val secondVariable: Int
)

data class SampleWithoutAnnotation(
    val firstVariable: String,
    val secondVariable: Int
)

data class SampleWithoutAnnotationWithJvmOverloads @JvmOverloads constructor(
    val firstVariable: String,
    val secondVariable: Int = 1
)

@JsonIgnoreProperties(allowSetters = true)
data class SampleWithWrongAnnotation(
    val firstVariable: String,
    val secondVariable: Int
)

@JsonIgnoreProperties(allowSetters = true, ignoreUnknown = true)
data class SampleWithMultipleAnnotation(
    val firstVariable: String,
    val secondVariable: Int
)
