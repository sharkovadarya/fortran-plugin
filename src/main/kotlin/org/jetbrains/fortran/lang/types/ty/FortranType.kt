package org.jetbrains.fortran.lang.types.ty

import com.intellij.xml.util.XmlStringUtil

open class FortranType {
    override fun toString(): String {
        return when (this) {
            is FortranLogicalType -> "logical"
            is FortranComplexType -> "complex"
            is FortranIntegerType -> "integer"
            is FortranRealType -> "real"
            is FortranCharacterType -> "character"
            else -> "unknown type"
        }
    }
}

val FortranType.escaped: String
    get() = XmlStringUtil.escapeString(this.toString())