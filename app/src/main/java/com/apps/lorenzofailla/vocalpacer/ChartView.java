package com.apps.lorenzofailla.vocalpacer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lorenzo.failla on 16/07/2016.
 */
public class ChartView extends View {

    Paint paint;
    Paint axes;

    int h;
    int w;

    static int VERTICAL_AXIS_PADDING = 15;
    static int HORIZONTAL_AXIS_PADDING = 35;

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(.1f);
        paint.setTextSize(28f);

        axes = new Paint();
        axes.setColor(Color.DKGRAY);
        axes.setTextSize(28f);
    }

    @Override
    protected void onDraw(Canvas canvas){

        super.onDraw(canvas);

        canvas.drawLine(VERTICAL_AXIS_PADDING, VERTICAL_AXIS_PADDING, VERTICAL_AXIS_PADDING, h-HORIZONTAL_AXIS_PADDING, axes);
        canvas.drawLine(VERTICAL_AXIS_PADDING, h-HORIZONTAL_AXIS_PADDING, w-VERTICAL_AXIS_PADDING, h-HORIZONTAL_AXIS_PADDING, axes);

        canvas.drawText("0", VERTICAL_AXIS_PADDING, h-HORIZONTAL_AXIS_PADDING+axes.getTextSize(), axes);

        canvas.save();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

        w=MeasureSpec.getSize(widthMeasureSpec);
        h=MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }


}
