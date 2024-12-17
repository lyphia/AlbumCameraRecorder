package com.zhongjh.albumcamerarecorder.widget.clickorlongbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.zhongjh.albumcamerarecorder.R;
import com.zhongjh.albumcamerarecorder.camera.listener.ClickOrLongListener;
import com.zhongjh.common.utils.DisplayMetricsUtils;

import java.util.ArrayList;


/**
 * 点击或者长按的按钮
 *
 * @author zhongjh
 */
public class ClickOrLongButton extends View {

    private static final String TAG = "ClickOrLongButton";
    /**
     * 按钮只能点击
     */
    public static final int BUTTON_STATE_ONLY_CLICK = 0x201;
    /**
     * 按钮只能长按
     */
    public static final int BUTTON_STATE_ONLY_LONG_CLICK = 0x202;
    /**
     * 按钮点击或者长按两者都可以
     */
    public static final int BUTTON_STATE_BOTH = 0x203;
    /**
     * 按钮点击即是长按模式
     */
    public static final int BUTTON_STATE_CLICK_AND_HOLD = 0x204;

    /**
     * 满进度
     */
    private static final float FULL_PROGRESS = 1F;
    /**
     * 90度
     */
    private static final int NINETY_DEGREES = 90;
    /**
     * 未启动状态
     */
    private static final int RECORD_NOT_STARTED = 0;
    /**
     * 启动状态
     */
    private static final int RECORD_STARTED = 1;
    /**
     * 结束状态
     */
    private static final int RECORD_ENDED = 2;
    /**
     * 当前为未触摸状态
     * <p>
     * 为了确保整个按钮的逻辑是：按下 - 放开手，按下时+1，松手+1，最后等于2就是整个流程结束
     * 如果中间中断或者重置，那就直接减1，就说明中断流程
     */
    private static final int STEP_NOT_TOUCH = 0;
    /**
     * 当前为按下状态
     * <p>
     * 为了确保整个按钮的逻辑是：按下 - 放开手，按下时+1，松手+1，最后等于2就是整个流程结束
     * 如果中间中断或者重置，那就直接减1，就说明中断流程
     */
    private static final int STEP_ACTION_DOWN = 1;
    /**
     * 当前为松手状态
     * <p>
     * 为了确保整个按钮的逻辑是：按下 - 放开手，按下时+1，松手+1，最后等于2就是整个流程结束
     * 如果中间中断或者重置，那就直接减1，就说明中断流程
     */
    private static final int STEP_ACTION_UP = 2;

    /**
     * 倍数,控制该控件的大小
     */
    private float multiple = 1F;
    /**
     * 录制时间
     */
    private float timeLimitInMils = 10000.0F;
    /**
     * 当前录制位置集，计算时间后的百分比
     */
    private final ArrayList<Float> mCurrentLocation = new ArrayList<>();
    /**
     * 当前录制的节点，以360度为单位
     */
    private Float mCurrentSumNumberDegrees = 0F;
    /**
     * 当前录制的时间点
     */
    private Long mCurrentSumTime = 0L;
    /**
     * 动画的预备时间
     */
    private int mMinDurationAnimation = 1500;
    /**
     * 当前状态的动画预备时间
     */
    private int mMinDurationAnimationCurrent = mMinDurationAnimation;
    /**
     * 记录当前录制的总共多长的时间秒
     */
    private long mRecordedTime;
    private static final float PROGRESS_LIM_TO_FINISH_STARTING_ANIM = 0.1F;
    private int mBoundingBoxSize;
    private int mOutCircleWidth;
    private int mOuterCircleWidthInc;
    private float mInnerCircleRadius;

    private TouchTimeHandler touchTimeHandler;
    private boolean touchable;
    private boolean recordable;

