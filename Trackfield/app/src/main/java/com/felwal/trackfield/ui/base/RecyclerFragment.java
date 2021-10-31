package com.felwal.trackfield.ui.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.felwal.trackfield.R;
import com.felwal.trackfield.data.db.DbReader;
import com.felwal.trackfield.ui.common.model.Exerlite;
import com.felwal.trackfield.ui.common.model.Header;
import com.felwal.trackfield.ui.common.model.RecyclerItem;
import com.felwal.trackfield.ui.common.model.Sorter;
import com.felwal.trackfield.ui.widget.sheet.SortSheet;
import com.felwal.trackfield.ui.main.MainActivity;
import com.felwal.trackfield.utils.LayoutUtils;
import com.felwal.trackfield.utils.model.SortMode;

import java.util.ArrayList;

public abstract class RecyclerFragment extends Fragment implements DelegateClickListener {

    protected Activity a;
    protected DbReader reader;
    protected RecyclerView.LayoutManager manager;

    // empty page contents
    protected TextView emptyTitle;
    protected TextView emptyMessage;
    protected ImageView emptyImage;

    // items
    protected ArrayList<RecyclerItem> allItems = new ArrayList<>();
    protected final ArrayList<RecyclerItem> items = new ArrayList<>();

    private RecyclerView recycler;
    private BaseAdapter adapter;
    private ConstraintLayout emptyCl;

    // extends Fragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        a = getActivity();
        reader = DbReader.get(a);

        recycler = view.findViewById(R.id.rv_recycler);
        manager = new LinearLayoutManager(a);
        recycler.setLayoutManager(manager);
        recycler.setHasFixedSize(true);

        // empty page
        emptyCl = view.findViewById(R.id.cl_recycler_empty);
        emptyTitle = emptyCl.findViewById(R.id.tv_recycler_empty);
        emptyMessage = emptyCl.findViewById(R.id.tv_recycler_message);
        emptyImage = emptyCl.findViewById(R.id.iv_recycler_empty);
        emptyCl.setVisibility(View.GONE);

        setEmptyPage();
        setSorter();
        setAdapter();

        // set scroll listener
        if (a instanceof MainActivity) {
            ((MainActivity) a).setRecyclerScrollListener(recycler, this);
        }

        return view;
    }

    // set

    protected abstract void setEmptyPage();

    protected abstract void setSorter();

    private void setAdapter() {
        if (adapter == null) {
            new Thread(() -> {
                // add items
                if (items.size() == 0) {
                    items.addAll(getRecyclerItems());
                    allItems.addAll(items);
                }
                a.runOnUiThread(() -> {
                    // set adapter
                    recycler.setAdapter(adapter = getAdapter());
                    LayoutUtils.crossfadeRecycler(recycler);
                });
            }).start();
        }
        else recycler.setAdapter(adapter);
    }

    // get

    protected abstract BaseAdapter getAdapter();

    protected abstract ArrayList<RecyclerItem> getRecyclerItems();

    protected RecyclerItem getItem(int position) {
        if (position < 0 || position >= adapter.getItems().size()) return null;
        return adapter.getItems().get(position);
    }

    protected ArrayList<RecyclerItem> getVisibleItems() {
        ArrayList<RecyclerItem> visibleItems = new ArrayList<>();

        for (RecyclerItem item : allItems) {
            if (item.isVisible()) visibleItems.add(item);
        }
        return visibleItems;
    }

    // update

    public void updateRecycler(final ArrayList<RecyclerItem> newItems) {
        class RecyclerItemCallback extends DiffUtil.Callback {

            private final ArrayList<RecyclerItem> oldList;
            private final ArrayList<RecyclerItem> newList;

            private RecyclerItemCallback(ArrayList<RecyclerItem> oldList, ArrayList<RecyclerItem> newList) {
                this.oldList = oldList;
                this.newList = newList;
            }

            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                RecyclerItem oldItem = oldList.get(oldItemPosition);
                RecyclerItem newItem = newList.get(newItemPosition);
                return oldItem.sameItemAs(newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                RecyclerItem oldItem = oldList.get(oldItemPosition);
                RecyclerItem newItem = newList.get(newItemPosition);
                return oldItem.sameContentAs(newItem);
            }

        }
        final DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new RecyclerItemCallback(items, newItems));

        items.clear();
        items.addAll(newItems);
        //allItems.clear();
        //allItems.addAll(items);
        diff.dispatchUpdatesTo(adapter);
    }

    public void updateRecycler() {
        new Thread(() -> {
            allItems = getRecyclerItems();
            final ArrayList<RecyclerItem> newItems = getVisibleItems();
            a.runOnUiThread(() -> updateRecycler(newItems));
        }).start();
    }

    public void scrollToTop() {
        //recycler.scrollToPosition(25);
        recycler.smoothScrollToPosition(0);
    }

    // item clicks

    /**
     * Delegation of delegate click to handle common item {@link Sorter}
     */
    protected void onDelegateClick(RecyclerItem item) {
        if (item instanceof Sorter) {
            Sorter sorter = (Sorter) item;

            SortSheet sheet = SortSheet.newInstance(sorter);
            sheet.show(getChildFragmentManager());
            getChildFragmentManager().executePendingTransactions();
        }
    }

    public abstract void onSortSheetDismiss(int selectedIndex);

    // tools

    protected void addItemsWithHeaders(ArrayList<RecyclerItem> toItemList, ArrayList<Exerlite> fromExerliteList,
        SortMode.Mode sortMode) {

        if (sortMode == SortMode.Mode.DATE) {
            int year = -1;
            int newYear;
            for (Exerlite e : fromExerliteList) {
                if ((newYear = e.getDate().getYear()) != year) {
                    toItemList.add(new Header(newYear + "", Header.Type.REC));
                    year = newYear;
                }
                toItemList.add(e);
            }
        }
        else {
            for (int i = 0; i < fromExerliteList.size(); i++) {
                if (i == 0) toItemList.add(new Header("Top " + 3, Header.Type.REC));
                else if (i == 3) toItemList.add(new Header("Top " + 10, Header.Type.REC));
                else if (i % 10 == 0) toItemList.add(new Header("Top " + (i + 10), Header.Type.REC));
                toItemList.add(fromExerliteList.get(i));
            }
        }
    }

    protected void fadeInEmpty() {
        a.runOnUiThread(() -> LayoutUtils.crossfadeIn(emptyCl, 1));
    }

    protected void fadeOutEmpty() {
        a.runOnUiThread(() -> LayoutUtils.crossfadeOut(emptyCl));
    }

}
