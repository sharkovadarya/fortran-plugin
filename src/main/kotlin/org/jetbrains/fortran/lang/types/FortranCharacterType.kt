package org.jetbrains.fortran.lang.types

class FortranCharacterType : FortranType() {

    override fun isAssignable(fortranType: FortranType): Boolean {
        if (fortranType is FortranCharacterType) {
            return true
        }
        problemDescription = if (fortranType.isProblemDescriptionSet)
            fortranType.typeProblemDescription()
        else incorrectTypeProblemDescription
                .format(fortranType.getName(), "character")
        return false
    }

    override fun getName(): String {
        return "character"
    }

}