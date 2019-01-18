package com.example.new2;

import android.view.View;
import android.widget.TextView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.materialize.holder.StringHolder;

import java.util.List;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SimpleItem extends AbstractItem<SimpleItem, SimpleItem.ViewHolder> {
    public String name;

    public SimpleItem(String name) {
        this.name = name;
    }

    //The unique ID for this type of item
    @Override
    public int getType() {
        return R.id.permissions_item_id;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.sample_item;
    }

    @Override
    public void bindView(ViewHolder holder, List<Object> payloads) {
        super.bindView(holder, payloads);
        holder.name.setText(this.name);
    }

    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    /**
     * our ViewHolder
     */
    protected static class ViewHolder extends FastAdapter.ViewHolder<SimpleItem> {
        @BindView(R.id.package_name)
        TextView name;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        @Override
        public void bindView(SimpleItem item, List<Object> payloads) {
            StringHolder.applyTo(new StringHolder(item.name), name);
        }

        @Override
        public void unbindView(SimpleItem item) {
            name.setText(null);
        }
    }
}