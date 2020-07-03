package com.example.androidtest;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;


public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.MyViewHolder> {
    /**
     * Custom recyclerview adapter
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView location_text;

        public MyViewHolder(View view) {
            super(view);
            location_text = (TextView) view.findViewById(R.id.textView);
        }
    }
    Context c;
    ArrayList<Locations> locations;


    public LocationsAdapter(Context c, ArrayList<Locations> locations) {
        this.c = c;
        this.locations = locations;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(c).inflate(R.layout.item_detail,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.location_text.setText(locations.get(position).toString());


    }

    @Override
    public int getItemCount() {
        return locations.size();
    }
}
