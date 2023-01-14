package net.homeip.cave_man.pokemontagger.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PokemonFamily
{
    private String pokecardexCode;
    private String Name;
    private String Icon;
    private int NumberOfCards;
    private String SecondaryNumberOfCards;          // some families have multiple total number of cards
    private int MaxSecretBefore2;                   // thresold to move to second number set

    private int index;
    private int indexML;
    //private String Area;

    public final static int FAKE_NB_CARD_PROMO = 9999;
    public final static int INDEX_CARD_PROMO=17;

    public PokemonFamily(String pokecardexCode, String Name, String Icon, int numcards, int index, int indexML, String SecondaryNumberOfCards, int MaxSecretBefore2)
    {
        this.pokecardexCode = pokecardexCode;
        this.Name = Name;
        this.Icon = Icon;
        this.NumberOfCards = numcards;
        this.index = index;
        this.indexML = indexML;
        this.SecondaryNumberOfCards = SecondaryNumberOfCards;
        this.MaxSecretBefore2 = MaxSecretBefore2;
    }

    public String getCardexCode() { return pokecardexCode; }

    public String getName()
    {
        return Name;
    }

    public void setName(String Name)
    {
        this.Name = Name;
    }

    public String getIcon()
    {
        return Icon;
    }

    public void setIcon(String Icon)
    {
        this.Icon = Icon;
    }

    public int getNumberOfCards() { return this.NumberOfCards; }

    public Boolean isTotalNumber(String inputstring)
    {
        try
        {
            int inputcd = Integer.parseInt(inputstring);
            if (inputcd == NumberOfCards)
                return true;
        }
        catch (Exception e)
        {

        }


        try
        {
            Pattern  p = Pattern.compile(this.SecondaryNumberOfCards);
            Matcher m = p.matcher(inputstring);
            boolean b = m.matches();

            if (b == true)
                return true;
        }
        catch (Exception e)
        {

        }


        return false;
    }


    public String GetPokeCardexUrl(String CardN)
    {
        String url = null;

        try
        {
            String fam;
            Integer cardNi;

            if (this.getIndex() != PokemonFamily.INDEX_CARD_PROMO)
            {
                if ((!Character.isDigit(CardN.charAt(0))) && (MaxSecretBefore2 !=0))
                {
                    cardNi = Integer.parseInt(CardN.replaceAll("\\D+", ""));
                    cardNi = cardNi + MaxSecretBefore2;
                }
                else
                {
                    cardNi = Integer.parseInt(CardN.replaceAll("\\D+", ""));
                }

                fam = this.getCardexCode();
            }
            else
            {
                cardNi = Integer.parseInt(CardN.replaceAll("\\D+", ""));
                fam = CardN.replaceAll("[0-9]*", "");

                if (fam=="HGSS")    fam="HS";

                fam = "PR"+fam;
            }

            url = "https://www.pokecardex.com/assets/images/sets/" + fam + "/HD/" + cardNi  + ".jpg";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return  url;
    }



    public int getIndex() { return this.index; }

    public int getIndexML() { return this.indexML; };

    // Text show in Spinner
    @Override
    public String toString()
    {
        return this.getName();
    }


    public static PokemonFamily[] loadFamiliesFromFile(int nbFamily, Context context)
    {

        PokemonFamily[] familyTable= new PokemonFamily[nbFamily];
        AssetManager assetManager = context.getAssets();
        InputStream is = null;

        try
        {
            is = assetManager.open("families.csv");
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

        String line = "";
        StringTokenizer st = null;
        int i=0;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                String[] tabChaine = line.split(";");

                if (i!=0)       // skip the first line
                {
                    String cardex = tabChaine[0];
                    String frenchname = tabChaine[1];
                    String icone = tabChaine[2];
                    String nbcard = tabChaine[3];
                    String index = tabChaine[4];
                    String indexml = tabChaine[5];

                    String nbcard2=null;
                    if (tabChaine.length >6)
                         nbcard2 = tabChaine[6];

                    int threi=0;
                    if (tabChaine.length >7)
                    {
                        try
                        {
                            threi = Integer.parseInt(tabChaine[7]);
                        }
                        catch(Exception e)
                        {

                        }


                    }


                    PokemonFamily obj= new PokemonFamily (
                            cardex,
                            frenchname,
                            icone,
                            Integer.parseInt(nbcard),
                            Integer.parseInt(index),
                            Integer.parseInt(indexml),
                            nbcard2,
                            threi);

                    familyTable[i-1] = obj;
                }
                i++;
            }
        }
        catch (IOException e)
        {

            e.printStackTrace();
        }

        return familyTable;
    }
}

