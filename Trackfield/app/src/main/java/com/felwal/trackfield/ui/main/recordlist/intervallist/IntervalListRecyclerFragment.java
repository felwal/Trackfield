package com.felwal.trackfield.ui.main.recordlist.intervallist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.felwal.android.widget.sheet.SortMode;
import com.felwal.trackfield.R;
import com.felwal.trackfield.data.prefs.Prefs;
import com.felwal.trackfield.ui.base.BaseAdapter;
import com.felwal.trackfield.ui.base.RecyclerFragment;
import com.felwal.trackfield.ui.common.model.RecyclerItem;
import com.felwal.trackfield.ui.common.model.SorterItem;
import com.felwal.trackfield.ui.main.recordlist.RecordListFragment;
import com.felwal.trackfield.ui.main.recordlist.intervallist.model.IntervalItem;
import com.felwal.trackfield.ui.recorddetail.intervaldetail.IntervalDetailActivity;
import com.felwal.trackfield.utils.AppConsts;

import java.util.ArrayList;

public class IntervalListRecyclerFragment extends RecyclerFragment {

    private final SorterItem sorter = new SorterItem(
        new SortMode("Recent", SorterItem.Mode.DATE, false),
        new SortMode("Amount", SorterItem.Mode.AMOUNT, false)
    );

    // extends Fragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Inflates toolbar menu in place of {@link RecordListFragment#onCreateOptionsMenu(Menu, MenuInflater)}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        menu.clear(); // remove R.menu.menu_toolbar_main_recs
        inflater.inflate(R.menu.menu_toolbar_main_intervallist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // extends RecyclerFragment

    @Override
    protected void setEmptyPage() {
        emptyTitle.setText(getString(R.string.tv_text_empty_intervallist_title));
        emptyMessage.setText(getString(R.string.tv_text_empty_intervallist_msg));
        emptyImage.setImageResource(R.drawable.ic_empty_interval);
    }

    @Override
    protected void setSorter() {
        sorter.setSelection(
            Prefs.getSorterIndex(AppConsts.Layout.INTERVALS),
            Prefs.getSorterInversion(AppConsts.Layout.INTERVALS));
    }

    @Override
    protected BaseAdapter getAdapter() {
        return new IntervalListAdapter(a, this, items);
    }

    @Override
    protected ArrayList<RecyclerItem> getRecyclerItems() {
        ArrayList<RecyclerItem> itemList = new ArrayList<>();
        ArrayList<IntervalItem> intervalItemList = reader.getIntervalItems(sorter.getMode(), sorter.getAscending(),
            Prefs.areHiddenRoutesShown());

        itemList.add(sorter.copy());
        itemList.addAll(intervalItemList);
        if (intervalItemList.size() == 0) {
            itemList.remove(sorter);
            fadeInEmpty();
        }
        else fadeOutEmpty();

        return itemList;
    }

    @Override
    public void onSortSheetDismiss(int selectedIndex) {
        sorter.select(selectedIndex);
        Prefs.setSorter(AppConsts.Layout.INTERVALS, sorter.getSelectedIndex(), sorter.getOrderReversed());
        updateRecycler();
    }

    // implements DelegateClickListener

    @Override
    public void onDelegateClick(View view, int position) {
        RecyclerItem item = getItem(position);

        if (item instanceof IntervalItem) {
            IntervalDetailActivity.startActivity(a, ((IntervalItem) items.get(position)).getInterval());
        }

        super.onDelegateClick(item);
    }

}
