package com.app.noisepollution;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class MainActivity extends AppCompatActivity {


    ThirdOctaveGraph octaveGraph;
    AudioRecord audioRecord;
    TextView lAeqTimeLabel,lMax,lAeqTime,lMin,currentReading, LAeqRealtime, durationTimeRealtime;
    Thread thread = null;
    boolean isRecording = false;
    DoubleFFT_1D fftObject = null;
    double filter = 0;
    float gain;

    // check for level
    String levelToShow;

    // Running Leq
    //Leq (or LAeq) is the Equivalent Continuous Sound Pressure Level.
    // Equivalent Continuous Sound Pressure Level, or Leq/LAeq,
    // is the constant noise level that would result in the same total sound energy being produced over a given period
    double linearFftAGlobalRealtime = 0;
    long fftCount = 0;
    double dbFftAGlobalRealtime;
    double minmum;
    double dbATime;
    double maximum;
    int fftA_GlobalMin_First = 0;
    int fftA_GlobalMax_First = 0;
    double fftA_GlobalMinTemp = 0;
    double fftA_GlobalMaxTemp = 0;
    private float[] thirdOctave = {16, 20, 25, 31.5f, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 20000};
    float[] dbBandMax = new float[thirdOctave.length];
    float[] dbBandMin = new float[thirdOctave.length];
    int v = 0;
    private int timeLog;
    private int timeDisplay;




    //A-weighted decibels, abbreviated dBA, or dBa, or dB(a),
    // are an expression of the relative loudness of sounds in air as perceived by the human ear.
    // In the A-weighted system, the decibel values of sounds at low frequencies are reduced,
    // compared with unweighted decibels, in which no correction is made for audio frequency.

    private final static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final static int RECORDER_SAMPLERATE = 44100;
    private final static int BYTES_PER_ELEMENT = 2;

    //Breaking down the audio// and getting a block
    private final static int BLOCK_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) / BYTES_PER_ELEMENT;
    private final static int BLOCK_SIZE_FFT = 1764;
    private final static int NUMBER_OF_FFT_PER_SECOND = RECORDER_SAMPLERATE / BLOCK_SIZE_FFT;
    private final static double FREQ_IN_HZ = ((double) RECORDER_SAMPLERATE) / BLOCK_SIZE_FFT;
    double[] weightedA = new double[BLOCK_SIZE_FFT];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        isRecordStoragePermissionGranted();


        init();


    }

    public void init(){
        //Init Views
        octaveGraph = (ThirdOctaveGraph) findViewById(R.id.thirdOctave);
        lMin = (TextView) findViewById(R.id.minTv);
        currentReading = (TextView) findViewById(R.id.currentReading);
        LAeqRealtime = (TextView) findViewById(R.id.lAeqRunning);
        durationTimeRealtime = (TextView) findViewById(R.id.DurationTimeRunning);
        lAeqTimeLabel = (TextView) findViewById(R.id.lAeqTimelabel);
        lMax = (TextView) findViewById(R.id.maxTv);
        lAeqTime = (TextView) findViewById(R.id.lAeqTime);



    }


    private void startRecording(final float gain, final int finalCountTimeDisplay, final int finalCountTimeLog) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BLOCK_SIZE * BYTES_PER_ELEMENT);




        audioRecord.startRecording();
        isRecording = true;

        fftObject = new DoubleFFT_1D(BLOCK_SIZE_FFT); //fft from BLOCK_SIZE_FFT points to BLOCK_SIZE_FFT / 2 , each from FREQHz
        thread = new Thread(new Runnable() {
            public void run() {

                // Array of raw data BLOCK_SIZE_FFT * 2 bytes
                short rawData[] = new short[BLOCK_SIZE_FFT];
                // Array of unweighted magnitude
                final float fftUnweighted[] = new float[BLOCK_SIZE_FFT / 2];
                // Array of weighted magnitude
                final float fftWeighted[] = new float[BLOCK_SIZE_FFT / 2];
                float normalizedData;
                // The fft has real part and imaginary part
                double[] audioDataForFFT = new double[BLOCK_SIZE_FFT * 2];
                // Threshold of hearing
                float amp = 0.00002f;//threshold of hearing
                // third octaves
                final float[] dbBand = new float[thirdOctave.length];
                final float[] linearBand = new float[thirdOctave.length];
                final float[] linearBandCount = new float[thirdOctave.length];

                int indexTime = 1;
                double linearATime = 0;
                final float[] dbAHistoryTimeDisplay = new float[60];




                while (isRecording) {

                    //Get Data
                    audioRecord.read(rawData, 0, BLOCK_SIZE_FFT);

                    for (int i = 0, j = 0; i < BLOCK_SIZE_FFT; i++, j += 2) {
                        // Range [-1,1]
                        normalizedData = (float) rawData[i] / (float) Short.MAX_VALUE;// Nomalize the data
                        filter = normalizedData;

                        // Hannings window

                        //The Hanning window is defined as. w ( n ) = 0.5 − 0.5 c o s ( 2 π n M − 1 ) 0 ≤ n ≤ M − 1.
                        // The Hanning was named for Julius von Hann, an Austrian meteorologist.
                        // It is also known as the Cosine Bell.
                        // Some authors prefer that it be called a Hann window,
                        // to help avoid confusion with the very similar Hamming window.
                        //Hanning window touches zero at both ends, removing any discontinuity.
                        // The Hamming window stops just shy of zero,
                        // meaning that the signal will still have a slight discontinuity
                        double x = (2 * Math.PI * i) / (BLOCK_SIZE_FFT - 1);
                        double winValue = (1 - Math.cos(x)) * 0.5d;

                        // Real part
                        audioDataForFFT[j] = filter * winValue;
                        //[real,imaginary,real,imaginary..............]
                        // Imaginary part
                        audioDataForFFT[j + 1] = 0.0;
                    }


                    // FFT
                    fftObject.complexForward(audioDataForFFT);

                    double linearFftAGlobal = 0;

                    // index for third octave
                    int k = 0;

                    for (int ki = 0; ki < thirdOctave.length; ki++) {
                        linearBandCount[ki] = 0;
                        linearBand[ki] = 0;
                        dbBand[ki] = 0;
                    }

                    // BLOCK_SIZE_FFT / 2 as it has 2 bytes

                    for (int i = 0, j = 0; i < BLOCK_SIZE_FFT / 2; i++, j += 2) {

                        double realPart = audioDataForFFT[j];
                        double imaginaryPart = audioDataForFFT[j + 1];

                        // Magnitude
                        double magnitude = Math.sqrt((realPart * realPart) + (imaginaryPart * imaginaryPart));

                        // A-weighted
                        double weightFormula = weightedA[i];

                        fftUnweighted[i] = (float) (10 * Math.log10(magnitude * magnitude / amp)) + (float) gain;
                        fftWeighted[i] = (float) (10 * Math.log10(magnitude * magnitude * weightFormula / amp)) + (float) gain;
                        
                        linearFftAGlobal += Math.pow(10, (float) fftWeighted[i] / 10f);

                        float linearFft = (float) Math.pow(10, (float) fftUnweighted[i] / 10f);



                        //Ranges for frequency
                        if ((0 <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 17.8f)) {
                            linearBandCount[0] += 1;
                            linearBand[0] += linearFft;
                            dbBand[0] =  (float) (10 * Math.log10(linearBand[0]));
                        }

                        if ((17.8f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 22.4f)) {
                            linearBandCount[1] += 1;
                            linearBand[1] += linearFft;
                            dbBand[1] =  (float) (10 * Math.log10(linearBand[1]));
                        }

                        if ((22.4f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 28.2f)) {
                            linearBandCount[2] += 1;
                            linearBand[2] += linearFft;
                            dbBand[2] =  (float) (10 * Math.log10(linearBand[2]));
                        }

                        if ((28.2f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 35.5f)) {
                            linearBandCount[3] += 1;
                            linearBand[3] += linearFft;
                            dbBand[3] =  (float) (10 * Math.log10(linearBand[3]));
                        }

                        if ((35.5f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 44.7f)) {
                            linearBandCount[4] += 1;
                            linearBand[4] += linearFft;
                            dbBand[4] =  (float) (10 * Math.log10(linearBand[4]));
                        }

                        if ((44.7f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 56.2f)) {
                            linearBandCount[5] += 1;
                            linearBand[5] += linearFft;
                            dbBand[5] =  (float) (10 * Math.log10(linearBand[5]));
                        }

                        if ((56.2f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 70.8f)) {
                            linearBandCount[6] += 1;
                            linearBand[6] += linearFft;
                            dbBand[6] =  (float) (10 * Math.log10(linearBand[6]));
                        }

                        if ((70.8f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 89.1f)) {
                            linearBandCount[7] += 1;
                            linearBand[7] += linearFft;
                            dbBand[7] =  (float) (10 * Math.log10(linearBand[7]));
                        }

                        if ((89.1f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 112f)) {
                            linearBandCount[8] += 1;
                            linearBand[8] += linearFft;
                            dbBand[8] =  (float) (10 * Math.log10(linearBand[8]));
                        }

                        if ((112f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 141f)) {
                            linearBandCount[9] += 1;
                            linearBand[9] += linearFft;
                            dbBand[9] =  (float) (10 * Math.log10(linearBand[9]));
                        }

                        if ((141f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 178f)) {
                            linearBandCount[10] += 1;
                            linearBand[10] += linearFft;
                            dbBand[10] =  (float) (10 * Math.log10(linearBand[10]));
                        }

                        if ((178f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 224f)) {
                            linearBandCount[11] += 1;
                            linearBand[11] += linearFft;
                            dbBand[11] =  (float) (10 * Math.log10(linearBand[11]));
                        }

                        if ((224f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 282f)) {
                            linearBandCount[12] += 1;
                            linearBand[12] += linearFft;
                            dbBand[12] =  (float) (10 * Math.log10(linearBand[12]));
                        }

                        if ((282f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 355f)) {
                            linearBandCount[13] += 1;
                            linearBand[13] += linearFft;
                            dbBand[13] =  (float) (10 * Math.log10(linearBand[13]));
                        }

                        if ((355f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 447f)) {
                            linearBandCount[14] += 1;
                            linearBand[14] += linearFft;
                            dbBand[14] =  (float) (10 * Math.log10(linearBand[14]));
                        }

                        if ((447f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 562f)) {
                            linearBandCount[15] += 1;
                            linearBand[15] += linearFft;
                            dbBand[15] =  (float) (10 * Math.log10(linearBand[15]));
                        }

                        if ((562f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 708f)) {
                            linearBandCount[16] += 1;
                            linearBand[16] += linearFft;
                            dbBand[16] =  (float) (10 * Math.log10(linearBand[16]));
                        }

                        if ((708f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 891f)) {
                            linearBandCount[17] += 1;
                            linearBand[17] += linearFft;
                            dbBand[17] =  (float) (10 * Math.log10(linearBand[17]));
                        }

                        if ((891f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 1122f)) {
                            linearBandCount[18] += 1;
                            linearBand[18] += linearFft;
                            dbBand[18] =  (float) (10 * Math.log10(linearBand[18]));
                        }

                        if ((1122f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 1413f)) {
                            linearBandCount[19] += 1;
                            linearBand[19] += linearFft;
                            dbBand[19] =  (float) (10 * Math.log10(linearBand[19]));
                        }

                        if ((1413f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 1778f)) {
                            linearBandCount[20] += 1;
                            linearBand[20] += linearFft;
                            dbBand[20] =  (float) (10 * Math.log10(linearBand[20]));
                        }

                        if ((1778f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 2239f)) {
                            linearBandCount[21] += 1;
                            linearBand[21] += linearFft;
                            dbBand[21] =  (float) (10 * Math.log10(linearBand[21]));
                        }

                        if ((2239f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 2818f)) {
                            linearBandCount[22] += 1;
                            linearBand[22] += linearFft;
                            dbBand[22] =  (float) (10 * Math.log10(linearBand[22]));
                        }

                        if ((2818f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 3548f)) {
                            linearBandCount[23] += 1;
                            linearBand[23] += linearFft;
                            dbBand[23] =  (float) (10 * Math.log10(linearBand[23]));
                        }

                        if ((3548f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 4467f)) {
                            linearBandCount[24] += 1;
                            linearBand[24] += linearFft;
                            dbBand[24] =  (float) (10 * Math.log10(linearBand[24]));
                        }

                        if ((4467f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 5623f)) {
                            linearBandCount[25] += 1;
                            linearBand[25] += linearFft;
                            dbBand[25] =  (float) (10 * Math.log10(linearBand[25]));
                        }

                        if ((5623f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 7079f)) {
                            linearBandCount[26] += 1;
                            linearBand[26] += linearFft;
                            dbBand[26] =  (float) (10 * Math.log10(linearBand[26]));
                        }

                        if ((7079f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 8913f)) {
                            linearBandCount[27] += 1;
                            linearBand[27] += linearFft;
                            dbBand[27] =  (float) (10 * Math.log10(linearBand[27]));
                        }

                        if ((8913f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 11220f)) {
                            linearBandCount[28] += 1;
                            linearBand[28] += linearFft;
                            dbBand[28] =  (float) (10 * Math.log10(linearBand[28]));
                        }

                        if ((11220f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 14130f)) {
                            linearBandCount[29] += 1;
                            linearBand[29] += linearFft;
                            dbBand[29] =  (float) (10 * Math.log10(linearBand[29]));
                        }

                        if ((14130f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 17780f)) {
                            linearBandCount[30] += 1;
                            linearBand[30] += linearFft;
                            dbBand[30] =  (float) (10 * Math.log10(linearBand[30]));
                        }

                        if ((17780f <= i * FREQ_IN_HZ) && (i * FREQ_IN_HZ < 22390f)) {
                            linearBandCount[31] += 1;
                            linearBand[31] += linearFft;
                            dbBand[31] =  (float) (10 * Math.log10(linearBand[31]));
                        }

                        //only calculating it for 32 bars


                    }



                    final double dbFftAGlobal = 10 * Math.log10(linearFftAGlobal);


                    //Displaying data from maximum and minimum levels
                    //min and max calculation of the global A-weighted FFT value
                     if (dbFftAGlobal > 0) {
                        if (fftA_GlobalMin_First == 0) {
                            fftA_GlobalMinTemp = dbFftAGlobal;
                            fftA_GlobalMin_First = 1;
                        } else {
                            if (fftA_GlobalMinTemp > dbFftAGlobal) {
                                fftA_GlobalMinTemp = dbFftAGlobal;
                            }
                        }
                        if (fftA_GlobalMax_First == 0){
                            fftA_GlobalMaxTemp = dbFftAGlobal;
                            fftA_GlobalMax_First = 1;
                        } else {
                            if (fftA_GlobalMaxTemp < dbFftAGlobal){
                                fftA_GlobalMaxTemp = dbFftAGlobal;
                            }
                        }
                    }
                    minmum = fftA_GlobalMinTemp;
                    maximum = fftA_GlobalMaxTemp;
                    
                    
                    // Realtime Leq
                    fftCount++;
                    linearFftAGlobalRealtime += linearFftAGlobal;
                    dbFftAGlobalRealtime = 10 * Math.log10(linearFftAGlobalRealtime /fftCount);


                    final int TimeRunning = (int) fftCount / NUMBER_OF_FFT_PER_SECOND;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lMax.setText(String.format("%.1f", maximum));
                            LAeqRealtime.setText(String.format("%.1f", dbFftAGlobalRealtime));
                            durationTimeRealtime.setText(String.format("%02d:%02d:%02d",  (TimeRunning % (3600*24) / 3600), (TimeRunning % 3600) / 60, (TimeRunning % 60)));
                            lMin.setText(String.format("%.1f", minmum));
                            if (levelToShow == "dbFftAGlobalMin") {
                                currentReading.setText(String.format("%.1f", minmum));
                            }
                            if (levelToShow == "dbFftAGlobalMax") {
                                currentReading.setText(String.format("%.1f", maximum));
                            }
                            if (levelToShow == "dbFftAGlobalRunning") {
                                currentReading.setText(String.format("%.1f", dbFftAGlobalRealtime));
                            }

                        }
                    });


                    // min and max calculation for unweighted dbBand
                    for (int kk = 0; kk < dbBand.length; kk++) {
                        if (dbBandMax[kk] < dbBand[kk]) {
                            dbBandMax[kk] = dbBand[kk];
                        }
                        if (v >= 10) {
                            // goodness level only if v> 10 measure well

                            if (dbBandMin[kk] == 0f) {
                                if (dbBand[kk] > 0) {
                                    dbBandMin[kk] = dbBand[kk];
                                }
                            } else if (dbBandMin[kk] > dbBand[kk]) {
                                dbBandMin[kk] = dbBand[kk];
                            }
                        }
                    }
                    v++;


                    // ThirdOctave
                    if (octaveGraph.getVisibility() == View.VISIBLE) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                octaveGraph.plotGraph(dbBand, dbBandMin, dbBandMax);
                            }
                        });
                    };

                    //Calculation of Averages for Time Display
                    linearATime += linearFftAGlobal;
                    if (indexTime < finalCountTimeDisplay) {
                        indexTime++;
                    } else {
                        dbATime = 10 * Math.log10(linearATime/finalCountTimeDisplay);
                        indexTime = 1;
                        linearATime = 0;

                        for (int i=1; i<60; i++){
                            dbAHistoryTimeDisplay[i-1] = dbAHistoryTimeDisplay[i];
                        }
                        dbAHistoryTimeDisplay[59] = (float) dbATime;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                lAeqTime.setText(String.format("%.1f", dbATime));
                                if (levelToShow == "dbATimeDisplay") {
                                    currentReading.setText(String.format("%.1f", dbATime));
                                }

                            }
                        });
                    }



                }
