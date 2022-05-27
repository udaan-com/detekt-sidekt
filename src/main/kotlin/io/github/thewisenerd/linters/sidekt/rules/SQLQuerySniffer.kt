package io.github.thewisenerd.linters.sidekt.rules

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.github.thewisenerd.linters.sidekt.helpers.Debugger
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

class SQLQuerySniffer(config: Config): Rule(config) {

    override val issue = Issue(
        SQLQuerySniffer::class.java.simpleName,
        Severity.Performance,
        "SQL Query sniffed",
        Debt.TWENTY_MINS
    )

    override fun visitImportList(importList: KtImportList) {
        super.visitImportList(importList)
        importList
            .imports
            .mapNotNull { it.importPath }
            .filter { UDAAN_PACKAGE_DETECT_REGEX.matches(it.pathStr) }
            .let {
                udaanPackageImportList.addAll(
                    it.map { import -> import.pathStr }
                )
            }
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.isInterface()) {
            klass.annotationEntries.any {
                it.originalElement.text == stringTemplateMarker
            }.takeIf {
                it
            }?.let {
                scanResourceFiles = true
            }
        } else {
            // Dont process non-interfaces
        }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val originalText = property.originalElement.text
        property.name?.let { keyValue ->
            originalText.substringAfter(
                "$keyValue ="
            )
                .trim()
                .let {
                    propertyMap.put(
                        keyValue, it.trim('"')
                    )
                }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        function.annotationEntries.forEach {
            val annotationValue = it.shortName.toString()
            if (sniffSQLAnnotation.contains(annotationValue)) {
                val sqlQuery = it.originalElement.text.preProcessAnnotation(annotationValue)
                val metaDataComment = prepareFunctionParamsWithStore(sqlQuery, function)
                val linkPrefix = generateAnalyserLink()
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.Companion.from(function),
                        message = "${metaDataComment}<br>[Click to Analyse Query](${linkPrefix})"
                    )
                )
            }
        }
    }

    private fun prepareFunctionParamsWithStore(query: String, function: KtNamedFunction): String {
        val extraStoreParams = mutableListOf<String>()
        return function.let {
            it.valueParameters.map { param ->
                val pName = extractParamFromAnnotation(param) ?: param.name ?: ""
                val (typeName, extractedTypeName) = extractParamType(param) ?: "String" to "String"
                extractedTypeName.let { type ->
                    udaanPackageImportList.firstOrNull { import ->
                        import.endsWith(type)
                    }?.let { import ->
                        buildClassPropertyMap(type, import)
                            .also { extraParams ->
                                extraStoreParams.addAll(
                                    extraParams.plus(type)
                                )
                            }
                    }
                }
                pName to typeName
            }.let { params ->
                val pMap = params.toMap()
                gson.toJson(
                    SniffedQueryMetaData(
                        query = processQuery(query),
                        params = pMap,
                        classStoreHelper = getClassAttributeStore(extraStoreParams),
                        enumStoreHelper = getEnumAttributeStore(extraStoreParams)
                    )
                ).let { processedString ->
                    "SNIFFED $processedString"
                }
            }

        }
    }

    private fun extractParamFromAnnotation(parameter: KtParameter): String? {
        parameter
            .annotationEntries
            .forEach {
                val annotation = it.originalElement.text
                sqlBindTypes.forEach { sbt ->
                    if (sbt.matches(annotation)) {
                        return sbt.find(annotation).extractValue()
                    }
                }
            }
        return null
    }

    private fun extractParamType(parameter: KtParameter): Pair<String, String>? {
        return parameter
            .typeReference
            ?.typeElement
            ?.originalElement
            ?.text
            ?.let { type ->
                sqlParamExtractType
                    .forEach { spt ->
                        if (spt.matches(type)) {
                            val extractedType = spt.find(type).extractValue() ?: type
                            return@let type to extractedType
                        }
                    }
                type to type
            }
    }

    private fun processQuery(q: String): String {
        if (propertyMap.isEmpty()) return q
        var newQuery: String = q
        propertyMap.forEach { (key, value) ->
            newQuery = newQuery.replace("$${key}", value)
        }
        return newQuery
            .replace("*", "ALL")
    }

    private fun getClassAttributeStore(params: List<String>): Map<String, Map<String, String>> {
        val camStoreKeys = classAttributeMap.keys
        return params.mapNotNull { p ->
            camStoreKeys.firstOrNull { cKey ->
                cKey == p
            }?.let {
                p to classAttributeMap.getValue(it)
            }
        }.toMap()
    }

    private fun getEnumAttributeStore(params: List<String>): Map<String, List<String>> {
        val enumStoreKeys = enumClassAttributeMap.keys
        return params.mapNotNull { p ->
            enumStoreKeys.firstOrNull { eKey ->
                eKey.endsWith(p)
            }?.let {
                p to enumClassAttributeMap.getValue(it)
            }
        }.toMap()
    }

    private fun buildClassPropertyMap(typeName: String, importPath: String): MutableList<String> {
        val dbg = Debugger.make(SQLQuerySniffer::class.java.simpleName, debugStream)
        if (classAttributeMap.containsKey(importPath) || enumClassAttributeMap.containsKey(importPath)) return mutableListOf()
        val newIdentifiedParams = mutableListOf<String>()
        try {
            val clazz = Class.forName(importPath)
            val kClazz = clazz.kotlin
            when {
                kClazz.isData -> {
                    val propertyMap = mutableMapOf<String, String>()
                    clazz.declaredFields.forEach { f ->
                        if (UDAAN_PACKAGE_DETECT_REGEX.matches(f.genericType.typeName)) {
                            buildClassPropertyMap(f.genericType.typeName, f.genericType.typeName)
                            newIdentifiedParams.add(f.genericType.typeName)
                        }
                        propertyMap[f.name] = f.genericType.typeName.processType()
                    }
                    classAttributeMap[typeName] = propertyMap
                }
                clazz.isEnum -> {
                    clazz
                        .enumConstants
                        .filter { it != "\$VALUES" }
                        .map {
                            it.toString()
                        }
                        .let {
                            enumClassAttributeMap[importPath] = it
                        }
                }
                else -> { /* Dont process this shit */ }
            }
        } catch (e: ClassNotFoundException) {
            dbg.i("No class found for Import Path: $importPath")
        } catch (e: Exception) {
            dbg.i("Something went wrong: $e")
        }
        return newIdentifiedParams
    }

    private fun String.preProcessAnnotation(queryType: String): String {
        val queryTypeExtractor = """@$queryType""".toRegex()
        return queryTypeExtractor.split(this)
            .last()
            .trim()
            .split("+").joinToString(" ") {
                it
                    .trim()
                    .replace("\"", "")
                    .replace("\\n", "")
            }

    }

    private fun MatchResult?.extractValue(): String? = this?.destructured?.component1()
    private fun String.processType(): String = paramTypeMap[this] ?: this

    private val debugStream by lazy {
        valueOrNull<String>("debug")?.let {
            Debugger.getOutputStreamForDebugger(it)
        }
    }

    private fun generateAnalyserLink(): String {
        val baseLink = ruleSetConfig
            .valueOrNull("analyser-link")
            ?: "https://tools.udaan.dev/infraprobs/docs-analyser/github-analyser"
        val prNumber = System.getenv("GH_PR_NUMBER") ?: ""
        val repoName = System.getenv("GH_REPO") ?: ""
        return if (prNumber.isNotEmpty() && repoName.isNotEmpty()) {
            listOf(
                "<REPO_NAME>" to repoName,
                "<PULL_NUMBER>" to prNumber
            ).fold(GITHUB_PR_URL_TEMPLATE) { s: String, it: Pair<String, String> ->
                s.replace(it.first, it.second)
            }.let { prUrl ->
                "${baseLink}?githubPrUrl=${prUrl}"
            }
        } else baseLink
    }

    companion object {

        private const val GITHUB_PR_URL_TEMPLATE = "https://github.com/udaan-com/<REPO_NAME>/pull/<PULL_NUMBER>"

        private val UDAAN_PACKAGE_DETECT_REGEX = "com.udaan.*".toRegex()

        private val sqlBindTypes = listOf(
            "@Bind\\(\"([\\w]+)\"\\)".toRegex(),
            "@BindList\\(\"([\\w]+)\"\\)".toRegex()
        )

        private val sqlParamExtractType = listOf(
            "Collection<([\\w]+)>".toRegex(),
            "List<([\\w]+)>".toRegex(),
            "Set<([\\w]+)>".toRegex()
        )

        private val paramTypeMap = mapOf(
            "java.lang.String" to "String",
            "boolean" to "Boolean"
        )

        private val propertyMap = mutableMapOf<String, String>()
        private val udaanPackageImportList = mutableListOf<String>()
        private val classAttributeMap = mutableMapOf<String, Map<String, String>>()
        private val enumClassAttributeMap = mutableMapOf<String, List<String>>()

        private val sniffSQLAnnotation = listOf("SqlQuery")
        private val stringTemplateMarker = "@UseStringTemplate3StatementLocator"
        private var scanResourceFiles = false

        private val objectMapper by lazy { ObjectMapper() }
        private val gson by lazy { Gson() }
    }
}

data class SniffedQueryMetaData(
    val query: String,
    val params: Map<String, String>,
    val classStoreHelper: Map<String, Map<String, String>>,
    val enumStoreHelper: Map<String, List<String>>
)
