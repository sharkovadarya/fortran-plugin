package org.jetbrains.fortran.lang.types

class FortranRealType : FortranType() {

    override fun isAssignable(fortranType: FortranType): Boolean {
        if (fortranType is FortranRealType || fortranType is FortranIntegerType) {
            return true
        }

        problemDescription = if (fortranType.isProblemDescriptionSet)
            fortranType.typeProblemDescription()
        else incorrectTypeProblemDescription
                .format(fortranType.getName(), "real")
        return false
    }

    override fun getName(): String {
        return "real"
    }
}
