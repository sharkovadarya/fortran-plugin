package org.jetbrains.fortran.lang.types.infer

import org.jetbrains.fortran.lang.psi.*
import org.jetbrains.fortran.lang.psi.ext.FortranEntitiesOwner
import org.jetbrains.fortran.lang.psi.impl.FortranDataPathImpl
import org.jetbrains.fortran.lang.types.ty.*
import org.jetbrains.fortran.lang.utils.FortranDiagnostic

fun inferTypesIn(element: FortranEntitiesOwner): FortranInferenceResult {
    return FortranInferenceContext(element).infer()
}

class FortranInferenceContext(val element: FortranEntitiesOwner) {
    private val exprTypes: MutableMap<FortranExpr, FortranType> = mutableMapOf()
    private val noDeclarationVariableTypes: MutableMap<String, FortranType> = mutableMapOf()
    val diagnostics: MutableList<FortranDiagnostic> = mutableListOf()

    fun infer(): FortranInferenceResult {

        if (element is FortranMainProgram) {
            for (actionStmt in element.block!!.actionStmtList) {
                if (actionStmt is FortranAssignmentStmt) {
                    processAssignmentStmt(actionStmt)
                }
            }
        }

        for (v in element.variables) {
            val typeDeclarationStmt = v.parent as FortranTypeDeclarationStmt
            val expectedType = processTypeDeclarationStatement(typeDeclarationStmt)
            processVariable(typeDeclarationStmt.entityDeclList, expectedType)
        }

        return FortranInferenceResult(exprTypes, diagnostics)
    }

    private fun processTypeDeclarationStatement(typeDeclarationStmt: FortranTypeDeclarationStmt) : FortranType {
        return if (!typeDeclarationStmt.attrSpecList.isEmpty() &&
                typeDeclarationStmt.attrSpecList[0].text.substring(0, 9) == "dimension") {
            inferFortranArrayType(typeDeclarationStmt)
        } else {
            FortranPrimitiveType.fromTypeSpec(typeDeclarationStmt.intrinsicTypeSpec) as FortranType
        }
    }

    private fun processVariable(entityDeclList: List<FortranEntityDecl>, expected: FortranType) {
        // TODO when can there be more than one entry in entityDeclList and how to handle?
        val entityDecl = entityDeclList[0]
        val expr = entityDecl.expr

        // error diagnostics has been handled inside inferArrayConstructors()
        if (expr is FortranArrayConstructor) {
            return
        }

        if (expr != null) {
            val inferred = inferType(expr, expected)
            if (!combineTypes(expected, inferred)) {
                reportTypeMismatch(expr, expected, inferred)
            }
        }
    }

    private fun processAssignmentStmt(stmt: FortranAssignmentStmt) {
        if (stmt.exprList.size < 2) {
            return
        }

        val variable = stmt.exprList[0] as? FortranDesignator ?: return
        val variableType = inferFortranDesignatorType(variable)
        if (variableType !== FortranUnknownType) {
            val expr = stmt.exprList[1]
            val exprType = inferType(expr, variableType)

            // error diagnostics has been handled inside inferArrayConstructors()
            if (expr is FortranArrayConstructor) {
                return
            }

            if (!combineTypes(variableType, exprType)) {
                reportTypeMismatch(expr, variableType, exprType)
            }
        }
    }

    private fun inferType(expr: FortranExpr, expected: FortranType? = null) : FortranType {

        val type = when (expr) {
            is FortranBinaryExpr -> inferBinaryExprType(expr)
            is FortranUnaryAddExpr -> inferUnaryAddExprType(expr)
            is FortranNotExpr -> inferNotExprType(expr)
            is FortranConstant -> inferConstantType(expr)
            is FortranLogicalLiteral -> FortranLogicalType
            is FortranComplexLiteral -> FortranComplexType
            is FortranParenthesisedExpr -> inferParenthesisedExprType(expr)
            is FortranDesignator -> inferFortranDesignatorType(expr)
            is FortranArrayConstructor -> inferArrayConstructorType(expr, expected!!)
            else -> FortranUnknownType
        }

        writeExprType(expr, type)
        return type
    }

