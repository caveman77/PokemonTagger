package net.homeip.cave_man.pokemontagger.view;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.mlkit.vision.text.TextRecognizer;

import net.homeip.cave_man.pokemontagger.R;
import net.homeip.cave_man.pokemontagger.utils.ChunckStatusKot;
import net.homeip.cave_man.pokemontagger.utils.DataBaseHandler;
import net.homeip.cave_man.pokemontagger.utils.LocalResponse;
import net.homeip.cave_man.pokemontagger.utils.PokemonCard;
import net.homeip.cave_man.pokemontagger.utils.PokemonFamily;
import net.homeip.cave_man.pokemontagger.utils.PokemonFamilySpinner;

import java.util.ArrayList;

import static java.lang.Math.max;

public class TableFragment extends Fragment {
    RecyclerView recyclerView;
    private DataBaseHandler myDatabase;
    private SQLiteDatabase db;
    private ArrayList<LocalResponse> singleRowArrayList;
    private LocalResponse singleRow;
    String image;
    int uid;
    Cursor cursor;

    private static final String TAG = "TableFragment";

    // for text reco:
    /*
    private VisionImageProcessor imageProcessor;
    //private GraphicOverlay graphicOverlay;
    private static final String SIZE_SCREEN = "w:screen"; // Match screen width
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
    private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
    boolean isLandScape;
    private int imageMaxWidth;
    private int imageMaxHeight;
    private String selectedSize = SIZE_SCREEN;
*/
    private TextRecognizer textRecognizer;


    private PokemonCard card;
    private EditText editView;
    private PokemonFamily[] pokefamilylist;
    private RadioGroup radioGroup;
    //private ArrayList<ChunckStatusKot> ChuckStatusList;
    AsyncTask<Void, Integer, String> runningTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //View view = inflater.inflate(R.layout.local_fragment,container,false);
        View view = inflater.inflate(R.layout.table_fragment,null,false);
        editView = (EditText)view.findViewById(R.id.CardNumber);

        //recyclerView = view.findViewById(R.id.recyclerview);
        myDatabase = new DataBaseHandler(getContext());
        db = myDatabase.getWritableDatabase();

        Long id = this.getArguments().getLong("ID");
        card = new PokemonCard(getContext(), this.getActivity().getAssets(), id);
        radioGroup = (RadioGroup) view.findViewById(R.id.radio_group);

