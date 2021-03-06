
package com.rapidbizapps.rapidshare.docs;

import android.graphics.Bitmap;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Task {
    private static final String VIEW_NAME = "tasks";
    private static final String DOC_TYPE = "task";
    private static final String TAG = "rsTask";

    public static Query getQuery(Database database, String projectDocId) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper map = new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (DOC_TYPE.equals(document.get("type"))) {
                        List<Object> keys = new ArrayList<Object>();
                        keys.add(document.get("project_id"));
                        keys.add(document.get("created_at"));
                        emitter.emit(keys, document);
                    }
                }
            };
            view.setMap(map, "1");
        }

        Query query = view.createQuery();
        query.setDescending(true);

        List<Object> startKeys = new ArrayList<Object>();
        startKeys.add(projectDocId);
        startKeys.add(new HashMap<String, Object>());

        List<Object> endKeys = new ArrayList<Object>();
        endKeys.add(projectDocId);

        query.setStartKey(startKeys);
        query.setEndKey(endKeys);

        return query;
    }

    public static Document createTask(Database database,
                                      String title,
                                      Bitmap image,
                                      String projectId ,
                                      String expire_on) throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", DOC_TYPE);
        properties.put("title", title);
        properties.put("checked", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        properties.put("project_id", projectId);
        properties.put("due_at" , expire_on);
        Document document = database.createDocument();

        UnsavedRevision revision = document.createRevision();
        revision.setUserProperties(properties);

        if (image != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 50, out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            revision.setAttachment("image", "image/jpg", in);
        }

        revision.save();
        Log.d(TAG, "Created doc: %s", document.getId());
        return document;
    }

    public static void attachImage(Document task, Bitmap image) throws CouchbaseLiteException {
        if (task == null || image == null) return;

        UnsavedRevision revision = task.createRevision();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        revision.setAttachment("image", "image/jpg", in);

        revision.save();
    }

    public static void updateCheckedStatus(Document task, boolean checked)
            throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(task.getProperties());
        properties.put("checked", checked);
        task.putProperties(properties);
    }

    public static void deleteTask(Document task) throws CouchbaseLiteException {
        task.delete();
        Log.d(TAG, "Deleted doc: %s", task.getId());
    }
}
