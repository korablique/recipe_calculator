package korablique.recipecalculator.util

import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.thirdparty.GPAuthorizer

class FakeGPAuthorizer : GPAuthorizer {
    var authResult: GPAuthResult = GPAuthResult.Success("gptoken")

    override suspend fun auth(context: BaseActivity): GPAuthResult {
        return authResult
    }
}
