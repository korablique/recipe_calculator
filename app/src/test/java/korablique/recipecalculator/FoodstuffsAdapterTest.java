package korablique.recipecalculator;

import android.support.v7.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FoodstuffsAdapterTest {
    private FoodstuffsAdapter adapter;

    @Before
    public void setUp() {
        adapter = new FoodstuffsAdapter(new FoodstuffsAdapter.Observer() {
            @Override
            public void onItemClicked(Foodstuff foodstuff, int position) {
            }
            @Override
            public void onItemsCountChanged(int count) {
            }
        });

        RecyclerView v = new RecyclerView(RuntimeEnvironment.application);
        v.setAdapter(adapter);
    }

    @Test
    public void filteringWorks() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "asdasd";
        adapter.addItem(new Foodstuff(name1, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name2, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name3, 1, 2, 3, 4, 5));

        // Проверим, что адаптер готов к тесту (имеет валидное состояние)
        assertEquals(3, adapter.getItemCount());
        assertEquals(name1, adapter.getItem(0).getName());
        assertEquals(name2, adapter.getItem(1).getName());
        assertEquals(name3, adapter.getItem(2).getName());

        // Проверим фильтрацию
        adapter.setNameFilter("name");
        assertEquals(2, adapter.getItemCount());
        assertEquals(name1, adapter.getItem(0).getName());
        assertEquals(name2, adapter.getItem(1).getName());
    }

    @Test
    public void deletingWorks() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "asdasd";
        adapter.addItem(new Foodstuff(name1, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name2, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name3, 1, 2, 3, 4, 5));

        adapter.deleteItem(1);
        assertEquals(2, adapter.getItemCount());
        assertEquals(name1, adapter.getItem(0).getName());
        assertEquals(name3, adapter.getItem(1).getName());
    }

    @Test
    public void replacingWorks() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "asdasd";
        adapter.addItem(new Foodstuff(name1, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name2, 1, 2, 3, 4, 5));
        adapter.addItem(new Foodstuff(name3, 1, 2, 3, 4, 5));

        Foodstuff newFoodstuff = new Foodstuff("newName", 1, 2, 3, 4, 5);
        adapter.replaceItem(newFoodstuff, 0);
        assertEquals(3, adapter.getItemCount());
        assertEquals("newName", adapter.getItem(0).getName());
        assertEquals(name2, adapter.getItem(1).getName());
        assertEquals(name3, adapter.getItem(2).getName());
    }
}
