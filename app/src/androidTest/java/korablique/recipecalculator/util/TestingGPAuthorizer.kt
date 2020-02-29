package korablique.recipecalculator.util

import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.thirdparty.GPAuthorizer
import java.lang.IllegalStateException

class TestingGPAuthorizer : GPAuthorizer() {
    override suspend fun auth(context: BaseActivity): GPAuthResult {
        return GPAuthResult.Failure(IllegalStateException("Not supported yet"))
    }
}