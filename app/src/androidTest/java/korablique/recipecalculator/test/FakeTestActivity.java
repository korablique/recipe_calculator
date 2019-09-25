package korablique.recipecalculator.test;

import korablique.recipecalculator.base.BaseActivity;

/**
 * Fake test Activity class for tests which need an Activity to perform some actions
 * (for example, often an Activity is required to start another Activity).
 */
public class FakeTestActivity extends BaseActivity {
    @Override
    protected Integer getLayoutId() {
        return null;
    }
}
