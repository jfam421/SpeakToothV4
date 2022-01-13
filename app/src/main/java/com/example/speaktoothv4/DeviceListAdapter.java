package com.example.speaktoothv4;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<DeviceItem> {
    private Context mContext;
    int mResource;

    public DeviceListAdapter(@NonNull Context context, int resource, ArrayList<DeviceItem> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String name = getItem(position).getName();
        int color = getItem(position).getColor();

        LayoutInflater layoutInflater = ((Activity)mContext).getLayoutInflater();
        convertView = layoutInflater.inflate(mResource,parent, false);

        TextView icon_letter_list = convertView.findViewById(R.id.icon_letter_list);
        TextView title = convertView.findViewById(R.id.title_list);
        View square = convertView.findViewById(R.id.left_icon_main);

        title.setText(name);
        icon_letter_list.setText(name);
        square.setBackgroundColor(color);

        return convertView;
    }
}
