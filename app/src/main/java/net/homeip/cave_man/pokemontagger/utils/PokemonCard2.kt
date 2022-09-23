package net.homeip.cave_man.pokemontagger.utils

import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import net.homeip.cave_man.pokemontagger.BuildConfig
import net.homeip.cave_man.pokemontagger.tflite.Classifier
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*


class PokemonCard
{
    //private var totalCardFamilyDetected : Int  = -1
    private var CardNumberDetected: String? = null       // can have letters in them for promo family
    private var totalCardFamilyDetectedString : String? = null

    private var family: PokemonFamily? = null
    private var OriginalPictureFile: File? = null
    var dbID: Long
        private set
    private var OriginalPictureWidth: Long = 0
    private var OriginalPictureHeight: Long = 0
    private var OriginalPictureFileName: String? = null
    private var db: SQLiteDatabase
    var databaseHandler: DataBaseHandler
    private var Exif_rotation // as per exif
            = 0
    private var Exif_Flash = 0
    private var Exif_Iso = 0
    private var chunkstatusTable: Array<ChunckStatusKot?>? = null;
    private var localContext: Context
    //private var tflite: Interpreter? = null
    //private var tfilemodel: ByteBuffer? = null
    private var imageChuncks: ArrayList<Bitmap>? = null // Image for each chunck

    //private ArrayList<Float[]> ProbFamilies = null;     // Probabilities predicted for each family  per chunck
    //private var ProbFamilies: ArrayList<List<Classifier.Recognition>> // Probabilities predicted for each family  per chunck

    private val mModelPath = "v46_model.tflite"           // was "converted_model.tflite"
    private val mLabelPath = "label _pokemon.txt"
    private lateinit var classifier: Classifier

