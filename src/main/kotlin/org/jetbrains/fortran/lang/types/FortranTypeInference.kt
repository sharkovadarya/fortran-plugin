package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.FortranAssignStmt
import org.jetbrains.fortran.lang.psi.FortranIntrinsicTypeSpec

class FortranTypeInference {
    companion object {
        fun getType(typeSpec: FortranIntrinsicTypeSpec) : FortranType {
            if (typeSpec.characterTypeKeyword != null) {
                return FortranCharacterType(typeSpec.charSelector?.lengthSelector?.expr?.text?.toLongOrNull() ?: 1)
            } else if (typeSpec.numberTypeKeyword != null) {
                when {
                    typeSpec.numberTypeKeyword?.text == "integer" -> return FortranIntegerType()
                    typeSpec.numberTypeKeyword?.text == "logical" -> return FortranLogicalType()
                    typeSpec.numberTypeKeyword?.text == "real" -> return FortranRealType()
                    typeSpec.numberTypeKeyword?.text == "complex" -> return FortranComplexType()
                }
            }
            // could not infer type
            // TODO a custom exception?
            throw Exception()
        }

        fun getType(assignStmt: FortranAssignStmt) {

        }
    }
}