    private fun inferBinaryExprType(expr: FortranBinaryExpr) : FortranType {

        if (expr is FortranPowerExpr) {
            return inferBinaryPowerExprType(expr)
        }

        if (expr is FortranConcatExpr) {
            return inferBinaryConcatExprType(expr)
        }

        val leftType = inferType(expr.left())
        if (expr.right() == null) {
            return leftType
        }

        val rightType = inferType(expr.right()!!)

        // specific handler for complex numbers
        if (leftType === FortranComplexType && rightType === FortranComplexType &&
                (expr is FortranAddExpr || expr is FortranMultExpr)) {
            return FortranComplexType
        }

        val exprType = if (expr is FortranAddExpr || expr is FortranMultExpr ||
                expr is FortranRelExpr)
            inferArithmBinaryExprType(leftType, rightType)
        else inferLogicalBinaryExprType(leftType, rightType)

        if (exprType === FortranUnknownType) {
            reportMalformedBinaryExpression(expr, leftType, rightType)
        }

        return exprType
    }

    private fun inferArithmBinaryExprType(leftType: FortranType, rightType: FortranType) : FortranType {
        if (leftType === FortranIntegerType) {
            if (rightType === FortranIntegerType) {
                return FortranIntegerType
            }

            if (rightType === FortranRealType) {
                return FortranRealType
            }
        }

        if (rightType === FortranIntegerType) {
            if (leftType === FortranRealType) {
                return FortranRealType
            }
        }

        if (leftType === FortranRealType && rightType === FortranRealType) {
            return FortranRealType
        }

        if (leftType is FortranArrayType) {
            when (rightType) {
                is FortranArrayType -> {
                    if (leftType.size == rightType.size) {
                        val baseType = inferArithmBinaryExprType(leftType.base, rightType.base)
                        return if (baseType is FortranPrimitiveType) {
                            FortranArrayType(baseType, leftType.size)
                        } else {
                            FortranUnknownType
                        }
                    }

                    return FortranUnknownType
                }
                else -> {
                    val baseType = inferArithmBinaryExprType(leftType.base, rightType)
                    return if (baseType is FortranPrimitiveType) {
                        FortranArrayType(baseType, leftType.size)
                    } else {
                        FortranUnknownType
                    }
                }
            }
        } else if (rightType is FortranArrayType) {
            return inferArithmBinaryExprType(rightType, leftType)
        }

        return FortranUnknownType
    }

    private fun inferLogicalBinaryExprType(leftType: FortranType, rightType: FortranType) : FortranType {
        if (leftType === FortranLogicalType && rightType === FortranLogicalType) {
            return FortranLogicalType
        }

        return FortranUnknownType
    }

    private fun inferBinaryPowerExprType(expr: FortranPowerExpr): FortranType {
        val leftType = inferType(expr.left())
        if (expr.right() == null) {
            return if (leftType === FortranIntegerType || leftType === FortranRealType) {
                leftType
            } else {
                FortranUnknownType
            }
        }

        val rightType = inferType(expr.right()!!)
        if (rightType !== FortranIntegerType) {
            reportMalformedBinaryExpression(expr, leftType, rightType)
            return FortranUnknownType
        }

        return when (leftType) {
            is FortranIntegerType -> FortranIntegerType
            is FortranRealType -> FortranRealType
            else -> {
                reportMalformedBinaryExpression(expr, leftType, rightType)
                FortranUnknownType
            }
        }
    }

    private fun inferBinaryConcatExprType(expr: FortranConcatExpr): FortranType {
        val leftType = inferType(expr.left())
        if (expr.right() == null) {
            return if (leftType === FortranCharacterType) {
                leftType
            } else {
                FortranUnknownType
            }
        }

        val rightType = inferType(expr.right()!!)

        return if (leftType === FortranCharacterType && rightType === FortranCharacterType) {
            FortranCharacterType
        } else {
            reportMalformedBinaryExpression(expr, leftType, rightType)
            FortranUnknownType
        }
    }

    private fun inferParenthesisedExprType(expr: FortranParenthesisedExpr) : FortranType {
        val subexpr = expr.expr ?: return FortranUnknownType
        return inferType(subexpr)
    }

    private fun inferUnaryAddExprType(expr: FortranUnaryAddExpr) : FortranType {
        val subexpr = expr.expr ?: return FortranUnknownType
        val subexprType = inferType(subexpr)

        return when (subexprType) {
            is FortranIntegerType -> FortranIntegerType
            is FortranRealType -> FortranRealType
            else -> FortranUnknownType
        }
    }

