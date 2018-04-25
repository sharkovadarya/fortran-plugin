package org.jetbrains.fortran.lang.types

class FortranUnrecognizedType() : FortranType() {
    constructor(description: String) : this() {
        problemDescription = description
        isProblemDescriptionSet = true
    }

    override fun isAssignable(fortranType: FortranType): Boolean {
        return false
    }

    override fun getName(): String {
        return "unrecognized type"
    }
}