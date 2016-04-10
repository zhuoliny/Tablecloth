package edu.buffalo.tablecloth.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import edu.buffalo.tablecloth.Application;
import edu.buffalo.tablecloth.R;

public class LightsLayout extends GridLayout {
    private LightView[] lights = new LightView[LIGHTS_COUNTS];
    private boolean[] status;
    private Context mContext;

    private static final int LIGHTS_COUNTS = 96;
    private static final int LEFT_LIGHTS_COUNT = 8;
    private static final int TOP_LIGHTS_COUNT = 12;
    private static final int CARD_MARGIN = 2;

    public LightsLayout(Context context) {
        this(context, null);
    }

    public LightsLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LightsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.mContext = context;
        initViews();
    }

    private void initViews() {
        setColumnCount(LEFT_LIGHTS_COUNT);
        setRowCount(TOP_LIGHTS_COUNT);

        int cardSize = getCardSize(Application.width, Application.height);

        for (int location = 0; location < LIGHTS_COUNTS; location++) {
            LightView lightView = new LightView(mContext);
            lights[location] = lightView;
            addView(lightView, cardSize, cardSize);
        }
    }

    private int getCardSize(int width, int height) {
        return ((Math.min(width, height) - (GridLayout.ALIGN_MARGINS * (TOP_LIGHTS_COUNT + 1)))) / TOP_LIGHTS_COUNT;
    }

    public int[] getLightsStatus() {
        int[] status = new int[LIGHTS_COUNTS];
        for (int i = 0; i < lights.length; i++) {
            status[i] = lights[i].getStatus();
        }
        return status;
    }

    public void refreshLights(int[] status) {
        if (status.length != LIGHTS_COUNTS) {
            return;
        }
        for (int i = 0; i < LIGHTS_COUNTS; i++) {
            LightView light = lights[i];
            light.setToken(status[i]);
            light.setStatus(status[i]);
            light.updateCard(status[i]);
        }
    }

    public void restoreLightsStatus() {
        for (int i = 0; i < lights.length; i++) {
            LightView light = lights[i];
            light.token = 0;
            light.setStatus(LightView.COLOR_GRAY);
        }
    }

    private class LightView extends FrameLayout {

        public static final int COLOR_GRAY = 0;
        public static final int COLOR_GREEN = 1;
        public static final int COLOR_RED = 2;
        private final int[] COLOR_BOX = {COLOR_GRAY, COLOR_GREEN, COLOR_RED};

        private int token = 0;
        private int status = COLOR_BOX[token];

        private TextView card;

        public LightView(Context context) {
            super(context);

            initView(context);
        }

        private void initView(Context context) {
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(CARD_MARGIN, CARD_MARGIN, 0, 0);

            card = new TextView(context);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundResource(R.color.gray);
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickChange();
                }
            });
            addView(card, lp);
        }

        public void setToken(int token) {
            this.token = token;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public void onClickChange() {
            token++;
            if (token > COLOR_BOX.length - 1) {
                token = 0;
            }
            status = COLOR_BOX[token];
            updateCard(status);
        }

        public void updateCard(int status) {
            if (status == COLOR_GRAY) {
                card.setBackgroundResource(R.color.gray);
            } else if (status == COLOR_GREEN) {
                card.setBackgroundResource(R.color.green);
            } else if (status == COLOR_RED) {
                card.setBackgroundResource(R.color.red);
            }
        }
    }

}
