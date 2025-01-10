package io.github.windedge.copybuilder.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val KOTLIN_PACKAGE = FqName("kotlin")
private val KOTLIN_COLLECTIONS_PACKAGE = FqName("kotlin.collections")

private fun ClassId.createType(session: FirSession, nullable: Boolean = false): ConeKotlinType {
    return createConeType(session, nullable = nullable)
}

fun FirSession.stringType(): ConeKotlinType {
    return ClassId(KOTLIN_PACKAGE, Name.identifier("String")).createType(this)
}

fun FirSession.booleanType(): ConeKotlinType {
    return ClassId(KOTLIN_PACKAGE, Name.identifier("Boolean")).createType(this)
}

fun FirSession.anyType(): ConeKotlinType {
    return ClassId(KOTLIN_PACKAGE, Name.identifier("Any")).createType(this)
}

fun FirSession.anyNType(): ConeKotlinType {
    return ClassId(KOTLIN_PACKAGE, Name.identifier("Any")).createType(this, nullable = true)
}

fun FirSession.unitType(): ConeKotlinType {
    return ClassId(KOTLIN_PACKAGE, Name.identifier("Unit")).createType(this)
}

fun FirSession.mapType(
    typeArguments: Array<ConeTypeProjection> = emptyArray()
): ConeKotlinType {
    val classId = ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("Map"))
    return classId.createConeType(this, typeArguments)
}

fun FirSession.mutableMapType(
    typeArguments: Array<ConeTypeProjection> = emptyArray()
): ConeKotlinType {
    val classId = ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("MutableMap"))
    return classId.createConeType(this, typeArguments)
}

fun FirSession.setType(
    typeArguments: Array<ConeTypeProjection> = emptyArray()
): ConeKotlinType {
    val classId = ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("Set"))
    return classId.createConeType(this, typeArguments)
}

fun FirSession.mutableSetType(
    typeArguments: Array<ConeTypeProjection> = emptyArray()
): ConeKotlinType {
    val classId = ClassId(KOTLIN_COLLECTIONS_PACKAGE, Name.identifier("MutableSet"))
    return classId.createConeType(this, typeArguments)
}

fun FirSession.kClassType(
    typeArguments: Array<ConeTypeProjection> = emptyArray()
): ConeKotlinType {
    val classId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
    return classId.createConeType(this, typeArguments)
}
