/**
 * Line View class for drawing a graph.
 */

package com.example.cryptocurrency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class GraphView extends View {
    private Paint lineChart = new Paint();
    private float yMin, yMax;
    private List<PointF> points = new ArrayList<>();
    private List<String> selectedSymbols;
    private String symbolName;
    private String[] yAxisValue;
    private String[] xAxisValue;
    private String[] xAxisValueSecondLine;
    private String sharedTimeFrame;
    private float width, height;
    private float[] yDots;
    private int scaleFactor;
    private int numColumns, numRows;
    private int cellWidth, cellHeight;
    private int paddingOffset;
    private Paint blackPaint;
    private Paint textAxisPaint;
    private Paint textTitlePaint;
    private int[] timeAxis;
    private int yPrecision;
    private int textAxisSize = 20;          // Text size of axis


    public GraphView(Context context) {
        super(context);
    }


    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set all drawing parameters for selected Cryptocurrency.
     *
     * @param points        - List of points which would be displayed
     * @param timeAxis      - Time in seconds - array
     * @param timeFrame     - Time frame, e.g. "day", "hour", "minute"
     * @param numberRows    - Number of rows for graph plotting
     * @param numberColumns - Number of columns for graph plotting
     * @param selSymbol     - List of loaded symbols
     * @param forSymbolName - Selected symbol
     */
    public void setAllDrawingParameters(List<PointF> points, int[] timeAxis, String timeFrame, int numberRows, int numberColumns, List<String> selSymbol, String forSymbolName) {
        blackPaint = new Paint();
        textTitlePaint = new Paint();
        textAxisPaint = new Paint();
        textTitlePaint.setTextSize(30);
        textTitlePaint.setFakeBoldText(true);
        textTitlePaint.setTextAlign(Paint.Align.CENTER);
        textAxisPaint.setTextSize(textAxisSize);
        this.points = points;
        this.timeAxis = timeAxis;
        this.sharedTimeFrame = timeFrame;
        this.numRows = numberRows;
        this.numColumns = numberColumns;
        this.selectedSymbols = selSymbol;
        this.symbolName = forSymbolName;
        calculateDimensions();
        invalidate();
        requestLayout();
    }

    // Find minimum and maximum value for Y axis.
    public void findMaxMin() {
        if(points.size() != 0) {
            yMin = points.get(0).y;
            yMax = points.get(0).y;
        }
        for(int i = 0; i < points.size(); i++) {
            if (points.get(i).y > yMax)
                yMax = points.get(i).y;
            if (points.get(i).y < yMin)
                yMin = points.get(i).y;
        }
    }

    // Scale Y values between 1 and 10. Scale factor will be shown above Y axis in format 10e<scaleFactor>
    public void scalePointY() {
        scaleFactor = 0;
        // If yMin > 10, decrease yMin and yMax
        while (yMin > 10) {
            scaleFactor ++;
            yMin = yMin / 10;
            yMax = yMax / 10;
        }
        // If yMin < 1, increase yMin and yMax
        while (yMin < 1) {
            scaleFactor --;
            yMin = yMin * 10;
            yMax = yMax * 10;
        }
        if(points.size() > 0)
            yDots = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            yDots[i] = points.get(i).y * (float) Math.pow(10, -scaleFactor);
        }
    }

    // Find the grid for Y axis. It could be defined with different precision.
    private void findGridForYAxis () {
        // Precision of rounding numbers to 2 decimals. It could be changed.
        yPrecision = 2;
        yMax = (float) Math.ceil(yMax * Math.pow(10, yPrecision));
        yMin = (float) (Math.ceil(yMin * Math.pow(10, yPrecision)) - 1);

        // After finding min i max value for Y axis, scaling those numbers to format ( Y values > 1 ) i.e. 1.22, 4.57 ...
        yMax = (float) (yMax * Math.pow(10, -yPrecision));
        yMin = (float) (yMin * Math.pow(10, -yPrecision));
    }

    // Create yAxisValue - an array of strings which would be shown as values of Y axis.
    private void setYAxisValue() {
        float yDeltaGrid = (yMax - yMin) / numRows;
        yAxisValue = new String[numRows + 1];
        yAxisValue[0] = String.format("%." + (yPrecision + 1) + "f", yMin);
        for(int i = 1; i <= numRows; i++)
            yAxisValue[i] = String.format("%." + (yPrecision + 1) + "f",  yMin + yDeltaGrid * i);
    }

    // Create xAxisValue - an array of strings which would be shown as values of X axis.
    private void setXAxisValue() {
        int xDeltaGrid = (timeAxis.length - 1) / numColumns;
        xAxisValue = new String[numColumns + 1];
        xAxisValueSecondLine = new String[numColumns + 1];
        for (int i = 0; i <= numColumns; i++) {
            xAxisValue[i] = " " + getMonth(timeAxis[i * xDeltaGrid])+ " " + getDay(timeAxis[i * xDeltaGrid]);
            xAxisValueSecondLine[i] =  getHour(timeAxis[i * xDeltaGrid]) + ":" + getMinute(timeAxis[i * xDeltaGrid]) + getAmPm(timeAxis[i * xDeltaGrid]);
        }
    }

    // Setting Y coordinates of points for plotting on canvas.
    private void setDotsY() {
        float yGrid = height / (yMax - yMin);
        for(int i = 0; i < points.size(); i++)
            yDots[i] = ((height + paddingOffset) - (yDots[i] - yMin) * yGrid);
    }

    // Setting X coordinates of points for plotting on canvas.
    private void setDotsX() {
        float xGrid = width / (points.size() / selectedSymbols.size() - 1);
        for(int i = 0; i < selectedSymbols.size(); i++) {
            for(int j = 0; j < points.size() / selectedSymbols.size(); j++)
                points.get(j + i * points.size() / selectedSymbols.size()).x = j * xGrid + paddingOffset;
        }
    }

    // Method for drawing graphic lines on canvas.
    @Override
    protected void onDraw(Canvas canvas) {

        // If column or row number didn't set.
        if (numColumns == 0 || numRows == 0) {
            return;
        }

        canvas.drawColor(Color.WHITE);
        Random rnd = new Random();
        lineChart.setColor(Color.RED);
        lineChart.setStrokeWidth(5);
        lineChart.setTextSize(25);

        // Obtaining canvas dimensions for placing graph.
        width =  (super.getWidth() - 2 * paddingOffset);
        height =  (super.getHeight() - 2 * paddingOffset);

        // Initializing all chart adjustment functions.
        if(points.size() != 0){

            calculateDimensions();
            findMaxMin();
            scalePointY();
            findGridForYAxis();
            setYAxisValue();
            setXAxisValue();
            setDotsY();
            setDotsX();

            // Drawing the Y axis values.
            for(int i = 0; i <= numRows; i++) {
                canvas.drawText(yAxisValue[i], paddingOffset / 5, paddingOffset + height - cellHeight * i + textAxisSize / 2, textAxisPaint);
            }

            // Drawing the X axis values.
            for (int i = 0; i <= numColumns; i++) {
                canvas.drawText(xAxisValue[i], paddingOffset / 2 + i * cellWidth, paddingOffset + height + textAxisSize * 2, textAxisPaint);
                canvas.drawText(xAxisValueSecondLine[i], paddingOffset / 2 + i * cellWidth, paddingOffset + height + textAxisSize * 3, textAxisPaint);
            }

            // Drawing the title
            float xPos = paddingOffset + width / 2f;
            float yPos = paddingOffset / 2 - ((textTitlePaint.descent() + textTitlePaint.ascent()) / 2) ;
            canvas.drawText(symbolName + " value comparison - by " + sharedTimeFrame, xPos, yPos, textTitlePaint);

            // Drawing the scaleFactor
            canvas.drawText("10e" + scaleFactor, paddingOffset / 5, paddingOffset / 2, textAxisPaint);
        }

        // Drawing columns.
        for (int i = 0; i <= numColumns; i++) {
            canvas.drawLine(i * cellWidth + paddingOffset, 0 + paddingOffset, i * cellWidth + paddingOffset, paddingOffset + height, blackPaint);
        }

        // Drawing rows
        for (int i = 0; i <= numRows; i++) {
            canvas.drawLine(0 + paddingOffset, i * cellHeight + paddingOffset, width + paddingOffset, paddingOffset+ i * cellHeight, blackPaint);
        }
        // Drawing graphic lines
        for(int i = 0; i < selectedSymbols.size(); i++) {
            // Draw graphic line for each symbol from selectedSymbols.
            for (int j = i * points.size() / selectedSymbols.size(); j < (i + 1) * (points.size() / selectedSymbols.size()) - 1; j++) {
                canvas.drawLine(points.get(j).x, yDots[j], points.get(j + 1).x, yDots[j+1], lineChart);
            }
            // Draw legend for each line.
            canvas.drawText(selectedSymbols.get(i), width + paddingOffset + 5, paddingOffset + textAxisSize * (i + 1) , lineChart);
            lineChart.setARGB(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        }

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    // Calculating the graph size depending on number of Columns and Rows.
    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }
        paddingOffset = 80;
        cellWidth =  ((super.getWidth() - paddingOffset * 2) / numColumns);
        cellHeight = ((super.getHeight() - paddingOffset * 2) / numRows);
    }

    /**
     * Transforming the Month string from "MM" (where M is number) to short text strings.
     *
     * @param month - Month in number format
     * @return      - String as short text name of Month
     */
    private String getMonthTextFromNum(String month){
        switch (month){
            case "01":
                month = "Jan"; break;
            case "02":
                month = "Feb"; break;
            case "03":
                month = "Mar"; break;
            case "04":
                month = "Apr"; break;
            case "05":
                month = "May"; break;
            case "06":
                month = "Jun"; break;
            case "07":
                month = "Jul"; break;
            case "08":
                month = "Aug"; break;
            case "09":
                month = "Sep"; break;
            case "10":
                month = "Okt"; break;
            case "11":
                month = "Nov"; break;
            case "12":
                month = "Dec"; break;
        }
        return month;
    }

    /**
     * Timestamp convert function for month.
     *
     * @param time - Time in seconds
     * @return     - String, return Month as two digits
     */
    private String getMonth(long time) {
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        cal.setTimeInMillis(time * 1000);
        String month = DateFormat.format("MM", cal).toString();
        return getMonthTextFromNum(month);
    }

    /**
     * Timestamp convert function for day.
     *
     * @param time - Time in seconds
     * @return     - String, return Day as two digits
     */
    @NotNull
    private String getDay(long time) {
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("dd", cal).toString();
    }

    /**
     * Timestamp convert function for am/pm.
     *
     * @param time - Time in seconds
     * @return     - String, return am/pm
     */
    // Timestamp convert function for am/pm.
    @NotNull
    private String getAmPm(long time) {
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("a", cal).toString();
    }

    /**
     * Timestamp convert function for hour.
     *
     * @param time - Time in seconds
     * @return     - String, return hour as two digits
     */
    @NotNull
    private String getHour(long time) {
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("hh", cal).toString();
    }

    /**
     * Timestamp convert function for minute.
     *
     * @param time - Time in seconds
     * @return     - String, return minute as two digits
     */
    @NotNull
    private String getMinute(long time) {
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("mm", cal).toString();
    }

}