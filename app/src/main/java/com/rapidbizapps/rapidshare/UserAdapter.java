package com.rapidbizapps.rapidshare;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.rapidbizapps.rapidshare.docs.Project;

import java.util.List;

/**
 * Created by sdhaker on 5/28/2015.
 */
public  class UserAdapter extends LiveQueryAdapter {

    Document mCurrentProject;

    public UserAdapter(Context context, LiveQuery query, Document mCurrentProject) {
        super(context, query);
        this.mCurrentProject = mCurrentProject;
    }

    private boolean isMemberOfTheCurrentProject(Document user) {
        java.util.List<String> members =
                (java.util.List<String>) mCurrentProject.getProperty("members");
        return members != null ? members.contains(user.getId()) : false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.user_item, null);
        }

        final Document task = (Document) getItem(position);

        TextView text = (TextView) convertView.findViewById(R.id.text);
        text.setText((String) task.getProperty("name"));

        final Document user = (Document) getItem(position);
        final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checked);
        boolean checked = isMemberOfTheCurrentProject(user);
        checkBox.setChecked(checked);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (checkBox.isChecked()) {
                        Project.addMemberToProject(mCurrentProject, user);
                    } else {
                        Project.removeMemberFromProject(mCurrentProject, user);
                    }
                } catch (CouchbaseLiteException e) {
                    Log.e(Application.TAG, "Cannot update a member to a list", e);
                }
            }
        });
        return convertView;
    }
}
