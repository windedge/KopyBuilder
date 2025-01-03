package io.github.windedge.copybuilder.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CopyBuilderSupertypeGenerationExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val KOPY_BUILDER_PACKAGE = FqName("io.github.windedge.copybuilder")
        private val copyBuilderHostClassId = ClassId(KOPY_BUILDER_PACKAGE, Name.identifier("CopyBuilderHost"))
        private val PREDICATE = DeclarationPredicate.create {
            annotated(KOPY_BUILDER_PACKAGE.child(Name.identifier("KopyBuilder")))
        }
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<ConeKotlinType> {
        if (classLikeDeclaration !is FirRegularClass) return emptyList()
        when (classLikeDeclaration.classKind) {
            ClassKind.CLASS,
            ClassKind.OBJECT -> {}
            else -> return emptyList()
        }
        if (resolvedSupertypes.any { it.coneType.classId == copyBuilderHostClassId }) return emptyList()
        return listOf(copyBuilderHostClassId.constructClassLikeType(emptyArray(), isMarkedNullable = false))
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
