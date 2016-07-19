package com.stromberg.scott.thaumcraftaspectgrapher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AspectSearchListAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private ArrayList<Aspect> originalList;
    private ArrayList<Aspect> suggestions = new ArrayList<>();
    private Filter filter = new CustomFilter();

    public AspectSearchListAdapter(Context context, ArrayList<Aspect> originalList) {
        this.context = context;
        this.originalList = originalList;
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }


    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.aspect_list_row, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = (TextView) convertView.findViewById(R.id.aspect_list_row_name);
            holder.aspectImageView = (ImageView) convertView.findViewById(R.id.aspect_list_row_image);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.nameTextView.setText(suggestions.get(position).getName());
        holder.aspectImageView.setImageResource(suggestions.get(position).getImageResourceId());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.selectedAspect = suggestions.get(position);
                ((MainActivity)context).lastAddedAspect = suggestions.get(position);

                ((MainActivity)context).toggleAspectMenu(suggestions.get(position), true);

                View textView = ((MainActivity)context).getWindow().getCurrentFocus();
                if (textView != null) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                }
            }
        });

        return convertView;
    }


    @Override
    public Filter getFilter() {
        return filter;
    }

    private static class ViewHolder {
        TextView nameTextView;
        ImageView aspectImageView;
    }

    /**
     * Our Custom Filter Class.
     */
    private class CustomFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            suggestions.clear();

            if (originalList != null && constraint != null) { // Check if the Original List and Constraint aren't null.
                for (int i = 0; i < originalList.size(); i++) {
                    if (originalList.get(i).getName().toLowerCase().startsWith(constraint.toString())) { // Compare item in original list if it contains constraints.
                        suggestions.add(originalList.get(i)); // If TRUE add item in Suggestions.
                    }
                }
            }

            Collections.sort(suggestions, new Comparator<Aspect>() {
                @Override
                public int compare(Aspect aspect1, Aspect aspect2)
                {
                    return aspect1.getName().compareTo(aspect2.getName());
                }
            });

            FilterResults results = new FilterResults(); // Create new Filter Results and return this to publishResults;
            results.values = suggestions;
            results.count = suggestions.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}