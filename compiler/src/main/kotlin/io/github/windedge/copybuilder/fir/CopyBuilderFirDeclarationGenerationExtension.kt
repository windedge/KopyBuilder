package io.github.windedge.copybuilder.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.builder.buildCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class CopyBuilderFirDeclarationGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val KOPY_BUILDER_PACKAGE = FqName("io.github.windedge.copybuilder")
        private val PREDICATE =
            LookupPredicate.create { annotated(KOPY_BUILDER_PACKAGE.child(Name.identifier("KopyBuilder"))) }
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "CopyBuilderGeneratorKey"
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }
    private val classIdsForMatchedClasses: Map<ClassId, FirRegularClassSymbol> by lazy {
        matchedClasses.associateBy {
            val implClassName = "${it.classId.shortClassName}CopyBuilderImpl"
            ClassId(it.classId.packageFqName, Name.identifier(implClassName))
        }
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val matchedClass = classIdsForMatchedClasses[classId] ?: return null
        if (!matchedClass.isData) {
            error("@KopyBuilder can only be applied to data classes")
        }

        return createTopLevelClass(classId, Key).also {
            it.matchedClass = matchedClass.classId
        }.symbol
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        val owner = classSymbol as? FirRegularClassSymbol ?: return emptySet()
        owner.matchedClass ?: return emptySet()

        return setOf(
            SpecialNames.INIT,
            Name.identifier("contains"),
            Name.identifier("get"),
            Name.identifier("put"),
            Name.identifier("build"),

            // properties
            Name.identifier("source"),
            Name.identifier("values"),
            Name.identifier("properties"),
            Name.identifier("privateProperties"),
        )
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        val matchedClass = classIdsForMatchedClasses[classId] ?: return emptyList()
        return listOf(createConstructor(context.owner, Key, isPrimary = true) {
            valueParameter(Name.identifier("source"), matchedClass.defaultType())
        }.symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()
//        val matchedClassSymbol = session.symbolProvider.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()

        return when (callableId.callableName.asString()) {
            "source" -> listOf(
                createMemberProperty(owner, Key, Name.identifier("source"), matchedClassSymbol.defaultType()) {
                    visibility = Visibilities.Private
                }.apply {
                    val parameterSymbol = owner.primaryConstructorSymbol(session)
                        ?.valueParameterSymbols?.first()
                        ?: return emptyList()

                    replaceInitializer(
                        buildCallableReferenceAccess {
                            calleeReference = buildResolvedNamedReference {
                                name = parameterSymbol.name
                                resolvedSymbol = parameterSymbol
                            }
                            coneTypeOrNull = parameterSymbol.resolvedReturnType
                        }
                    )
                }.symbol
            )

            "values" -> return listOf(
                createMemberProperty(
                    owner,
                    Key,
                    Name.identifier("values"),
                    BuiltinTypes.mutableMapType(session),
                ) {
                    visibility = Visibilities.Private
                }.symbol
            )

            "properties" -> return listOf(
                createMemberProperty(
                    owner,
                    Key,
                    Name.identifier("properties"),
                    BuiltinTypes.mapType(session),
                ) {
                    visibility = Visibilities.Private
                }.symbol
            )

            "privateProperties" -> return listOf(
                createMemberProperty(
                    owner,
                    Key,
                    Name.identifier("privateProperties"),
                    BuiltinTypes.setType(session),
                ) {
                    visibility = Visibilities.Private
                }.symbol
            )

            else -> emptyList()
        }
    }


/*
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()
//        val matchedClassSymbol = session.symbolProvider.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()

        return when (callableId.callableName.asString()) {
            "contains" -> listOf(
                createMemberFunction(
                    owner,
                    Key,
                    Name.identifier("contains"),
                    BuiltinTypes.booleanType(session)
                ) {
                    valueParameter(Name.identifier("key"), BuiltinTypes.stringType(session))
                }.symbol
            )

            "get" -> listOf(createMemberFunction(owner, Key, Name.identifier("get"), BuiltinTypes.anyNType(session)) {
                valueParameter(Name.identifier("key"), BuiltinTypes.stringType(session))
            }.symbol)

            "put" -> listOf(createMemberFunction(owner, Key, Name.identifier("put"), BuiltinTypes.unitType(session)) {
                valueParameter(Name.identifier("key"), BuiltinTypes.stringType(session))
                valueParameter(Name.identifier("value"), BuiltinTypes.anyNType(session))
            }.symbol)

            "build" -> listOf(
                createMemberFunction(owner, Key, Name.identifier("build"), matchedClassSymbol.defaultType()).symbol
            )

            else -> emptyList()
        }
    }
*/

    override fun getTopLevelClassIds(): Set<ClassId> {
        return classIdsForMatchedClasses.keys
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

private object MatchedClassAttributeKey : FirDeclarationDataKey()

private var FirRegularClass.matchedClass: ClassId? by FirDeclarationDataRegistry.data(MatchedClassAttributeKey)
private val FirRegularClassSymbol.matchedClass: ClassId? by FirDeclarationDataRegistry.symbolAccessor(
    MatchedClassAttributeKey
)