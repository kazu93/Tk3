package com.tk3_beacon_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class CustomView extends View {

    private Rect rectangle,
            positionRect;

    private Paint paint,
            paintPoint,
            textPaint,
            positionPaint;

    public CustomView(Context context) {
        super(context);
        int x = 100, y= 100, sideLength = 100;

        // create a rectangle that we'll draw later
        rectangle = new Rect(x, y, sideLength, sideLength);
        positionRect = new Rect(400, 540, 360, 590);

        // create the Paint and set its color
        paint = new Paint();
        paint.setColor(Color.GRAY);

        paintPoint = new Paint();
        paintPoint.setColor(Color.RED);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);

        positionPaint = new Paint();
        positionPaint.setColor(Color.GREEN);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(rectangle, paint);


        /*TODO:
         * double[][] positions = new double[][];
         * positions.add beacon
         *
         * double[] distances = new double[];
         * distances.add handy position to beacon
         *
         *
         * */

        double[][] positions = new double[][] { { 0, 0 }, { 0, 10 }, { 10, 10 }, { 10, 0 } };
        double[] distances = new double[] { 8.06, 13.97, 23.32, 15.31 };

        int amountOfBeacons = positions.length;

        for(int i = 0; i < amountOfBeacons; i++){
            if (i == 0){
                canvas.drawCircle(100,100, 20, paintPoint);
                canvas.drawText("Beacon 1", 100, 60, textPaint);
            }

            else if (i == 1){
                canvas.drawCircle(100,1000, 20, paintPoint);
                canvas.drawText("Beacon 2", 100, 1060, textPaint);
            }

            else if (i == 2){
                canvas.drawCircle(1000, 1000, 20, paintPoint);
                canvas.drawText("Beacon 3", 900, 1060, textPaint);
            }

            else if (i == 3){
                canvas.drawCircle(1000,100, 20, paintPoint);
                canvas.drawText("Beacon 4", 900, 60, textPaint);
            }
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();


        double[] centroid = optimum.getPoint().toArray();
        RealVector standardDeviation = optimum.getSigma(0);
        RealMatrix covarianceMatrix = optimum.getCovariances(0);


        canvas.drawRect(positionRect, positionPaint);
        canvas.drawText("my position", 350, 650, textPaint);

        canvas.drawText(amountOfBeacons + " beacons are currently active", 100, 1200, textPaint);
        String  distanceString =  "";
        for (int i = 0; i < distances.length; i++) {
            distanceString += String.valueOf(distances[i] + "m ");
            
        }
        canvas.drawText("Distance to beacons: " + distanceString, 100, 1400, textPaint);
    }
}
