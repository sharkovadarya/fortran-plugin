package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

class FortranLogicalType : FortranType() {
    override fun isAssignable(expr: FortranExpr?): Boolean {

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

        notAssignableNumericType(expr, "logical")

        if (expr is FortranComplexLiteral) {
            problemDescription = incorrectTypeProblemDescription.format("complex", "logical")
            return false
        }

        when (expr) {
            is FortranLogicalLiteral -> return true
            is FortranParenthesisedExpr -> return isAssignable(expr.expr)
            is FortranAndExpr -> return isAssignableExprList(expr.exprList)
            is FortranOrExpr -> return isAssignableExprList(expr.exprList)
            is FortranNotExpr -> return isAssignable(expr.expr)
            // TODO functions
        }

        // no assignment at all is the only option
        return expr == null
    }
}
