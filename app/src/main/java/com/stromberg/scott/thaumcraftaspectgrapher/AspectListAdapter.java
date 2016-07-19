package com.stromberg.scott.thaumcraftaspectgrapher;

import android.app.Dialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AspectListAdapter extends RecyclerView.Adapter<AspectListAdapter.AspectViewHolder> {
    private List<Aspect> items;
    private AspectListOnClickListener onClickListener;
    private Dialog dialog;
    private int dialogType;

    public AspectListAdapter(AspectListOnClickListener onClickListener, Dialog dialog, int dialogType) {
        this.onClickListener = onClickListener;
        this.dialog = dialog;
        this.dialogType = dialogType;
    }

    @Override
    public AspectViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.aspect_list_row, parent, false);
        return new AspectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AspectViewHolder holder, int position) {
        final Aspect aspect = items.get(position);
        holder.mAspectNameTextView.setText(aspect.getName());
        holder.mAspectImageView.setImageResource(aspect.getImageResourceId());

        final boolean isLink = aspect.getLinkedAspects().contains(MainActivity.selectedAspect);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onClick(aspect, !isLink);
                dialog.dismiss();
            }
        });
        holder.mAspectLinkImageView.setVisibility(isLink ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        if(items == null) {
            return 0;
        }
        else {
            return items.size();
        }
    }

    public void setItems(List<Aspect> items) {
        this.items = items;
    }

    public List<Aspect> getItems() {
        return items;
    }

    public class AspectViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mAspectImageView;
        public final ImageView mAspectLinkImageView;
        public final TextView mAspectNameTextView;

        public AspectViewHolder(View itemView) {
            super(itemView);
            mAspectImageView = (ImageView) itemView.findViewById(R.id.aspect_list_row_image);
            mAspectLinkImageView = (ImageView) itemView.findViewById(R.id.aspect_list_row_link_image);
            mAspectNameTextView = (TextView) itemView.findViewById(R.id.aspect_list_row_name);
        }
    }
}