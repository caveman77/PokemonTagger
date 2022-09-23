package net.homeip.cave_man.pokemontagger.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DataBaseHandler extends SQLiteOpenHelper {
    public Context context;
    public static final String DATABASE_NAME = "dataManager";

    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "data";
    public static final String KEY_ID = "id";
    public static final String KEY_IMG_URL = "ImgFavourite";
    public static final String KEY_IMG_NAME = "ImgName";
    public static final String KEY_IMG_FAMILY = "ImgFamily";
    public static final String KEY_CARD_NUMBER = "CardNumber";

    public static final String KEY_DEVICE = "Device";
    public static final String KEY_MANUFACTURER = "Manufacturer";

    public static final String KEY_WIDTH_ORI = "Width_Ori";                         // Width of the original image
    public static final String KEY_HEIGHT_ORI = "Height_Ori";

    public static final String KEY_LOGO_30POSITIONS = "Logo_30Positions";                      // Starting at 0 26x26 picture of 30x30
    public static final String KEY_CARDN_30POSITIONS = "CardN_30Positions";
    public static final String KEY_LOGO_TL_CORNER_X_ORI = "Logo_TL_Cornder_X_Ori";            // Top left corner of logo rectangle. Orignal image
    public static final String KEY_LOGO_BR_CORNER_X_ORI = "Logo_BR_Cornder_X_Ori";            // Bottom right corder
    public static final String KEY_LOGO_TL_CORNER_Y_ORI = "Logo_TL_Cornder_Y_Ori";
    public static final String KEY_LOGO_BR_CORNER_Y_ORI = "Logo_BR_Cornder_Y_Ori";

    public static final String KEY_CARDN_TL_CORNER_X_ORI = "CardN_TL_Cornder_X_Ori";            // Top left corner of logo rectangle. Orignal image
    public static final String KEY_CARDN_BR_CORNER_X_ORI = "CardN_BR_Cornder_X_Ori";            // Bottom right corder
    public static final String KEY_CARDN_TL_CORNER_Y_ORI = "CardN_TL_Cornder_Y_Ori";
    public static final String KEY_CARDN_BR_CORNER_Y_ORI = "CardN_BR_Cornder_Y_Ori";

    public static final String KEY_LOGO_TL_CORNER_X_400 = "Logo_TL_Cornder_X_400";            // Top left corner of logo rectangle. 400gnal image
    public static final String KEY_LOGO_BR_CORNER_X_400 = "Logo_BR_Cornder_X_400";            // Bottom right corder
    public static final String KEY_LOGO_TL_CORNER_Y_400 = "Logo_TL_Cornder_Y_400";
    public static final String KEY_LOGO_BR_CORNER_Y_400 = "Logo_BR_Cornder_Y_400";

    public static final String KEY_CARDN_TL_CORNER_X_400 = "CardN_TL_Cornder_X_400";            // Top left corner of logo rectangle. 400gnal image
    public static final String KEY_CARDN_BR_CORNER_X_400 = "CardN_BR_Cornder_X_400";            // Bottom right corder
    public static final String KEY_CARDN_TL_CORNER_Y_400 = "CardN_TL_Cornder_Y_400";
    public static final String KEY_CARDN_BR_CORNER_Y_400 = "CardN_BR_Cornder_Y_400";
    
    public static final String KEY_IMG_EXIF_FLASH = "Exif_Tag_Flash";
    public static final String KEY_IMG_EXIF_ISO = "Exif_Tag_Iso";
    public static final String KEY_IMG_EXIF_ROTATION = "Exif_Tag_Rotation";

    public static final String KEY_NB_CHUNCKS = "NB_CHUNCKS";

    public DataBaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;

    }

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_IMG_URL + " TEXT, "
            + KEY_IMG_FAMILY + " TEXT, "
            + KEY_IMG_NAME + " TEXT, "
            + KEY_DEVICE + " TEXT, "
            + KEY_MANUFACTURER + " TEXT, "
            + KEY_CARD_NUMBER + " TEXT, "             // must be text as promo family has letter into it
            + KEY_NB_CHUNCKS + " INTEGER , "
            + KEY_WIDTH_ORI + " INTEGER , "
            + KEY_HEIGHT_ORI + " INTEGER , "

            + KEY_LOGO_30POSITIONS + " TEXT , "
            + KEY_CARDN_30POSITIONS + " TEXT , "

            + KEY_LOGO_TL_CORNER_X_ORI + " INTEGER , "
            + KEY_LOGO_BR_CORNER_X_ORI + " INTEGER , "
            + KEY_LOGO_TL_CORNER_Y_ORI + " INTEGER , "
            + KEY_LOGO_BR_CORNER_Y_ORI + " INTEGER , "

            + KEY_CARDN_TL_CORNER_X_ORI + " INTEGER , "
            + KEY_CARDN_BR_CORNER_X_ORI + " INTEGER , "
            + KEY_CARDN_TL_CORNER_Y_ORI + " INTEGER , "
            + KEY_CARDN_BR_CORNER_Y_ORI + " INTEGER,  "

            + KEY_LOGO_TL_CORNER_X_400 + " INTEGER , "
            + KEY_LOGO_BR_CORNER_X_400 + " INTEGER , "
            + KEY_LOGO_TL_CORNER_Y_400 + " INTEGER , "
            + KEY_LOGO_BR_CORNER_Y_400 + " INTEGER , "

            + KEY_CARDN_TL_CORNER_X_400 + " INTEGER , "
            + KEY_CARDN_BR_CORNER_X_400 + " INTEGER , "
            + KEY_CARDN_TL_CORNER_Y_400 + " INTEGER , "
            + KEY_CARDN_BR_CORNER_Y_400 + " INTEGER,  "
            
            + KEY_IMG_EXIF_FLASH + " INTEGER,  "
            + KEY_IMG_EXIF_ROTATION + " INTEGER,  "
            + KEY_IMG_EXIF_ISO + " INTEGER  "
            + ")";
    public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + "";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }

    public void deleteEntry(long row) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.delete(TABLE_NAME, KEY_ID + "=" + row, null);
    }

}
