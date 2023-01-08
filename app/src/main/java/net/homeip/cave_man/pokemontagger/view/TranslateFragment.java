package net.homeip.cave_man.pokemontagger.view;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.text.TextRecognizer;
import net.homeip.cave_man.pokemontagger.R;
import net.homeip.cave_man.pokemontagger.utils.ChunckStatusKot;
import net.homeip.cave_man.pokemontagger.utils.DataBaseHandler;
import net.homeip.cave_man.pokemontagger.utils.LocalResponse;
import net.homeip.cave_man.pokemontagger.utils.PokemonCard;
import net.homeip.cave_man.pokemontagger.utils.PokemonFamily;
import net.homeip.cave_man.pokemontagger.utils.PokemonFamilySpinner;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class TranslateFragment extends Fragment {
    RecyclerView recyclerView;
    private DataBaseHandler myDatabase;
    private SQLiteDatabase db;
    private ArrayList<LocalResponse> singleRowArrayList;
    private LocalResponse singleRow;
    String image;
    int uid;
    Cursor cursor;

    private static final String TAG = "TableFragment";


    private TextRecognizer textRecognizer;


    private PokemonCard card;
    private EditText editView;
    private Spinner mySpinner;
    private PokemonFamily[] pokefamilylist;
    private RadioGroup radioGroup;
    private Button OpenButton;
    //private ArrayList<ChunckStatusKot> ChuckStatusList;
    AsyncTask<Void, Integer, String> runningTask;
    public Boolean Store;       // Determine if the sub images are to be stored for future ML

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
        Store = this.getArguments().getBoolean("STORE");

        card = new PokemonCard(getContext(), this.getActivity().getAssets(), id);
        radioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
        pokefamilylist = PokemonFamily.loadFamiliesFromFile(32, getContext());

        mySpinner = (Spinner)view.findViewById(R.id.spinner_pokemonfamily);
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
                holder.Photo = img;

                img.setTag(holder);
                card.setChunckStatus(holder);

                position++;
            }
            table.addView(viewLine);
        }

        OpenButton = (Button) view.findViewById(R.id.save_button);
        OpenButton.setText("Open");
        OpenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                // save all and launch card in browser
                if (editView.getText() != null)
                {
                    String CardN = editView.getText().toString();

                    if (Store)
                    {
                        int isDone = card.Save(CardN);

                        switch (isDone)
                        {
                            case 0:
                                Toast.makeText(getActivity(), R.string.Saved, Toast.LENGTH_SHORT).show();
                                OpenPokeCardex(card, CardN);
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
                        OpenPokeCardex(card, CardN);
                    }


                }
                else
                {
                    Toast.makeText(getActivity(), R.string.CardNumberEmpty, Toast.LENGTH_LONG).show();
                }
             }
        });

        OpenButton.setEnabled(false);
        runningTask = new LongOperation();
        runningTask.execute();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        runningTask.cancel(true);
    }

    public void OpenPokeCardex(PokemonCard card, String CardN)
    {
        try
        {
            PokemonFamily poke = card.getFamily();
            //String fam = card.getTotalCardFamilyDetected();
            String url = poke.GetPokeCardexUrl(CardN);

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        catch (Exception e)
        {
            Toast.makeText(getActivity(), R.string.CardNumberInvalid, Toast.LENGTH_LONG).show();
        }

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
            int tmpFamilyFound = -1;
            card.DectectCardNumber();

            String nbCardTotalinFamily = card.getTotalCardFamilyDetected();
            int nbFamilyHavingSameThisTotal=0;

            if (nbCardTotalinFamily != null)
            {
                for (int i=0 ; i < pokefamilylist.length; i++ )
                {
                    if (pokefamilylist[i].isTotalNumber(nbCardTotalinFamily))
                    {
                        tmpFamilyFound = i;
                        nbFamilyHavingSameThisTotal++;
                    }
                }
            }

            boolean pastrouve = true;
            if (nbFamilyHavingSameThisTotal==1)
            {
                // No need to search for logo as only one family has this total of card to save time. Outcome is the same
                card.setFamily(pokefamilylist[tmpFamilyFound]);
                pastrouve = false;

            }


            // family logo are usually below so better start with the last ones
            for (int p=card.getNumberChuncks()-1; p>=0 && ((pastrouve == true) || (Store==true)) && (!isCancelled()); p--)
            {
                card.DectectLogoFamily(p);
                publishProgress(p);


                if (nbFamilyHavingSameThisTotal >1)
                {
                    int family = card.getPredictedFamily(p);
                    if (family > 0)
                    {
                        PokemonFamily poke = GetFamilyFromMlIndex(family);
                        if (poke.isTotalNumber(nbCardTotalinFamily))
                        {
                            pastrouve = false;
                            card.setFamily(poke);
                        }
                    }
                }
            }

            // Text reco didn't found family. ML family recognition went up to the end. Taking the highest probability
            if (pastrouve)
            {
                PokemonFamily fam = card.getPredictedFamilyMaxProbaIndex(pokefamilylist, true);

                if (fam != null)
                    card.setFamily(fam);
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
                PokemonFamily poke = GetFamilyFromMlIndex(family);


                try
                {
                    String uri = "@drawable/" + poke.getIcon();
                    int imageResource = getContext().getResources().getIdentifier(uri, null, getContext().getPackageName());
                    family_detected.setImageResource(imageResource);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }

            if (p==card.getNumberChuncks()-1)       // as the text reco has been played already. Let's update the layout accordingly
            {
                PokemonFamily pokefamily = card.getFamily();
                if (pokefamily != null)
                {
                    mySpinner.setSelection(pokefamily.getIndex());
                }


                String numero = card.getCardNumberDetected();
                if (numero != null)
                {
                    Integer chunkNumber = card.getCardNumberChunk();
                    if (chunkNumber != null)
                    {
                        ChunckStatusKot c2 = card.getChunck(chunkNumber);
                        View v = c2.Photo;
                        v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    }

                    editView.setText(numero);
                }
            }


        }

        private PokemonFamily GetFamilyFromMlIndex(int index)
        {
            int j=0;
            boolean found = false;
            PokemonFamily fam;

            do
            {
                fam = pokefamilylist[j];

                if (fam.getIndexML() == index)
                    found=true;

                j++;
            }
            while ((j < pokefamilylist.length) && (found == false));

            if (found)
                return fam;

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            // sometimes even the first chunck is not analysed
            String CardN = editView.getText().toString();
            if (StringUtils.isEmpty(CardN))
            {
                CardN = card.getCardNumberDetected();
                if (!StringUtils.isEmpty(CardN))
                {
                    editView.setText(CardN);

                    Integer chunkNumber = card.getCardNumberChunk();
                    if (chunkNumber != null)
                    {
                        ChunckStatusKot c2 = card.getChunck(chunkNumber);
                        View v = c2.Photo;
                        v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    }
                }
            }

            PokemonFamily family = card.getFamily();
            if (family != null)
            {
                mySpinner.setSelection(family.getIndex());

                if (!Store)     // User is to press Open button
                {

                    OpenPokeCardex(card, CardN);
                }
            }





            OpenButton.setEnabled(true);
        }


    }

}
