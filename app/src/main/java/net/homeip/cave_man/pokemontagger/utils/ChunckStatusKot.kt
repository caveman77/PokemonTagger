package net.homeip.cave_man.pokemontagger.utils

import android.widget.ImageView
import net.homeip.cave_man.pokemontagger.tflite.Classifier
import net.homeip.cave_man.pokemontagger.tflite.Classifier.Recognition
import java.util.ArrayList

class ChunckStatusKot  constructor()
{
    @kotlin.jvm.JvmField
    var isLogo: Boolean = false

    @kotlin.jvm.JvmField
    var isCardN: Boolean = false

    @kotlin.jvm.JvmField
    var position = 0

    @kotlin.jvm.JvmField
    public var FamilyLogo: ImageView? = null        // ViewID of the image of the detected family

    @kotlin.jvm.JvmField
    public var Photo: ImageView? = null             // ViewID of the chunck picture

    @kotlin.jvm.JvmField
    public var RecognitionsList: List<Recognition>


    //CheckBox checkbox_logo_family;
    //TextView populationView;
    init
    {
        RecognitionsList = ArrayList<Classifier.Recognition>()
    }

    public fun setRecognitionList (reco : List<Classifier.Recognition>)
    {
        RecognitionsList = reco

    }


}