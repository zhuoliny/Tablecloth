package edu.buffalo.tablecloth.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.tablecloth.Application;
import edu.buffalo.tablecloth.R;
import edu.buffalo.tablecloth.listenner.OrderListenner;

public class OrderLayout extends GridLayout {
    public int orderNumber = 0;
    private int cardSize;

    private Context mContext;
    private OrderListenner mOrderListenner;
    private List<OrderView> orderViews = new ArrayList<>();

    public OrderLayout(Context context) {
        this(context, null);
    }

    public OrderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OrderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.mContext = context;
        initView();
    }

    private void initView() {
        cardSize = getCardSize(Application.width, Application.height);
    }

    private int getCardSize(int width, int height) {
        return Math.min(width, height) / 10;
    }

    public void setmOrderListenner(OrderListenner orderListenner) {
        this.mOrderListenner = orderListenner;
    }

    public void addOrderView() {
        OrderView orderView = new OrderView(mContext);
        addView(orderView, cardSize, cardSize);
        orderViews.add(orderView);
        orderNumber++;
    }

    public void removeOrderView(int location) {
        if (location < 0 || location > orderViews.size() - 1) {
            return;
        }
        OrderView orderView = orderViews.remove(location);
        removeView(orderView);
        orderNumber--;
        sortOrderViews();
    }

    private void sortOrderViews() {
        for (int i = 0; i < orderViews.size(); i++) {
            ((TextView) orderViews.get(i).getChildAt(0)).setText(i + "");
        }
    }

    public void saveLightsStatus(int[] status, int location) {
        if (orderViews.isEmpty() && orderViews.size() == 0) {
            return;
        }
        orderViews.get(location).setStatus(status);
    }

    private class OrderView extends FrameLayout {

        private static final int STATUS_COUNTS = 96;
        private final int CARD_MARGIN = 2;

        private int[] status = new int[STATUS_COUNTS];

        private TextView card;

        public OrderView(Context context) {
            super(context);

            initView(context);
        }

        private void initView(Context context) {
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(CARD_MARGIN, CARD_MARGIN, 0, 0);

            card = new TextView(context);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundResource(R.color.yellow);
            card.setText(orderNumber + "");
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickChange();
                }
            });
            addView(card, lp);
        }

        public void onClickChange() {
            mOrderListenner.showOrder(getStatus(), Integer.parseInt(card.getText().toString()));
        }

        public int[] getStatus() {
            return status;
        }

        public void setStatus(int[] status) {
            this.status = status;
        }
    }
}
