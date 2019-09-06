package org.ole.planet.myplanet.ui.team.teamMember;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.RealmMyTeam;
import org.ole.planet.myplanet.model.RealmTeamLog;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.userprofile.AdapterOtherInfo;
import org.ole.planet.myplanet.utilities.Utilities;

import java.lang.reflect.Array;
import java.util.List;

import io.realm.Realm;

public class AdapterJoinedMemeber extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<RealmUserModel> list;
    private Realm mRealm;
    private String teamId;
    private RealmUserModel currentUser;
    private String teamLeaderId;
    public AdapterJoinedMemeber(Context context, List<RealmUserModel> list, Realm mRealm, String teamId) {
        this.context = context;
        this.list = list;
        this.mRealm = mRealm;
        this.teamId = teamId;
        this.currentUser = new UserProfileDbHandler(context).getUserModel();
        RealmMyTeam leaderTeam = mRealm.where(RealmMyTeam.class).equalTo("isLeader", true).findFirst();
        if (leaderTeam!=null){
            this.teamLeaderId = leaderTeam.getUserId();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_joined_user, parent, false);
        return new ViewHolderUser(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderUser) {
            ((ViewHolderUser) holder).tvTitle.setText(list.get(position).getName());
            ((ViewHolderUser) holder).tvDescription.setText(list.get(position).getRoleAsString() + " (" + RealmTeamLog.getVisitCount(mRealm, list.get(position).getName()) + " visits )");

            if (this.teamLeaderId!=null && this.teamLeaderId.equals(this.currentUser.getId())){
                ((ViewHolderUser) holder).icMore.setVisibility(View.VISIBLE);
            }else{
                ((ViewHolderUser) holder).icMore.setVisibility(View.GONE);
            }

            ((ViewHolderUser) holder).icMore.setOnClickListener(view -> {
                String[] s = {"Remove", "Make Leader"};
                new AlertDialog.Builder(context)
                        .setItems(s, (dialogInterface, i) -> {
                            if (i == 0) {
                                reject(list.get(position), position);
                            } else {
                                makeLeader(list.get(position), position);
                            }
                        }).setNegativeButton("Dismiss", null).show();
            });
        }
    }

    private void makeLeader(RealmUserModel userModel, int position) {
        if (!mRealm.isInTransaction())
            mRealm.beginTransaction();
        RealmMyTeam team = mRealm.where(RealmMyTeam.class).equalTo("teamId", teamId).equalTo("userId", userModel.getId()).findFirst();
        RealmMyTeam teamLeader = mRealm.where(RealmMyTeam.class).equalTo("teamId", teamId).equalTo("isLeader", true).findFirst();
        if (team != null) {
            team.setLeader(true);
        }
        if (teamLeader!=null){
            teamLeader.setLeader(false);
        }
        mRealm.commitTransaction();
        notifyDataSetChanged();
        Utilities.toast(context,"Leader selected");
    }

    private void reject(RealmUserModel userModel, int position) {
        if (!mRealm.isInTransaction())
            mRealm.beginTransaction();
        RealmMyTeam team = mRealm.where(RealmMyTeam.class).equalTo("teamId", teamId).equalTo("userId", userModel.getId()).findFirst();
        if (team != null) {
            team.deleteFromRealm();
        }
        mRealm.commitTransaction();
        list.remove(position);
        notifyDataSetChanged();
        Utilities.toast(context,"User removed from team");

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolderUser extends AdapterOtherInfo.ViewHolderOtherInfo {
        ImageView icMore;

        public ViewHolderUser(View itemView) {
            super(itemView);
            icMore = itemView.findViewById(R.id.ic_more);
        }
    }
}
