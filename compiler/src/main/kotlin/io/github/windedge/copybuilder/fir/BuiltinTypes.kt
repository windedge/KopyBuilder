package io.github.windedge.copybuilder.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object BuiltinTypes {
    private val KOTLIN_PACKAGE = FqName("kotlin")
    private val KOTLIN_COLLECTIONS_PACKAGE = FqName("kotlin.collections")

    private fun ClassId.createType(session: FirSession, nullable: Boolean = false): ConeKotlinType {
        return createConeType(session, nullable = nullable)
    }

    fun stringType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_PACKAGE, Name.identifier("String")).createType(session)
    }

    fun booleanType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_PACKAGE, Name.identifier("Boolean")).createType(session)
    }

    fun anyType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_PACKAGE, Name.identifier("Any")).createType(session)
    }

    fun anyNType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_PACKAGE, Name.identifier("Any")).createType(session, nullable = true)
    }

    fun unitType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_PACKAGE, Name.identifier("Unit")).createType(session)
    }

    fun mapType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("Map")).createType(session)
    }

    fun mutableMapType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("MutableMap")).createType(session)
    }

    fun setType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("Set")).createType(session)
    }

    fun mutableSetType(session: FirSession): ConeKotlinType {
        return ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("MutableSet")).createType(session)
    }
}
