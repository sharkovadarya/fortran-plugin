package org.jetbrains.fortran.ide.inspections

class FortranTypeCheckInspectionTest()
    : FortranInspectionsBaseTestCase(FortranTypeCheckInspection()) {

    fun testDeclarationCharacterToInteger() = checkByText("""
        program p
        <warning descr="Assigning character value to a variable of integer type">integer :: a = "a"</warning>
        end program
    """, true)

    fun testDeclarationIntegerToComplex() = checkByText("""
        program p
        <warning descr="Assigning integer value to a variable of complex type">complex :: a = 1</warning>
        end program
    """, true)

    fun testDeclarationLogicalToReal() = checkByText("""
        program p
        <warning descr="Assigning logical value to a variable of real type">real :: a = .true. .AND. (.false. .OR. .true.)</warning>
        end program
    """, true)

    fun testDeclarationComplexToCharacter() = checkByText("""
        program p
        <warning descr="Assigning complex value to a variable of character type">character(len=17) :: a = (0.3, 2) + (1, 0)</warning>
        end program
    """, true)

    fun testDeclarationUnrecognizableType() = checkByText("""
        program p
        <warning descr="Can't infer type of this addition expression where arguments are: real, logical">real(kind=8) :: a = (2 + 9.0) + .true.</warning>
        end program
    """, true)

    fun testAssignmentComplexToCharacter() = checkByText("""
        program p
        character(len=6) :: a
        <warning descr="Assigning complex value to a variable of character type">a = (1, 2)</warning>
        end program p
    """, true)

    fun testAssignmentLogicalToIntegerArray() = checkByText("""
        program p
        integer, dimension(6) :: a
        <warning descr="Assigning character value to a variable of integer type">a(3) = "string"</warning>
        end program p
    """, true)


}