package com.rtsoft.growtopia;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.gt.launcher.FloatingService;

class AppGLSurfaceView extends GLSurfaceView {
    boolean rendererSet;

    public AppGLSurfaceView(Context context) {
        super(context);
    }

    public AppGLSurfaceView(Context context, SharedActivity sharedActivity) {
        super(context);

        setEGLContextClientVersion(2);
        app = sharedActivity;

        if (SharedActivity.m_editText != null) {
            Log.d(SharedActivity.PackageName, "Setting focus options...");
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        }

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mRenderer = new AppRenderer(sharedActivity);
        setRenderer(mRenderer);
        rendererSet = true;

        setPreserveEGLContextOnPause(false);

        /* Establish whether the "new" class is available to us */

        try {
            WrapSharedMultiTouchInput.checkAvailable(app);
            mMultiTouchClassAvailable = true;
        }
        catch (Throwable th) {
            mMultiTouchClassAvailable = false;
        }
    }

    public void onPause() {
        // super.onPause(); // Something is wrong with this line. - ZTz
        if (!SharedActivity.bIsShuttingDown) {
            nativePause();
        }
    }

    public void onResume() {
        super.onResume();
        if (!SharedActivity.bIsShuttingDown) {
            nativeResume();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public synchronized boolean onTouchEvent(MotionEvent motionEvent) {
        if (app.is_demo) {
            app.showDialog(0);
        }

        FloatingService.updateViewLayout(motionEvent);

        if (mMultiTouchClassAvailable) {
            return WrapSharedMultiTouchInput.OnInput(motionEvent);
        }

        int finger = 0; // Planning ahead for multi touch
        nativeOnTouch(motionEvent.getAction(), motionEvent.getX(), motionEvent.getY(), finger);
        performClick();
        return true;
    }

    AppRenderer mRenderer;

    public static native void nativeOnTouch(int action, float x, float y, int finger);
    private static native void nativePause();
    private static native void nativeResume();
    public SharedActivity app;
    private static boolean mMultiTouchClassAvailable;
}
