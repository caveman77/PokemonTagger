package net.homeip.cave_man.pokemontagger.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.homeip.cave_man.pokemontagger.R;

public class PokemonFamilySpinner extends ArrayAdapter<PokemonFamily>
{

    private PokemonFamily[] mFamilies;
    private LayoutInflater layoutInflater;

    public PokemonFamilySpinner(Context context, int ItemLayoutResourceId,
                                PokemonFamily[] objects)
    {
        super(context, ItemLayoutResourceId, objects);
        mFamilies = objects;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent)
    {

        View row= layoutInflater.inflate(R.layout.spinner_item, null);

        TextView label=(TextView) row.findViewById(R.id.name_family);
        label.setText(mFamilies[position].getName());

        ImageView icon=(ImageView)row.findViewById(R.id.icon_family);

        String uri = "@drawable/" + mFamilies[position].getIcon();
        int imageResource = getContext().getResources().getIdentifier(uri, null, getContext().getPackageName());
        icon.setImageResource(imageResource);

        return row;
    }
}