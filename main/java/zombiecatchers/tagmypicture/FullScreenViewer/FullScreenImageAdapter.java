package zombiecatchers.tagmypicture.FullScreenViewer;

import zombiecatchers.tagmypicture.R;
import zombiecatchers.tagmypicture.FullScreenViewer.helper.TouchImageView;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FullScreenImageAdapter extends PagerAdapter {
    private Activity _activity;
    private ArrayList<String> _imagePaths;
    private LayoutInflater inflater;
    int DeviceWidth;
    int DeviceHeight;

    // constructor
    public FullScreenImageAdapter(Activity activity,
                                  ArrayList<String> imagePaths,int DeviceWidth,int DeviceHeight) {
        this._activity = activity;
        this._imagePaths = imagePaths;
        this.DeviceWidth=DeviceWidth;
        this.DeviceHeight=DeviceHeight;
    }

    @Override
    public int getCount() {
        return this._imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    public float angle=0;
    public void rotatePicture(ViewGroup container, int position){
        angle=30;

    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final TouchImageView imgDisplay;
        Button btnClose;

        inflater = (LayoutInflater) _activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false);

        imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(_imagePaths.get(position), options);

        int inSampleSize = 1;

        if (options.outHeight > DeviceHeight || options.outWidth > DeviceWidth) {

            final int halfHeight = options.outHeight;
            final int halfWidth = options.outWidth;

            while ((halfHeight / inSampleSize) >= DeviceHeight
                    && (halfWidth / inSampleSize) >= DeviceWidth) {
                inSampleSize *= 2;
            }
        }
        options.inSampleSize=inSampleSize;
        options.inJustDecodeBounds = false;

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                String path = params[0];
                final Bitmap resizedBitmap;

                resizedBitmap = BitmapFactory.decodeFile(_imagePaths.get(position), options);
                _activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgDisplay.setImageBitmap(resizedBitmap);
                    }
                });
                return null;
            }
        }.execute(_imagePaths.get(position));

        ((ViewPager) container).addView(viewLayout);
        ((ViewPager)container).setVisibility(View.VISIBLE);
        return viewLayout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);

    }
}




