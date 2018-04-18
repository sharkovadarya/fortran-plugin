package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

abstract class FortranType {

    protected var problemDescription = "Incorrect assignment"
    protected var incorrectTypeProblemDescription = "Assigning %s value to a variable of %s type"

    abstract fun isAssignable(expr : FortranExpr?) : Boolean

    fun isAssignableExprList(exprList: List<FortranExpr?>) : Boolean {
        var res = true
        for (element in exprList) {
            res = res && isAssignable(element)
        }
        return res
    }

    fun isAssignableNumericType(expr: FortranExpr?) : Boolean {
        when (expr) {
            is FortranUnaryAddExpr -> return isAssignable(expr.expr)
            is FortranAddExpr -> return isAssignableExprList(expr.exprList)
            is FortranMultExpr -> return isAssignableExprList(expr.exprList)
            is FortranPowerExpr -> return isAssignableExprList(expr.exprList)
            is FortranParenthesisedExpr -> return isAssignable(expr.expr)
        }

        // TODO this piece of code doesn't really belong here, how to fix?
        return expr == null
    }

    fun notAssignableNumericType(expr: FortranExpr?, typeKeyword: String) {
        if (expr is FortranAddExpr || expr is FortranMultExpr ||
            expr is FortranUnaryAddExpr || expr is FortranPowerExpr) {
            problemDescription = incorrectTypeProblemDescription.format("numeric", typeKeyword)
        }
    }

    fun notAssignableLogicalType(expr: FortranExpr?, typeKeyword: String) {
        if (expr is FortranLogicalLiteral || expr is FortranAndExpr ||
                expr is FortranOrExpr || expr is FortranNotExpr) {
            problemDescription = incorrectTypeProblemDescription.format("logical", typeKeyword)
        }
    }

    fun typeProblemDescription() : String {
        return problemDescription
    }

}