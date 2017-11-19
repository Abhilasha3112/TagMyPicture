package zombiecatchers.tagmypicture;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
//import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AbsListView.MultiChoiceModeListener;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import zombiecatchers.tagmypicture.ImageGrid.TagImageGrid;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.PendingIntent.getActivity;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tagged_ID;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tagged_TABLE;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tagged_uri;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tags_ID;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tags_Name;
import static zombiecatchers.tagmypicture.MySQLiteHelper.Tags_TABLE;

public class TagGallery extends AppCompatActivity {
    ListView lv;
    List<String> CoverPics;
    List<String> TagNames;
    MySQLiteHelper ds;
    SQLiteDatabase db;
    FloatingActionButton addTagBtn;
    String uri;
    String imgDecodableString;
    private static final int REQUEST_FOR_STORAGE_PERMISSION = 123;
    final Context context = this;
    CustomListAdapter adapter;
    SparseBooleanArray selected;
    ActionMode mActionMode = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tag_gallery);

        ds=new MySQLiteHelper(this);
        addTagBtn=(FloatingActionButton)findViewById(R.id.addTag);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab=getSupportActionBar();
        boolean mboolean = false;

        SharedPreferences settings = getSharedPreferences("PREFS_NAME", 0);
        mboolean = settings.getBoolean("FIRST_RUN", false);
        if (!mboolean) {
            createDatabase();
            settings = getSharedPreferences("PREFS_NAME", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("FIRST_RUN", true);
            editor.commit();
        }
        if(mayRequestGalleryImages()) {
            saveDialogue();
        }

        addTagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String [] extractedTags=extractAllTags();
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Add a New Tag"); //Set Alert dialog title here
                alert.setMessage("Enter Tag Name"); //Message here

                // Set an EditText view to get user input
                final AutoCompleteTextView input = new AutoCompleteTextView(context);
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<String>(TagGallery.this, android.R.layout.simple_list_item_1,extractedTags);
                input.setThreshold(1);
                input.setAdapter(adapter);
                alert.setView(input);

                alert.setPositiveButton("Save Tag", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String tagedit = input.getEditableText().toString();
                        if(tagedit.length()>20)
                            Toast.makeText(TagGallery.this,"Max length is 20",Toast.LENGTH_SHORT).show();
                    else
                        saveTag(tagedit,null);
                    } // End of onClick(DialogInterface dialog, int whichButton)
                }); //End of alert.setPositiveButton
                alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        dialog.cancel();
                    }
                }); //End of alert.setNegativeButton
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
            }
        });

        lv=(ListView)findViewById(R.id.list);
        lv.setHeaderDividersEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tagName = lv.getItemAtPosition(position).toString();
                Intent intent = new Intent(TagGallery.this, TagImageGrid.class);
                intent.putExtra("tagName", tagName);
                startActivity(intent);
            }
        });

        lv.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                // Capture total checked items
                final int checkedCount = lv.getCheckedItemCount();
                if(checkedCount>1)
                {
                    mode.getMenu().findItem(R.id.rename).setVisible(false);
                }
                else
                {
                    mode.getMenu().findItem(R.id.rename).setVisible(true);
                }
                // Set the CAB title according to total checked items
                mode.setTitle(checkedCount + " Selected");
                // Calls toggleSelection method from ListViewAdapter Class
                adapter.toggleSelection(position);
            }
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.list_menu, menu);
                mActionMode=mode;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                selected = adapter.getSelectedIds();
                switch (item.getItemId()) {
                    case R.id.delete:
                        AlertDialog.Builder alert = new AlertDialog.Builder(TagGallery.this);
                        alert.setTitle("Delete Tags"); //Set Alert dialog title here
                        alert.setMessage("Are you sure you want to remove these Tags?"); //Message here

                        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                for (int i = (selected.size() - 1); i >= 0; i--) {
                                    if (selected.valueAt(i)) {

                                        String a = adapter.getItem(selected.keyAt(i));
                                        deleteTag(a);
                                        }
                                }
                            } // End of onClick(DialogInterface dialog, int whichButton)
                        });
                        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        });
                        AlertDialog alertDialog = alert.create();
                        alertDialog.show();
                        mode.finish();
                        return true;
                case R.id.rename:
                    String [] extractedTags=extractAllTags();

                    AlertDialog.Builder renameAlert = new AlertDialog.Builder(context);
                    renameAlert.setTitle("Change Tag Name"); //Set Alert dialog title here


                    // Set an EditText view to get user input
                    final AutoCompleteTextView input = new AutoCompleteTextView(context);
                    ArrayAdapter<String> autoCompleteAdapter =
                            new ArrayAdapter<String>(TagGallery.this, android.R.layout.simple_list_item_1,extractedTags);
                    input.setThreshold(1);
                    input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(40)});
                    input.setAdapter(autoCompleteAdapter);
                    renameAlert.setView(input);

                    int selectedId=-1;
                    for(int i=0; i< autoCompleteAdapter.getCount() ;i++){
                        if(selected.get(i)==true)
                            selectedId=i;
                    }
                    renameAlert.setMessage("Current Name: "+lv.getItemAtPosition(selectedId)); //Message here

                    renameAlert.setPositiveButton("Save Tag", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String tagedit = input.getEditableText().toString();
                            if(tagedit.length()>40)
                                Toast.makeText(TagGallery.this,"Max length is 40",Toast.LENGTH_SHORT).show();
                            else {
                                for (int i = (selected.size() - 1); i >= 0; i--) {
                                    if (selected.valueAt(i)) {

                                        String a = adapter.getItem(selected.keyAt(i));
                                        saveTag(tagedit,a);
                                    }
                                }
                            }
                        } // End of onClick(DialogInterface dialog, int whichButton)
                    }); //End of alert.setPositiveButton
                    renameAlert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            dialog.cancel();
                        }
                    }); //End of alert.setNegativeButton
                    AlertDialog renameAlertDialog = renameAlert.create();
                    renameAlertDialog.show();
                    mode.finish();
                    return true;
                    default:
                        return false;
                }

            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // TODO Auto-generated method stub
                adapter.removeSelection();
                mActionMode=null;
                mode.finish();
                TagGallery.this.findViewById(R.id.my_toolbar).setVisibility(View.VISIBLE);

            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // TODO Auto-generated method stub
                //Toast.makeText(getApplicationContext(), "menu", Toast.LENGTH_LONG).show();
                TagGallery.this.findViewById(R.id.my_toolbar).setVisibility(View.GONE);
                return false;
            }
        });
    }

    public void createDatabase()
    {
        db=openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE,null);
        db.execSQL("create table if not exists "
                + Tags_TABLE + "( " + Tags_ID
                + " integer primary key autoincrement, " + Tags_Name
                + " varchar not null);");

        db.execSQL("create table if not exists "
                + Tagged_TABLE + "( " + Tagged_ID
                + " integer primary key autoincrement, " + Tags_Name
                + " varchar not null,"+ Tagged_uri +" varchar not null);");
        db.close();
        //Toast.makeText(getApplicationContext(),"Database created...", Toast.LENGTH_LONG).show();

    }


    @SuppressLint("NewApi")
    private boolean mayRequestGalleryImages() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
            //promptStoragePermission();
            showPermissionRationaleSnackBar();
        } else {
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_FOR_STORAGE_PERMISSION);
        }

        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_FOR_STORAGE_PERMISSION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        populateListView();
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                            showPermissionRationaleSnackBar();
                        } else {
                            //Toast.makeText(this, "Go to settings and enable permission", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            }
        }
    }

    private void showPermissionRationaleSnackBar() {
        Snackbar.make(findViewById(R.id.addTag), getString(R.string.permission_rationale),
                Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Request the permission
                ActivityCompat.requestPermissions(TagGallery.this,
                        new String[]{READ_EXTERNAL_STORAGE},
                        REQUEST_FOR_STORAGE_PERMISSION);
            }
        }).show();

    }

    public void saveDialogue() {

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();
        if(Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            String [] extractedTags=extractAllTags();
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Tag Photo(s)"); //Set Alert dialog title here
            alert.setMessage("Enter Tag here"); //Message here

            // Set an EditText view to get user input
            final AutoCompleteTextView input = new AutoCompleteTextView(context);
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,extractedTags);
            input.setThreshold(1);
            input.setAdapter(adapter);
            alert.setView(input);

            alert.setPositiveButton("Save Tag", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //You will get as string input data in this variable.
                    // here we convert the input to a string and show in a toast.

                    String tagedit = input.getEditableText().toString();

                    if (Intent.ACTION_SEND.equals(action) && type != null) {
                        if (type.startsWith("image/")) {
                            handleSendImage(tagedit, intent); // Handle single image being sent
                        }
                    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                        if (type.startsWith("image/")) {
                            handleSendMultipleImages(tagedit, intent); // Handle multiple images being sent
                        }
                    } else {
                        // Handle other intents, such as being started from the home screen
                        Toast.makeText(getApplicationContext(), "Please select an image", Toast.LENGTH_LONG).show();
                    }

                } // End of onClick(DialogInterface dialog, int whichButton)
            }); //End of alert.setPositiveButton
            alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                    dialog.cancel();
                }
            }); //End of alert.setNegativeButton
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
            populateListView();
        }
    }
    void handleSendImage(String tagedit,Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            if (imageUri.toString().startsWith("content://com.google.android.apps.photos.content")){
                try {
                    int slash=imageUri.toString().lastIndexOf("/");
                    String filename=imageUri.toString().substring(slash+1);
                    InputStream is = getContentResolver().openInputStream(imageUri);
                    if (is != null) {
                        Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                        uri=saveToInternalStorage(pictureBitmap,filename);
                        imgDecodableString=uri;      //You can use this bitmap according to your purpose or Set bitmap to imageview
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            else {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(imageUri,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                uri = cursor.getString(column_index);
            }
                saveTagfromGallery(tagedit, uri);
                Toast.makeText(getApplicationContext(), "Tag Saved Successfully", Toast.LENGTH_LONG).show();
                // Update UI to reflect image being shared
                populateListView();

        }
    }


    void handleSendMultipleImages(String tagedit, Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {




            for(int i =0; i< imageUris.size();i++)
            {
                if (imageUris.get(i).toString().startsWith("content://com.google.android.apps.photos.content")){
                    try {
                        int slash=imageUris.get(i).toString().lastIndexOf("/");
                        String filename=imageUris.get(i).toString().substring(slash+1);
                        InputStream is = getContentResolver().openInputStream(imageUris.get(i));
                        if (is != null) {
                            Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                            uri=saveToInternalStorage(pictureBitmap,filename);
                            imgDecodableString=uri;      //You can use this bitmap according to your purpose or Set bitmap to imageview
                        }
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                else {
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(imageUris.get(i),
                            filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                     uri = cursor.getString( column_index );
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();
                }
                //display multiple images in grid
                saveTagfromGallery(tagedit, uri);
                // Toast.makeText(getApplicationContext(),"TAG  "+uri, Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getApplicationContext(), "Tag Saved Successfully", Toast.LENGTH_LONG).show();
            populateListView();

            // Update UI to reflect multiple images being shared
        }
    }
    private void populateListView()
    {
        Tag tag=new Tag();
        List<Tag> AllTags=ds.getAllTags();
        TagNames=new ArrayList<>();
        CoverPics=new ArrayList<>();
        List<Integer> CountOfPicsList=new ArrayList<>();

        try {
            db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);
            for (int i = 0; i < AllTags.size(); i++) {
                TagNames.add(AllTags.get(i).gettagName());
                CountOfPicsList.add(AllTags.get(i).getCountOfPics());
                String query="select uri from "+ Tagged_TABLE+" where "+ Tags_Name+"='"+
                        TagNames.get(i)+"' limit 1";
                Cursor cur=db.rawQuery(query,null);
                if (cur.moveToFirst()) {
                    File file=new File(cur.getString(cur.getColumnIndex(Tagged_uri)));
                    CoverPics.add(Uri.fromFile(file).toString());
                }
                else
                    CoverPics.add("null");
            }
        }
        catch (Exception e)
        {
            Log.e("coverpix",e.getMessage());
        }
        finally {
            if(db!=null)
                db.close();
        }
        adapter = new
                CustomListAdapter(TagGallery.this, TagNames, CoverPics,CountOfPicsList);


        //ArrayAdapter<Tag> adapter = new ArrayAdapter<Tag>(this,android.R.layout.simple_selectable_list_item,AllTags);
        ListView list=(ListView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }
    protected void saveTag(String tagName, String OldName){
        String tn = tagName.trim();
        if(tn.equals("")){
            Toast.makeText(getApplicationContext(),"Please enter a tag name", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            tn=tn.replace('\'','`');
            db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);
            String query = "select * from " + Tags_TABLE + " where " + Tags_Name + "='" + tn + "';";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.getCount() <= 0) {
                String sqLiteStatement="";
                if(OldName==null)
                    sqLiteStatement = "INSERT INTO " +
                        Tags_TABLE + " (" + Tags_Name + ") VALUES('"+tn +"');";
                else {
                    //update the tagNames for the tagged pictures first
                    sqLiteStatement = "Update " +
                            Tagged_TABLE + " set "+Tags_Name+" = '"+tn+"' where "+Tags_Name+" = '"+OldName+"'";
                    db.execSQL(sqLiteStatement);
                    sqLiteStatement = "Update " +
                            Tags_TABLE + " set "+Tags_Name+" = '"+tn+"' where "+Tags_Name+" = '"+OldName+"'";
                }
                db.execSQL(sqLiteStatement);
                db.close();
                Toast.makeText(getApplicationContext(), "Tag Saved Successfully", Toast.LENGTH_LONG).show();
                populateListView();
            } else
                Toast.makeText(getApplicationContext(), "Tag Already Exists", Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    protected void deleteTag(String tagName){
        MySQLiteHelper mySQLiteHelper=new MySQLiteHelper(TagGallery.this);
        String msg=mySQLiteHelper.deleteTag(tagName);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        populateListView();
    }
    //below is the duplicate method...to be removed later n called from tagpicture
    protected void saveTagfromGallery(String tagName,String fileUri){
        if(tagName==null || fileUri==null)
        {
            Toast.makeText(getApplicationContext(),"There seems to be some problem.Try Again.", Toast.LENGTH_LONG).show();
            return;
        }
        String tn = tagName.trim();
        String fu = fileUri.trim();
        if(tn.equals("")){
            Toast.makeText(getApplicationContext(),"Please enter a tag name", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            MySQLiteHelper ds=  new MySQLiteHelper(getApplicationContext());
            db = openOrCreateDatabase(ds.DATABASE_NAME, this.MODE_PRIVATE, null);
            // db = openOrCreateDatabase("Tag.db", this.MODE_PRIVATE, null);
            String query = "select * from " + Tags_TABLE + " where " + Tags_Name + "=='" + tagName + "'" ;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.getCount() <= 0) {
                query = "INSERT INTO " + Tags_TABLE + " (" + Tags_Name + ") VALUES('" + tn + "');";
                db.execSQL(query);
            }
            query = "select * from " + Tagged_TABLE + " where uri='" + uri + "'and tags_name='"+ tagName+"';";
            cursor = db.rawQuery(query, null);
            if (cursor.getCount() <= 0) {
                query = "insert into " + Tagged_TABLE + "(" + Tags_Name + "," + Tagged_uri +
                        ") values('" + tagName + "','" + imgDecodableString + "');";
                db.execSQL(query);
            }
        }
        catch(Exception e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        finally{
            db.close();
        }
    }
    public String[] extractAllTags()
    {
        MySQLiteHelper ds=  new MySQLiteHelper(getApplicationContext());
        db = openOrCreateDatabase(ds.DATABASE_NAME, this.MODE_PRIVATE, null);
        // db = openOrCreateDatabase("Tag.db", this.MODE_PRIVATE, null);
        String query = "select tags_name from " + Tags_TABLE ;
        Cursor cursor = db.rawQuery(query, null);
        String []tagsfromdb=new String[cursor.getCount()];
        cursor.moveToFirst();
        for(int i=0;i<cursor.getCount();i++)
        {
            tagsfromdb[i]  =cursor.getString(0);
            cursor.moveToNext();
        }
        return tagsfromdb;
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if(selected!=null)
            selected.clear();
    }

    protected void onResume()
    {
        super.onResume();
        populateListView();
    }

    protected void onPause()
    {
        super.onPause();
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                AlertDialog.Builder alert = new AlertDialog.Builder(TagGallery.this);
                alert.setTitle("Reset App?"); //Set Alert dialog title here
                alert.setMessage("You will lose all your tagged information."); //Message here
                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Context context=TagGallery.this;
                        context.deleteDatabase("Tag.db");
                        createDatabase();
                        populateListView();
                        Toast.makeText(getApplicationContext(),"App is reset...", Toast.LENGTH_LONG).show();
                    } // End of onClick(DialogInterface dialog, int whichButton)
                });
                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
                return true;

            case R.id.about:
                Intent intent = new Intent(TagGallery.this, About.class);
                startActivity(intent);

            default:
                return super.onOptionsItemSelected(item);

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // ...

        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when action item collapses
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                return true;  // Return true to expand action view
            }
        };

        // Get the MenuItem for the action item
        MenuItem actionMenuItem = menu.findItem(R.id.reset);

        // Assign the listener to that action item
        MenuItemCompat.setOnActionExpandListener(actionMenuItem, expandListener);

        // Any other things you have to do when creating the options menu…

        return true;
    }
    private String saveToInternalStorage(Bitmap bitmapImage,String filename){
     /*   ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");*/
        File direct = new File(Environment.getExternalStorageDirectory() + "/Download");

        if (!direct.exists()) {
            File wallpaperDirectory = new File("/sdcard/Download/");
            wallpaperDirectory.mkdirs();
        }

        File file = new File(new File("/sdcard/Download/"), filename+".jpg");
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

}