// add
                //                    float maxNoiseLevel = 316.2278f;
                float maxNoiseLevel = 0.8f;
                // Giả sử ngưỡng âm thanh là 50 dB //
                // Chuyển đổi giá trị
                // fftA_GlobalMaxTemp sang dB
//                    double dbFftA_GlobalMaxTemp = 20 * Math.log10 (fftA_GlobalMaxTemp);

                // Check if the noise level exceeds the limit
                if (dbATime > maxNoiseLevel) {
                    // Show an alert dialog with a custom message
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Cảnh báo");
                            builder.setMessage("Mức độ ồn đã vượt quá mức cho phép. Hãy giảm âm lượng hoặc di chuyển đến một nơi yên tĩnh hơn.");
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do something when the user clicks OK
                                }
                            });
                            builder.show();
                        }
                    });
                }
//                end
            }
        }, "Thread");
        thread.start();

    }

    private void stopRecording() {
        // stops the recording activity
        if (audioRecord != null) {
            isRecording = false;
            try {
                thread.join();
            } catch (Exception e) {

            }
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            thread = null;
        }
    }


    private void precalculate_weighted_a() {

        for (int i = 0; i < BLOCK_SIZE_FFT; i++) {
            double freq = FREQ_IN_HZ * i;double freqSQ = freq * freq;double freqFour = freqSQ * freqSQ;double freqEight = freqFour * freqFour;

            double t1 = 20.598997 * 20.598997 + freqSQ;
            t1 = t1 * t1;
            double t2 = 107.65265 * 107.65265 + freqSQ;
            double t3 = 737.86223 * 737.86223 + freqSQ;
            double t4 = 12194.217 * 12194.217 + freqSQ;
            t4 = t4 * t4;

            double weightFormula = (3.5041384e16 * freqEight) / (t1 * t2 * t3 * t4);

            weightedA[i] = weightFormula;
        }
    }



    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopRecording();
        finish();
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        stopRecording();
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String gainString = sharedPref.getString("gain", "");
            String timeLogString = sharedPref.getString("timeLog", "");
            String timeDisplayString = sharedPref.getString("timeDisplay", "");
            gain = Float.parseFloat(gainString);
            timeDisplay = Integer.parseInt(timeDisplayString);
            timeLog = Integer.parseInt(timeLogString);

        } catch (Exception e) {
            gain = 0.0f;
            timeDisplay = 1;
            timeLog = 1;
        }

        final int finalCountTimeDisplay = (int) (timeDisplay * NUMBER_OF_FFT_PER_SECOND);
        final int finalCountTimeLog = (int) (timeLog * NUMBER_OF_FFT_PER_SECOND);

        lAeqTimeLabel.setText("LAeq (" + timeDisplay + " s)");
        levelToShow = "dbATimeDisplay";



        precalculate_weighted_a();

        startRecording((Float) gain, (Integer) finalCountTimeDisplay, (Integer) finalCountTimeLog);

    }





    public  boolean isRecordStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted2");\
                return true;
            } else {

                //Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 3: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(         MainActivity.this,
                            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
