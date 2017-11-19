package zombiecatchers.tagmypicture.FullScreenViewer;

import zombiecatchers.tagmypicture.TagGallery;
import zombiecatchers.tagmypicture.MySQLiteHelper;
import zombiecatchers.tagmypicture.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class FullScreenViewActivity extends Activity{
	private FullScreenImageAdapter adapter;
	private ViewPager viewPager;
	String tagName;
	int position;
    SQLiteDatabase db;
    FloatingActionButton ShareButton;
    FloatingActionButton DeleteButton;
    FloatingActionButton UseAsButton;
    FloatingActionButton RotateButton;
    ArrayList<String> ImageList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		savedInstanceState = getIntent().getExtras();
		tagName = savedInstanceState.getString("tagName");
		viewPager = (ViewPager) findViewById(R.id.pager);



		SharedPreferences indexPrefs = getSharedPreferences("currentIndex",
				MODE_PRIVATE);
		if (indexPrefs.contains("currentIndex")) {
			position = indexPrefs.getInt("currentIndex", 0);
		}

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
        ImageList=FindFiles();
		adapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
				ImageList,width,height);

		viewPager.setAdapter(adapter);
        viewPager.setBackgroundColor(Color.rgb(0,0,0));
        viewPager.setOffscreenPageLimit(0);
        viewPager.setCurrentItem(position);

        ShareButton = (FloatingActionButton) findViewById(R.id.ShareButton);
        ShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                position=viewPager.getCurrentItem();
                ShareMe(getBaseContext(), ImageList.get(position));
            }
        });

        DeleteButton = (FloatingActionButton) findViewById(R.id.DeleteButton);
        DeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(FullScreenViewActivity.this);
                alert.setTitle("Remove Tag"); //Set Alert dialog title here
                alert.setMessage("Are you sure you want to untag this picture? ");
                alert.setPositiveButton("Delete Tag", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        position=viewPager.getCurrentItem();
                        DeleteMe(getBaseContext(), ImageList.get(position));
                    } // End of onClick(DialogInterface dialog, int whichButton)
                }); //End of alert.setPositiveButton
                alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }); //End of alert.setNegativeButton
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
            }
        });

        UseAsButton = (FloatingActionButton) findViewById(R.id.UseAsButton);
        UseAsButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                position=viewPager.getCurrentItem();
                UseMeAs(getBaseContext(),ImageList.get(position));
            }
        });

        RotateButton =(FloatingActionButton) findViewById(R.id.RotateButton);
        RotateButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                position=viewPager.getCurrentItem();
                adapter.rotatePicture(viewPager,position);
            }
        });
	}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        toggleFabVisibility();
        return true;
    }

    static int MotionDown=0;
    static float MotionDownX;
    static float MotionDownY;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getAction()==MotionEvent.ACTION_DOWN) {
            MotionDown=1;
            MotionDownX=ev.getX();
            MotionDownY=ev.getY();
        }
        else if(ev.getAction()==MotionEvent.ACTION_UP){
            if(MotionDown==1) toggleFabVisibility();
        }
        else{
            if(MotionDownX<=ev.getX()-10 || MotionDownX>=ev.getX()+10 ||
                MotionDownY<=ev.getY()-10 || MotionDownY>=ev.getY()+10 )
                MotionDown=0;
        }
        return super.dispatchTouchEvent(ev);
    }
    private ArrayList<String> FindFiles() {
		final ArrayList<String> tFileList = new ArrayList<String>();
        SQLiteDatabase db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);
        try {
            String query = "SELECT * FROM " + MySQLiteHelper.Tagged_TABLE + " WHERE " + MySQLiteHelper.Tags_Name + " = '" + tagName + "';";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                do {
                    tFileList.add(cursor.getString(cursor.getColumnIndex(MySQLiteHelper.Tagged_uri)));
                } while (cursor.moveToNext());
            }
        }
		catch (Exception e){}
		finally {
			db.close();
		}
		return tFileList;
	}
    public void toggleFabVisibility(){
        if(ShareButton.getVisibility()==View.INVISIBLE)
        {
            ShareButton.setVisibility(View.VISIBLE);
            UseAsButton.setVisibility(View.VISIBLE);
            DeleteButton.setVisibility(View.VISIBLE);

        }else {
            ShareButton.setVisibility(View.INVISIBLE);
            UseAsButton.setVisibility(View.INVISIBLE);
            DeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    public void ShareMe(Context context, String pathToImage) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        else
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        shareIntent.setType("image/*");

        // For a file in shared storage.  For data in private storage, use a ContentProvider.
        File file = new File(pathToImage);
        Uri uri = Uri.fromFile(file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "Share image using"));

    }

    public void UseMeAs(Context context, String pathToImage) {
        Intent shareIntent = new Intent(Intent.ACTION_ATTACH_DATA);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        else
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        shareIntent.setType("image/*");

        // For a file in shared storage.  For data in private storage, use a ContentProvider.
        File file = new File(pathToImage);
        Uri uri = Uri.fromFile(file);
        shareIntent.addCategory(Intent.CATEGORY_DEFAULT);
        shareIntent.setDataAndType(uri, "image/jpeg");
        shareIntent.putExtra("mimeType", "image/jpeg");
        this.startActivity(Intent.createChooser(shareIntent, "Set as:"));
    }

    public void DeleteMe(Context context, String pathToImage)
    {
        SQLiteDatabase db = openOrCreateDatabase(MySQLiteHelper.DATABASE_NAME, TagGallery.MODE_PRIVATE, null);
        try {
            String query = "Delete from "+MySQLiteHelper.Tagged_TABLE+" where "+MySQLiteHelper.Tags_Name+"='" + tagName + "'and "+MySQLiteHelper.Tagged_uri+"= '" + pathToImage + "';";
            db.execSQL(query);
            Toast.makeText(getApplicationContext(), "Tag Deleted Successfully", Toast.LENGTH_LONG).show();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        finally {
            db.close();
        }
        finish();
    }
}



