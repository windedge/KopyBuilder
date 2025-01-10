package io.github.windedge.copybuilder.fir

import io.github.windedge.copybuilder.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class CopyBuilderFirDeclarationGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create { annotated(CopyBuilderClassFqn) }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }
    private val classIdsForMatchedClasses: Map<ClassId, FirRegularClassSymbol> by lazy {
        matchedClasses.associateBy {
            val implClassName = generateImplClassName(it.classId.shortClassName.asString())
            ClassId(it.classId.packageFqName, implClassName)
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
            CONTAINS_NAME,
            GET_NAME,
            PUT_NAME,
            BUILD_NAME,

            // properties
            SOURCE_NAME,
            VALUES_NAME,
            PROPERTIES_NAME,
            PRIVATE_PROPERTIES_NAME,
        )
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        val matchedClass = classIdsForMatchedClasses[classId] ?: return emptyList()
        val constructor = createConstructor(context.owner, Key, isPrimary = true) {
            valueParameter(SOURCE_PARAMETER_NAME, matchedClass.defaultType())
        }
        return listOf(constructor.symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()

        return when (callableId.callableName) {
            SOURCE_NAME -> listOf(
                createMemberProperty(owner, Key, SOURCE_NAME, matchedClassSymbol.defaultType()) {
                    visibility = Visibilities.Private
                }.symbol
            )

            VALUES_NAME -> {
                val returnType = session.mutableMapType(arrayOf(session.stringType(), session.anyNType()))
                return listOf(
                    createMemberProperty(owner, Key, VALUES_NAME, returnType) {
                        visibility = Visibilities.Private
                    }.symbol
                )
            }

            PROPERTIES_NAME -> {
                val kClassType = session.kClassType(arrayOf(ConeStarProjection))
                val returnType = session.mapType(arrayOf(session.stringType(), kClassType))
                return listOf(
                    createMemberProperty(owner, Key, PROPERTIES_NAME, returnType) {
                        visibility = Visibilities.Private
                    }.symbol
                )
            }

            PRIVATE_PROPERTIES_NAME -> {
                val returnType = session.setType(arrayOf(session.stringType()))
                return listOf(
                    createMemberProperty(owner, Key, PRIVATE_PROPERTIES_NAME, returnType) {
                        visibility = Visibilities.Private
                    }.symbol
                )
            }

            else -> emptyList()
        }
    }


    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()

        return when (callableId.callableName) {
            CONTAINS_NAME -> listOf(
                createMemberFunction(
                    owner,
                    Key,
                    CONTAINS_NAME,
                    session.booleanType()
                ) {
                    valueParameter(Name.identifier("key"), session.stringType())
                }.symbol
            )

            GET_NAME -> listOf(createMemberFunction(owner, Key, GET_NAME, session.anyNType()) {
                valueParameter(Name.identifier("key"), session.stringType())
            }.symbol)

            PUT_NAME -> listOf(createMemberFunction(owner, Key, PUT_NAME, session.unitType()) {
                valueParameter(Name.identifier("key"), session.stringType())
                valueParameter(Name.identifier("value"), session.anyNType())
            }.symbol)

            BUILD_NAME -> listOf(
                createMemberFunction(owner, Key, BUILD_NAME, matchedClassSymbol.defaultType()).symbol
            )

            else -> emptyList()
        }
    }

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