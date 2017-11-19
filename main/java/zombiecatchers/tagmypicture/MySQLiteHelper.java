package zombiecatchers.tagmypicture;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Allen on 22/01/2017.
 */

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String Tagged_TABLE = "tagged_table";
    public static final String Tagged_ID = "tagged_id";
    public static final String Tagged_uri = "uri";

    public static final String Tags_TABLE = "tags_table";
    public static final String Tags_ID = "tags_id";
    public static final String Tags_Name = "tags_name";

    public static final String DATABASE_NAME = "Tag.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String Tagged_Table_Create = "create table if not exists "
            + Tagged_TABLE + "( " + Tagged_ID
            + " integer primary key autoincrement, " + Tags_Name
            + " varchar not null,"+ Tagged_uri +" varchar not null);";
    private static final String Tags_Table_Create="create table if not exists "
            + Tags_TABLE + "( " + Tags_ID
            + " integer primary key autoincrement, " + Tags_Name
            + " varchar not null);";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database)
    {
        database.execSQL(Tags_Table_Create);
        database.execSQL(Tagged_Table_Create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + Tagged_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Tags_TABLE);
        onCreate(db);
    }


    private SQLiteDatabase database;

    public String deleteTag(String tag_name ) {
        try {
            database=getWritableDatabase();
            database.delete(MySQLiteHelper.Tagged_TABLE, MySQLiteHelper.Tags_Name
                    + " = '" + tag_name+"';", null);
            database.delete(MySQLiteHelper.Tags_TABLE, MySQLiteHelper.Tags_Name
                    + " = '" + tag_name+"';",null);
            return new String("Tag deleted Successfully");
        }
        catch(Exception e) {
            return new String("Error: "+e.getMessage());
        }
        finally
        {
            database.close();
        }
    }

    public String untagPic(String tag_name,ArrayList<String> SelectedItems ) {
        try {
            database=getWritableDatabase();
            for(int i=0;i<SelectedItems.size();i++) {
                Uri uri=Uri.parse(SelectedItems.get(i));
                String realpath=uri.getPath();
                database.delete(MySQLiteHelper.Tagged_TABLE, MySQLiteHelper.Tags_Name
                        + " = '" + tag_name + "' and " + MySQLiteHelper.Tagged_uri + "='" + realpath + "';", null);
            }
            return new String("Picture untagged Successfully");
        }
        catch(Exception e) {
            return new String("Error: "+e.getMessage());
        }
        finally
        {
            database.close();
        }
    }

    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<Tag>();
        database = getWritableDatabase();
        String query = "SELECT * FROM " + MySQLiteHelper.Tags_TABLE +" order by "+MySQLiteHelper.Tags_Name+";";
        Cursor cursor = database.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Tag tag = new Tag();
                tag.settag(cursor.getLong(cursor.getColumnIndex(MySQLiteHelper.Tags_ID)),
                        cursor.getString(cursor.getColumnIndex(MySQLiteHelper.Tags_Name)));
                String CountQuery = "SELECT count("+MySQLiteHelper.Tagged_ID+") as count FROM " + MySQLiteHelper.Tagged_TABLE +" where "
                        + MySQLiteHelper.Tags_Name + " = '"
                        + cursor.getString(cursor.getColumnIndex(MySQLiteHelper.Tags_Name)) + "';";
                Cursor CountCursor = database.rawQuery(CountQuery, null);
                if(CountCursor.moveToFirst()){
                    tag.setCountOfPics(CountCursor.getInt(CountCursor.getColumnIndex("count")));
                }
                tags.add(tag);
            }while (cursor.moveToNext());
        }
        cursor.close();
        close();
        return tags;
    }
}