    private Paint centerCirclePaint;
    private Paint outBlackCirclePaint;
    private Paint outMostBlackCirclePaint;
    private float innerCircleRadiusToDraw;
    /**
     * 外圈的画布
     */
    private RectF outMostCircleRect;
    private float outBlackCircleRadius;
    private float outMostBlackCircleRadius;
    private int colorRoundBorder;
    private int colorRecord;
    private int colorWhiteP60;
    private float startAngle270;
    private float percentInDegree;
    private float centerX;
    private float centerY;
    /**
     * 按下去显示的进度外圈
     */
    private Paint processBarPaint;
    /**
     * 静止状态时的外圈
     */
    private Paint outMostWhiteCirclePaint;
    /**
     * 静止状态时的进度外圈
     */
    private Paint outProcessCirclePaint;
    private Paint translucentPaint;
    private int translucentCircleRadius = 0;
    private float outMostCircleRadius;
    private float innerCircleRadiusWhenRecord;
    private long btnPressTime;
    private int outBlackCircleRadiusInc;
    /**
     * 为了确保整个按钮的逻辑从按下-放开手都是流畅的，会用 按下+1，放开手+1，最后等于2的方式执行
     * 如果中间中断或者重置，那就直接减1，就说明中断流程
     */
    private int step;

    /**
     * 当前状态
     */
    private int recordState;

    /**
     * 按钮可执行的功能状态（点击,长按,两者,按钮点击即是长按模式）
     */
    private int mButtonState;


    private final TouchTimeHandler.Task updateUITask = new TouchTimeHandler.Task() {
        @Override
        public void run() {
            // 判断如果是 点击即长按 模式的情况下，判断进度是否>=100
            if (mButtonState == BUTTON_STATE_CLICK_AND_HOLD) {
                if (mRecordedTime / timeLimitInMils >= FULL_PROGRESS) {
                    Log.d(TAG, "满足100" + (mRecordedTime / timeLimitInMils >= FULL_PROGRESS));
                    step++;
                    refreshView();
                    return;
                }
            }
            if (!mCurrentLocation.isEmpty()) {
                // 当处于分段录制模式并且有分段数据的时候，关闭启动前奏
                mMinDurationAnimationCurrent = 0;
            }
            long timeLapse = System.currentTimeMillis() - btnPressTime;
            mRecordedTime = (timeLapse - mMinDurationAnimationCurrent);
            mRecordedTime = mRecordedTime + mCurrentSumTime;
            float percent = mRecordedTime / timeLimitInMils;
            Log.d(TAG, "mCurrentSumTime " + mCurrentSumTime);
            Log.d(TAG, "mRecordedTime " + mRecordedTime);
            if (!mActionDown && timeLapse >= 1) {
                boolean actionDown = mClickOrLongListener != null && (mButtonState == BUTTON_STATE_ONLY_CLICK || mButtonState == BUTTON_STATE_BOTH);
                if (actionDown) {
                    mClickOrLongListener.actionDown();
                    mActionDown = true;
                }
            }

            startAnimation(timeLapse, percent);
        }
    };