    private fun inferConstantType(expr: FortranConstant) : FortranType {
        when {
            expr.integerliteral != null -> return FortranIntegerType
            expr.floatingpointliteral != null -> return FortranRealType
            expr.doubleprecisionliteral != null -> return FortranRealType
            expr.stringliteral != null -> return FortranCharacterType
        }

        return FortranUnknownType
    }

    private fun inferNotExprType(expr: FortranNotExpr) : FortranType {
        val subexpr = expr.expr ?: return FortranUnknownType
        val subexprType = inferType(subexpr)

        return if (subexprType != FortranLogicalType) FortranUnknownType else FortranLogicalType
    }

    private fun inferArrayConstructorType(expr: FortranArrayConstructor, expected: FortranType) : FortranType {
        val elementTypes: MutableMap<FortranExpr, FortranType> = mutableMapOf()
        var valuesSize = 0
        var extraSize = 0
        var extraType: FortranType? = null
        for (spec in expr.acSpec.acValueList) {
            val specExpr = spec.expr
            if (specExpr != null) {
                valuesSize++
                elementTypes[specExpr] = inferType(specExpr)
            } else {
                val acImpliedDo = spec.acImpliedDo
                if (acImpliedDo != null) {
                    val impliedDoType = handleAcImpliedDo(acImpliedDo)
                            as? FortranArrayType ?: return FortranUnknownType
                    extraSize += impliedDoType.size
                    extraType = impliedDoType.base // should always be Integer
                }
            }
        }

        val arrayTypes = elementTypes.values
        var arrayBaseType: FortranType
        if (!arrayTypes.isEmpty()) {
            arrayBaseType = arrayTypes.fold(arrayTypes.toList()[0], {t1, t2 -> unifyTypes(t1, t2)})
            if (extraType != null) {
                arrayBaseType = unifyTypes(arrayBaseType, extraType)
            }
        } else if (extraType != null) {
            arrayBaseType = extraType
        } else {
            arrayBaseType = FortranUnknownType
        }

        if (arrayBaseType is FortranPrimitiveType) {
            val arrayType = FortranArrayType(arrayBaseType, valuesSize + extraSize)
            if (!combineTypes(expected, arrayType)) {
                reportTypeMismatch(expr, expected, arrayType)
            }

            return arrayType
        }

        val expectedBase = (expected as FortranArrayType).base

        if (extraType != null && !combineTypes(expectedBase, extraType)) {
            for (spec in expr.acSpec.acValueList) {
                reportMalformedArrayConstructor(expr, expected, extraType)
            }
        }

        if (expectedBase is FortranPrimitiveType) {
            for (entry in elementTypes) {
                if (!combineTypes(expectedBase, entry.value)) {
                    reportMalformedArrayConstructor(entry.key, expectedBase, entry.value)
                }
            }
        }


        return FortranUnknownType
    }

    private fun handleAcImpliedDo(ac: FortranAcImpliedDo): FortranType {
        if (ac.idLoopStmt.exprList.size < 3) {
            return FortranUnknownType
        }

        val impliedDoLoopStart = ac.idLoopStmt.exprList[1]
        val impliedDoLoopEnd = ac.idLoopStmt.exprList[2]

        var size = 0
        try {
            val impliedDoLoopStartValue = impliedDoLoopStart.text.toInt()
            val impliedDoLoopEndValue = impliedDoLoopEnd.text.toInt()
            size = impliedDoLoopEndValue - impliedDoLoopStartValue + 1
        } catch (e: NumberFormatException) {
            // TODO get values from variable names
        }
        val impliedDoLoopStartType = inferType(impliedDoLoopStart)
        val impliedDoLoopEndType = inferType(impliedDoLoopEnd)
        if (impliedDoLoopEndType !== FortranIntegerType) {
            reportMalformedImpliedDoLoop(impliedDoLoopEnd, impliedDoLoopEndType)
            return FortranUnknownType
        }

        if (impliedDoLoopStartType !== FortranIntegerType) {
            reportMalformedImpliedDoLoop(impliedDoLoopStart, impliedDoLoopStartType)
            return FortranUnknownType
        }

        return FortranArrayType(FortranIntegerType, size)
    }

    private fun inferFortranDesignatorType(expr: FortranDesignator) : FortranType {
        return try {
            val variableEntityDecl = expr.dataPath?.reference?.resolve() as FortranEntityDecl
            val variableTypeDecl = variableEntityDecl.parent as FortranTypeDeclarationStmt
            val variableType = processTypeDeclarationStatement(variableTypeDecl)
            // accessing array element instead of array
            if ((expr.dataPath as FortranDataPathImpl).getSectionSubscript() != null &&
                    variableType is FortranArrayType) {
                variableType.base
            } else {
                variableType
            }
        } catch (e: Exception) {
            return FortranUnknownType
            //noDeclarationVariableTypes[expr.text] ?: FortranUnknownType
        }
    }

