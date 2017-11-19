package zombiecatchers.tagmypicture;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import java.util.List;

public class CustomListAdapter extends ArrayAdapter<String>{

    private final Activity context;
    private final List<String> tags;
    private final List<String> coverPics;
    private final List<Integer> CountOfPicsList;
    private SparseBooleanArray mSelectedItemsIds;

    public CustomListAdapter(Activity context,
                             List<String> tags, List<String> coverPics,List<Integer> CountOfPicsList) {
        super(context, R.layout.tag_list_item, tags);
        this.context = context;
        this.tags = tags;
        mSelectedItemsIds = new SparseBooleanArray();
        this.coverPics = coverPics;
        this.CountOfPicsList=CountOfPicsList;
    }
    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }
    public void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }
    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }
    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.tag_list_item, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.tag);
        TextView count=(TextView) rowView.findViewById(R.id.CountOfPics);
        count.setText(CountOfPicsList.get(position).toString());

        final ImageView imageView = (ImageView) rowView.findViewById(R.id.coverPic);
        txtTitle.setText(tags.get(position));
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(context).load(coverPics.get(position)).asBitmap().override(100,100).centerCrop()
                                .error(R.drawable.ic_launcher)
                                .into(new BitmapImageViewTarget(imageView) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                                circularBitmapDrawable.setCircular(true);
                                imageView.setImageDrawable(circularBitmapDrawable);
                            }
                        });
                    }
                });
                return null;
            }
        }.execute();
        if(mSelectedItemsIds.get(position))
        {
            TextView CountOfPics = (TextView) rowView.findViewById(R.id.CountOfPics);
            rowView.setBackgroundColor(Color.parseColor("#90ef9a9a"));

        }
        return rowView;
    }
}