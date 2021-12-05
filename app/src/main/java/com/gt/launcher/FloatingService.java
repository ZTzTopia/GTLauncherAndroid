package com.gt.launcher;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.rtsoft.growtopia.AppRenderer;
import com.rtsoft.growtopia.SharedActivity;

public class FloatingService extends Service {
    public static FloatingService   mFloatingService;
    public static FrameLayout       mFrameLayout;
    public static View              mFloatingWidget;
    public static WindowManager     mWindowManager;
    public static RelativeLayout    mFloatingWidgetContent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    @Override
    public void onCreate() {
        /*
         * I know the keyboard will not showing if u not press edittext.
         * And yes i think i need to change the screen size if we in floating mode, so
         * the text not bugged.
         */

        mFloatingService = this;
        super.onCreate();

        // The root frame layout of floating window and start floating window button.
        mFrameLayout = new FrameLayout(this);
        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SharedActivity.app.aww(false);
                return true;
            }
        });

        mFloatingWidget = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);
        mFloatingWidget.setVisibility(View.GONE);
        mFloatingWidget.setAlpha(0.75f);

        // Button to start floating window.
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        imageView.setVisibility(View.VISIBLE);
        imageView.setAlpha(0.75f);

        int applyDimension = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32,
                getResources().getDisplayMetrics());
        imageView.getLayoutParams().height = applyDimension;
        imageView.getLayoutParams().width = applyDimension;

        imageView.setImageResource(R.drawable.ic_baseline_arrow_back_24);
        imageView.setBackgroundResource(R.drawable.round_border_black);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFloatingWidget.getVisibility() == View.VISIBLE) {
                    // Hide the floating window.
                    updateWindowManagerParams(true, false, false);
                    mFloatingWidget.setVisibility(View.GONE);

                    // Set the button border to visible.
                    imageView.setBackgroundResource(R.drawable.round_border_black);

                    // Content stuff.
                    mFloatingWidgetContent.removeView(SharedActivity.app.mGLView);
                    mFloatingWidgetContent.removeView(SharedActivity.m_editTextRoot);
                    SharedActivity.app.mViewGroup.addView(SharedActivity.app.mGLView);
                    SharedActivity.app.mViewGroup.addView(SharedActivity.m_editTextRoot);

                    // Reload the surface.
                    SharedActivity.app.mGLView.onPause();
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SharedActivity.app.mGLView.onResume();
                        }
                    }, 800);

                    SharedActivity.app.isInFloatingMode = false;
                }
                else {
                    // Show the floating window.
                    updateWindowManagerParams(false, false, false);
                    mFloatingWidget.setVisibility(View.VISIBLE);

                    // Set the button border to transparent.
                    imageView.setBackgroundResource(R.drawable.round_border_transparent);

                    // Content stuff.
                    SharedActivity.app.mViewGroup.removeView(SharedActivity.app.mGLView);
                    SharedActivity.app.mViewGroup.removeView(SharedActivity.m_editTextRoot);
                    mFloatingWidgetContent.addView(SharedActivity.app.mGLView);
                    mFloatingWidgetContent.addView(SharedActivity.m_editTextRoot);

                    // Reload the surface.
                    SharedActivity.app.mGLView.onPause();
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SharedActivity.app.mGLView.onResume();
                        }
                    }, 800);

                    SharedActivity.app.isInFloatingMode = true;
                }
            }
        });

        mFrameLayout.addView(mFloatingWidget);
        mFrameLayout.addView(imageView);

        // Set floating window params.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int) (32 * 2.5f),
                (int) (32 * 2.5f),
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_PHONE :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 16; // Initial Position of window
        params.y = 16; // Initial Position of window

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFrameLayout, params);

        // Floating window content.
        mFloatingWidgetContent = mFloatingWidget.findViewById(R.id.id_floating_widget_content);

        // Move floating window while drag at title bar.
        RelativeLayout floatingWidgetTitleBar = mFloatingWidget.findViewById(R.id.id_floating_widget_title_bar);
        floatingWidgetTitleBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                updateViewLayout(event);
                return true;
            }
        });

        ImageView floatingCloseButton = mFloatingWidget.findViewById(R.id.id_floating_close_button);
        floatingCloseButton.setAlpha(0.75f);
        floatingCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
                if (mFloatingWidget != null) {
                    mWindowManager.removeView(mFrameLayout);
                }

                AppRenderer.finishApp();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        if (mFloatingWidget != null) {
            mWindowManager.removeView(mFrameLayout);
        }
    }

    public void updateWindowManagerParams(boolean isHideMode, boolean isKeyboardShown, boolean z) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFrameLayout.getLayoutParams();

        if (z) {
            if (isKeyboardShown) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            }
            else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            }

            mWindowManager.updateViewLayout(mFrameLayout, params);
            return;
        }

        if (isHideMode) {
            params.x = 16; // Reset Position of window
            params.y = 16; // Reset Position of window
            params.width = (int) (32 * 2.5f);
            params.height = (int) (32 * 2.5f);
        }
        else {
            // Calculate the size of floating window.
            DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
            int widthPixels = displayMetrics.widthPixels;
            int heightPixels = displayMetrics.heightPixels;
            if (widthPixels < heightPixels) {
                widthPixels = displayMetrics.heightPixels;
                heightPixels = displayMetrics.widthPixels;
            }

            widthPixels = (int) (((float) widthPixels) / 2.5f);
            heightPixels = (int) (((float) heightPixels) / 2.0f);

            params.x = 16; // Reset Position of window
            params.y = 16; // Reset Position of window
            params.width = widthPixels;
            params.height = heightPixels;
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        mWindowManager.removeView(mFrameLayout);
        mWindowManager.addView(mFrameLayout, params);
    }

    private static int initialX = 0;
    private static int initialY = 0;
    private static float initialTouchX = 0;
    private static float initialTouchY = 0;

    public static void updateViewLayout(MotionEvent event) {
        if (mFrameLayout == null || mFloatingWidget.getVisibility() != View.VISIBLE) {
            return;
        }

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFrameLayout.getLayoutParams();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                mWindowManager.updateViewLayout(mFrameLayout, params);
                break;
            default:
                break;
        }
    }
}
