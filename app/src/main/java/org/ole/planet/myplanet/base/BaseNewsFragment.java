package org.ole.planet.myplanet.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnHomeItemClickListener;
import org.ole.planet.myplanet.model.RealmNews;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.news.AdapterNews;
import org.ole.planet.myplanet.ui.news.ReplyActivity;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.JsonUtils;

import java.io.File;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

@RequiresApi(api = Build.VERSION_CODES.O)
public abstract class BaseNewsFragment extends BaseContainerFragment implements AdapterNews.OnNewsItemClickListener {
    public Realm mRealm;
    public OnHomeItemClickListener homeItemClickListener;
    public UserProfileDbHandler profileDbHandler;
    protected RealmList<String> imageList;
    protected LinearLayout llImage;
    protected AdapterNews adapterNews;
    protected ActivityResultLauncher<Intent> openFolderLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageList = new RealmList<>();
        profileDbHandler = new UserProfileDbHandler(getActivity());
        openFolderLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                Uri url;
                String path;
                url = data.getData();
                path = FileUtils.getRealPathFromURI(requireActivity(), url);
                if (TextUtils.isEmpty(path)) {
                    path = getImagePath(url);
                }
                JsonObject object = new JsonObject();
                object.addProperty("imageUrl", path);
                object.addProperty("fileName", FileUtils.getFileNameFromUrl(path));
                imageList.add(new Gson().toJson(object));
                try {
                    llImage.removeAllViews();
                    llImage.setVisibility(View.VISIBLE);
                    for (String img : imageList) {
                        JsonObject ob = new Gson().fromJson(img, JsonObject.class);
                        View inflater = LayoutInflater.from(getActivity()).inflate(R.layout.image_thumb, null);
                        ImageView imgView = inflater.findViewById(R.id.thumb);
                        Glide.with(requireActivity()).load(new File(JsonUtils.getString("imageUrl", ob))).into(imgView);
                        llImage.addView(inflater);
                    }
                    if (result.getResultCode() == 102)
                        adapterNews.setImageList(imageList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeItemClickListener)
            homeItemClickListener = (OnHomeItemClickListener) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (profileDbHandler != null) profileDbHandler.onDestory();
    }

    @Override
    public void showReply(RealmNews news, boolean fromLogin) {
        startActivity(new Intent(getActivity(), ReplyActivity.class).putExtra("id", news.id).putExtra("fromLogin", fromLogin));
    }

    public abstract void setData(List<RealmNews> list);

    public void showNoData(View v, int count) {
        BaseRecyclerFragment.showNoData(v, count);
    }

    public String getImagePath(Uri uri) {
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContext().getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    public void changeLayoutManager(int orientation, RecyclerView recyclerView) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
    }

    @Override
    public void addImage(LinearLayout llImage) {
        this.llImage = llImage;
        Intent openFolderIntent = FileUtils.openOleFolder();
        openFolderLauncher.launch(openFolderIntent);
    }
}