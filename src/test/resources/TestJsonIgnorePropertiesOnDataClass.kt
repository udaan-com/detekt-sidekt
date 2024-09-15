import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SampleWithAnnotation(
    val firstVariable: String,
    val secondVariable: Int,
)

data class SampleWithoutAnnotation(
    val firstVariable: String,
    val secondVariable: Int,
)

data class SampleWithoutAnnotationWithJvmOverloads @JvmOverloads constructor(
    val firstVariable: String,
    val secondVariable: Int,
)

@JsonIgnoreProperties(allowSetters = true)
data class SampleWithAnnotationV2(
    val firstVariable: String,
    val secondVariable: Int,
)
