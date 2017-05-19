package lvr.sidnapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Vasil on 27.01.2017.
 * Отрисовывает радиусы окружностей от углов "комнаты" и положение объекта. Т.е. маяки предполагаются точно в углах прямоугольной комнаты.
 * Все параметры (размеры "комнаты", координаты объекта, радиусы - в неких единицах, допустим метрах
 * Координаты отсчитываются от левого верхнего угла "комнаты"
 * Маяки (углы) маркируются так - TL - TopLeft - верхний левый, ... , BR - BottomRight - нижний правый
 */

public class PositionView extends View {

    private double
            roomWidth,
            roomHeight,
            posX, posY,
            scale;

    Position[] beaconPositions;
    double[] radiuses, strengths;

    private Paint roomBackgroundPaint, roomBorderPaint, beaconPaint, posPaint, circlePaint;

    public PositionView(Context context) {
        super(context);
        init();
    }

    public PositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        this.roomBackgroundPaint = new Paint(); this.roomBackgroundPaint.setARGB(255,0,0,0);this.roomBackgroundPaint.setStyle(Paint.Style.STROKE);
        this.roomBorderPaint = new Paint(); this.roomBorderPaint.setARGB(255,128,128,128);
        this.posPaint = new Paint(); this.posPaint.setARGB(255,0,255,0);
        this.beaconPaint = new Paint(); this.beaconPaint.setARGB(255,255,255,0);
        this.circlePaint = new Paint(); this.circlePaint.setARGB(255,0,0,255);this.circlePaint.setStyle(Paint.Style.STROKE);
    }


    public void setRoom(double width, double height, Position[] beaconPositions) {
        roomWidth = width;
        roomHeight = height;
        this.beaconPositions = beaconPositions;
    }

    public void setPos(double x, double y, double[] radiuses, double[] strengths) {
        posX = x;
        posY = y;
        this.radiuses = radiuses;
        this.strengths = strengths;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0,getWidth(), getHeight(), this.roomBackgroundPaint);
        //todo: border
        canvas.drawCircle(getX(posX), getY(posY), getWidth() / 20, this.posPaint);

        if (radiuses != null && beaconPositions != null) {
            for (int i = 0; i < radiuses.length; i++) {
                canvas.drawCircle(getX(beaconPositions[i].getX()), getY(beaconPositions[i].getY()), (int)Math.round(strengths[i]*5.0), this.beaconPaint);
                canvas.drawCircle(getX(beaconPositions[i].getX()), getY(beaconPositions[i].getY()), (float) (radiuses[i] * scale), this.circlePaint);
            }
        }
    }

    private float getX(double x) {
        return (float)(getWidth() * x / roomWidth);
    }

    private float getY(double y) {
        return (float)(getHeight() * y / roomHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (roomHeight <= 0 || roomWidth <= 0) {
            return;
        }

        int w = getMeasuredWidth(), h = getMeasuredHeight();

        if (w/roomWidth < h/roomHeight) {
            h = (int)Math.round(roomHeight * w / roomWidth);
            scale = w / roomWidth;
        } else {
            w = (int)Math.round(roomWidth * h / roomHeight);
            scale = h / roomHeight;
        }

        setMeasuredDimension(w, h);

    }
}
