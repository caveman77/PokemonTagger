package net.homeip.cave_man.pokemontagger.view;

import android.Manifest;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.homeip.cave_man.pokemontagger.MainActivity;
import net.homeip.cave_man.pokemontagger.R;
import net.homeip.cave_man.pokemontagger.utils.DataBaseHandler;
import net.homeip.cave_man.pokemontagger.utils.FileUtils;
import net.homeip.cave_man.pokemontagger.utils.PokemonCard;
import net.homeip.cave_man.pokemontagger.utils.SqliteExporter;

import java.io.IOException;

public class CameraFragment extends Fragment {
    private static final int CAMERA_REQUEST = 1888;
    private static final int PICTURE_RESULT_STORE = 1887;
    private static final int PICTURE_RESULT_TRANSLATE = 1888;
    private static final int PICTURE_RESULT_TRANSLATE_AND_STORE = 1889;

    TextView textStore,textTranslateAndStore, textExportDb, textTranslate;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int MY_CAMERA_PERMISSION_CODE_TRANSLATE = 101;
    private static final int MY_CAMERA_PERMISSION_CODE_TRANSLATE_AND_STORE = 102;

    //Bitmap photo;
    String photo;


    Bitmap theImage;

    //long CurrentPhotoID = 0;
    private PokemonCard card;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
       View view = inflater.inflate(R.layout.camera_fragment,container,false);


       // imageView =view. findViewById(R.id.imageView1);
        textStore = view.findViewById(R.id.textStore);
        textTranslateAndStore = view.findViewById(R.id.textTranslateAndStore);
        textExportDb = view.findViewById(R.id.textExportDB);
        textTranslate = view.findViewById(R.id.textTranslate);

        card = new PokemonCard(getContext(), this.getActivity().getAssets());


        textStore.setOnClickListener(
                new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v)
            {
                if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                        (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                      sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_STORE);

                }
            }
        });

        textTranslate.setOnClickListener(
                new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v)
                    {
                        if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                        {
                            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_CAMERA_PERMISSION_CODE_TRANSLATE_AND_STORE);
                        }
                        else
                        {
                            sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_TRANSLATE);

                        }
                    }
                });

        textTranslateAndStore.setOnClickListener(
                new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v)
                    {
                        if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                        {
                            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_CAMERA_PERMISSION_CODE_TRANSLATE);
                        }
                        else
                        {
                            sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_TRANSLATE_AND_STORE);

                        }
                    }
                });


        textExportDb.setOnClickListener(
                new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v)
                    {

                        DataBaseHandler databaseHandler = new DataBaseHandler(getContext());
                        SQLiteDatabase db = databaseHandler.getWritableDatabase();
                        SqliteExporter exporter = new SqliteExporter();
                        try
                        {
                            exporter.export(db, getContext());
                            Toast.makeText(getContext(), R.string.ExportDone, Toast.LENGTH_SHORT).show();
                        }
                        catch (IOException e)
                        {
                            Toast.makeText(getContext(), R.string.ExportFailed, Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                });
       return view;
    }





    private void sendIntentToCapture(Uri uri, int intentResultID)
    {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, intentResultID );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
             if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getActivity(), R.string.PermissionReceived, Toast.LENGTH_LONG).show();
                //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(cameraIntent, CAMERA_REQUEST);

                if (FileUtils.getReadyForStorage(getContext()))
                {
                    sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_STORE);
                }
            }
            else
            {
                Toast.makeText(getActivity(), R.string.PermissionDenied, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == MY_CAMERA_PERMISSION_CODE_TRANSLATE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getActivity(), R.string.PermissionReceived, Toast.LENGTH_LONG).show();
                //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(cameraIntent, CAMERA_REQUEST);

                if (FileUtils.getReadyForStorage(getContext()))
                {
                    sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_TRANSLATE);
                }
            }
            else
            {
                Toast.makeText(getActivity(), R.string.PermissionDenied, Toast.LENGTH_LONG).show();
            }
        }


        if (requestCode == MY_CAMERA_PERMISSION_CODE_TRANSLATE_AND_STORE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getActivity(), R.string.PermissionReceived, Toast.LENGTH_LONG).show();
                //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(cameraIntent, CAMERA_REQUEST);

                if (FileUtils.getReadyForStorage(getContext()))
                {
                    sendIntentToCapture(card.getOriginalPictureUri(), PICTURE_RESULT_TRANSLATE_AND_STORE);
                }
            }
            else
            {
                Toast.makeText(getActivity(), R.string.PermissionDenied, Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * Start an activity for result
     * @param requestCode
     * @param resultCode
     * @param data
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICTURE_RESULT_STORE && resultCode == Activity.RESULT_OK)
        {
            Bundle bundle = new Bundle();
            bundle.putLong("ID", card.getDbID() );
            TableFragment fragInfo = new TableFragment();
            fragInfo.setArguments(bundle);

          ((MainActivity) getActivity()).loadFragment(fragInfo, true);

        }

        if (requestCode == PICTURE_RESULT_TRANSLATE && resultCode == Activity.RESULT_OK)
        {

            Bundle bundle = new Bundle();
            bundle.putLong("ID", card.getDbID() );
            bundle.putBoolean("STORE", false );
            TranslateFragment fragInfo = new TranslateFragment();
            fragInfo.setArguments(bundle);

            ((MainActivity) getActivity()).loadFragment(fragInfo, true);

        }

        if (requestCode == PICTURE_RESULT_TRANSLATE_AND_STORE && resultCode == Activity.RESULT_OK)
        {


            Bundle bundle = new Bundle();
            bundle.putLong("ID", card.getDbID() );
            bundle.putBoolean("STORE", true );
            TranslateFragment fragInfo = new TranslateFragment();
            fragInfo.setArguments(bundle);

            ((MainActivity) getActivity()).loadFragment(fragInfo, true);

        }


    }


}