    private fun inferFortranArrayType(declarationStmt: FortranTypeDeclarationStmt): FortranType {
        // TODO handle multidimensional arrays
        return inferOneDimensionalArrayType(declarationStmt, 0)
    }

    private fun inferOneDimensionalArrayType(declarationStmt: FortranTypeDeclarationStmt, index: Int): FortranType {
        val arraySizeBounds = declarationStmt.attrSpecList[0].explicitShapeSpecList[index].exprList
        return try {
            val arraySize = if (arraySizeBounds.size == 1) arraySizeBounds[0].text.toInt() else {
                arraySizeBounds[1].text.toInt() - arraySizeBounds[0].text.toInt() + 1
            }
            val primitiveType = FortranPrimitiveType
                    .fromTypeSpec(declarationStmt.intrinsicTypeSpec)
                    ?: return FortranUnknownType
            FortranArrayType(primitiveType, arraySize)
        } catch (e: NumberFormatException) {
            // TODO parse variable values
            FortranUnknownType
        }
    }

    private fun combineTypes(expected: FortranType, inferred: FortranType): Boolean {
        if (expected === inferred) {
            return true
        }

        if (expected is FortranArrayType && inferred is FortranArrayType) {
            return combineArrayTypes(expected, inferred)
        }

        if (expected === FortranRealType && inferred === FortranIntegerType) {
            return true
        }

        return false
    }

    private fun unifyTypes(type1: FortranType, type2: FortranType): FortranType {
        if (type1 == type2) {
            return type1
        }

        if (combineTypes(type1, type2) && type1 === FortranRealType && type2 === FortranIntegerType) {
            return FortranRealType
        }

        if (combineTypes(type2, type1) && type2 === FortranRealType && type1 === FortranIntegerType) {
            return FortranRealType
        }

        if (type1 is FortranArrayType && type2 is FortranArrayType) {
            return unifyArrayTypes(type1, type2)
        }

        return FortranUnknownType
    }

    private fun unifyArrayTypes(type1: FortranArrayType, type2: FortranArrayType): FortranType {
        if (type1 == type2) {
            return type1
        }

        if (type1.size != type2.size || type1.base === FortranUnknownType || type2.base === FortranUnknownType) {
            return FortranUnknownType
        }
        val unifiedBaseType = unifyTypes(type1.base, type2.base) as? FortranPrimitiveType ?: return FortranUnknownType
        return FortranArrayType(unifiedBaseType, type1.size)
    }

    private fun combineArrayTypes(expected: FortranArrayType, inferred: FortranArrayType) : Boolean {
        if (expected == inferred) {
            return true
        }

        if (expected.base == FortranRealType && inferred.base == FortranIntegerType &&
                expected.size == inferred.size) {
            return true
        }

        return false
    }


    private fun addDiagnostic(diagnostic: FortranDiagnostic) {
        diagnostics.add(diagnostic)
    }

    private fun reportTypeMismatch(expr: FortranExpr, expected: FortranType, actual: FortranType) {
        addDiagnostic(FortranDiagnostic.TypeError(expr, expected, actual))
    }

    private fun reportMalformedBinaryExpression(expr: FortranBinaryExpr,
                                                leftType: FortranType, rightType: FortranType) {
        addDiagnostic(FortranDiagnostic.MalformedBinaryExpression(expr, leftType, rightType))
    }

    private fun reportMalformedArrayConstructor(expr: FortranExpr, arrayBaseType: FortranType, elementType: FortranType) {
        addDiagnostic(FortranDiagnostic.MalformedArrayConstructor(expr, arrayBaseType, elementType))
    }

    private fun reportMalformedImpliedDoLoop(expr: FortranExpr, type: FortranType) {
        addDiagnostic(FortranDiagnostic.MalformedImplicitDoLoop(expr, type))
    }

    private fun writeExprType(expr: FortranExpr, type: FortranType) {
        exprTypes[expr] = type
    }

    private fun writeNoDeclVariableType(variableName: String, type: FortranType) {
        noDeclarationVariableTypes[variableName] = type
    }
}