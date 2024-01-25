package com.example.dandydrop;

import android.widget.ImageView;

public interface RecyclerViewInterface {
    void onFileDelete(int position);
    void onFileSelect(int position, ImageView check_box);
}
