package korablique.recipecalculator.outside

import java.lang.RuntimeException

class ServerErrorException(val errResp: ServerErrorResponse)
    : RuntimeException(errResp.toString()) {

    override fun toString(): String {
        return errResp.toString()
    }
}