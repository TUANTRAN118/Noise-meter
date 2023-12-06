package com.app.noisepollution;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ThirdOctaveGraph extends View {

    //Apna graph
    private float [] thirdOctave = {16, 20, 25, 31.5f, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 20000};
    private float[] band;
    private float[] bandMinimum = new float[thirdOctave.length];
    private float[] bandMaximum = new float[thirdOctave.length];
    private float fontSize;
    private Paint pGraphLines, pYAxisColor, pXAxisColor, pMaxFandDB, pBackgroundFill, pBackgroundStroke, pLinear, pLinearMax;


    public ThirdOctaveGraph(Context context) {
        this(context, null, 0);
    }

    public ThirdOctaveGraph(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThirdOctaveGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        pGraphLines = new Paint();
        pYAxisColor = new Paint();
        pXAxisColor = new Paint();
        pMaxFandDB = new Paint();
        pBackgroundFill = new Paint();
        pBackgroundStroke = new Paint();
        pLinear = new Paint();
        pLinearMax = new Paint();
        //Graph Lines Colors
        pGraphLines.setColor(0xffDADADA);//horizontal lines
        pGraphLines.setStyle(Paint.Style.FILL_AND_STROKE);
        pGraphLines.setStrokeWidth(1.0f);
        //Y axis labels
        pYAxisColor.setColor(Color.BLACK);
        pYAxisColor.setTextSize(20);
        pYAxisColor.setTextAlign(Paint.Align.RIGHT);
        //X axis labels
        pXAxisColor.setColor(Color.BLACK);
        pXAxisColor.setTextSize(20);
        pXAxisColor.setTextAlign(Paint.Align.CENTER);
        //Decibel  and Frequency Color on top right
        pMaxFandDB.setColor(0xff8BC34A);//frequcy color
        pMaxFandDB.setTextSize(20);
        pMaxFandDB.setTextAlign(Paint.Align.RIGHT);
        //Background Colors of the graph
        pBackgroundFill.setColor(Color.WHITE);
        pBackgroundFill.setStyle(Paint.Style.FILL);
        pBackgroundStroke.setColor(Color.WHITE);
        pBackgroundStroke.setStyle(Paint.Style.STROKE);
        pBackgroundStroke.setStrokeWidth(2.0f);
        //RealTime values
        pLinear.setColor(0xffFF3232);
        pLinear.setStyle(Paint.Style.FILL_AND_STROKE);
        pLinear.setStrokeWidth(2.0f);
        //Maximum reached in certain time period
        pLinearMax.setColor(Color.GRAY);
        pLinearMax.setStyle(Paint.Style.FILL_AND_STROKE);
        pLinearMax.setStrokeWidth(2.0f);




    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String [] label_x_axis = {"16", "", "", "31", "", "", "63", "", "", "125", "", "", "250", "", "", "500", "", "", "1k", "", "", "2k", "", "", "4k", "", "", "8k", "", "", "16k", ""};


        float width = getWidth();
        //FontSize For labels on Y and X axis
        fontSize = width * 0.045f;
        pXAxisColor.setTextSize(fontSize);
        pYAxisColor.setTextSize(fontSize);
        pMaxFandDB.setTextSize(fontSize);

        //minus the width of labels on Y axis
        float graph_area_w = getWidth() - pXAxisColor.measureText(Float.toString(pYAxisColor.getTextSize())); //* 0.925f;


        //Ascent - The recommended distance above the baseline for singled spaced text.
        //Descent - The recommended distance below the baseline for singled spaced text.
        float dY = (pYAxisColor.descent() - pYAxisColor.ascent());
        float dX = (pXAxisColor.descent() - pXAxisColor.ascent());
        float dMax = (pMaxFandDB.descent() - pMaxFandDB.ascent());

        //minus height of X axis labels
        float graph_area_h = getHeight() - dX - dY;
        float yMaxAxis = 100f;
        float barWeight = graph_area_w / (float) thirdOctave.length;// Siz of the bar(width)

        //Unweighted FFT graph
        //Frequency-domain graphs– also called spectrum plots and Fast Fourier transform graphs (FFT graphs for short)
        //show which frequencies are present in a vibration during a certain period of time




        if (band != null) {
            float dbyMaxIst = band[0];
            int iMaxIst = 0;


            //This is the bars you see on screen. We have 32 bars
            for (int i = 0; i < band.length; i++) {
                float xValue = width - graph_area_w + i * barWeight; //position of first bar

                //Log.e("z1", "onDraw: "+xValue );

                float yMax = bandMaximum[i] * graph_area_h / yMaxAxis;//100//getting length of Y MAX
                yMax = graph_area_h - yMax;//Area between YMax(gray) and graph area in height

                //Drawing Max here(The one you see in gray color)


                canvas.drawRect(xValue, dY + yMax, xValue + barWeight, dY + graph_area_h, pLinearMax);

                float y = band[i] * graph_area_h / yMaxAxis;//100
                y = graph_area_h - y;//Realtime value of Y not Max
                Log.e("z1", "onDraw: "+xValue );

                //dY + graph_area_h = Bottom line of graph on xxis
                //xValue
                //Drawing Linear here(The one you see in Red color)
                canvas.drawRect(xValue, dY + y, xValue + barWeight, dY + graph_area_h, pLinear);

                if(dbyMaxIst < band[i]){

                    dbyMaxIst = band[i];//Assigment maximun to di value
                    iMaxIst = i;// increment it
                }

                float yMin = bandMinimum[i] * graph_area_h / yMaxAxis;
                yMin = graph_area_h - yMin;



                if (label_x_axis[i] != "") {
                    canvas.drawText("" + label_x_axis[i], xValue + barWeight / 2, dY + graph_area_h - pXAxisColor.ascent(), pXAxisColor);
                    canvas.drawLine(xValue + barWeight / 2, dY + graph_area_h, xValue + barWeight / 2, dY + graph_area_h + pXAxisColor.ascent()*0.75f, pGraphLines);
                }
            }


            // This one is for vertical line
            //On y axis
            canvas.drawLine(width - graph_area_w, dY + graph_area_h - yMaxAxis * graph_area_h / yMaxAxis, width - graph_area_w, dY + graph_area_h, pGraphLines);


            for (int i = 0; i <= yMaxAxis; i += 10) {

                //horizontal big lines
                canvas.drawLine(width - graph_area_w, dY + graph_area_h - i * graph_area_h / yMaxAxis, width, dY + graph_area_h - i * graph_area_h / yMaxAxis, pGraphLines);
                canvas.drawText("" + i, width - graph_area_w - 5, dY + graph_area_h - i * graph_area_h / yMaxAxis + pXAxisColor.descent(), pYAxisColor);
            }

            // Writing maximum values to The top right corner for frequency and decibel
            canvas.drawRect(width - pMaxFandDB.measureText(" 20000 Hz ") - 10, dMax, width - 10, 3 * dMax + pMaxFandDB.descent(), pBackgroundStroke);
            canvas.drawRect(width - pMaxFandDB.measureText(" 20000 Hz ") - 10, dMax, width - 10, 3 * dMax  + pMaxFandDB.descent(), pBackgroundFill);
            canvas.drawText(String.format("%.1f", dbyMaxIst) + " dB ", width - 10, 2 * dMax, pMaxFandDB);
            canvas.drawText(String.format("%.0f", thirdOctave[iMaxIst]) + " Hz ", width - 10, 3 * dMax, pMaxFandDB);
        }


    }

    public void plotGraph(float[] data1, float[] data2, float[] data3) {

        //https://www.youtube.com/watch?v=XeJXx-Te1Eo&ab_channel=GradeUpgrade explanation at 8:00
        if (band == null || band.length != data1.length){
            band = new float[data1.length];

        }

        System.arraycopy(data1, 0, band, 0, data1.length);

        if (bandMinimum == null || bandMinimum.length != data2.length){
            bandMinimum = new float[data2.length];

        }
        System.arraycopy(data2, 0, bandMinimum, 0, data2.length);

        if (bandMaximum == null || bandMaximum.length != data3.length){
            bandMaximum = new float[data3.length];

        }

        System.arraycopy(data3, 0, bandMaximum, 0, data3.length);
        // alogrith
        band[1] = band[0];
        band[3] = band[2];
        band[4] = band[5];
        band[6] = band[7];
        bandMinimum[1] = bandMinimum[0];
        bandMinimum[3] = bandMinimum[2];
        bandMinimum[4] = bandMinimum[5];
        bandMinimum[6] = bandMinimum[7];
        bandMaximum[1] = bandMaximum[0];
        bandMaximum[3] = bandMaximum[2];
        bandMaximum[4] = bandMaximum[5];
        bandMaximum[6] = bandMaximum[7];

        //
        invalidate();
    }
}