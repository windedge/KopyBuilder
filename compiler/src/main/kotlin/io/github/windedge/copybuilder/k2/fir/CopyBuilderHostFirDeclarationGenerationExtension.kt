package io.github.windedge.copybuilder.k2.fir

import io.github.windedge.copybuilder.*
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class CopyBuilderHostFirDeclarationGenerationExtension(session: FirSession) :
    FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = DeclarationPredicate.create {
            annotated(KopyBuilderClassFqn)
        }
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> = setOf(TO_COPY_BUILDER_NAME, COPY_BUILD_NAME)

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner as? FirRegularClassSymbol ?: return emptyList()
        if (!owner.hasAnnotation(KopyBuilderClassId, session)) return emptyList()

        return when (callableId.callableName) {
            TO_COPY_BUILDER_NAME -> listOf(generateToCopyBuilderFunction(owner))
            COPY_BUILD_NAME -> listOf(generateCopyBuildFunction(owner))
            else -> emptyList()
        }
    }

    private fun generateToCopyBuilderFunction(owner: FirRegularClassSymbol): FirNamedFunctionSymbol {
        val ownerType = owner.defaultType()
        val returnType = CopyBuilderClassId.constructClassLikeType(
            arrayOf(ownerType)
        )
        return createMemberFunction(owner, Key, TO_COPY_BUILDER_NAME, returnType) {
            status { isOverride = true }
            visibility = Visibilities.Public
            modality = Modality.OPEN
        }.symbol
    }

    private fun generateCopyBuildFunction(owner: FirRegularClassSymbol): FirNamedFunctionSymbol {
        val ownerType = owner.defaultType()
        val builderType = CopyBuilderClassId.constructClassLikeType(
            arrayOf(ownerType)
        )
        val parameterType = createFunctionType(
            FunctionTypeKind.Function,
            parameters = emptyList(),
            receiverType = builderType,
            rawReturnType = session.builtinTypes.unitType.coneType,
            contextReceivers = emptyList()
        )
        return createMemberFunction(owner, Key, COPY_BUILD_NAME, ownerType) {
            status { isOverride = true }
            visibility = Visibilities.Public
            modality = Modality.OPEN

            valueParameter(Name.identifier("initialize"), parameterType)
        }.symbol
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
