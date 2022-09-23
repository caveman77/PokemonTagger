package net.homeip.cave_man.pokemontagger.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class FileUtils
{
    private static final String APPROOTDIR = "PokemonTager";
    private static final String ORIGINALSUBDIR = "Originals";
    private static final String CHUNKCSSUBDIR = "Chuncks";
    private static final String BACKUPSSUBDIR = "Backups";
    private static final String TAG= "FileUtils";

    public static File getBackupFileDirectory(Context context)
    {
        File imagePath =   getRootAppDirectory( context);
        imagePath = new File(imagePath, BACKUPSSUBDIR);

        return imagePath;
        //return App.getContext().getExternalFilesDir(null) + "/" + App.getContext().getString(R.string.app_name);
    }



    public static File getOriginalFileDirectory(Context context)
    {
        File imagePath =   getRootAppDirectory( context);
        imagePath = new File(imagePath, ORIGINALSUBDIR);

        return imagePath;
    }

    public static File getChuncksFileDirectory(Context context)
    {
        File imagePath =   getRootAppDirectory( context);
        imagePath = new File(imagePath, CHUNKCSSUBDIR);

        return imagePath;
    }

    public static File getRootAppDirectory(Context context)
    {

        File imagePath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);

        File[] appsDir= ContextCompat.getExternalFilesDirs(context,null);


        if (appsDir != null)   // there is some external storage. SD Card or not
        {
            // Need to take the second drive for samsung to be on SD card
            if(Build.DEVICE.toLowerCase(Locale.ROOT).contains("samsung")
                    || Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("samsung"))
            {
                if (appsDir.length > 1)
                {
                    imagePath = appsDir[1];
                }
            }


        }


        imagePath = new File(imagePath, APPROOTDIR );

        return imagePath;
    }

    public static File createDirIfNotExist(String path){
        File dir = new File(path);
        if( !dir.exists() ){
            dir.mkdir();
        }
        return dir;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    public static boolean  getReadyForStorage(Context context)
    {
        boolean success = true;

        try
        {
            //File ruta_sd = Environment.getExternalStorageDirectory();
            File approotdir = FileUtils.getRootAppDirectory(context);

            if (!approotdir.exists())
            {
                success = approotdir.mkdir();
            }


            if (success)
            {

                if (success)
                {
                    File folder = new File(approotdir, ORIGINALSUBDIR);
                    if (!folder.exists())
                    {
                        success = folder.mkdir();
                    }
                }

                if (success)
                {
                    File folder = new File(approotdir, CHUNKCSSUBDIR);
                    if (!folder.exists())
                    {
                        success = folder.mkdir();
                    }
                }

                if (success)
                {
                    File folder = new File(approotdir, BACKUPSSUBDIR);
                    if (!folder.exists())
                    {
                        success = folder.mkdir();
                    }
                }

            }
            else
            {
                Log.w(TAG, "Failed to created ");
            }

        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error al crear Carpeta a tarjeta SD");
        }

        return success;
    }



    // Assumption : Bitmap is provided as ARGB_8888. 1 pixel is stored over 4 bytes (1 byte for each 3 color + 1 transparency)
    static ByteBuffer ConvertBitmapToBWByte(Bitmap image, int image_size, boolean toLog)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate( 4 * image_size * image_size);  // 4 Bytes per float to store
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[image_size * image_size];

        try
        {
            image.getPixels(intValues, 0, image_size, 0, 0, image_size, image_size);
        }
        catch (Exception e)
        {
            throw  e;
        }
        int pixel=0;
        for (int i=0; i<image_size; i++)
        {
            for (int j=0; j<image_size;j++)
            {
                int color = intValues[pixel++];

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = (color >> 0) & 0xFF;

                // going to B&W between -1 and 1
                float fr = r, fg = g, fb = b;

                float newcol = (float) ((((fr+ fg+fb)/ ((float)255*3) ) - 0.5)*2);

                if (toLog)
                {
                    Log.v(
                            TAG,
                            "convertBitmapToByteBuffer - Pixel ("+String.format("%02d",i)+","+String.format("%02d",j)+"): color int:" + String.format("%05d",color) + " colors:" + String.format("%03d",r) + "," + String.format("%03d",g)+ "," + String.format("%03d",b)+" Float: " + newcol

                    );
                }

                byteBuffer.putFloat(newcol);

            }
        }

        return byteBuffer;
    }

    static public void SaveBitMapAsFile(Bitmap bitmap, String directory, String name, Bitmap.CompressFormat format)
    {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(directory);
        myDir.mkdir();
        //Random generator = new Random();
        //int n = 10000;
        //n = generator.nextInt(n);
        //String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, name);
        Log.i(TAG, "" + file);
        if (file.exists())
            file.delete();
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(format, 100, out);
            out.flush();
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public static Bitmap convertToBlackWhite(Bitmap bmp)
    {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        int alpha = 0xFF << 24; // ?bitmap?24?
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }

    /**
     * Gets the Amount of Degress of rotation using the exif integer to determine how much
     * we should rotate the image.
     * @param exifOrientation - the Exif data for Image Orientation
     * @return - how much to rotate in degress
     */
    public static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }


}
