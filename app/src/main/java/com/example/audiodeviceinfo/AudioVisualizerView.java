package com.example.audiodeviceinfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AudioVisualizerView extends View {
    private byte[] mBytes;
    private Paint mPaint;
    private Path mPath;
    private int[] mColors;  // 渐变色数组
    private float[] mPositions; // 渐变色位置

    // 配置参数
    private static final int DENSITY = 4;       // 采样密度：数值越大，线条越平滑，细节越少(建议 2-6)
    private static final float AMPLTTUDE_FACTOR = 1.2f;     // 振幅放大倍数，让波形跳动更明显

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mBytes = null;

        // 初始化画笔
        mPaint = new Paint();
        mPaint.setStrokeWidth(2f);
        mPaint.setAntiAlias(true);      // 抗锯齿，线条更平滑
        mPaint.setStyle(Paint.Style.FILL);      // 填充模式，而不是描边

        // 初始化路径
        mPath = new Path();

        // 定义渐变色：从透明->蓝色->紫色->红色->透明
        mColors = new int[]{
                Color.TRANSPARENT,
                Color.parseColor("#00FFFF"),    // 青色
                Color.parseColor("#0000FF"),    // 蓝色
                Color.parseColor("#FF00FF"),    // 紫色
                Color.TRANSPARENT
        };
        // 对应颜色的位置分布 (0.0 - 1.0)
        mPositions = new float[]{0f, 0.2f, 0.5f, 0.8f, 1f};
    }

    /**
     * 当View大小改变时(例如屏幕旋转或布局加载)，重新计算渐变
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 创建线性渐变：从左到右
        LinearGradient shader = new LinearGradient(
                0, 0, w, 0,
                mColors, mPositions,
                Shader.TileMode.CLAMP
        );
        mPaint.setShader(shader);
    }

    /**
     * 接收 Visualizer 传来的原始波形数据
     *
     * @param bytes
     */
    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();   //请求重绘
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        // 1. 准备数据
        mPath.reset();
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2f;    // 垂直中心线

        // 移动路径起点到左侧中心
        mPath.moveTo(0, centerY);

        // 2. 绘制上半部分波形
        for (int i = 0; i < mBytes.length; i += DENSITY) {
            // 计算 X 坐标
            float x = width * i / (float) mBytes.length;

            // 计算振幅(byte范围是-128到127，转为0到255的正数，再减去128得到相对于中心线的偏移)
            // byte 是有符号的，mBytes[i] + 128 将其变为0~255的无符号值
            // 然后-128是为了把它归零到中心
            float rawAmplitude = (float) (mBytes[i] + 128) + 128;

            // 放大振幅让效果更明显
            float amplitude = rawAmplitude * AMPLTTUDE_FACTOR;

            // 计算 Y 坐标(向上偏移)
            float y = centerY - Math.abs(amplitude) * (height / 2f) / 128f;

            // 连接点
            mPath.lineTo(x, y);
        }

        // 连接到右侧中心
        mPath.lineTo(width, centerY);

        // 3.绘制下半部分波形（形成镜像对称）
        // 倒序遍历，确保路径闭合
        for (int i = mBytes.length - 1; i >= 0; i -= DENSITY) {
            float x = width * i / (float) mBytes.length;

            float rawAmplitude = (float) (mBytes[i] + 128) - 128;
            float amplitude = rawAmplitude * AMPLTTUDE_FACTOR;

            // 计算 Y 坐标(向下偏移，与上面对称)
            float y = centerY + Math.abs(amplitude) * (height / 2f) / 128f;

            mPath.lineTo(x, y);
        }

        // 4.闭合路径并绘制
        mPath.close();
        canvas.drawPath(mPath, mPaint);
    }
}
