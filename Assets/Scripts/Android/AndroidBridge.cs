using System;
using UnityEngine;

public sealed class AndroidBridge : IDisposable
{
#if UNITY_ANDROID && !UNITY_EDITOR
    readonly AndroidJavaClass bridge = new AndroidJavaClass("com.xrealcanvas.bridge.XrealCanvasBridge");
    AndroidJavaObject Activity => new AndroidJavaClass("com.unity3d.player.UnityPlayer").GetStatic<AndroidJavaObject>("currentActivity");
#endif

    public string InitializeShizuku()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<string>("initializeShizuku", Activity);
#else
        return "editor";
#endif
    }
    public int StartVirtualDisplay()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<int>("startVirtualDisplay", Activity);
#else
        throw new PlatformNotSupportedException();
#endif
    }
    public void OpenAppPicker()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        bridge.CallStatic("openPicker", Activity);
#else
        throw new PlatformNotSupportedException();
#endif
    }
    public string ConsumeSelectedComponent()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<string>("consumeSelectedComponent");
#else
        return null;
#endif
    }
    public string LaunchOnDisplay(int displayId, string component)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<string>("launchOnDisplay", displayId, component);
#else
        throw new PlatformNotSupportedException();
#endif
    }
    public string Tap(int displayId, int x, int y)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<string>("tap", displayId, x, y);
#else
        throw new PlatformNotSupportedException();
#endif
    }
    public byte[] CopyLatestFrame()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<byte[]>("latestFrame");
#else
        return null;
#endif
    }
    public long FrameCount()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<long>("frameCount");
#else
        return 0;
#endif
    }
    public string ShizukuStatus()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<string>("shizukuStatus");
#else
        return "editor";
#endif
    }
    public int ShizukuServerUid()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<int>("shizukuServerUid");
#else
        return -1;
#endif
    }
    public int ShizukuServiceUid()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return bridge.CallStatic<int>("shizukuServiceUid");
#else
        return -1;
#endif
    }
    public void Dispose()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        bridge?.Dispose();
#endif
    }
}
