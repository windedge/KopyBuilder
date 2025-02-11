package io.github.windedge.copybuilder.k2.fir

import io.github.windedge.copybuilder.KopyBuilderClassFqn
import io.github.windedge.copybuilder.CopyBuilderHostClassId
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.*

class CopyBuilderSupertypeGenerationExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val PREDICATE = DeclarationPredicate.create {
            annotated(KopyBuilderClassFqn)
        }
    }

    @OptIn(FirImplementationDetail::class)
    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<ConeKotlinType> {
        if (classLikeDeclaration !is FirRegularClass) return emptyList()
        when (classLikeDeclaration.classKind) {
            ClassKind.CLASS,
            ClassKind.OBJECT -> {
            }

            else -> return emptyList()
        }
        if (resolvedSupertypes.any { it.coneType.classId == CopyBuilderHostClassId }) return emptyList()

        val coneClassLikeType = CopyBuilderHostClassId.constructClassLikeType(
            arrayOf(
                classLikeDeclaration.defaultType().toTypeProjection(ProjectionKind.INVARIANT)
            ),
        )
        return listOf(coneClassLikeType)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
