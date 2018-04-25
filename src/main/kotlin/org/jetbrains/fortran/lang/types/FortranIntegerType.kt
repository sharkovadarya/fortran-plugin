package org.jetbrains.fortran.lang.types

class FortranIntegerType : FortranType() {

    override fun isAssignable(fortranType: FortranType): Boolean {
        if (fortranType is FortranIntegerType) {
            return true
        }
        // TODO code style
        problemDescription = if (fortranType.isProblemDescriptionSet)
                                 fortranType.typeProblemDescription()
                             else incorrectTypeProblemDescription
                                  .format(fortranType.getName(), "integer")
        return false
    }

    override fun getName(): String {
        return "integer"
    }
}