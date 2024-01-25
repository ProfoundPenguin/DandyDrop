package com.example.dandydrop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FileDataAdapter extends RecyclerView.Adapter<FileDataAdapter.ViewHolder> {
    private final RecyclerViewInterface recyclerViewInterface;
    private List<FileData> fileDataList;
    private LayoutInflater inflater;

    public FileDataAdapter(Context context, List<FileData> fileDataList, RecyclerViewInterface recyclerViewInterface) {
        this.inflater = LayoutInflater.from(context);
        this.fileDataList = fileDataList;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_file_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileData fileData = fileDataList.get(position);
        holder.textViewFileName.setText(fileData.getName());
        holder.size.setText(fileData.getReadableFilesize());
        // Bind other data as needed
    }

    @Override
    public int getItemCount() {
        return fileDataList.size();
    }

    public void addFileData(FileData newData) {
        fileDataList.add(newData);
        notifyItemInserted(fileDataList.size() - 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFileName;
        TextView size;
        ImageButton deleteBtn;

        ImageView check_box;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFileName = itemView.findViewById(R.id.textFileName);
            size = itemView.findViewById(R.id.filesize);

            deleteBtn = itemView.findViewById(R.id.deleteBtn);

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (recyclerViewInterface != null) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            recyclerViewInterface.onFileDelete(pos);
                        }
                    }
                }
            });

            check_box = itemView.findViewById(R.id.imageViewCheckbox);

            check_box.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (recyclerViewInterface != null) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            recyclerViewInterface.onFileSelect(pos, check_box);
                        }
                    }
                }
            });
        }
    }
}
