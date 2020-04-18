package com.example.alumnguide.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alumnguide.ChatActivity;
import com.example.alumnguide.R;
import com.example.alumnguide.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.MyHolder> {

    Context context;
    List<ModelUser> userList;

    public AdapterUser(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_users,viewGroup,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int i) {
        final String hisUID = userList.get(i).getId();
        String image = userList.get(i).getImage();
        String username = userList.get(i).getUsername();
        final String email = userList.get(i).getEmail();
        String courseStudying = userList.get(i).getCourseStudying();
        String currentYear = userList.get(i).getCurrentYear();

        holder.mNameTv.setText(username);
        holder.mEmailTv.setText(email);
        holder.mCurrentYear.setText(currentYear);
        holder.mCourseStudying.setText(courseStudying);

        try{
            Picasso.get().load(image).placeholder(R.drawable.ic_default_blue).into(holder.mAvatarIv);

        }catch (Exception e){

        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUid",hisUID);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{
        ImageView mAvatarIv;
        TextView mNameTv,mEmailTv,mCurrentYear,mCourseStudying;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            mAvatarIv = itemView.findViewById(R.id.avatarIv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
            mCurrentYear = itemView.findViewById(R.id.currentYearTv);
            mCourseStudying = itemView.findViewById(R.id.courseStudyingTv);

        }
    }
}
