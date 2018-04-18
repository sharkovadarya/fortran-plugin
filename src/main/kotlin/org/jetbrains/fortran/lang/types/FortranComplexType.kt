package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.FortranComplexLiteral
import org.jetbrains.fortran.lang.psi.FortranConstant
import org.jetbrains.fortran.lang.psi.FortranExpr

class FortranComplexType : FortranType() {
    override fun isAssignable(expr: FortranExpr?): Boolean {
        // TODO kinds and functions
        if (expr is FortranComplexLiteral) {
            return true
        }

        // TODO this repeats FortranLogicalType code; should I make a method?
        if (expr is FortranConstant) {
            when {
                expr.stringliteral != null ->
                    problemDescription = incorrectTypeProblemDescription.format("character", "logical")
                expr.integerliteral != null ->
                    problemDescription = incorrectTypeProblemDescription.format("integer", "logical")
                expr.floatingpointliteral != null || expr.doubleprecisionliteral != null ->
                    problemDescription = incorrectTypeProblemDescription.format("real", "logical")
            }
            return false
        }

        notAssignableLogicalType(expr, "complex")
        notAssignableNumericType(expr, "complex")

        return expr == null
    }
}