package korablique.recipecalculator.ui.nestingadapters;

import android.view.ViewGroup;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.ui.nestingadapters.AdapterParent.ChildWithPosition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdapterParentTest {
    private AdapterParent adapterParent;
    private AdapterChild child1;
    private AdapterChild child2;

    @Before
    public void setUp() {
        adapterParent = new AdapterParent();
        child1 = mock(AdapterChild.class);
        child2 = mock(AdapterChild.class);
        adapterParent.addChild(child1);
        adapterParent.addChild(child2);
    }

    @Test
    public void transformingChildPositionIntoParentPositionWorks() {
        // 2 чайлда
        // в каждом по 2 элемента
        when(child1.getItemCount()).thenReturn(2);
        when(child2.getItemCount()).thenReturn(2);

        // позиция "1" во втором чайлде == позиция "3" в родителе
        int parentPosition = adapterParent.transformChildPositionIntoParentPosition(1, child2);

        Assert.assertEquals(3, parentPosition);
    }

    @Test
    public void transformingParentPositionIntoChildPositionWorks() {
        // 2 чайлда
        // в каждом по 2 элемента
        when(child1.getItemCount()).thenReturn(2);
        when(child2.getItemCount()).thenReturn(2);

        // позиция "3" в родителе == позиция "1" во втором чайлде
        ChildWithPosition childWithPosition = adapterParent.transformParentPositionIntoChildPosition(3);

        Assert.assertEquals(1, childWithPosition.position);
        Assert.assertEquals(child2, childWithPosition.child);
    }

    // когда viewType = 1, то дергается у первого чайлда onCreateViewHolder,
    // а когда 3 - у второго
    @Test
    public void onCreateViewHolderInvokedInCorrectChild() {
        when(child1.getItemCount()).thenReturn(2);
        when(child2.getItemCount()).thenReturn(2);
        when(child1.getItemViewTypesCount()).thenReturn(2);
        when(child2.getItemViewTypesCount()).thenReturn(2);

        ViewGroup parent = mock(ViewGroup.class);

        verify(child1, never()).onCreateViewHolder(parent, 1);
        adapterParent.onCreateViewHolder(parent, 1);
        verify(child1).onCreateViewHolder(parent, 1);

        verify(child2, never()).onCreateViewHolder(parent, 1);
        adapterParent.onCreateViewHolder(parent, 2);
        verify(child2).onCreateViewHolder(parent, 0);
    }

    // когда передаем позишн = 1, viewType = 1
    // когда 3 - viewType = 3
    @Test
    public void getItemViewTypeWorksCorrectly() {
        when(child1.getItemCount()).thenReturn(2);
        when(child2.getItemCount()).thenReturn(2);
        when(child1.getItemViewTypesCount()).thenReturn(2);
        when(child2.getItemViewTypesCount()).thenReturn(2);

        when(child1.getItemViewType(0)).thenReturn(0);
        when(child1.getItemViewType(1)).thenReturn(1);
        when(child2.getItemViewType(0)).thenReturn(0);
        when(child2.getItemViewType(1)).thenReturn(1);

        int viewType = adapterParent.getItemViewType(1);
        Assert.assertEquals(1, viewType);
        viewType = adapterParent.getItemViewType(3);
        Assert.assertEquals(3, viewType);
    }

    @Test
    public void childrenRemovalWorks() {
        when(child1.getItemCount()).thenReturn(2);
        Assert.assertEquals(2, adapterParent.getItemCount());

        adapterParent.removeChild(child1);
        Assert.assertEquals(0, adapterParent.getItemCount());
    }
}
