package org.jetbrains.fortran.lang.types

class FortranLogicalType : FortranType() {
    override fun isAssignable(fortranType: FortranType): Boolean {
        if (fortranType is FortranLogicalType) {
            return true
        }

        problemDescription = if (fortranType.isProblemDescriptionSet)
            fortranType.typeProblemDescription()
        else incorrectTypeProblemDescription
                .format(fortranType.getName(), "logical")
        return false
    }

    override fun getName(): String {
        return "logical"
    }
}
