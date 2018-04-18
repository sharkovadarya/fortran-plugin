package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

// TODO maximal string length is implementation-defined, should it be BigInteger?
class FortranCharacterType(val len : Long) : FortranType() {

    override fun isAssignable(expr: FortranExpr?): Boolean {
        // for now, all available assignments are constants
        // TODO add functions into consideration
        if (expr is FortranParenthesisedExpr) {
            return isAssignable(expr.expr)
        }
        // len + 2 for quotation marks
        if (expr is FortranConstant && expr.stringliteral != null && expr.text.length <= len + 2) {
            return true
        }

        if (expr is FortranConstant) {
            when {
                expr.integerliteral != null ->
                    problemDescription = incorrectTypeProblemDescription.format("integer", "character")
                expr.floatingpointliteral != null || expr.doubleprecisionliteral != null ->
                    problemDescription = incorrectTypeProblemDescription.format("real", "character")
                expr.stringliteral != null && expr.text.length > len + 2 ->
                    problemDescription = "Value length exceeds maximal string length"
            }
        }

        notAssignableNumericType(expr, "character")
        notAssignableLogicalType(expr, "character")

        if (expr is FortranComplexLiteral) {
            problemDescription = incorrectTypeProblemDescription.format("complex", "character")
        }

        return expr == null
    }

}