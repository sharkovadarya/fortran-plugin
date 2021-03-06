package org.jetbrains.fortran.lang.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.fortran.lang.psi.FortranProgramUnit
import org.jetbrains.fortran.lang.psi.ext.FortranNamedElement
import org.jetbrains.fortran.lang.psi.impl.FortranConstructNameDeclImpl
import org.jetbrains.fortran.lang.psi.mixin.FortranConstructNameImplMixin

class FortranConstructNameReferenceImpl(element: FortranConstructNameImplMixin) :
        FortranReferenceBase<FortranConstructNameImplMixin>(element), FortranReference {

    override val FortranConstructNameImplMixin.referenceAnchor: PsiElement get() = referenceNameElement

    override fun getVariants(): Array<Any> = emptyArray()

    override fun resolveInner(): List<FortranNamedElement>  {
        val programUnit = PsiTreeUtil.getParentOfType(element, FortranProgramUnit::class.java) ?: return emptyList()
        // find all labels in it
        return PsiTreeUtil.findChildrenOfType(programUnit, FortranConstructNameDeclImpl::class.java)
                .filter {element.getLabelValue().equals(it.getLabelValue(), true) }
    }
}