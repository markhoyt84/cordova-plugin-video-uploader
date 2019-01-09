package org.apache.cordova.videouploader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.icu.text.PluralFormat;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import com.cloudinary.android.*;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoUploader extends CordovaPlugin {

    private static final String TAG = "VideoUploader";

    private CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute method starting");

        this.callback = callbackContext;

        if (action.equals("upload")) {
            try {
                this.uploadVideo(args);
            } catch (IOException e) {
                callback.error(e.toString());
            }
            return true;
        }

        return false;
    }

    private void uploadVideo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "UPLOADING VIDEO firing");

        JSONObject arguments = args.optJSONObject(0);
        Log.d(TAG, "options: " + arguments.toString());

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "video");
        options.put("width", arguments.getInt("width"));
        options.put("height", arguments.getInt("height"));
        options.put("x", arguments.getInt("startX"));
        options.put("y", arguments.getInt("startY"));

//        final String videoSrcPath = arguments.getString("fileUri");
        File inFile = this.resolveLocalFileSystemURI(arguments.getString("fileUri"));
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }

        String videoSrcPath = inFile.getAbsolutePath();
        Log.d(TAG, "videoSrcPath: " + videoSrcPath);

        MediaManager.get().upload(videoSrcPath).unsigned("stkpionicmobile").options(options).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                // your code here
            }
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // example code starts here
                Double progress = (double) bytes/totalBytes;
                // post progress to app UI (e.g. progress bar, notification)
                // example code ends here
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj.put("progress", progress);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
                progressResult.setKeepCallback(true);
                callback.sendPluginResult(progressResult);
            }
            @Override
            public void onSuccess(String requestId, Map resultData) {
                JSONObject json = new JSONObject(resultData);
                callback.success(json);
            }
            @Override
            public void onError(String requestId, ErrorInfo error) {
                callback.error(error.getDescription());
            }
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // your code here
            }})
                .dispatch();

//        cordova.getThreadPool().execute(new Runnable() {
//            public void run() {
//
//                try {
//
//                    String requestId = MediaManager.get().upload(videoSrcPath).options(options).callback(new UploadCallback() {
//                      @Override
//                      public void onStart(String requestId) {
//                        // your code here
//                      }
//                      @Override
//                      public void onProgress(String requestId, long bytes, long totalBytes) {
//                                // example code starts here
//                        Double progress = (double) bytes/totalBytes;
//                        // post progress to app UI (e.g. progress bar, notification)
//                                // example code ends here
//                          JSONObject jsonObj = new JSONObject();
//                          try {
//                              jsonObj.put("progress", progress);
//                          } catch (JSONException e) {
//                              e.printStackTrace();
//                          }
//
//                          PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
//                          progressResult.setKeepCallback(true);
//                          callback.sendPluginResult(progressResult);
//                      }
//                      @Override
//                      public void onSuccess(String requestId, Map resultData) {
//                          JSONObject json = new JSONObject(resultData);
//                         callback.success(json);
//                      }
//                      @Override
//                      public void onError(String requestId, ErrorInfo error) {
//                          callback.error(error.getDescription());
//                      }
//                      @Override
//                      public void onReschedule(String requestId, ErrorInfo error) {
//                        // your code here
//                      }})
//                      .dispatch();
//                } catch (Throwable e) {
//                    Log.d(TAG, "transcode exception ", e);
//                    callback.error(e.toString());
//                }
//
//            }
//        });
    }

    @SuppressWarnings("deprecation")
    private File resolveLocalFileSystemURI(String url) throws IOException, JSONException {
        String decoded = URLDecoder.decode(url, "UTF-8");

        File fp = null;

        // Handle the special case where you get an Android content:// uri.
        if (decoded.startsWith("content:")) {
            fp = new File(getPath(this.cordova.getActivity().getApplicationContext(), Uri.parse(decoded)));
        } else {
            // Test to see if this is a valid URL first
            @SuppressWarnings("unused")
            URL testUrl = new URL(decoded);

            if (decoded.startsWith("file://")) {
                int questionMark = decoded.indexOf("?");
                if (questionMark < 0) {
                    fp = new File(decoded.substring(7, decoded.length()));
                } else {
                    fp = new File(decoded.substring(7, questionMark));
                }
            } else if (decoded.startsWith("file:/")) {
                fp = new File(decoded.substring(6, decoded.length()));
            } else {
                fp = new File(decoded);
            }
        }

        if (!fp.exists()) {
            throw new FileNotFoundException( "" + url + " -> " + fp.getCanonicalPath());
        }
        if (!fp.canRead()) {
            throw new IOException("can't read file: " + url + " -> " + fp.getCanonicalPath());
        }
        return fp;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
