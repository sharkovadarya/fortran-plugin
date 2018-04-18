package org.jetbrains.fortran.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.fortran.ide.inspections.fixes.SubstituteTextFix
import org.jetbrains.fortran.lang.psi.*
import org.jetbrains.fortran.lang.psi.ext.smartPointer
import org.jetbrains.fortran.lang.types.FortranTypeInference

class FortranTypeCheckInspection : LocalInspectionTool() {
    override fun getDisplayName() = "Incorrect type"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
            object : FortranVisitor() {
                // under construction
                override fun visitAssignmentStmt(e: FortranAssignmentStmt) {
                    val variable = e.exprList[0]
                    val expr = e.exprList[1]
                    holder.registerProblem(variable,
                            "Incorrect assignment",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            SubstituteTextFix(e.smartPointer(), "Assign another value", "Incorrect Type Fix"))


                }

                override fun visitTypeDeclarationStmt(e: FortranTypeDeclarationStmt) {
                    val a = e.intrinsicTypeSpec as FortranIntrinsicTypeSpec
                    val type = FortranTypeInference.getType(a)
                    val lst = e.entityDeclList
                    for (d in lst) {
                        if (!type.isAssignable(d.expr)) {
                            holder.registerProblem(e,
                                    type.typeProblemDescription(),
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    SubstituteTextFix(e.smartPointer(), "Assign another value", "Incorrect Type Fix"))
                        }
                    }
                }
            }
}