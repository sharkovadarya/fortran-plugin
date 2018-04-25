package org.jetbrains.fortran.lang.types

import org.jetbrains.fortran.lang.psi.*

class FortranTypeInference {

    companion object {

        fun getType(typeSpec: FortranIntrinsicTypeSpec) : FortranType {
            if (typeSpec.characterTypeKeyword != null) {
                return FortranCharacterType()
            } else if (typeSpec.numberTypeKeyword != null) {
                when {
                    typeSpec.numberTypeKeyword?.text == "integer" -> return FortranIntegerType()
                    typeSpec.numberTypeKeyword?.text == "logical" -> return FortranLogicalType()
                    typeSpec.numberTypeKeyword?.text == "real" -> return FortranRealType()
                    typeSpec.numberTypeKeyword?.text == "complex" -> return FortranComplexType()
                }
            }

            // could not infer type
            return FortranUnrecognizedType("Can't infer expression type")
        }

        fun getType(expr: FortranConstant) : FortranType {
            when {
                expr.stringliteral != null -> return FortranCharacterType()
                expr.integerliteral != null -> return FortranIntegerType()
                expr.floatingpointliteral != null || expr.doubleprecisionliteral != null -> return FortranRealType()
            }

            return FortranUnrecognizedType("Can't infer type of this constant")
        }

        // might be necessary later
        fun getType(expr: FortranLogicalLiteral) : FortranType {
            return FortranLogicalType()
        }

        fun getType(expr: FortranComplexLiteral) : FortranType {
            return FortranComplexType()
        }

        fun getType(expr: FortranAddExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpArithmExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                // adding other types is not supported thus can't infer expression type
                return FortranUnrecognizedType("Can't infer type of this addition expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return exprType
        }

        fun getType(expr: FortranMultExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpArithmExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                return FortranUnrecognizedType("Can't infer type of this multiplication expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return exprType
        }

        fun getType(expr: FortranPowerExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpArithmExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                return FortranUnrecognizedType("Can't infer type of this power expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return exprType
        }

        fun getType(expr: FortranRelExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpArithmExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                return FortranUnrecognizedType("Can't infer type of this binary relation expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return FortranLogicalType()
        }

        fun getType(expr: FortranAndExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpLogicalExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                return FortranUnrecognizedType("Can't infer type of this \"logical and\" expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return exprType
        }

        fun getType(expr: FortranOrExpr) : FortranType {
            if (expr.exprList.size != 2) {
                return FortranUnrecognizedType()
            }

            val leftArgumentType = FortranTypeInference.getType(expr.exprList[0])
            val rightArgumentType = FortranTypeInference.getType(expr.exprList[1])

            val exprType = getBinaryOpLogicalExprType(leftArgumentType, rightArgumentType)
            if (exprType is FortranUnrecognizedType && !exprType.isProblemDescriptionSet) {
                return FortranUnrecognizedType("Can't infer type of this \"logical or\" expression where arguments are: " +
                        leftArgumentType.getName() + ", " + rightArgumentType.getName())
            }

            return exprType
        }

        fun getType(expr: FortranUnaryAddExpr) : FortranType {
            val subexpr = expr.expr ?: return FortranUnrecognizedType()
            val exprType = FortranTypeInference.getType(subexpr)
            if (exprType !is FortranIntegerType && exprType !is FortranRealType) {
                if (!exprType.isProblemDescriptionSet) {
                    return FortranUnrecognizedType("Can't infer type of this unary addition expression where argument is: " +
                            exprType.getName())
                }

                return FortranUnrecognizedType(exprType.typeProblemDescription())
            }

            return exprType
        }

        fun getType(expr: FortranNotExpr) : FortranType {
            val subexpr = expr.expr ?: return FortranUnrecognizedType()
            val exprType = FortranTypeInference.getType(subexpr)
            if (exprType !is FortranLogicalType) {
                if (!exprType.isProblemDescriptionSet) {
                    return FortranUnrecognizedType("Can't infer type of this \"logical not\" expression where argument is: " +
                            exprType.getName())
                }

                return FortranUnrecognizedType(exprType.typeProblemDescription())
            }

            return exprType
        }

        fun getType(expr: FortranParenthesisedExpr) : FortranType {
            val subexpr = expr.expr ?: return FortranUnrecognizedType()
            return FortranTypeInference.getType(subexpr)
        }

        fun getType(expr: FortranExpr) : FortranType {
            when (expr) {
                is FortranConstant -> return getType(expr)
                is FortranAddExpr -> return getType(expr)
                is FortranMultExpr -> return getType(expr)
                is FortranPowerExpr -> return getType(expr)
                is FortranRelExpr -> return getType(expr)
                is FortranAndExpr -> return getType(expr)
                is FortranOrExpr -> return getType(expr)
                is FortranNotExpr -> return getType(expr)
                is FortranUnaryAddExpr -> return getType(expr)
                is FortranLogicalLiteral -> return getType(expr)
                is FortranComplexLiteral -> return getType(expr)
                is FortranParenthesisedExpr -> return getType(expr)
            }

            return FortranUnrecognizedType("Unknown expression type")
        }


        private fun getBinaryOpArithmExprType(leftArgumentType: FortranType,
                                              rightArgumentType: FortranType) : FortranType {

            if (leftArgumentType is FortranUnrecognizedType ||
                    rightArgumentType is FortranUnrecognizedType) {
                return FortranUnrecognizedType()
            }

            if (leftArgumentType is FortranRealType &&
                    (rightArgumentType is FortranRealType || rightArgumentType is FortranIntegerType)) {
                return FortranRealType()
            }

            if (leftArgumentType is FortranIntegerType) {
                if (rightArgumentType is FortranRealType) {
                    return FortranRealType()
                } else if (rightArgumentType is FortranIntegerType) {
                    return FortranIntegerType()
                }

                return FortranUnrecognizedType()
            }

            if (leftArgumentType is FortranComplexType && rightArgumentType is FortranComplexType) {
                return FortranComplexType()
            }

            return FortranUnrecognizedType()
        }

        private fun getBinaryOpLogicalExprType(leftArgumentType: FortranType,
                                               rightArgumentType: FortranType) : FortranType {
            if (leftArgumentType is FortranLogicalType && rightArgumentType is FortranLogicalType) {
                return FortranLogicalType()
            }

            return FortranUnrecognizedType()
        }

    }
}