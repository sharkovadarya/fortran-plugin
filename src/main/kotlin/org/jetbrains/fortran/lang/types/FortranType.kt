package org.jetbrains.fortran.lang.types

abstract class FortranType {

    var isProblemDescriptionSet = false
    protected var problemDescription = "Incorrect assignment"
    protected var incorrectTypeProblemDescription = "Assigning %s value to a variable of %s type"

    abstract fun isAssignable(fortranType: FortranType) : Boolean

    fun typeProblemDescription() : String {
        return problemDescription
    }

    abstract fun getName() : String

}