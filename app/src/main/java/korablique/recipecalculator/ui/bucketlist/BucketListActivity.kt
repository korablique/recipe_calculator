package korablique.recipecalculator.ui.bucketlist

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.model.Recipe
import javax.inject.Inject

@VisibleForTesting
const val EXTRA_PRODUCED_RECIPE = "EXTRA_CREATED_RECIPE"
@VisibleForTesting
const val ACTION_EDIT_RECIPE = "ACTION_EDIT_RECIPE"
@VisibleForTesting
const val EXTRA_RECIPE = "EXTRA_RECIPE"


class BucketListActivity() : BaseActivity(), HasSupportFragmentInjector {
    @JvmField
    @Inject
    var fragmentInjector: DispatchingAndroidInjector<Fragment>? = null
    @JvmField
    @Inject
    var controller: BucketListActivityController? = null

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector!!
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_bucket_list
    }

    companion object {
        @JvmStatic
        fun createRecipeResultIntent(recipe: Recipe?): Intent =
                BucketListActivityController.createRecipeResultIntent(recipe)
        @JvmStatic
        fun start(context: Activity, requestCode: Int) =
                BucketListActivityController.start(context, requestCode)
        @JvmStatic
        fun createIntent(context: Context): Intent =
                BucketListActivityController.createIntent(context)
        @JvmStatic
        fun startForRecipe(fragment: Fragment, requestCode: Int, recipe: Recipe) =
                BucketListActivityController.startForRecipe(fragment, requestCode, recipe)
        @JvmStatic
        fun createIntent(context: Context, recipe: Recipe): Intent =
                BucketListActivityController.createIntent(context, recipe)
    }
}