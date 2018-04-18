package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

class FortranIntegerType : FortranType() {

    override fun isAssignable(expr: FortranExpr?): Boolean {
        if (expr is FortranConstant) {
            if (expr.integerliteral == null) {
                when {
                    expr.stringliteral != null ->
                        problemDescription = incorrectTypeProblemDescription.format("character", "integer")
                    expr.floatingpointliteral != null || expr.doubleprecisionliteral != null ->
                        problemDescription = incorrectTypeProblemDescription.format("real", "integer")
                }
                return false
            }
            // TODO: consider various kinds
            try {
                expr.text.toLong() // if it exceeds Long it's definitely incorrect
            } catch (exception : NumberFormatException) {
                problemDescription = "Provided value causes overflow"
                return false
            }

            return true
        }

        if (expr is FortranComplexLiteral) {
            problemDescription = incorrectTypeProblemDescription.format("complex", "integer")
            return false
        }

        notAssignableLogicalType(expr, "integer")

        return isAssignableNumericType(expr)
    }
}