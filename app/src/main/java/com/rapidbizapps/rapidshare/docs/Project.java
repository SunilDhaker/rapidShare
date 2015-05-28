/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 3/4/14.
 */

package com.rapidbizapps.rapidshare.docs;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Revision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.util.Log;
import com.rapidbizapps.rapidshare.Application;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class Project {
    private static final String VIEW_NAME = "projects";
    private static final String DOC_TYPE = "project";

    public static Query getQuery(Database database) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper mapper = new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String)document.get("type");
                    if (DOC_TYPE.equals(type)) {
                        emitter.emit(document.get("title"), document);
                    }
                }
            };
            view.setMap(mapper, "1");
        }

        Query query = view.createQuery();
        return query;
    }

    public static Document createNewProject(Database database, String title, String userId)
            throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "project");
        properties.put("title", title);
        properties.put("created_at", currentTimeString);
        properties.put("members", new ArrayList<String>());
        if (userId != null)
            properties.put("owner", "profile:" + userId);

        Document document = database.createDocument();
        document.putProperties(properties);

        return document;
    }

    public static void assignOwnerToProjectsIfNeeded(Database database, Document user)
            throws CouchbaseLiteException {
        QueryEnumerator enumerator = getQuery(database).run();

        if (enumerator == null)
            return;

        while (enumerator.hasNext()) {
            Document document = enumerator.next().getDocument();

            String owner = (String) document.getProperty("owner");
            if (owner != null) continue;

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.putAll(document.getProperties());
            properties.put("owner", user.getId());
            document.putProperties(properties);
        }
    }

    public static void addMemberToProject(Document project, Document user)
            throws CouchbaseLiteException {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.putAll(project.getProperties());

        java.util.List<String> members = (java.util.List<String>) newProperties.get("members");
        if (members == null) members = new ArrayList<String>();
        members.add(user.getId());
        newProperties.put("members", members);

        try {
            project.putProperties(newProperties);
        } catch (CouchbaseLiteException e) {
            Log.e(Application.TAG, "Cannot add member to the project", e);
        }
    }

    public static void removeMemberFromProject(Document project, Document user)
            throws CouchbaseLiteException {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.putAll(project.getProperties());

        java.util.List<String> members = (java.util.List<String>) newProperties.get("members");
        if (members != null) members.remove(user.getId());
        newProperties.put("members", members);

        project.putProperties(newProperties);
    }
}
