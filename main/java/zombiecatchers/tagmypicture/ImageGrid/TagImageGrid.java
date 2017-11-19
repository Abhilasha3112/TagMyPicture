package zombiecatchers.tagmypicture.ImageGrid;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import zombiecatchers.tagmypicture.TagGallery;
import zombiecatchers.tagmypicture.MySQLiteHelper;
import zombiecatchers.tagmypicture.R;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class TagImageGrid extends AppCompatActivity {

	private ImageAdapter imageAdapter;

    private static String tagName;
    TextView TagNameTV;
    FloatingActionButton addPics;
    FloatingActionButton delPics;
    private static int RESULT_LOAD_IMG = 1;
    RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_multi_photo_select);
        tagName = getIntent().getExtras().getString("tagName");
        TagNameTV = (TextView) findViewById(R.id.TagNameTV);
        TagNameTV.setText(tagName);

        addPics = (FloatingActionButton) findViewById(R.id.addPics);
        addPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImagefromGallery(v);
            }
        });

        populateImages();

        delPics = (FloatingActionButton) findViewById(R.id.delPics);
        delPics.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
           final ArrayList<String> selectedItems = imageAdapter.getCheckedItems();
           if (selectedItems != null && selectedItems.size() > 0) {
               AlertDialog.Builder alert = new AlertDialog.Builder(TagImageGrid.this);
               alert.setTitle("Confirm to untag Pictures"); //Set Alert dialog title here
               alert.setMessage("No. of selected pictures: "+selectedItems.size()); //Message here

               alert.setPositiveButton("UnTag", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                       MySQLiteHelper mySQLiteHelper = new MySQLiteHelper(TagImageGrid.this);
                       String msg = mySQLiteHelper.untagPic(tagName, selectedItems);
                       Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                       populateImages();
                       toggleDeleteVisibility(false);
                   } // End of onClick(DialogInterface dialog, int whichButton)
               });
               alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                       dialog.cancel();
                   }
               });
               AlertDialog alertDialog = alert.create();
               alertDialog.show();
           }
           else
           {
               Toast.makeText(getApplicationContext(), "Select an image to untag", Toast.LENGTH_LONG).show();
           }
           }
        });
	}

	public void toggleDeleteVisibility(boolean val){
        if(val==false)
            delPics.setVisibility(View.GONE);
        else
            delPics.setVisibility(View.VISIBLE);
    }

	private void populateImages() {
		ArrayList<String> imageUrls = loadPhotos();
		initializeRecyclerView(imageUrls);
	}

	private ArrayList<String> loadPhotos() {
		ArrayList<String> imageUrls = new ArrayList<String>();
        SQLiteDatabase db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);
        try {
            String query = "SELECT * FROM " + MySQLiteHelper.Tagged_TABLE + " WHERE " + MySQLiteHelper.Tags_Name + " = '" + tagName + "';";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                do {
                    //File file=new File();
                    //imageUrls.add(Uri.fromFile(file).toString());
                    imageUrls.add(cursor.getString(cursor.getColumnIndex(MySQLiteHelper.Tagged_uri)));
                } while (cursor.moveToNext());
            }
        }
        catch (Exception e){}
        finally {
            db.close();
        }
		return imageUrls;
	}

	private void initializeRecyclerView(ArrayList<String> imageUrls) {
        imageAdapter = new ImageAdapter(TagImageGrid.this, this, imageUrls);
        if (recyclerView==null) {
            RecyclerView.LayoutManager layoutManager;
            if(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE)
                layoutManager = new GridLayoutManager(getApplicationContext(), 4);
            else
                layoutManager = new GridLayoutManager(getApplicationContext(), 3);
            recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.item_offset));
        }
        recyclerView.setAdapter(imageAdapter);
	}

    public void loadImagefromGallery(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), RESULT_LOAD_IMG);
    }
    @TargetApi(16)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SQLiteDatabase db = null;
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data
                final Intent intent = getIntent();
                String[] imageUri =  data.getStringArrayExtra("all_path");
                ArrayList<Uri> imageUris=new ArrayList<Uri>();
                Uri[] photoUris = new Uri[0];
                if (data.getData() != null && data.getClipData()==null) {
                   photoUris = new Uri[1];
                    photoUris[0] = data.getData();

                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        photoUris = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            photoUris[i] = clipData.getItemAt(i).getUri();
                        }
                    }
                }
                int flag=0;
                for (int i = 0; i < photoUris.length; i++) {
                  //  uris[i] = clipData.getItemAt(i).getUri();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query( photoUris[i],
                            filePathColumn, null, null, null);

                    // Move to first row
                    cursor.moveToFirst();
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String uri = cursor.getString(column_index);
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();

                    db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);

                    String query = "select * from " + MySQLiteHelper.Tagged_TABLE + " where uri='" + uri + "'and tags_name='"+ tagName+"';";
                    cursor = db.rawQuery(query, null);

                    if (cursor.getCount() > 0) {
                        flag=1;
                    } else {
                        query = "insert into " + MySQLiteHelper.Tagged_TABLE + "(" + MySQLiteHelper.Tags_Name + "," + MySQLiteHelper.Tagged_uri +
                                ") values('" + tagName + "','" + imgDecodableString + "');";
                        db.execSQL(query);

                        populateImages();
                    }
                }
                if(flag!=0)
                    Toast.makeText(this, "One or more Images were tagged already",
                            Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Image(s) added",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "You haven't picked an Image",
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT)
                    .show();
        } finally {
            if (db != null)
                db.close();
        }

    }


    public static String retTagName()
    {
        return tagName;
    }

    public void onResume()
    {
        super.onResume();
        populateImages();
        imageAdapter.setCheckeditemsSparseArray(
                getIntent().getExtras().getStringArrayList("checked items")
        );
    }

    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        getIntent().putExtra("checked items",imageAdapter.getCheckedItems());
    }
}