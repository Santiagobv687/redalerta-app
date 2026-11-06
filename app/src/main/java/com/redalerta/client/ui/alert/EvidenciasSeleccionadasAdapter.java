package com.redalerta.client.ui.alert;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ItemEvidenciaSeleccionadaBinding;

import java.util.List;

public class EvidenciasSeleccionadasAdapter extends RecyclerView.Adapter<EvidenciasSeleccionadasAdapter.EvidenciaViewHolder> {

    public interface OnEliminarEvidenciaListener {
        void onEliminar(int position);
    }

    public static class EvidenciaItem {
        private Uri uriLocal;
        private String urlSubida;
        private boolean subiendo;

        public EvidenciaItem(Uri uriLocal) {
            this.uriLocal = uriLocal;
            this.subiendo = false;
        }

        public Uri getUriLocal() { return uriLocal; }
        public void setUriLocal(Uri uriLocal) { this.uriLocal = uriLocal; }

        public String getUrlSubida() { return urlSubida; }
        public void setUrlSubida(String urlSubida) { this.urlSubida = urlSubida; }

        public boolean isSubiendo() { return subiendo; }
        public void setSubiendo(boolean subiendo) { this.subiendo = subiendo; }
    }

    private final List<EvidenciaItem> lista;
    private final OnEliminarEvidenciaListener eliminarListener;

    public EvidenciasSeleccionadasAdapter(List<EvidenciaItem> lista, OnEliminarEvidenciaListener eliminarListener) {
        this.lista = lista;
        this.eliminarListener = eliminarListener;
    }

    @NonNull
    @Override
    public EvidenciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEvidenciaSeleccionadaBinding binding = ItemEvidenciaSeleccionadaBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new EvidenciaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EvidenciaViewHolder holder, int position) {
        EvidenciaItem item = lista.get(position);
        holder.bind(item, position, eliminarListener);
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class EvidenciaViewHolder extends RecyclerView.ViewHolder {
        private final ItemEvidenciaSeleccionadaBinding binding;

        public EvidenciaViewHolder(@NonNull ItemEvidenciaSeleccionadaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(EvidenciaItem item, int position, OnEliminarEvidenciaListener listener) {
            Object fuente = item.getUrlSubida() != null ? item.getUrlSubida() : item.getUriLocal();

            Glide.with(itemView.getContext())
                    .load(fuente)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo_library)
                    .error(R.drawable.ic_photo_library)
                    .into(binding.imgThumbnail);

            binding.pbSubiendo.setVisibility(item.isSubiendo() ? View.VISIBLE : View.GONE);

            binding.btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminar(position);
                }
            });
        }
    }
}
