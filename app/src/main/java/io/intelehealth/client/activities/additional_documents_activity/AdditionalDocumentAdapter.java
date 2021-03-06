package io.intelehealth.client.activities.additional_documents_activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.intelehealth.client.R;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;

/**
 * Created by Dexter Barretto on 6/28/17.
 * Github : @dbarretto
 */

public class AdditionalDocumentAdapter extends RecyclerView.Adapter<AdditionalDocumentViewHolder> {

    int screen_height;
    int screen_width;

    private List<DocumentObject> documentList = new ArrayList<>();
    private Context context;
    private String filePath;

    private static final String TAG = AdditionalDocumentAdapter.class.getSimpleName();

    public AdditionalDocumentAdapter(Context context, List<DocumentObject> documentList,String filePath) {
        this.documentList = documentList;
        this.context = context;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screen_height = displayMetrics.heightPixels;
        screen_width = displayMetrics.widthPixels;
        this.filePath = filePath;

    }

    @Override
    public AdditionalDocumentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_additional_doc, null);
        AdditionalDocumentViewHolder rcv = new AdditionalDocumentViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(final AdditionalDocumentViewHolder holder, final int position) {

        holder.getDocumentNameTextView().setText(documentList.get(position).getDocumentName());

        final File image = new File(documentList.get(position).getDocumentPhoto());

        Glide.with(context)
                .load(image)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .thumbnail(0.1f)
                .into(holder.getDocumentPhotoImageView());

        holder.getRootView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayImage(image);
            }
        });

        holder.getDeleteDocumentImageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(image.exists()) image.delete();
                documentList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position,documentList.size());
                String imageName = holder.getDocumentNameTextView().getText().toString();
                String dir = filePath + File.separator + imageName;
                deleteImageFromDatabase(dir);
            }
        });
    }

    private void deleteImageFromDatabase(String imagePath) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(context);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        localdb.execSQL("DELETE FROM image_records WHERE image_path=" + "'" + imagePath + "'");
        localdb.close();
    }


    public void add(DocumentObject doc){
        boolean bool = documentList.add(doc);
        if (bool) Log.d(TAG, "add: Item added to list");
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.documentList.size();
    }


    public void displayImage(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        

        final AlertDialog dialog = builder.create();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogLayout = inflater.inflate(R.layout.image_confirmation_dialog, null);
        dialog.setView(dialogLayout);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                ImageView imageView = (ImageView) dialog.findViewById(R.id.confirmationImageView);
                final ProgressBar progressBar = (ProgressBar) dialog.findViewById(R.id.progressBar);
                Glide.with(context)
                        .load(file)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .listener(new RequestListener<File, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, File file, Target<GlideDrawable> target, boolean b) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable glideDrawable, File file, Target<GlideDrawable> target, boolean b, boolean b1) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .override(screen_width,screen_height)
                        .into(imageView);
            }
        });

        dialog.show();

    }
}
