package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

class FortranRealType : FortranType() {

    override fun isAssignable(expr: FortranExpr?): Boolean {
        if (expr is FortranConstant) {
            if (expr.floatingpointliteral == null && expr.integerliteral == null &&
                expr.doubleprecisionliteral == null) {
                problemDescription = incorrectTypeProblemDescription.format("character", "real")
                return false
            }
            // TODO handle kinds and double precision
            /*try {
                expr.text.toDouble()
            } catch (exception : NumberFormatException) {
                problemDescription = "Provided value causes overflow"
                return false
            }*/

            return true
        }

        notAssignableLogicalType(expr, "real")

        if (expr is FortranComplexLiteral) {
            problemDescription = incorrectTypeProblemDescription.format("complex", "real")
            return false
        }

        return isAssignableNumericType(expr)
    }
}
