package au.com.dss.gatehouse;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public class GatehouseFileProvider extends ContentProvider {
    public static final String AUTHORITY = "au.com.dss.gatehouse.fileprovider";

    @Override
    public boolean onCreate() {
        return true;
    }

    public static Uri getUriForFile(File file) {
        return Uri.parse("content://" + AUTHORITY + "/" + file.getName());
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null) throw new FileNotFoundException("Invalid URI: " + uri);
        File file = new File(getContext().getCacheDir(), name);
        if (!file.exists()) {
            file = new File(getContext().getExternalFilesDir(null), name);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + name);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String name = uri.getLastPathSegment();
        if (name == null) name = "gatehouse.apk";
        File file = new File(getContext().getCacheDir(), name);
        if (!file.exists()) {
            file = new File(getContext().getExternalFilesDir(null), name);
        }
        long size = file.exists() ? file.length() : 0;
        MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        cursor.addRow(new Object[]{name, size});
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}