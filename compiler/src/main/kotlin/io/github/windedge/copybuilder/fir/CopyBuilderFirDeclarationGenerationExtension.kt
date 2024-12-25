package io.github.windedge.copybuilder.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


//fun String.fqn(): FqName = FqName("org.jetbrains.kotlin.fir.plugin.$this")

class CopyBuilderFirDeclarationGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val BUILDER_CLASS_NAME = Name.identifier("Builder")
        private val PREDICATE =
            LookupPredicate.create { annotated(FqName("io.github.windedge.copybuilder.KopyBuilder")) }
    }

    object Key : GeneratedDeclarationKey()

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirRegularClassSymbol {
        val klass = createTopLevelClass(classId, Key, ClassKind.CLASS)
        println("generateTopLevelClassLikeDeclaration, kclass = ${klass.classId.shortClassName}")

        return klass.symbol
    }


    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelClassIds(): Set<ClassId> {
        return matchedClasses.map {
            ClassId(it.classId.packageFqName, Name.identifier("${it.classId.shortClassName.asString()}CopyBuilderImpl"))
        }.toSet()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }


}