        pokefamilylist = PokemonFamily.loadFamiliesFromFile(32, getContext());
        Spinner mySpinner = (Spinner)view.findViewById(R.id.spinner_pokemonfamily);
        mySpinner.setAdapter(new PokemonFamilySpinner(getContext(), R.layout.spinner_item, pokefamilylist));

        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                PokemonFamily currentFamily = pokefamilylist[position];
                card.setFamily(currentFamily);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {
                card.resetFamily();
            }

        });





        ArrayList<Bitmap> imageChunks = card.SplitImage();


        TableLayout table = (TableLayout) view.findViewById(R.id.table);
        int nb_line_column = (int) Math.sqrt(imageChunks.size());

        //ChuckStatusList = new ArrayList<ChunckStatusKot>(imageChunks.size());

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                hideKeyboard(getActivity());

                ChunckStatusKot holder = (ChunckStatusKot) v.getTag();


                int selectedId = radioGroup.getCheckedRadioButtonId();

                if (selectedId == R.id.radio_logo)
                {
                    holder.isLogo = !holder.isLogo;
                }
                else
                {
                    holder.isCardN = !holder.isCardN;
                }

                if (holder.isLogo)
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                else if (holder.isCardN)
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                else
                    v.setBackgroundColor(getResources().getColor(android.R.color.white));

                card.setChunckStatus(holder);
            }
        };

        int position=0;
        //for (int i=0;i<nb_line_column;i++)
            for (int i=0;i<nb_line_column;i++)
        {
            TableRow viewLine = (TableRow) inflater.inflate(R.layout.table_line,null,false);
            //for (int j=0;j<nb_line_column;j++)
                for (int j=0;j<nb_line_column;j++)
            {
                View viewCell = inflater.inflate(R.layout.grid_item,null,false);

                ImageView img = (ImageView) viewCell.findViewById(R.id.imageView_patch);
                img.setImageBitmap(imageChunks.get(position));
                img.setOnClickListener(onClickListener);
                viewLine.addView(viewCell);

                ChunckStatusKot holder = new ChunckStatusKot();
                holder.position = position;

                ImageView family_detected =  (ImageView) viewCell.findViewById(R.id.ml_family);
                holder.FamilyLogo = family_detected;

                img.setTag(holder);
                card.setChunckStatus(holder);
                //ChuckStatusList.add(holder);
/*

                int family = card.getPredictedFamily(position);
                if (family >= 0)
                {
                    String uri = "@drawable/" + pokefamilylist[family].getIcon();
                    int imageResource = getContext().getResources().getIdentifier(uri, null, getContext().getPackageName());
                    family_detected.setImageResource(imageResource);
                }
*/
                position++;
            }
            table.addView(viewLine);
        }

        Button button = (Button) view.findViewById(R.id.save_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {


                if (editView.getText() != null)
                {
                    String CardN = editView.getText().toString();

                    int isDone = card.Save(CardN);

                    switch (isDone)
                    {
                        case 0:
                            Toast.makeText(getActivity(), R.string.Saved, Toast.LENGTH_SHORT).show();
                            break;

                        case 2:
                            Toast.makeText(getActivity(), R.string.SelectFamilyNeeded, Toast.LENGTH_LONG).show();
                            break;

                        case 3:
                            Toast.makeText(getActivity(), R.string.SelectCardNNeeded, Toast.LENGTH_LONG).show();
                            break;

                        default:
                            Toast.makeText(getActivity(), R.string.UnknownError, Toast.LENGTH_LONG).show();
                    }

                }
                else
                {
                    Toast.makeText(getActivity(), R.string.CardNumberEmpty, Toast.LENGTH_LONG).show();
                }

            }
        });

        runningTask = new LongOperation();
        runningTask.execute();

        return view;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }



    private final class LongOperation extends AsyncTask<Void, Integer, String>
    {

        @Override
        protected String doInBackground(Void... params)
        {
            card.DectectCardNumber();

            //card.launchML();

            // family logo are usually below so better start with the last ones
            for (int p=card.getNumberChuncks()-1; p>=0; p--)
            {
                card.DectectLogoFamily(p);
                publishProgress(p);
            }




            return "Executed";
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);

            int p = values[values.length - 1];
            ChunckStatusKot c = card.getChunck(p);
            ImageView family_detected = c.FamilyLogo;

            int family = card.getPredictedFamily(p);
            if (family >= 0)
            {
                int j=0;
                boolean found = false;
                PokemonFamily fam;

                do
                {
                    fam = pokefamilylist[j];

                    if (fam.getIndexML() == family)
                        found=true;

                    j++;
                }
                while ((j < pokefamilylist.length) && (found == false));

                String uri = "@drawable/" + fam.getIcon();
                int imageResource = getContext().getResources().getIdentifier(uri, null, getContext().getPackageName());
                family_detected.setImageResource(imageResource);
            }


        }

        @Override
        protected void onPostExecute(String result)
        {
/*
            for (int p=0;p<ChuckStatusList.size(); p++)
            {
                ChunckStatus c = ChuckStatusList.get(p);
                ImageView family_detected = c.FamilyLogo;

                int family = card.getPredictedFamily(p);
                if (family >= 0)
                {
                    String uri = "@drawable/" + pokefamilylist[family].getIcon();
                    int imageResource = getContext().getResources().getIdentifier(uri, null, getContext().getPackageName());
                    family_detected.setImageResource(imageResource);
                }

            }
*/
        }

        /*
        private void tryReloadAndDetectInImage() {
            Log.d(TAG, "Try reload and detect image");
            try {
                if (imageUri == null) {
                    return;
                }

                if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
                    // UI layout has not finished yet, will reload once it's ready.
                    return;
                }

                Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
                if (imageBitmap == null) {
                    return;
                }

                // Clear the overlay first
                graphicOverlay.clear();

                Bitmap resizedBitmap;
                if (selectedSize.equals(SIZE_ORIGINAL)) {
                    resizedBitmap = imageBitmap;
                } else {
                    // Get the dimensions of the image view
                    Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                    // Determine how much to scale down the image
                    float scaleFactor =
                            max(
                                    (float) imageBitmap.getWidth() / (float) targetedSize.first,
                                    (float) imageBitmap.getHeight() / (float) targetedSize.second);

                    resizedBitmap =
                            Bitmap.createScaledBitmap(
                                    imageBitmap,
                                    (int) (imageBitmap.getWidth() / scaleFactor),
                                    (int) (imageBitmap.getHeight() / scaleFactor),
                                    true);
                }

                //preview.setImageBitmap(resizedBitmap);

                if (imageProcessor != null) {
                    graphicOverlay.setImageSourceInfo(
                            resizedBitmap.getWidth(), resizedBitmap.getHeight(),  false);
                    imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
                } else {
                    Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error retrieving saved image");
                imageUri = null;
            }
        }


        private Pair<Integer, Integer> getTargetedWidthHeight() {
            int targetWidth;
            int targetHeight;

            switch (selectedSize) {
                case SIZE_SCREEN:
                    targetWidth = imageMaxWidth;
                    targetHeight = imageMaxHeight;
                    break;
                case SIZE_640_480:
                    targetWidth = isLandScape ? 640 : 480;
                    targetHeight = isLandScape ? 480 : 640;
                    break;
                case SIZE_1024_768:
                    targetWidth = isLandScape ? 1024 : 768;
                    targetHeight = isLandScape ? 768 : 1024;
                    break;
                default:
                    throw new IllegalStateException("Unknown size");
            }

            return new Pair<>(targetWidth, targetHeight);
        }
        */

    }

}
