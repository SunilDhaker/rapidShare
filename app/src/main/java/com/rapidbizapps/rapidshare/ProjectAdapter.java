package com.rapidbizapps.rapidshare;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;


public class ProjectAdapter extends LiveQueryRecyclerAdapter<ProjectAdapter.ViewHolder> implements View.OnClickListener {

    private OnItemClickListener onItemClickListener;

    public ProjectAdapter(Context context, LiveQuery liveQuery) {
        super(context, liveQuery);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(this.context).inflate(R.layout.project_item, viewGroup, false);
        ViewHolder holder = new ViewHolder(view, i);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ViewHolder projectViewHolder = (ViewHolder) viewHolder;
        final Document task = (Document) getItem(position);
        projectViewHolder.projectName.setText((String) task.getProperty("title"));
        projectViewHolder.projectName.setOnClickListener(this);
        projectViewHolder.projectName.setTag(position);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
       // if (viewId == R.id.listRowText) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, (Integer) view.getTag());
            }
       // }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public static interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    /*
    The ViewHolder class extends the RecyclerView.ViewHolder and
    is responsible for storing the inflated views in order to
    recycle them. It's a parameter type on the ProjectAdapter class.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView projectName , date , owner;

        public ViewHolder(View itemView, int ViewType) {
            super(itemView);
            projectName = (TextView) itemView.findViewById(R.id.titleLabel);
            date = (TextView) itemView.findViewById(R.id.updatedDateLabel);
            owner = (TextView) itemView.findViewById(R.id.userIdLabel);
        }


    }
}