    /**
     * 判断是否超过预备时间就开始具体动画
     *
     * @param timeLapse 当前时间 - 点击的那一刻时间 = 点击后度过了多久
     * @param percent   当前百分比
     */
    private void startAnimation(long timeLapse, float percent) {
        Log.d(TAG, "startAnimation timeLapse " + timeLapse);
        Log.d(TAG, "startAnimation mMinDurationAnimationCurrent " + mMinDurationAnimationCurrent);
        if (timeLapse >= mMinDurationAnimationCurrent) {
            synchronized (ClickOrLongButton.this) {
                if (recordState == RECORD_NOT_STARTED) {
                    setRecordState(RECORD_STARTED);
                    if (mClickOrLongListener != null) {
                        Log.d(TAG, "timeLapse " + timeLapse);
                        mClickOrLongListener.onLongClick();
                        // 如果禁止点击，那么就轮到长按触发actionDown
                        if (!mActionDown && mClickOrLongListener != null && mButtonState == BUTTON_STATE_ONLY_LONG_CLICK) {
                            // 如果禁止点击也不能触发该事件
                            mClickOrLongListener.actionDown();
                            mActionDown = true;
                        }
                    }
                }
            }
            if (!recordable) {
                return;
            }
            centerCirclePaint.setColor(colorRecord);
            outMostWhiteCirclePaint.setColor(colorRoundBorder);
            percentInDegree = (360.0F * percent);
            if (!mCurrentLocation.isEmpty() || (timeLapse - mMinDurationAnimationCurrent) >= mMinDurationAnimationCurrent) {
                mCurrentSumNumberDegrees = percentInDegree;
            }

            Log.d(TAG, "timeLapse:" + timeLapse);
            Log.d(TAG, "percent:" + percent);
            Log.d(TAG, "percentInDegree:" + percentInDegree);

            if (percent <= FULL_PROGRESS) {
                if (percent <= PROGRESS_LIM_TO_FINISH_STARTING_ANIM) {
                    float calPercent = percent / PROGRESS_LIM_TO_FINISH_STARTING_ANIM;
                    float outIncDis = outBlackCircleRadiusInc * calPercent;
                    float curOutCircleWidth = mOutCircleWidth + mOuterCircleWidthInc * calPercent;
                    processBarPaint.setStrokeWidth(curOutCircleWidth);
                    outProcessCirclePaint.setStrokeWidth(curOutCircleWidth);
                    outMostWhiteCirclePaint.setStrokeWidth(curOutCircleWidth);
                    outBlackCircleRadius = (outMostCircleRadius + outIncDis - curOutCircleWidth / 2.0F);
                    outMostBlackCircleRadius = (curOutCircleWidth / 2.0F + (outMostCircleRadius + outIncDis));
                    outMostCircleRect = new RectF(centerX - outMostCircleRadius - outIncDis, centerY - outMostCircleRadius - outIncDis, centerX + outMostCircleRadius + outIncDis, centerY + outMostCircleRadius + outIncDis);
                    translucentCircleRadius = (int) (outIncDis + outMostCircleRadius);
                    innerCircleRadiusToDraw = calPercent * innerCircleRadiusWhenRecord;
                }
                invalidate();
            } else {
                Log.d(TAG, "满足100" + (mRecordedTime / timeLimitInMils >= FULL_PROGRESS));
                step++;
                refreshView();
            }
        }
    }

    public ClickOrLongButton(Context paramContext) {
        super(paramContext);
        init(null);
    }

    public ClickOrLongButton(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init(paramAttributeSet);
    }

