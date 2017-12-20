package com.zf.sync;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.IDisplayManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.Surface;

import java.lang.reflect.Method;

public class SurfaceControlVirtualDisplayFactory implements VirtualDisplayFactory {
    private static final String TAG = "SCVDF";
    private Rect displayRect;
    private Point displaySize = getCurrentDisplaySize();

    private static Point getCurrentDisplaySize() {
        return getCurrentDisplaySize(true);
    }

    static Point getCurrentDisplaySize(boolean rotate) {
        try {
            @SuppressLint("PrivateApi")
            Method getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            Point displaySize = new Point();
            IWindowManager windowManager;
            int rotation = 0;
            if (VERSION.SDK_INT >= 18) {
                windowManager = Stub.asInterface((IBinder) getServiceMethod.invoke(null, "window"));
                if (windowManager != null) {
                    windowManager.getInitialDisplaySize(0, displaySize);
                    rotation = windowManager.getRotation();
                }
            } else if (VERSION.SDK_INT == 17) {
                IDisplayManager displayManager = IDisplayManager.Stub.asInterface((IBinder) getServiceMethod.invoke(null, "display"));
                if (displayManager != null) {
                    DisplayInfo displayInfo = displayManager.getDisplayInfo(0);
                    displaySize.x = (Integer) DisplayInfo.class.getDeclaredField("logicalWidth").get(displayInfo);
                    displaySize.y = (Integer) DisplayInfo.class.getDeclaredField("logicalHeight").get(displayInfo);
                    rotation = (Integer) DisplayInfo.class.getDeclaredField("rotation").get(displayInfo);
                }
            } else {
                windowManager = Stub.asInterface((IBinder) getServiceMethod.invoke(null, "window"));
                if (windowManager != null) {
                    windowManager.getRealDisplaySize(displaySize);
                    rotation = windowManager.getRotation();
                }
            }

            if ((rotate && rotation == 1) || rotation == 3) {
                int swap = displaySize.x;
                //noinspection SuspiciousNameCombination
                displaySize.x = displaySize.y;
                displaySize.y = swap;
            }
            return displaySize;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int dpi, int flags, Surface surface, Handler handler) {
        try {
            Class surfaceControlClass = Class.forName("android.view.SurfaceControl");
            Class cls = surfaceControlClass;
            final IBinder token = (IBinder) cls.getDeclaredMethod("createDisplay", new Class[]{String.class, Boolean.TYPE}).invoke(null, name, Boolean.FALSE);
            cls = surfaceControlClass;
            Method setDisplaySurfaceMethod = cls.getDeclaredMethod("setDisplaySurface", IBinder.class, Surface.class);
            cls = surfaceControlClass;
            final Method setDisplayProjectionMethod = cls.getDeclaredMethod("setDisplayProjection", IBinder.class, Integer.TYPE, Rect.class, Rect.class);
            cls = surfaceControlClass;
            Method setDisplayLayerStackMethod = cls.getDeclaredMethod("setDisplayLayerStack", IBinder.class, Integer.TYPE);
            final Method openTransactionMethod = surfaceControlClass.getDeclaredMethod("openTransaction");
            final Method closeTransactionMethod = surfaceControlClass.getDeclaredMethod("closeTransaction");
            @SuppressLint("PrivateApi")
            final Method getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            this.displayRect = new Rect(0, 0, width, height);
            Rect layerStackRect = new Rect(0, 0, this.displaySize.x, this.displaySize.y);
            openTransactionMethod.invoke(null);
            setDisplaySurfaceMethod.invoke(null, token, surface);
            setDisplayProjectionMethod.invoke(null, token, 0, layerStackRect, this.displayRect);
            setDisplayLayerStackMethod.invoke(null, token, 0);
            closeTransactionMethod.invoke(null);
            cls = surfaceControlClass;
            final Method destroyDisplayMethod = cls.getDeclaredMethod("destroyDisplay", IBinder.class);

            return new VirtualDisplay() {
                IRotationWatcher watcher;
                IWindowManager wm = Stub.asInterface((IBinder) getServiceMethod.invoke(null, "window"));

                public void release() {
                    Log.i(SurfaceControlVirtualDisplayFactory.TAG, "VirtualDisplay released");
                    this.wm = null;
                    this.watcher = null;
                    try {
                        destroyDisplayMethod.invoke(null, token);
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            };
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public Point getDisplaySize() {
        return new Point(this.displaySize);
    }

    public Rect getDisplayRect() {
        return this.displayRect;
    }

    public void release() {
        Log.i(TAG, "factory released");
    }
}