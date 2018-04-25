package org.jetbrains.fortran.lang.types

class FortranComplexType : FortranType() {
    override fun isAssignable(fortranType: FortranType): Boolean {
        if (fortranType is FortranComplexType) {
            return true
        }

        problemDescription = if (fortranType.isProblemDescriptionSet)
            fortranType.typeProblemDescription()
        else incorrectTypeProblemDescription
                .format(fortranType.getName(), "complex")
        return false
    }

    override fun getName(): String {
        return "complex"
    }
}