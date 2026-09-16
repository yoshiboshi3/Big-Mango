using UnityEngine;

public sealed class ProofController : MonoBehaviour
{
    public SpatialWindowController Window;
    public HostedAppFrameSource Frames;
    AndroidBridge bridge;
    int displayId = -1;
    string selected;
    float x, y, z = 2f, scale = 1f;
    string status = "starting";

    void Start()
    {
        bridge = new AndroidBridge();
        status = bridge.InitializeShizuku();
        if (Frames != null) Frames.Initialize(bridge);
        Apply();
    }

    void Update()
    {
        if (string.IsNullOrEmpty(selected))
        {
            var picked = bridge.ConsumeSelectedComponent();
            if (!string.IsNullOrEmpty(picked))
            {
                selected = picked;
                if (displayId < 0) displayId = bridge.StartVirtualDisplay();
                status = bridge.LaunchOnDisplay(displayId, selected);
                Debug.Log("XREALCanvasProof launch: " + status);
            }
        }
    }

    void Apply() => Window?.Apply(new SpatialWindowState(x, y, z, scale));

    void OnGUI()
    {
        const float w = 420f;
        GUILayout.BeginArea(new Rect(20, 20, w, Screen.height - 40), GUI.skin.box);
        GUILayout.Label("SCOTTISH TARGE — arbitrary app spatial proof");
        GUILayout.Label("Shizuku: " + bridge?.ShizukuStatus());
        GUILayout.Label($"UID server/service: {bridge?.ShizukuServerUid()} / {bridge?.ShizukuServiceUid()}");
        GUILayout.Label("Display: " + displayId);
        GUILayout.Label("App: " + (selected ?? "none"));
        if (GUILayout.Button("ADD WINDOW", GUILayout.Height(54))) bridge?.OpenAppPicker();

        GUILayout.Label($"X {x:0.00}"); x = GUILayout.HorizontalSlider(x, -2f, 2f);
        GUILayout.Label($"Y {y:0.00}"); y = GUILayout.HorizontalSlider(y, -1.5f, 1.5f);
        GUILayout.Label($"Distance {z:0.00}"); z = GUILayout.HorizontalSlider(z, 0.5f, 8f);
        GUILayout.Label($"Scale {scale:0.00}"); scale = GUILayout.HorizontalSlider(scale, 0.25f, 4f);
        Apply();
        if (GUILayout.Button("CENTER")) { x = y = 0; z = 2f; scale = 1f; Apply(); }
        GUILayout.Space(12);
        GUILayout.Label("Touch hosted app:");
        var touchRect = GUILayoutUtility.GetRect(w - 30, 160);
        GUI.Box(touchRect, "tap here → virtual display");
        if (Event.current.type == EventType.MouseDown && touchRect.Contains(Event.current.mousePosition) && displayId >= 0)
        {
            float u = Mathf.InverseLerp(touchRect.xMin, touchRect.xMax, Event.current.mousePosition.x);
            float v = Mathf.InverseLerp(touchRect.yMin, touchRect.yMax, Event.current.mousePosition.y);
            var p = CoordinateMapper.Map(u, v, 960, 540);
            status = bridge.Tap(displayId, p.x, p.y);
            Event.current.Use();
        }
        GUILayout.Label("Result: " + status);
        GUILayout.Label("Frames: " + (bridge?.FrameCount() ?? 0));
        GUILayout.EndArea();
    }

    void OnDestroy() => bridge?.Dispose();
}
