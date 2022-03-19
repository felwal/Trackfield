package me.felwal.trackfield.ui.main.groupingpager.routelist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import me.felwal.trackfield.R;
import me.felwal.trackfield.data.prefs.Prefs;
import me.felwal.trackfield.ui.base.BaseListAdapter;
import me.felwal.trackfield.ui.base.RecyclerFragment;
import me.felwal.trackfield.ui.common.model.RecyclerItem;
import me.felwal.trackfield.ui.common.model.SorterItem;
import me.felwal.trackfield.ui.groupdetail.routedetail.RouteDetailActivity;
import me.felwal.trackfield.ui.main.groupingpager.GroupingPagerFragment;
import me.felwal.trackfield.ui.main.groupingpager.routelist.model.RouteItem;
import me.felwal.trackfield.utils.AppConsts;

import java.util.ArrayList;

import me.felwal.android.util.ResourcesKt;

public class RouteListFragment extends RecyclerFragment {

    private final SorterItem sorter = new SorterItem(
        SorterItem.sortByDate("Recent"),
        SorterItem.sortByDateAlt("Discovered"),
        SorterItem.sortByName(),
        SorterItem.sortByAmount(),
        SorterItem.sortByDistance("Avg distance"),
        SorterItem.sortByPace("Best pace")
    );

    // extends Fragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // show hidden
        MenuItem hiddenItem = menu.findItem(R.id.action_show_hidden_groups);
        hiddenItem.setChecked(Prefs.areHiddenGroupsShown());
        if (Prefs.areHiddenGroupsShown()) hiddenItem.setIcon(R.drawable.ic_show_filled)
            .setTitle(R.string.action_hide_hidden);
        else hiddenItem.setIcon(R.drawable.ic_show).setTitle(R.string.action_show_hidden);
    }

    /**
     * Inflates toolbar menu in place of {@link GroupingPagerFragment#onCreateOptionsMenu(Menu, MenuInflater)}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        menu.clear(); // remove R.menu.menu_toolbar_main_groupingpager
        inflater.inflate(R.menu.menu_toolbar_main_routelist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // extends RecyclerFragment

    @Override
    protected void setEmptyPage() {
        emptyTitle.setText(getString(R.string.tv_text_empty_routelist_title));
        emptyMessage.setText(getString(R.string.tv_text_empty_routelist_msg));
        emptyImage.setImageDrawable(ResourcesKt.getDrawableCompatWithTint(a, R.drawable.ic_route, R.attr.tf_colorRoute));
    }

    @Override
    protected void setSorter() {
        sorter.setSelection(
            Prefs.getSorterIndex(AppConsts.Layout.ROUTE_LIST),
            Prefs.getSorterInversion(AppConsts.Layout.ROUTE_LIST));
    }

    @Override
    protected BaseListAdapter getAdapter() {
        return new RouteListDelegationAdapter(a, this, items);
    }

    @Override
    protected ArrayList<RecyclerItem> getRecyclerItems() {
        ArrayList<RecyclerItem> itemList = new ArrayList<>();
        ArrayList<RouteItem> routeItemList = reader.getRouteItems(sorter.getMode(), sorter.getAscending(),
            Prefs.areHiddenGroupsShown(), Prefs.getMainFilter());

        itemList.add(sorter.copy());
        itemList.addAll(routeItemList);
        if (routeItemList.size() == 0) {
            itemList.remove(sorter);
            fadeInEmpty();
        }
        else fadeOutEmpty();

        return itemList;
    }

    @Override
    public void onSortSheetDismiss(int selectedIndex) {
        sorter.select(selectedIndex);
        Prefs.setSorter(AppConsts.Layout.ROUTE_LIST, sorter.getSelectedIndex(), sorter.getOrderReversed());
        updateRecycler();
    }

    // implements DelegateClickListener

    @Override
    public void onDelegateClick(View view, int position) {
        RecyclerItem item = getItem(position);

        if (item instanceof RouteItem) {
            RouteDetailActivity.startActivity(a, ((RouteItem) items.get(position)).getId());
        }

        super.onDelegateClick(item);
    }

}