    // Create a Pokemon card by reading DB
    constructor(context: Context, assetManager: AssetManager, ID: Long)
    {
        //totalCardFamilyDetected = - 1
        CardNumberDetected = null

        Log.v(
            TAG,
            "Creating card from database for:$ID"
        )
        FileUtils.getReadyForStorage(context)
        databaseHandler = DataBaseHandler(context)
        localContext = context
        dbID = ID
        //chunkstatusTable = new ChunckStatus[]{ isLogo= false;  isCardN = false};
        db = databaseHandler.writableDatabase
        val cursor = db.rawQuery(
            "SELECT "
                    + DataBaseHandler.KEY_IMG_FAMILY + ", "
                    + DataBaseHandler.KEY_IMG_NAME + ", "
                    + DataBaseHandler.KEY_LOGO_30POSITIONS + ", "
                    + DataBaseHandler.KEY_CARDN_30POSITIONS + ", "
                    + DataBaseHandler.KEY_NB_CHUNCKS
                    + " FROM data WHERE id = " + dbID + " Limit 1", null
        )
        if (cursor.moveToFirst()) {
            OriginalPictureFileName =
                cursor.getString(cursor.getColumnIndex(DataBaseHandler.KEY_IMG_NAME))
            //imgBitMap = LocalDataBaseAdapter.getBitmapFromEncodedString(img);
            //File imagePath = getContext().getExternalFilesDir("images_big");
            val imagePath = FileUtils.getOriginalFileDirectory(localContext)
            OriginalPictureFile = File(imagePath, OriginalPictureFileName)
            val nb_chuncks = cursor.getInt(cursor.getColumnIndex(DataBaseHandler.KEY_NB_CHUNCKS))
            if (nb_chuncks > 0) // picture must have been opened by the app once before
            {
                val jsonLogoPosition =
                    cursor.getString(cursor.getColumnIndex(DataBaseHandler.KEY_LOGO_30POSITIONS))
                val jsonCardNPosition =
                    cursor.getString(cursor.getColumnIndex(DataBaseHandler.KEY_CARDN_30POSITIONS))
                val logopositiontable = Gson().fromJson(
                    jsonLogoPosition,
                    IntArray::class.java
                )
                val cardpositiontable = Gson().fromJson(
                    jsonCardNPosition,
                    IntArray::class.java
                )
                for (i in logopositiontable.indices) {
                    chunkstatusTable!![i]!!.isLogo = true
                }
                for (i in cardpositiontable.indices) {
                    chunkstatusTable!![i]!!.isCardN = true
                }

                for (i in 0..nb_chuncks)
                {
                    chunkstatusTable!![i]!!.RecognitionsList =  ArrayList<Classifier.Recognition>()
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imagePath.absolutePath.toString()),
                null,
                null
            )
        }
        cursor.close()
        db.close()

        try {
            val exif = ExifInterface(OriginalPictureFile!!.absolutePath.toString())
            Exif_rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            OriginalPictureWidth =
                exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).toLong() // pixels per row
            OriginalPictureHeight =
                exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).toLong() // nb of rows
            Exif_Flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0)
            Exif_Iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        initClassifier(assetManager)

        //ProbFamilies = ArrayList<List<Classifier.Recognition>>()
    }

    constructor(context: Context, assetManager: AssetManager)
    {
        Log.v(TAG, "Creating card from scratch")

        //totalCardFamilyDetected = - 1
        localContext = context
        chunkstatusTable = null
        FileUtils.getReadyForStorage(context)
        databaseHandler = DataBaseHandler(context)
        db = databaseHandler.writableDatabase

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        OriginalPictureFileName = "photo_$timeStamp.jpg"
        dbID = setDataToDataBase(OriginalPictureFileName!!, -1)
        if (dbID > 0) {
            val imagePath = FileUtils.getOriginalFileDirectory(localContext)
            OriginalPictureFile = File(imagePath, OriginalPictureFileName)
            try {
                OriginalPictureFile!!.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Log.e(TAG, "Didn't succeed to get new ID from DB")
        }
        Log.v(TAG, "Creating card from scratch with ID" + dbID)
        initClassifier(assetManager)
        //ProbFamilies = ArrayList<List<Classifier.Recognition>>()

    }

    private fun initClassifier(assetManager: AssetManager)
    {
        classifier = Classifier(assetManager, mModelPath, mLabelPath, SIZE_CHUNCK)
    }

    fun getNumberChuncks(): Int
    {
        return imageChuncks!!.size;
    }

    fun getChunck(p : Int) : ChunckStatusKot?
    {
        return chunkstatusTable!!.get(p)
    }

    fun DectectCardNumber()
    {
        Log.i(TAG, "DectectCardNumber - Starting  " + Date())

        //totalCardFamilyDetected = - 1
        CardNumberDetected = null
        totalCardFamilyDetectedString = null

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(originalPictureBitmap, 0)

        val result = recognizer.process(image)
            .addOnSuccessListener { _ ->
                // Task completed successfully
                // ...
                Log.i(TAG, "DectectCardNumber - Completed succesfully :" + Date())

            }
            .addOnFailureListener { _ ->
                // Task failed with an exception
                // ...
                Log.e(TAG, "DectectCardNumber - Completed failed :" + Date())
            }


        try
        {
            val visionText = Tasks.await(result)

            //val resultText = visionText.text
            for (block in visionText.textBlocks)
            {
                for (line in block.lines)
                {
                    for (element in line.elements) {
                        val elementText = element.text


                        val regex = Regex("[A-Za-z]{0,2}[0-9oO]{1,3}/[A-Za-z]{0,2}[0-9oO]{1,3}|[Xx][Yy][0-9oO]{1,3}|[Hh][Gg][Ss][Ss][0-9oO]{1,3}|[Ss][Mm][0-9oO]{1,3}|[Bb][Ww][0-9oO]{1,3}|[Ss][Ww][Ss][Hh][0-9oO]{1,3}")
                        val finder = regex.find(elementText)

                        if (finder != null)
                        {
                            val elementWithoutSpec1  = elementText.replace(("[oO]").toRegex(), "0")
                            val elementWithoutSpec  = elementWithoutSpec1.replace(("[^0-9sSvV/]").toRegex(), "")
                            //val elementWithoutSpec  = elementWithoutSpec2.replace(("[^\\w\\d /]").toRegex(), "")        // Space may happear between digits. Not digit car might also happear

                            val slashpos = elementWithoutSpec.indexOf("/",0,false )

                            if (slashpos>0)
                            {
                                val spaceafterpos = elementWithoutSpec.indexOf(" ",slashpos,false )

                                var max = elementWithoutSpec.length
                                if ( spaceafterpos > 0)
                                    max = spaceafterpos

                                totalCardFamilyDetectedString = elementWithoutSpec.subSequence(slashpos + 1, max ).toString()
                                //totalCardFamilyDetectedString = totalCardFamilyDetectedString!!.replace(("[^0-9sSvV]").toRegex(), "") // remove any other thing than digit
                                totalCardFamilyDetectedString = totalCardFamilyDetectedString!!.substring(0,Math.min(totalCardFamilyDetectedString!!.length, 5))        // reduce to max 3 digits
                                totalCardFamilyDetectedString = totalCardFamilyDetectedString!!.uppercase()

                                //totalCardFamilyDetected = Integer.parseInt(totalCardFamilyDetectedString.toString())

                                var min =0
                                val spacebefore = elementWithoutSpec.lastIndexOf(" ", slashpos, false )

                                if ( spacebefore > 0)
                                    min = spacebefore +1

                                CardNumberDetected = elementWithoutSpec.subSequence(min, slashpos ).toString()  // o might happear instead of 0
                                CardNumberDetected = CardNumberDetected!!.uppercase()
                            }
                            else
                            {
                                // promo cards
                                //totalCardFamilyDetected = PokemonFamily.FAKE_NB_CARD_PROMO         // fake
                                totalCardFamilyDetectedString = PokemonFamily.FAKE_NB_CARD_PROMO.toString();
                                CardNumberDetected = elementWithoutSpec.uppercase()

                            }


                            Log.v(TAG, "DectectCardNumber - Found card:"+CardNumberDetected.toString()+"/"+totalCardFamilyDetectedString)
                        }
                    }
                }
            }

            Log.i(TAG, "DectectCardNumber - Parse completed")
        }
        catch (excep : Exception)
        {
            Log.e(TAG, "DectectCardNumber - Tasks.await Exception :" + Date())
        }

        Log.i(TAG, "DectectCardNumber - Fully Completed :" + Date())

    }

    fun Save(CardN: String): Int
    {
        if (CardN.length == 0) return 1
        val device = Build.DEVICE
        val manufacturer = Build.MANUFACTURER
        db = databaseHandler.writableDatabase
        val cv = ContentValues()
        //cv.put(databaseHandler.KEY_IMG_URL,getEncodedString(theImage));
        cv.put(DataBaseHandler.KEY_ID, dbID)
        cv.put(DataBaseHandler.KEY_IMG_NAME, OriginalPictureFileName)
        cv.put(DataBaseHandler.KEY_IMG_FAMILY, family!!.cardexCode)
        cv.put(DataBaseHandler.KEY_CARD_NUMBER, CardN.uppercase())
        cv.put(DataBaseHandler.KEY_DEVICE, device)
        cv.put(DataBaseHandler.KEY_MANUFACTURER, manufacturer)
        cv.put(DataBaseHandler.KEY_NB_CHUNCKS, imageChuncks!!.size)
        cv.put(DataBaseHandler.KEY_WIDTH_ORI, OriginalPictureWidth)
        cv.put(DataBaseHandler.KEY_HEIGHT_ORI, OriginalPictureHeight)

        //Bitmap original = getOriginalPictureBitmap();
        //int OriginalPictureWidth = original.getWidth();
        //int OriginalPictureHeight = original.getHeight();


        // delete all sub pictures (to manage multiple savings)
        val rootapp = FileUtils.getChuncksFileDirectory(localContext)
        val Logo_positions = ArrayList<Int>()
        val CardN_positions = ArrayList<Int>()



        // init aux opposés
        var LOGO_TL_X_400 = INTERMEDIARY_IMAGE_SIZE.toLong()
        var LOGO_TL_Y_400 = INTERMEDIARY_IMAGE_SIZE.toLong()
        var LOGO_BR_X_400: Long = 0
        var LOGO_BR_Y_400: Long = 0
        var CARD_TL_X_400 = INTERMEDIARY_IMAGE_SIZE.toLong()
        var CARD_TL_Y_400 = INTERMEDIARY_IMAGE_SIZE.toLong()
        var CARD_BR_X_400: Long = 0
        var CARD_BR_Y_400: Long = 0




        for (i in imageChuncks!!.indices)
        {
            var recol : List<Classifier.Recognition> =  listOf()       // empty by default
            var tobeSaved : Boolean = false

            var subdir: String? = "AAA_FAKE"
            if (chunkstatusTable!![i] != null)
            {
                recol  = chunkstatusTable!![i]!!.RecognitionsList

                if (chunkstatusTable!![i]!!.isLogo)
                {
                    Log.i(
                        TAG,
                        "Adding $i as Logo cell"
                    )
                    subdir = family!!.cardexCode
                    Logo_positions.add(i)
                    val tl_chucnk =
                        getXYfromPosition(chunkstatusTable!![i]!!.position, imageChuncks!!.size)
                    Log.i(
                        TAG,
                        "Cell position " + chunkstatusTable!![i]!!.position + " has (" + tl_chucnk + ") coordonates"
                    )
                    if (tl_chucnk[0] < LOGO_TL_X_400) LOGO_TL_X_400 = tl_chucnk[0]
                    if (tl_chucnk[1] < LOGO_TL_Y_400) LOGO_TL_Y_400 = tl_chucnk[1]
                    if (tl_chucnk[2] > LOGO_BR_X_400) LOGO_BR_X_400 = tl_chucnk[2]
                    if (tl_chucnk[3] > LOGO_BR_Y_400) LOGO_BR_Y_400 = tl_chucnk[3]

                    tobeSaved = true

                }
                if (chunkstatusTable!![i]!!.isCardN)
                {
                    Log.i(
                        TAG,
                        "Adding $i as card number cell"
                    )
                    CardN_positions.add(i)
                    val tl_chucnk =
                        getXYfromPosition(chunkstatusTable!![i]!!.position, imageChuncks!!.size)
                    Log.i(
                        TAG,
                        "Cell position " + chunkstatusTable!![i]!!.position + " has (" + tl_chucnk + ") coordonates"
                    )
                    if (tl_chucnk[0] < CARD_TL_X_400) CARD_TL_X_400 = tl_chucnk[0]
                    if (tl_chucnk[1] < CARD_TL_Y_400) CARD_TL_Y_400 = tl_chucnk[1]
                    if (tl_chucnk[2] > CARD_BR_X_400) CARD_BR_X_400 = tl_chucnk[2]
                    if (tl_chucnk[3] > CARD_BR_Y_400) CARD_BR_Y_400 = tl_chucnk[3]
                }
            }



            // Put the chunck in the sub dir of the image only if selected
            val dir = File(rootapp, subdir)
            //Bitmap bw = FileUtils.convertToBlackWhite(imageChuncks.get(i));
            //FileUtils.SaveBitMapAsFile(bw, dir.getAbsolutePath().toString(), OriginalPictureFileName.substring(0,OriginalPictureFileName.lastIndexOf(".")) + "_" + i + ".jpg", Bitmap.CompressFormat.JPEG);


            if (recol.size > 0)
            {
                val reco : Classifier.Recognition = recol.get(0)
                val familyid = reco.id.toInt()


                Log.i(
                    TAG,
                    "Save - Chunck $i - family $familyid "
                )

                // This is a fake
                if (  familyid != 0 )
                    tobeSaved=true
            }

            // We save only if the is the card location or if a fake
            if (tobeSaved)
            {
                FileUtils.SaveBitMapAsFile(
                    imageChuncks!![i],
                    dir.absolutePath.toString(),
                    OriginalPictureFileName!!.substring(
                        0,
                        OriginalPictureFileName!!.lastIndexOf(".")
                    ) + "_" + i + ".png",
                    Bitmap.CompressFormat.PNG
                )
            }

        }

        if (Logo_positions.size == 0) return 2
        if (CardN_positions.size == 0) return 3

        Log.i(
            TAG,
            "Logo rectangle on 400 picture ($LOGO_TL_X_400,$LOGO_TL_Y_400,$LOGO_BR_X_400,$LOGO_BR_Y_400)"
        )
        cv.put(DataBaseHandler.KEY_LOGO_TL_CORNER_X_400, LOGO_TL_X_400)
        cv.put(DataBaseHandler.KEY_LOGO_BR_CORNER_X_400, LOGO_BR_X_400)
        cv.put(DataBaseHandler.KEY_LOGO_TL_CORNER_Y_400, LOGO_TL_Y_400)
        cv.put(DataBaseHandler.KEY_LOGO_BR_CORNER_Y_400, LOGO_BR_Y_400)
        Log.i(
            TAG,
            "Card number rectangle on 400 picture ($CARD_TL_X_400,$CARD_TL_Y_400,$CARD_BR_X_400,$CARD_BR_Y_400)"
        )
        cv.put(DataBaseHandler.KEY_CARDN_TL_CORNER_X_400, CARD_TL_X_400)
        cv.put(DataBaseHandler.KEY_CARDN_BR_CORNER_X_400, CARD_BR_X_400)
        cv.put(DataBaseHandler.KEY_CARDN_TL_CORNER_Y_400, CARD_TL_Y_400)
        cv.put(DataBaseHandler.KEY_CARDN_BR_CORNER_Y_400, CARD_BR_Y_400)
        var width = OriginalPictureWidth
        var height = OriginalPictureHeight
        if ((Exif_rotation == ExifInterface.ORIENTATION_ROTATE_90) || (Exif_rotation == ExifInterface.ORIENTATION_ROTATE_270)) {
            val toto = width
            width = height
            height = toto
        }
        val LOGO_TL_X_ORI = LOGO_TL_X_400 * width / INTERMEDIARY_IMAGE_SIZE
        val LOGO_BR_X_ORI = LOGO_BR_X_400 * width / INTERMEDIARY_IMAGE_SIZE
        val LOGO_TL_Y_ORI = LOGO_TL_Y_400 * height / INTERMEDIARY_IMAGE_SIZE
        val LOGO_BR_Y_ORI = LOGO_BR_Y_400 * height / INTERMEDIARY_IMAGE_SIZE
        Log.i(
            TAG,
            "Logo rectangle on Original picture ($LOGO_TL_X_ORI,$LOGO_TL_Y_ORI,$LOGO_BR_X_ORI,$LOGO_BR_Y_ORI)"
        )
        cv.put(DataBaseHandler.KEY_LOGO_TL_CORNER_X_ORI, LOGO_TL_X_ORI)
        cv.put(DataBaseHandler.KEY_LOGO_BR_CORNER_X_ORI, LOGO_BR_X_ORI)
        cv.put(DataBaseHandler.KEY_LOGO_TL_CORNER_Y_ORI, LOGO_TL_Y_ORI)
        cv.put(DataBaseHandler.KEY_LOGO_BR_CORNER_Y_ORI, LOGO_BR_Y_ORI)
        val CARD_TL_X_ORI = CARD_TL_X_400 * width / INTERMEDIARY_IMAGE_SIZE
        val CARD_BR_X_ORI = CARD_BR_X_400 * width / INTERMEDIARY_IMAGE_SIZE
        val CARD_TL_Y_ORI = CARD_TL_Y_400 * height / INTERMEDIARY_IMAGE_SIZE
        val CARD_BR_Y_ORI = CARD_BR_Y_400 * height / INTERMEDIARY_IMAGE_SIZE
        Log.i(
            TAG,
            "Card number rectangle on Original picture ($CARD_TL_X_ORI,$CARD_TL_Y_ORI,$CARD_BR_X_ORI,$CARD_BR_Y_ORI)"
        )
        cv.put(DataBaseHandler.KEY_CARDN_TL_CORNER_X_ORI, CARD_TL_X_ORI)
        cv.put(DataBaseHandler.KEY_CARDN_BR_CORNER_X_ORI, CARD_BR_X_ORI)
        cv.put(DataBaseHandler.KEY_CARDN_TL_CORNER_Y_ORI, CARD_TL_Y_ORI)
        cv.put(DataBaseHandler.KEY_CARDN_BR_CORNER_Y_ORI, CARD_BR_Y_ORI)
        var json = Gson().toJson(Logo_positions)
        cv.put(DataBaseHandler.KEY_LOGO_30POSITIONS, json)
        json = Gson().toJson(CardN_positions)
        cv.put(DataBaseHandler.KEY_CARDN_30POSITIONS, json)
        cv.put(DataBaseHandler.KEY_IMG_EXIF_FLASH, Exif_Flash)
        cv.put(DataBaseHandler.KEY_IMG_EXIF_ISO, Exif_Iso)
        cv.put(DataBaseHandler.KEY_IMG_EXIF_ROTATION, Exif_rotation)
        db.update(
            DataBaseHandler.TABLE_NAME, cv, DataBaseHandler.KEY_ID + "=?", arrayOf(
                java.lang.Long.toString(
                    dbID
                )
            )
        )
        return 0
    }

    // return the top left  X,Y of a Chunck
    fun getXYfromPosition(position: Int, nb_chuncks: Int): LongArray {
        val nb_chuncks_per_line = Math.sqrt(nb_chuncks.toDouble()).toLong()
        val pos_in_line = position % nb_chuncks_per_line
        val line = position / nb_chuncks_per_line
        var tl_x =
            pos_in_line * SIZE_CHUNCK / 2
        var tl_y =
            line * SIZE_CHUNCK / 2
        var br_x =
            tl_x + SIZE_CHUNCK
        var br_y =
            tl_y + SIZE_CHUNCK
        if (br_x >= INTERMEDIARY_IMAGE_SIZE) {
            br_x =
                (INTERMEDIARY_IMAGE_SIZE - 1).toLong()
            tl_x = br_x - SIZE_CHUNCK
        }
        if (br_y >= INTERMEDIARY_IMAGE_SIZE) {
            br_y =
                (INTERMEDIARY_IMAGE_SIZE - 1).toLong()
            tl_y = br_y - SIZE_CHUNCK
        }
        return longArrayOf(tl_x, tl_y, br_x, br_y)
    }

    fun setChunckStatus(status: ChunckStatusKot) {
        chunkstatusTable!![status.position] = status
    }

    fun getTotalCardFamilyDetected() : String?
    {
        return totalCardFamilyDetectedString
    }

    fun getCardNumberDetected() : String?
    {
        return CardNumberDetected
    }


    fun setFamily(family: PokemonFamily?) {
        this.family = family
    }

    fun getFamily() : PokemonFamily?
    {
        return this.family
    }

    fun resetFamily() {
        family = null
    }

    val originalPictureBitmap: Bitmap
        get() = BitmapFactory.decodeFile(OriginalPictureFile!!.absolutePath.toString())
    val originalPictureUri: Uri
        get() {
            return FileProvider.getUriForFile(
                localContext, BuildConfig.APPLICATION_ID + ".provider",
                (OriginalPictureFile)!!
            )
        }

    private fun setDataToDataBase(ImagePath: String, family: Int): Long {
        db = databaseHandler.writableDatabase
        val cv = ContentValues()
        //cv.put(databaseHandler.KEY_IMG_URL,getEncodedString(theImage));
        cv.put(DataBaseHandler.KEY_IMG_NAME, ImagePath)
        cv.put(DataBaseHandler.KEY_IMG_FAMILY, family)
        return db.insert(DataBaseHandler.TABLE_NAME, null, cv)
    }

    // Split orginal image in chuncks
    fun SplitImage(): ArrayList<Bitmap>?
    {
        Log.i(TAG, "SplitImage - Starting")
        val theImage = originalPictureBitmap
        imageChuncks = null
        //if (tfilemodel == null) return null
        val patch_size = SIZE_CHUNCK
        val image_size = INTERMEDIARY_IMAGE_SIZE

        //For the number of rows and columns of the grid to be displayed

        //For height and width of the small image chunks
        val chunkHeight: Int
        val chunkWidth: Int

        //To store all the small image chunks in bitmap format in this list
        val chunkperaxis = ((image_size * 2) / patch_size) + 1
        val chunkNumbers = chunkperaxis * chunkperaxis
        val chunkedImages = ArrayList<Bitmap>(chunkNumbers)


        val chunkstat : ChunckStatusKot = ChunckStatusKot()


        //chunkstatusTable = arrayOfNulls(chunkNumbers)
        chunkstatusTable = Array<ChunckStatusKot?>(chunkNumbers, { _ -> chunkstat })

        //----- Getting the scaled bitmap of the source image
        val scaledBitmap = Bitmap.createScaledBitmap(theImage, image_size, image_size, true)
        val softwareBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
        Log.i(TAG, "SplitImage - Rotating based on Exif tag")
        // -- Rotate picture based on exif
        val rotatedBitmap: Bitmap
        try {
            val exif = ExifInterface(OriginalPictureFile!!.absolutePath.toString())
            Exif_rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationInDegrees = FileUtils.exifToDegrees(Exif_rotation)
            val matrix = Matrix()
            if (Exif_rotation.toFloat() != 0f) {
                matrix.preRotate(rotationInDegrees.toFloat())
            }
            rotatedBitmap = Bitmap.createBitmap(
                softwareBitmap,
                0,
                0,
                softwareBitmap.width,
                softwareBitmap.height,
                matrix,
                true
            )
        } catch (ex: IOException) {
            Log.e(TAG, "SplitImage - Failed to get Exif data", ex)
            return null
        }

        //------- Devide in truncks
        //rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = patch_size
        chunkWidth = patch_size

        //xCoord and yCoord are the pixel positions of the image chunks
        var yCoord = 0
        var p = 0
        var LastYDone = false
        while (yCoord <= (image_size - chunkHeight))
        {
            var LastXDone = false
            var xCoord = 0
            while (xCoord <= (image_size - chunkHeight)) {
                Log.i(
                    TAG,
                    "SplitImage - Processing chunck $p"
                )
                val currentChunk = Bitmap.createBitmap(rotatedBitmap, xCoord, yCoord, chunkWidth, chunkHeight)
                chunkedImages.add(currentChunk)
                //var tolog = false
                //if (p == 625) tolog = true
                // val chunckbuffer = FileUtils.ConvertBitmapToBWByte(currentChunk, patch_size, tolog)
                //chunkedBytes.add(chunckbuffer)
                xCoord += chunkWidth / 2

                // Take last patch on the border to make sure the whole picture is taken
                if ((xCoord > (image_size - chunkHeight)) && (LastXDone == false)) {
                    xCoord = image_size - chunkHeight
                    LastXDone = true
                }

                // Launch machine learning on chunck
                //ByteBuffer outputs = ByteBuffer.allocate(4 * NB_FAMILIES_PREDICTED);
                //outputs.order(ByteOrder.nativeOrder());
                /* for testing duration
                val result = classifier.recognizeImage(currentChunk)
                ProbFamilies.add(result)
                */

                p++
            }
            yCoord += chunkHeight / 2
            if ((yCoord > (image_size - chunkHeight)) && (LastYDone == false)) {
                yCoord = image_size - chunkHeight
                LastYDone = true
            }
        }
        imageChuncks = chunkedImages

        Log.i(TAG, "SplitImage - Completed")
        return chunkedImages
    }
/*
    fun launchML(): Boolean
    {

        Log.i(TAG, "launchML - Starting Machine Learning predict" + Date())
        for (currentChunk in imageChuncks!!)
        {
            val result = classifier.recognizeImage(currentChunk)
            ProbFamilies.add(result)
        }

        Log.i(TAG, "launchML - Starting Machine Learning completed" + Date())
        return true
    }
    */

    // Set Detect Family on a Chunck
    fun DectectLogoFamily(p: Int): Boolean
    {
        Log.i(TAG, "launchMLp - Starting Machine Learning predict for chunck ("+p + ") " + Date())

        if (imageChuncks == null)
            return false;

        if (imageChuncks!!.size < p)
            return false;

        val currentChunk : Bitmap = imageChuncks!!.get(p);


        val result = classifier.recognizeImage(currentChunk)
        chunkstatusTable!![p]!!.setRecognitionList( result )
        //ProbFamilies.add(result)


        Log.i(TAG, "launchMLp - Starting Machine Learning completed for chunck ("+p + ") " + Date())
        return true
    }

    // Set Predicted Family on a Chunck
    fun getPredictedFamily(Position: Int): Int
    {
        var indexmax = -1

        if (chunkstatusTable!!.size < (Position+1))
            return -1;

        //Float[] proba = ProbFamilies.get(Position);
        val nbproba = chunkstatusTable!![Position]!!.RecognitionsList.size

        if (nbproba > 0)
        {
            indexmax = chunkstatusTable!![Position]!!.RecognitionsList.get(0).id.toInt();            // 0 is not to be returned
        }

        if (indexmax > 0)
            return (indexmax -1);

        return -1;
    }

    // Determine the highest probability of all prediction over all chuncks
    // i.e. predict the card family
    // When possible, using totalCardFamilyDetectedString to filter on families having the same start
    fun getPredictedFamilyMaxProbaIndex(pokefamilylist: Array<PokemonFamily?>?, bCheckBasedonFamilyCardNumber : Boolean): PokemonFamily?
    {
        var maxproba : Float = 0f
        //var indexChunkMaxProba = 0
        var filteredFamilies  : ArrayList<PokemonFamily> = arrayListOf<PokemonFamily>()
        var bestFamily : PokemonFamily? = null

        if (pokefamilylist == null)
            return null;


        Log.i(TAG, "getPredictedFamilyMaxProbaIndex - Search might be restricted to ("+ totalCardFamilyDetectedString + ") " )

        for (family in pokefamilylist)
        {
            if (family != null)
            {
                if ((bCheckBasedonFamilyCardNumber) && (totalCardFamilyDetectedString != null))
                {
                    if (family.numberOfCards.toString().startsWith(totalCardFamilyDetectedString!!))
                    {
                        filteredFamilies.add(family)
                    }
                }
                else
                    filteredFamilies.add(family)

            }
        }

        Log.i(TAG, "getPredictedFamilyMaxProbaIndex - Found " + filteredFamilies.size + " families"  )


        // reduce pokemon family list to the ones starting good
        for (chunk in chunkstatusTable!!)
        {

            if (chunk!!.RecognitionsList.size >0)
            {
                val indexmax = chunk!!.RecognitionsList.get(0).id.toInt();
                if (indexmax >0)            // we don t take the empty ones
                {
                    val confidence = chunk!!.RecognitionsList.get(0).confidence;

                    if (confidence >= maxproba)
                    {
                        val foundedfamily : PokemonFamily? = filteredFamilies.find { pokefamily -> pokefamily.indexML == (indexmax-1) }

                        if (foundedfamily != null)
                        {
                            //indexChunkMaxProba = indexmax
                            maxproba = confidence
                            Log.i(TAG, "getPredictedFamilyMaxProbaIndex - chunck ("+ chunk.position + ") has better probability with " + confidence + " family ML:" + indexmax  )
                            Log.i(TAG, "getPredictedFamilyMaxProbaIndex - chunck ("+ chunk.position + ") has family total number of card matching " )

                            bestFamily = foundedfamily
                        }
                    }
                }
            }
        }


        return bestFamily;
    }


    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private val TAG = "Pokemoncard"
        private val SIZE_CHUNCK = 30 // number of pixel for each chunk
        private val INTERMEDIARY_IMAGE_SIZE = 400 // number of pixel for the intermediary image
        private val NB_FAMILIES_PREDICTED = 18
        private val PREDICT_THRESOLD = 0.5f
    }
}