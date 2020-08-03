package com.example.cs478_proj_4;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

public class GridAdapter extends BaseAdapter {

    Context context;
    private final ArrayList<Integer> images;
    View view;
    LayoutInflater layoutInflater;
    private int selectedPosition = -1;
    private int selectedGopherPos = -1;
    private int red;
    private int green;
    private int blue;

    public GridAdapter(Context context, ArrayList<Integer> images){
        this.context = context;
        this.images = images;
    }


    @Override
    public int getCount(){
        return images.size();
    }

    @Override
    public Object getItem(int position){
        return position;
    }

    @Override
    public long getItemId(int position){
        return 0;
    }

    public void setSelectedPosition(int position, int red, int green, int blue){
        selectedPosition = position;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public void setGopherPosition(int position){
        selectedGopherPos = position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        layoutInflater = ((Activity) context).getLayoutInflater();

        if(convertView == null){
            view = new View(context);
            view = layoutInflater.inflate(R.layout.single_item, null);
        } else {
            view = (View) convertView;
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        ImageView gopherImage = (ImageView) view.findViewById(R.id.gopher);
        imageView.setImageResource(images.get(position));

        if(position == selectedPosition) {
            view.setBackgroundColor(Color.rgb(red, green ,blue));
        }
//        else {
//            view.setBackgroundColor(Color.rgb(255, 255, 255));
//        }

        if(position == selectedGopherPos){
            gopherImage.setVisibility(View.VISIBLE);
        }

        return view;
    }



}
