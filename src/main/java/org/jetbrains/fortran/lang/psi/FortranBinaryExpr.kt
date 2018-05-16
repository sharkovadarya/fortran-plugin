package org.jetbrains.fortran.lang.psi

interface FortranBinaryExpr : FortranExpr {
    fun getExprList(): List<FortranExpr>
}

fun FortranBinaryExpr.left(): FortranExpr {
    return getExprList()[0]
}

fun FortranBinaryExpr.right(): FortranExpr? {
    if (getExprList().size > 1) {
        return getExprList()[1]
    }

    return null
}