    public ClickOrLongButton(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramAttributeSet);
    }

    private void init(AttributeSet paramAttributeSet) {
        if (getContext() == null || getContext().getTheme() == null) {
            return;
        }
        // 调取样式中的颜色
        TypedArray arrayRoundBorder = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.click_long_button_round_border});
        TypedArray arrayInnerCircleInOperation = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.click_long_button_inner_circle_in_operation});
        TypedArray arrayInnerCircleNoOperation = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.click_long_button_inner_circle_no_operation});
        TypedArray arrayClickOrLongButtonStyle = getContext().obtainStyledAttributes(paramAttributeSet, R.styleable.ClickOrLongButton);
        // 计算出倍数
        int size = arrayClickOrLongButtonStyle.getInt(R.styleable.ClickOrLongButton_size, 100);
        multiple = (float) size / 100;

        int defaultRoundBorderColor = ResourcesCompat.getColor(
                getResources(), R.color.click_long_button_round_border,
                getContext().getTheme());
        int defaultInnerCircleInOperationColor = ResourcesCompat.getColor(
                getResources(), R.color.click_long_button_inner_circle_in_operation,
                getContext().getTheme());
        int defaultInnerCircleNoOperationColor = ResourcesCompat.getColor(
                getResources(), R.color.click_long_button_inner_circle_no_operation,
                getContext().getTheme());

        touchable = recordable = true;
        // 整块
        mBoundingBoxSize = DisplayMetricsUtils.dip2px(100.0F * multiple);
        // 外线宽度
        mOutCircleWidth = DisplayMetricsUtils.dip2px(2.3F * multiple);
        mOuterCircleWidthInc = DisplayMetricsUtils.dip2px(4.3F * multiple);
        mInnerCircleRadius = DisplayMetricsUtils.dip2px(32.0F * multiple);

        TypedArray arrayInnerCircleNoOperationInterval = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.click_button_inner_circle_in_operation_interval});
        int defaultInnerCircleNoOperationColorInterval = ResourcesCompat.getColor(
                getResources(), R.color.click_button_inner_circle_no_operation_interval,
                getContext().getTheme());

        colorRecord = arrayInnerCircleInOperation.getColor(0, defaultInnerCircleInOperationColor);
        colorRoundBorder = arrayRoundBorder.getColor(0, defaultRoundBorderColor);
        colorWhiteP60 = arrayInnerCircleNoOperation.getColor(0, defaultInnerCircleNoOperationColor);

        initProcessBarPaint();
        initOutCircle(arrayInnerCircleNoOperationInterval, defaultInnerCircleNoOperationColorInterval);
        initCenterCircle();
        // 状态为两者都可以
        mButtonState = BUTTON_STATE_BOTH;

        arrayRoundBorder.recycle();
        arrayInnerCircleInOperation.recycle();
        arrayInnerCircleNoOperation.recycle();
        arrayClickOrLongButtonStyle.recycle();

    }

    /**
     * 初始化内圈操作中样式
     */
    private void initProcessBarPaint() {
        processBarPaint = new Paint();
        processBarPaint.setColor(colorRecord);
        processBarPaint.setAntiAlias(true);
        processBarPaint.setStrokeWidth(mOutCircleWidth);
        processBarPaint.setStyle(Style.STROKE);
        processBarPaint.setStrokeCap(Cap.ROUND);
    }

    /**
     * 初始化外圈样式
     */
    private void initOutCircle(TypedArray arrayInnerCircleNoOperationInterval, int defaultInnerCircleNoOperationColorInterval) {
        int colorInterval = arrayInnerCircleNoOperationInterval.getColor(0, defaultInnerCircleNoOperationColorInterval);

        outMostWhiteCirclePaint = new Paint();
        outMostWhiteCirclePaint.setColor(colorRoundBorder);
        outMostWhiteCirclePaint.setAntiAlias(true);
        outMostWhiteCirclePaint.setStrokeWidth(mOutCircleWidth);
        outMostWhiteCirclePaint.setStyle(Style.STROKE);

        outProcessCirclePaint = new Paint();
        outProcessCirclePaint.setColor(colorRecord);
        outProcessCirclePaint.setAntiAlias(true);
        outProcessCirclePaint.setStrokeWidth(mOutCircleWidth);
        outProcessCirclePaint.setStyle(Style.STROKE);
    }

    /**
     * 初始化内圈未操作中样式
     */
    private void initCenterCircle() {
        int colorBlackP40 = ContextCompat.getColor(getContext(), R.color.black_forty_percent);
        int colorBlackP80 = ContextCompat.getColor(getContext(), R.color.black_eighty_percent);
        int colorTranslucent = ContextCompat.getColor(getContext(), R.color.circle_shallow_translucent_bg);

        centerCirclePaint = new Paint();
        centerCirclePaint.setColor(colorWhiteP60);
        centerCirclePaint.setAntiAlias(true);
        centerCirclePaint.setStyle(Style.FILL_AND_STROKE);
        outBlackCirclePaint = new Paint();
        outBlackCirclePaint.setColor(colorBlackP40);
        outBlackCirclePaint.setAntiAlias(true);
        outBlackCirclePaint.setStyle(Style.STROKE);
        outBlackCirclePaint.setStrokeWidth(1.0F);
        outMostBlackCirclePaint = new Paint();
        outMostBlackCirclePaint.setColor(colorBlackP80);
        outMostBlackCirclePaint.setAntiAlias(true);
        outMostBlackCirclePaint.setStyle(Style.STROKE);
        outMostBlackCirclePaint.setStrokeWidth(1.0F);
        translucentPaint = new Paint();
        translucentPaint.setColor(colorTranslucent);
        translucentPaint.setAntiAlias(true);
        translucentPaint.setStyle(Style.FILL_AND_STROKE);
        centerX = (mBoundingBoxSize / 2f);
        centerY = (mBoundingBoxSize / 2f);
        outMostCircleRadius = DisplayMetricsUtils.dip2px(37.0F * multiple);
        outBlackCircleRadiusInc = DisplayMetricsUtils.dip2px(7.0F * multiple);
        innerCircleRadiusWhenRecord = DisplayMetricsUtils.dip2px(35.0F * multiple);
        innerCircleRadiusToDraw = mInnerCircleRadius;
        outBlackCircleRadius = (outMostCircleRadius - mOutCircleWidth / 2.0F);
        outMostBlackCircleRadius = (outMostCircleRadius + mOutCircleWidth / 2.0F);
        startAngle270 = 270.0F;
        percentInDegree = 0.0F;
        outMostCircleRect = new RectF(centerX - outMostCircleRadius, centerY - outMostCircleRadius, centerX + outMostCircleRadius, centerY + outMostCircleRadius);
        touchTimeHandler = new TouchTimeHandler(Looper.getMainLooper(), updateUITask);
    }

    /**
     * 销毁事件
     */
    public void onDestroy() {
        touchTimeHandler.clearMsg();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBoundingBoxSize, mBoundingBoxSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, translucentCircleRadius, translucentPaint);

        //center white-p40 circle  中心点+半径32，所以直接64就是内圈的宽高度了
        canvas.drawCircle(centerX, centerY, innerCircleRadiusToDraw, centerCirclePaint);

        // 静止状态时的外圈
        canvas.drawArc(outMostCircleRect, startAngle270, 360F, false, outMostWhiteCirclePaint);

        // 点击时的外圈进度
        canvas.drawArc(outMostCircleRect, startAngle270, percentInDegree, false, processBarPaint);

        // 静止状态时的外圈进度
        canvas.drawArc(outMostCircleRect, startAngle270, mCurrentSumNumberDegrees, false, outProcessCirclePaint);
        Log.d(TAG, "onDraw percentInDegree" + percentInDegree);
        Log.d(TAG, "onDraw mCurrentSumNumberDegrees" + mCurrentSumNumberDegrees);


        canvas.drawCircle(centerX, centerY, outBlackCircleRadius, outBlackCirclePaint);
        canvas.drawCircle(centerX, centerY, outMostBlackCircleRadius, outMostBlackCirclePaint);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mButtonState != BUTTON_STATE_CLICK_AND_HOLD) {
                    if (mRecordedTime / timeLimitInMils >= FULL_PROGRESS) {
                        // 进度已满,不执行任何动作
                        return true;
                    }
                    if (mCurrentSumTime / timeLimitInMils >= FULL_PROGRESS) {
                        // 进度已满,不执行任何动作
                        return true;
                    }
                    // 判断是否禁用模式
                    if (!touchable) {
                        mClickOrLongListener.onBanClickTips();
                        return true;
                    }
                    Log.d(TAG, "onTouchEvent: down");
                    step = STEP_ACTION_DOWN;
                    // 是否支持长按
                    boolean longClick = mClickOrLongListener != null
                            && (mButtonState == BUTTON_STATE_ONLY_LONG_CLICK ||
                            mButtonState == BUTTON_STATE_BOTH);
                    if (longClick) {
                        Log.d(TAG, "onTouchEvent: startTicking");
                        startTicking();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mButtonState == BUTTON_STATE_CLICK_AND_HOLD) {
                    // 点击即长按模式
                    if (recordState != RECORD_STARTED) {
                        // 判断是否禁用模式
                        if (!touchable) {
                            mClickOrLongListener.onBanClickTips();
                            return true;
                        }
                        // 未启动状态，即立刻启动长按动画
                        step = STEP_ACTION_DOWN;
                        startTicking();
                        mClickOrLongListener.onClickStopTips();
                    } else {
                        // 已经启动状态，刷新view执行事件
                        step++;
                        refreshView();
                    }
                } else {
                    // 其他模式
                    step++;
                    Log.d(TAG, "onTouchEvent: up");
                    refreshView();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mButtonState != BUTTON_STATE_CLICK_AND_HOLD) {
                    if (mRecordedTime / timeLimitInMils >= FULL_PROGRESS) {
                        step++;
                        Log.d(TAG, "onTouchEvent: move");
                        refreshView();
                        return true;
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 刷新view
     */
    public void refreshView() {
        Log.d(TAG, "reset: " + recordState);
        synchronized (ClickOrLongButton.this) {
            if (recordState == RECORD_STARTED) {
                if (mClickOrLongListener != null && step == STEP_ACTION_UP) {
                    // 回调录制结束
                    if (mRecordedTime / timeLimitInMils >= FULL_PROGRESS) {
                        mClickOrLongListener.onLongClickFinish();
                    } else {
                        mClickOrLongListener.onLongClickEnd(mRecordedTime);
                    }
                }
                setRecordState(RECORD_ENDED);
            } else if (recordState == RECORD_ENDED) {
                // 回到初始状态
                setRecordState(RECORD_NOT_STARTED);
            } else {
                // 如果只支持长按事件则不触发
                if (mClickOrLongListener != null &&
                        mButtonState != BUTTON_STATE_ONLY_LONG_CLICK &&
                        step == STEP_ACTION_UP) {
                    // 拍照
                    mClickOrLongListener.onClick();
                }
            }
        }
        reset();
    }

    /**
     * 重置
     */
    public void reset() {
        resetCommon();
        mCurrentSumTime = 0L;
        mCurrentLocation.clear();
        mCurrentSumNumberDegrees = 0F;
        invalidate();
    }

    /**
     * 中断当前操作
     */
    public void breakOff() {
        resetCommon();
        invalidate();
    }

    private void resetCommon() {
        step = STEP_NOT_TOUCH;
        mActionDown = false;
        touchTimeHandler.clearMsg();
        percentInDegree = 0.0F;
        mRecordedTime = 0;
        centerCirclePaint.setColor(colorWhiteP60);
        outMostWhiteCirclePaint.setColor(colorRoundBorder);
        innerCircleRadiusToDraw = mInnerCircleRadius;
        outMostCircleRect = new RectF(centerX - outMostCircleRadius, centerY - outMostCircleRadius, centerX + outMostCircleRadius, centerY + outMostCircleRadius);
        translucentCircleRadius = 0;
        processBarPaint.setStrokeWidth(mOutCircleWidth);
        outProcessCirclePaint.setStrokeWidth(mOutCircleWidth);
        outMostWhiteCirclePaint.setStrokeWidth(mOutCircleWidth);
        outBlackCircleRadius = (outMostCircleRadius - mOutCircleWidth / 2.0F);
        outMostBlackCircleRadius = (outMostCircleRadius + mOutCircleWidth / 2.0F);
    }

    public boolean isTouchable() {
        return touchable;
    }

    public boolean isRecordable() {
        return recordable;
    }

    public void setRecordable(boolean recordable) {
        this.recordable = recordable;
    }

    public void setTouchable(boolean touchable) {
        this.touchable = touchable;
    }

    private void startTicking() {
        synchronized (ClickOrLongButton.this) {
            if (recordState != RECORD_NOT_STARTED) {
                setRecordState(RECORD_NOT_STARTED);
            }
        }
        btnPressTime = System.currentTimeMillis();
        touchTimeHandler.sendLoopMsg(0L, 16L);
    }

    /**
     * 数据设置成适合当前圆形
     * <p>
     * // 计算方式1：270至360是一个初始点，类似0-90
     * // 计算方式2: 所以如果是小于90点，就直接+270
     * // 计算方式3：如果大于等于90点，就直接-90
     *
     * @return numberDegrees
     */
    private float getNumberDegrees(float numberDegrees) {
        if (numberDegrees >= NINETY_DEGREES) {
            numberDegrees = numberDegrees - 90;
        } else {
            numberDegrees = numberDegrees + 270;
        }
        return numberDegrees;
    }

    /**
     * 按钮回调接口
     */
    private ClickOrLongListener mClickOrLongListener;
    /**
     * 判断是否已经调用过isActionDwon,结束后重置此值
     */
    private boolean mActionDown;

    // region 对外方法

    /**
     * 设置最长录制时间
     *
     * @param duration 时间
     */
    public void setDuration(int duration) {
        timeLimitInMils = duration;
    }

    /**
     * 长按准备时间
     * 长按达到duration时间后，才开启录制
     *
     * @param duration 时间
     */
    public void setReadinessDuration(int duration) {
        mMinDurationAnimation = duration;
        mMinDurationAnimationCurrent = mMinDurationAnimation;
    }


    /**
     * 设置当前已录制的时间，用于分段录制
     */
    public void setCurrentTime(ArrayList<Long> currentTimes) {
        mCurrentLocation.clear();
        mCurrentSumNumberDegrees = 0F;
        mCurrentSumTime = 0L;
        // 当前录制时间集
        // 计算百分比红点
        for (int i = 0; i < currentTimes.size(); i++) {
            // 获取当前时间占比
            float percent = currentTimes.get(i) / timeLimitInMils;
            // 根据360度，以这个占比计算是具体多少度
            float numberDegrees = percent * 360;
            // 数据设置规范,适合当前圆形
            mCurrentLocation.add(getNumberDegrees(numberDegrees));
            mCurrentSumNumberDegrees = numberDegrees;
            mCurrentSumTime = currentTimes.get(i);
            mRecordedTime = currentTimes.get(i);
            Log.d(TAG, "setCurrentTime mCurrentSumTime " + mCurrentSumTime);
            Log.d(TAG, "setCurrentTime mCurrentSumNumberDegrees " + mCurrentSumNumberDegrees);
        }
        invalidate();
    }

    /**
     * 设置回调接口
     *
     * @param clickOrLongListener 回调接口
     */
    public void setRecordingListener(ClickOrLongListener clickOrLongListener) {
        this.mClickOrLongListener = clickOrLongListener;
    }

    /**
     * 设置按钮功能（点击和长按）
     *
     * @param buttonStateBoth {@link com.zhongjh.albumcamerarecorder.widget.clickorlongbutton.ClickOrLongButton#BUTTON_STATE_ONLY_CLICK 只能点击
     * @link com.zhongjh.albumcamerarecorder.widget.clickorlongbutton.ClickOrLongButton#BUTTON_STATE_ONLY_LONG_CLICK 只能长按
     * @link com.zhongjh.albumcamerarecorder.widget.clickorlongbutton.ClickOrLongButton#BUTTON_STATE_BOTH 两者皆可
     * }
     */
    public void setButtonFeatures(int buttonStateBoth) {
        this.mButtonState = buttonStateBoth;
        if (buttonStateBoth == BUTTON_STATE_CLICK_AND_HOLD) {
            mMinDurationAnimationCurrent = 0;
        }
    }

    /**
     * 分段录制回滚上一段
     * 一般用于录制时出现异常
     */
    public void selectionRecordRollBack() {
        invalidate();
    }

    /**
     * 重置状态
     */
    public void resetState() {
        // 回到初始状态
        setRecordState(RECORD_NOT_STARTED);
        // 预备时间也恢复到初始设置时的时间
        mMinDurationAnimationCurrent = mMinDurationAnimation;
        Log.d(TAG, "resetState");
    }

    private void setRecordState(int recordState) {
        this.recordState = recordState;
        Log.d(TAG, "setRecordState: " + recordState);
    }

    // endregion

}
