package zombiecatchers.tagmypicture.ImageGrid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.sip.SipAudioCall;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import zombiecatchers.tagmypicture.R;
import zombiecatchers.tagmypicture.FullScreenViewer.FullScreenViewActivity;

import static android.R.attr.path;
import static android.content.Context.MODE_PRIVATE;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> {

    private ArrayList<String> mImagesList;
    private Context mContext;
    private SparseBooleanArray mSparseBooleanArray;
    private Activity mActivity;

    public ImageAdapter(Activity activity, Context context, ArrayList<String> imageList) {
        this.mActivity=activity;
        this.mContext = context;
        this.mSparseBooleanArray = new SparseBooleanArray();
        this.mImagesList = new ArrayList<String>();
        this.mImagesList = imageList;

    }

    public ArrayList<String> getCheckedItems() {
        ArrayList<String> mTempArray = new ArrayList<String>();

        for(int i=0;i<mImagesList.size();i++) {
            if(mSparseBooleanArray.get(i)) {
                mTempArray.add(mImagesList.get(i));
            }
        }

        return mTempArray;
    }
/*
    public ArrayList<Integer> getCheckedItemsIntArray(){
        ArrayList<Integer> CheckedItems=new ArrayList<>();
        for(int i=0;i<mImagesList.size();i++){

        }
        return CheckedItems;
    }
    */

    public void setCheckeditemsSparseArray(ArrayList<String> newSparseArray){
        if(newSparseArray==null) return;
        for(int i=0;i<newSparseArray.size();i++) {
            for(int j=0; j<mImagesList.size();j++){
                String s1=newSparseArray.get(i);
                String s2=mImagesList.get(j);
                if(s1.equals(s2)){
                    mSparseBooleanArray.put(j,true);
                    break;
                }
            }
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSparseBooleanArray.put((Integer) buttonView.getTag(), isChecked);
            toggleDeleteVisibility();
        }
    };

    public void toggleDeleteVisibility()
    {
        if(getCheckedItems().size()>0)
        {
            ((TagImageGrid)mContext).toggleDeleteVisibility(true);
        }
        else {
            ((TagImageGrid)mContext).toggleDeleteVisibility(false);
        }
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_multiphoto_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        String imageUrl = mImagesList.get(position);
        Glide.with(mContext)
                .load(Uri.fromFile(new File(imageUrl)))
                .override(400,400)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher)
                .error(R.drawable.ic_launcher)
                .into(holder.imageView);

        holder.checkBox.setTag(position);
        holder.checkBox.setChecked(mSparseBooleanArray.get(position));
        holder.checkBox.setOnCheckedChangeListener(mCheckedChangeListener);

        holder.imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // TODO Auto-generated method stub
                SharedPreferences indexPrefs = mContext.getSharedPreferences("currentIndex",
                        MODE_PRIVATE);

                SharedPreferences.Editor indexEditor = indexPrefs.edit();
                indexEditor.putInt("currentIndex", position);
                indexEditor.commit();
                final Intent intent = new Intent(mContext, FullScreenViewActivity.class);
                String tagName = TagImageGrid.retTagName();
                intent.putExtra("tagName", tagName);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mImagesList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public CheckBox checkBox;
        public ImageView imageView;

        public MyViewHolder(View view) {
            super(view);

            checkBox = (CheckBox) view.findViewById(R.id.checkBox1);
            imageView = (ImageView) view.findViewById(R.id.imageView1);
        }
    }

}
