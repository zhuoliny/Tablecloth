package edu.buffalo.tablecloth.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PseudoColorTablecloth extends SurfaceView implements SurfaceHolder.Callback {
    private static final int MARGIN_LENGTH = 20;
    private static final int COUNT_ANALOG_POINT = 4;                                     //每两个点之间的模拟点数量
    private static final int COUNT_LEFT_REAL_POINT = 24;
    private static final int COUNT_TOP_REAL_POINT = 16;
    private static final int INNER_AREA_lEFT_COUNT = COUNT_LEFT_REAL_POINT
            + COUNT_ANALOG_POINT * (COUNT_LEFT_REAL_POINT - 1);                         // 每一列总的点数，包括虚拟点和真实压力点，默认70
    private static final int INNER_AREA_TOP_COUNT = COUNT_TOP_REAL_POINT
            + COUNT_ANALOG_POINT * (COUNT_TOP_REAL_POINT - 1);                          // 每一列行总的点数，包括虚拟点和真实压力点，默认46

    private static final int LENGTH_PRESSURES = COUNT_LEFT_REAL_POINT
            * COUNT_TOP_REAL_POINT;
    private static final int LENGTH_POINTS = INNER_AREA_lEFT_COUNT
            * INNER_AREA_TOP_COUNT;
    private final double[][][] mWeights = new double[INNER_AREA_lEFT_COUNT][INNER_AREA_TOP_COUNT][LENGTH_PRESSURES];
    private final double[][] mWeightSum = new double[INNER_AREA_lEFT_COUNT][INNER_AREA_TOP_COUNT];

    private SurfaceHolder mHolder;
    private final Handler mHandler = new Handler();

    private Point[] points = new Point[LENGTH_POINTS];
    private Point[] pressures = new Point[LENGTH_PRESSURES];

    private Paint paint = null;
    private Canvas canvas = null;

    public boolean initialized = false;

    public PseudoColorTablecloth(Context context) {
        super(context);
    }

    public PseudoColorTablecloth(Context context, AttributeSet attrs) {
        super(context, attrs);

        setZOrderOnTop(true);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public PseudoColorTablecloth(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
    }

    private float mScaleX, mScaleY;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        InitializeAsyncTask asyncTask = new InitializeAsyncTask();
        asyncTask.execute();

        mScaleX = (float) (getWidth() / (INNER_AREA_TOP_COUNT + (MARGIN_LENGTH * 1.95)));
        mScaleY = (float) (getHeight() / (INNER_AREA_lEFT_COUNT + (MARGIN_LENGTH * 1.95)));

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private class InitializeAsyncTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            initializeValues();
            return null;
        }
    }

    private static class Point {

        final int top;
        final int left;
        final boolean isPressure;

        private Point(int top, int left, boolean isPressure) {
            this.top = top;
            this.left = left;
            this.isPressure = isPressure;
        }

        static Point create(int top, int left, boolean isBorder) {
            return new Point(top, left, isBorder);
        }

    }

    private void drawInsoleImage(Bitmap bitmap) {

        try {
            canvas = mHolder.lockCanvas();
            if (canvas != null) {
                paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStrokeWidth(1);

                canvas.scale(mScaleX, mScaleY);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(bitmap, MARGIN_LENGTH, MARGIN_LENGTH, paint);
            }
        } finally {
            if (canvas != null) {
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // 画点
    private void drawPoint(int[] colors) {

        Canvas canvas = null;
        canvas = mHolder.lockCanvas();
        if (canvas != null) {
            paint.setStrokeWidth(1);

            canvas.scale(5.0f, 5.0f);
            int k = 0;
            for (int i = 0; i < INNER_AREA_lEFT_COUNT; i++) {
                for (int j = 0; j < INNER_AREA_TOP_COUNT; j++) {
                    paint.setColor(colors[k]);
                    canvas.drawPoint(j + 10, i + 10, paint);
                    k++;
                }
            }
        }
        mHolder.unlockCanvasAndPost(canvas);
    }

    private void initializeValues() {

        // 存好所有点，设置压力点和非压力点
        int k = 0;
        for (int i = 0; i < INNER_AREA_lEFT_COUNT; i++) {
            for (int j = 0; j < INNER_AREA_TOP_COUNT; j++) {
                if (j % (COUNT_ANALOG_POINT + 1) == 0
                        && i % (COUNT_ANALOG_POINT + 1) == 0) {
                    points[k] = Point.create(i, j, true);
                } else {
                    points[k] = Point.create(i, j, false);
                }
                k++;
            }
        }

        // 存好压力点
        int index = 0;
        for (int i = 0; i < points.length; i++) {
            if (points[i].isPressure) {
                pressures[index] = points[i];
                index++;
            }
        }

        // 计算保存权重，权重和
        for (int i = 0; i < INNER_AREA_lEFT_COUNT; i++) {
            for (int j = 0; j < INNER_AREA_TOP_COUNT; j++) {
                mWeightSum[i][j] = 0;
                for (int pressureIndex = 0; pressureIndex < pressures.length; pressureIndex++) {
                    double weight = gaussianWeight(i, j,
                            pressures[pressureIndex].top,
                            pressures[pressureIndex].left);
                    mWeights[i][j][pressureIndex] = weight;
                    mWeightSum[i][j] += weight;
                }
            }
        }
        initialized = true;
    }

    // 用来刷新图像的runnable
    private class PseudoColorInsoleGenerator implements Runnable {

        private final int[] data;

        private PseudoColorInsoleGenerator(int[] data) {
            this.data = data;
        }

        @Override
        public void run() {
            try {
                drawInsoleImage(generateInsoleImage(data));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap generateInsoleImage(int[] data) throws InterruptedException {
        long start = System.currentTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(INNER_AREA_TOP_COUNT,
                INNER_AREA_lEFT_COUNT, Bitmap.Config.ARGB_8888);
        for (int left = 0; left < INNER_AREA_lEFT_COUNT; left++) {
            for (int top = 0; top < INNER_AREA_TOP_COUNT; top++) {
                double weightValueSum = 0;
                for (int pressureIndex = 0; pressureIndex < pressures.length; pressureIndex++) {
                    weightValueSum += mWeights[left][top][pressureIndex]
                            * data[pressureIndex];
                }
                double weightMean = weightValueSum / mWeightSum[left][top];
                bitmap.setPixel(top, left, toPseudoColor((int) weightMean));
            }
        }

        return bitmap;
    }

    // 拿到颜色
    private int[] generateColor(int[] data) {

        int[] colors = new int[LENGTH_POINTS];
        int k = 0;
        for (int i = 0; i < INNER_AREA_lEFT_COUNT; i++) {
            for (int j = 0; j < INNER_AREA_TOP_COUNT; j++) {
                double weightValueSum = 0;
                for (int pressureIndex = 0; pressureIndex < pressures.length; pressureIndex++) {
                    weightValueSum += mWeights[i][j][pressureIndex]
                            * data[pressureIndex];
                }
                double weightMean = weightValueSum / mWeightSum[i][j];
                colors[k] = toPseudoColor((int) weightMean);
                k++;
            }
        }
        return colors;
    }

    // 权重算法
    private static final double GAUSSIAN_COEFFICIENT = 1 / Math
            .sqrt(2 * Math.PI);

    private double gaussianWeight(int x1, int y1, int x2, int y2) {
        double distanceSquare = -0.5
                * ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        return GAUSSIAN_COEFFICIENT * Math.pow(Math.E, distanceSquare);
    }

    private int toPseudoColor(int value) {
        int color;
        if (value < 51) {
            color = Color.argb(255 - (51 - value) * 5, 0, 0, value * 5);
        } else if (value <= 102) {
            value -= 51;
            color = Color.rgb(0, value * 5, 255 - value * 5);
        } else if (value <= 153) {
            value -= 102;
            color = Color.rgb(value * 5, 255, 0);
        } else if (value <= 204) {
            value -= 153;
            color = Color.rgb(255, 255 - (int) (128.0 * value / 51 + 0.5), 0);
        } else {
            value -= 204;
            color = Color.rgb(255, 127 - (int) (127.0 * value / 51 + 0.5), 0);
        }
        return color;
    }

    public void reFresh(int[] data) {
        if (initialized) {
            mHandler.post(new PseudoColorInsoleGenerator(data));
        }
    }


}
