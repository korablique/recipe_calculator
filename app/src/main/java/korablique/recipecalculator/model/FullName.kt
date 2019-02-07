package korablique.recipecalculator.model

class FullName(val firstName: String, val lastName: String) {
    override fun toString(): String {
        return "$firstName $lastName"
    }
}
