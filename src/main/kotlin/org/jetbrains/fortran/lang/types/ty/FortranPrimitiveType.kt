package org.jetbrains.fortran.lang.types.ty

import org.jetbrains.fortran.lang.psi.FortranIntrinsicTypeSpec

abstract class FortranPrimitiveType : FortranType() {
    companion object {
        fun fromTypeSpec(typeSpec: FortranIntrinsicTypeSpec?): FortranPrimitiveType? {

            if (typeSpec?.characterTypeKeyword != null) {
                return FortranCharacterType
            }

            val keyword = typeSpec?.numberTypeKeyword?.text

            return when (keyword) {
                "logical" -> FortranLogicalType
                "character" -> FortranCharacterType
                "complex" -> FortranComplexType
                "integer" -> FortranIntegerType
                "real" -> FortranRealType
                else -> null
            }
        }
    }
}

object FortranLogicalType : FortranPrimitiveType()

object FortranCharacterType : FortranPrimitiveType()

object FortranComplexType : FortranPrimitiveType()

object FortranIntegerType : FortranPrimitiveType()

object FortranRealType : FortranPrimitiveType()