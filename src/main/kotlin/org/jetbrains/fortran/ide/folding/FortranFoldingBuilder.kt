package org.jetbrains.fortran.ide.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.fortran.lang.psi.*
import org.jetbrains.fortran.lang.psi.ext.beginConstructStmt
import org.jetbrains.fortran.lang.psi.ext.beginUnitStmt
import org.jetbrains.fortran.lang.psi.ext.endConstructStmt
import org.jetbrains.fortran.lang.psi.ext.endUnitStmt
import java.util.*

class FortranFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<out FoldingDescriptor> {
        if (root !is FortranFile) return emptyArray()

        val descriptors: MutableList<FoldingDescriptor> = ArrayList()
        val visitor = FoldingVisitor(descriptors)
        PsiTreeUtil.processElements(root) { it.accept(visitor); true }

        return descriptors.toTypedArray()
    }

    private class FoldingVisitor(
            private val descriptors: MutableList<FoldingDescriptor>
    ) : FortranVisitor() {

        override fun visitProgramUnit(o: FortranProgramUnit) =
                foldBetweenStatements(o, o.beginUnitStmt, o.endUnitStmt)
        override fun visitExecutableConstruct(o: FortranExecutableConstruct) =
                foldBetweenStatements(o, o.beginConstructStmt, o.endConstructStmt)

        private fun foldBetweenStatements(element: PsiElement, left: FortranStmt?, right: FortranStmt?) {
            if (left != null && right != null) {
                val rightLabel = PsiTreeUtil.getChildOfType(right, FortranLabel::class.java)
                val rightOffset = if(rightLabel != null) {
                    var firstNonLabelElement = rightLabel.nextSibling
                    while (firstNonLabelElement is PsiWhiteSpace){
                        firstNonLabelElement = firstNonLabelElement.nextSibling
                    }
                    firstNonLabelElement.textOffset
                } else {
                    right.textOffset
                }
                val range = TextRange(left.textOffset + left.textLength, rightOffset)
                descriptors += FoldingDescriptor(element.node, range)
            }
        }
    }

    override fun isCollapsedByDefault(node: ASTNode) = false

}